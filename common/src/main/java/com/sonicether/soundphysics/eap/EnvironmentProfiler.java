package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.config.blocksound.BlockSoundConfigBase;
import com.sonicether.soundphysics.world.ClonedClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central EAP engine that runs every client tick, incrementally casts rays using
 * golden spiral distribution, traces multi-bounce reflections, builds immutable
 * EnvironmentProfiles, and crossfades between cycles.
 */
public class EnvironmentProfiler {

    // --- Constants ---

    private static final float PHI = 1.618033988F;
    private static final float GOLDEN_ANGLE = PHI * (float) Math.PI * 2F;
    private static final float MAX_DISTANCE = 256F;
    private static final int MAX_BOUNCES = 3;
    private static final double SPEED_OF_SOUND = 343.0;
    static final int CROSSFADE_TICKS = 25; // 1250ms at 20 ticks/sec — package-private for testing
    private static final int SNAPSHOT_CHUNK_RADIUS = 16;

    // --- Published state (read by audio threads) ---

    private final AtomicReference<EnvironmentProfile> currentProfile;
    private EnvironmentProfile previousProfile;
    private long crossfadeStartTick = -1L;

    // --- Cycle accumulation ---

    private List<ReflectionTap> accumulatedTaps;
    private Vec3[] allDirections;
    private boolean[] returned;
    private int currentRayIndex;

    // --- Configuration ---

    private int totalRays;
    private int raysPerTick;
    private final BlockSoundConfigBase reflectivityConfig;

    // --- Deferred config ---

    private int pendingTotalRays;
    private int pendingRaysPerTick;
    private boolean hasPendingConfig = false;

    // --- World snapshot ---

    private ClonedClientLevel levelSnapshot;

    // --- Constructor ---

    public EnvironmentProfiler(BlockSoundConfigBase reflectivityConfig, int totalRays, int raysPerTick) {
        this.reflectivityConfig = reflectivityConfig;
        this.totalRays = totalRays;
        this.raysPerTick = raysPerTick;
        this.currentProfile = new AtomicReference<>(EnvironmentProfile.OPEN);
        this.previousProfile = null;
        resetCycleState();
    }

    // --- tick ---

    /**
     * Called every client tick from the main client thread.
     * Fires {@code raysPerTick} rays per call using golden spiral distribution,
     * traces multi-bounce reflections, and completes cycles to build profiles.
     */
    public void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }

        // At cycle start, take a fresh level snapshot
        if (currentRayIndex == 0) {
            BlockPos playerBlockPos = minecraft.player.blockPosition();
            long tick = level.getGameTime();
            levelSnapshot = new ClonedClientLevel(level, playerBlockPos, tick, SNAPSHOT_CHUNK_RADIUS);
        }

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();

        int raysThisTick = Math.min(raysPerTick, totalRays - currentRayIndex);

        for (int r = 0; r < raysThisTick; r++) {
            int i = currentRayIndex + r;

            // Golden spiral direction
            float fiN = (2F * i + 1F) / (2F * totalRays);
            float longitude = GOLDEN_ANGLE * i;
            float latitude = (float) Math.asin(fiN * 2F - 1F);

            float cosLat = (float) Math.cos(latitude);
            float dirX = cosLat * (float) Math.cos(longitude);
            float dirY = cosLat * (float) Math.sin(longitude);
            float dirZ = (float) Math.sin(latitude);

            Vec3 rayDir = new Vec3(dirX, dirY, dirZ);
            allDirections[i] = rayDir;

            Vec3 rayEnd = cameraPos.add(rayDir.scale(MAX_DISTANCE));

            // Primary ray cast
            BlockHitResult primaryHit = EapRaycastUtils.rayCast(
                    levelSnapshot, cameraPos, rayEnd, BlockPos.containing(cameraPos));

            if (primaryHit.getType() != HitResult.Type.BLOCK) {
                // Ray escaped — no return
                returned[i] = false;
                continue;
            }

            // Ray hit something
            returned[i] = true;

            // Trace multi-bounce reflections
            Vec3 currentDir = rayDir;
            BlockHitResult currentHit = primaryHit;
            Vec3 hitPos = primaryHit.getLocation();
            BlockPos lastHitBlock = primaryHit.getBlockPos();
            double accumulatedDistance = cameraPos.distanceTo(hitPos);
            float currentEnergy = 1.0F;

            for (int bounce = 0; bounce < MAX_BOUNCES; bounce++) {
                // Get material at the hit block
                net.minecraft.world.level.block.state.BlockState hitBlockState =
                        levelSnapshot.getBlockState(lastHitBlock);

                // Get reflectivity and clamp to [0,1]
                float rawReflectivity = reflectivityConfig.getBlockDefinitionValue(hitBlockState);
                float reflectivity = Math.max(0F, Math.min(1F, rawReflectivity));

                // Attenuate energy
                currentEnergy *= reflectivity;

                // Create reflection tap (order is 1-indexed)
                ReflectionTap tap = ReflectionTap.of(
                        hitPos,
                        accumulatedDistance,
                        currentEnergy,
                        hitBlockState,
                        rayDir, // original ray direction for directional analysis
                        bounce + 1
                );
                accumulatedTaps.add(tap);

                // Reflect direction: newDir = dir - 2 * dot(dir, normal) * normal
                Vec3 normal = new Vec3(currentHit.getDirection().step());
                double dot = currentDir.dot(normal) * 2.0;
                Vec3 newDir = new Vec3(
                        currentDir.x - dot * normal.x,
                        currentDir.y - dot * normal.y,
                        currentDir.z - dot * normal.z
                );

                // Cast the reflected ray
                Vec3 bounceEnd = hitPos.add(newDir.scale(MAX_DISTANCE));
                BlockHitResult bounceHit = EapRaycastUtils.rayCast(
                        levelSnapshot, hitPos, bounceEnd, lastHitBlock);

                if (bounceHit.getType() != HitResult.Type.BLOCK) {
                    // Ray escaped after this bounce
                    break;
                }

                // Prepare for next bounce
                double segmentDistance = hitPos.distanceTo(bounceHit.getLocation());
                accumulatedDistance += segmentDistance;
                currentDir = newDir;
                hitPos = bounceHit.getLocation();
                lastHitBlock = bounceHit.getBlockPos();
                currentHit = bounceHit;
            }
        }

        currentRayIndex += raysThisTick;

        // Check if cycle is complete
        if (currentRayIndex >= totalRays) {
            long currentTick = level.getGameTime();
            completeCycle(currentTick);
        }
    }

    // --- completeCycle ---

    private void completeCycle(long currentTick) {
        // Build the new profile from accumulated taps
        EnvironmentProfile newProfile = EnvironmentProfile.fromTaps(
                accumulatedTaps, totalRays, allDirections, returned, reflectivityConfig);

        // Save previous profile for crossfade
        previousProfile = currentProfile.get();
        currentProfile.set(newProfile);
        crossfadeStartTick = currentTick;

        // Apply pending config if any
        if (hasPendingConfig) {
            totalRays = pendingTotalRays;
            raysPerTick = pendingRaysPerTick;
            hasPendingConfig = false;
        }

        // Reset cycle state for next cycle
        resetCycleState();
    }

    // --- getCurrentProfile (with crossfade) ---

    /**
     * Returns the current environment profile with crossfade interpolation applied.
     * During a crossfade, linearly interpolates between the previous and latest profiles.
     *
     * @param currentTick the current game tick
     * @return the interpolated profile
     */
    public EnvironmentProfile getCurrentProfile(long currentTick) {
        EnvironmentProfile latest = currentProfile.get();

        if (crossfadeStartTick < 0) {
            // No crossfade active
            return latest;
        }

        long elapsed = currentTick - crossfadeStartTick;

        if (elapsed >= CROSSFADE_TICKS) {
            // Crossfade complete
            previousProfile = null;
            crossfadeStartTick = -1L;
            return latest;
        }

        float t = (float) elapsed / (float) CROSSFADE_TICKS;

        // First cycle with no previous: fade from OPEN
        EnvironmentProfile from = previousProfile != null ? previousProfile : EnvironmentProfile.OPEN;
        return from.lerp(latest, t);
    }

    /**
     * Convenience overload that reads the current game time from the Minecraft instance.
     */
    public EnvironmentProfile getCurrentProfile() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return currentProfile.get();
        }
        return getCurrentProfile(mc.level.getGameTime());
    }

    // --- getLatestProfile ---

    /**
     * Returns the raw latest profile without crossfade interpolation.
     * Useful for tap positions in EarlyReflectionProcessor.
     */
    public EnvironmentProfile getLatestProfile() {
        return currentProfile.get();
    }

    // --- invalidate ---

    /**
     * Invalidates the current profile (e.g., on dimension change or teleportation).
     * Starts a crossfade from the current profile to OPEN, then resets the cycle.
     */
    public void invalidate(long currentTick) {
        previousProfile = currentProfile.get();
        currentProfile.set(EnvironmentProfile.OPEN);
        crossfadeStartTick = currentTick;
        resetCycleState();
        levelSnapshot = null;
    }

    /**
     * Convenience overload that reads the current game time from the Minecraft instance.
     */
    public void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            invalidate(mc.level.getGameTime());
        } else {
            invalidate(0L);
        }
    }

    // --- updateConfig ---

    /**
     * Updates ray configuration. If no cycle is in progress, applies immediately.
     * Otherwise, defers the change to the end of the current cycle.
     */
    public void updateConfig(int totalRays, int raysPerTick) {
        if (currentRayIndex == 0) {
            // No cycle in progress, apply immediately
            this.totalRays = totalRays;
            this.raysPerTick = raysPerTick;
            resetCycleState();
        } else {
            // Defer to end of current cycle
            pendingTotalRays = totalRays;
            pendingRaysPerTick = raysPerTick;
            hasPendingConfig = true;
        }
    }

    // --- Debug queries ---

    /**
     * Returns true if a crossfade is currently in progress.
     */
    public boolean isCrossfading(long currentTick) {
        if (crossfadeStartTick < 0) {
            return false;
        }
        return (currentTick - crossfadeStartTick) < CROSSFADE_TICKS;
    }

    /**
     * Returns cycle progress as a fraction from 0.0 to 1.0.
     */
    public float getCycleProgress() {
        return (float) currentRayIndex / (float) totalRays;
    }

    /**
     * Returns the total number of rays per cycle.
     */
    public int getTotalRays() {
        return totalRays;
    }

    /**
     * Returns the number of rays cast per tick.
     */
    public int getRaysPerTick() {
        return raysPerTick;
    }

    // --- Internal ---

    private void resetCycleState() {
        accumulatedTaps = new ArrayList<>();
        allDirections = new Vec3[totalRays];
        returned = new boolean[totalRays];
        currentRayIndex = 0;
    }
}
