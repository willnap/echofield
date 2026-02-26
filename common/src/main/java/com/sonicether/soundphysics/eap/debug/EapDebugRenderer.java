package com.sonicether.soundphysics.eap.debug;

import com.sonicether.soundphysics.SoundPhysicsMod;
<<<<<<< ours
<<<<<<< ours
import com.sonicether.soundphysics.eap.EapSystem;
import com.sonicether.soundphysics.eap.EarlyReflectionProcessor;
<<<<<<< ours
=======
import com.sonicether.soundphysics.eap.EapSystem;
>>>>>>> theirs
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import com.sonicether.soundphysics.eap.ExcitationSourceManager;
import com.sonicether.soundphysics.eap.ReflectionTap;
import com.sonicether.soundphysics.eap.emitter.Emitter;
import com.sonicether.soundphysics.eap.emitter.EmitterCategory;
import com.sonicether.soundphysics.eap.emitter.EmitterManager;
import com.sonicether.soundphysics.eap.emitter.EnvironmentConditions;
import com.sonicether.soundphysics.eap.hyperreality.HyperrealityDebugRenderer;
import com.sonicether.soundphysics.eap.hyperreality.HyperrealityPool;
import com.sonicether.soundphysics.eap.hyperreality.HyperrealitySource;
import com.sonicether.soundphysics.eap.hyperreality.HyperrealitySystem;
import com.sonicether.soundphysics.eap.hyperreality.TerrainFeature;
import com.sonicether.soundphysics.eap.hyperreality.TerrainFeatureType;
=======
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import com.sonicether.soundphysics.eap.ExcitationSourceManager;
import com.sonicether.soundphysics.eap.ReflectionTap;
import com.sonicether.soundphysics.eap.emitter.Emitter;
import com.sonicether.soundphysics.eap.emitter.EmitterCategory;
import com.sonicether.soundphysics.eap.emitter.EmitterManager;
import com.sonicether.soundphysics.eap.spatial.SpatialFieldProcessor;
<<<<<<< ours
>>>>>>> theirs
=======
import com.sonicether.soundphysics.eap.spatial.SurfaceCluster;
>>>>>>> theirs
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
<<<<<<< ours
=======
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import com.sonicether.soundphysics.eap.ExcitationSourceManager;
import com.sonicether.soundphysics.eap.ReflectionTap;
import net.minecraft.client.Minecraft;
>>>>>>> theirs
=======
>>>>>>> theirs
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Debug renderer for the EAP system. Renders:
 * <ul>
 *   <li>Excitation source positions as colored markers</li>
 *   <li>Reflection tap positions from the current environment profile</li>
 * </ul>
 *
 * <p>Uses the Gizmos API, matching the pattern established by {@link com.sonicether.soundphysics.debug.RaycastRenderer}.
 */
public class EapDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

    private static final Minecraft mc = Minecraft.getInstance();

    // Colors (ARGB format with alpha=0xFF)
    private static final int COLOR_WIND = 0xFF00BFFF;     // Deep sky blue
    private static final int COLOR_FOLIAGE = 0xFF228B22;   // Forest green
    private static final int COLOR_GRASS = 0xFF7CFC00;     // Lawn green
    private static final int COLOR_WATER = 0xFF1E90FF;     // Dodger blue
    private static final int COLOR_LAVA = 0xFFFF4500;      // Orange red
    private static final int COLOR_TAP = 0xFFFFD700;       // Gold
    private static final int COLOR_TAP_DIM = 0xFFB8860B;   // Dark goldenrod

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
    // Emitter category colors
    private static final int COLOR_EMITTER_WIND = 0xFF87CEEB;    // Sky blue
    private static final int COLOR_EMITTER_WATER = 0xFF4169E1;   // Royal blue
    private static final int COLOR_EMITTER_LAVA = 0xFFFF6347;    // Tomato
    private static final int COLOR_EMITTER_FAUNA = 0xFF90EE90;   // Light green
    private static final int COLOR_EMITTER_CAVE = 0xFF9370DB;    // Medium purple
    private static final int COLOR_EMITTER_MECH = 0xFFCD853F;    // Peru
<<<<<<< ours

    private volatile ExcitationSourceManager excitationRef;
    private volatile EnvironmentProfile profileRef;
    private volatile EmitterManager emitterManagerRef;
    private volatile HyperrealitySystem hyperrealityRef;
    private volatile EnvironmentConditions conditionsRef;
=======
=======
    private static final int COLOR_SPATIAL = 0xFFFF69B4;         // Hot pink

>>>>>>> theirs
    private volatile ExcitationSourceManager excitationRef;
    private volatile EnvironmentProfile profileRef;
<<<<<<< ours
>>>>>>> theirs
=======
    private volatile EmitterManager emitterManagerRef;
    private volatile SpatialFieldProcessor spatialFieldRef;
>>>>>>> theirs

    public void setExcitationManager(ExcitationSourceManager manager) {
        this.excitationRef = manager;
    }

    public void setCurrentProfile(EnvironmentProfile profile) {
        this.profileRef = profile;
    }

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
    public void setEmitterManager(EmitterManager manager) {
        this.emitterManagerRef = manager;
    }

<<<<<<< ours
    public void setHyperrealitySystem(HyperrealitySystem system) {
        this.hyperrealityRef = system;
    }

    public void setConditions(EnvironmentConditions conditions) {
        this.conditionsRef = conditions;
    }

=======
>>>>>>> theirs
=======
    public void setSpatialField(SpatialFieldProcessor field) {
        this.spatialFieldRef = field;
    }

>>>>>>> theirs
    @Override
    public void emitGizmos(double camX, double camY, double camZ,
                           DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
        if (mc.level == null) {
            return;
        }

        if (SoundPhysicsMod.EAP_CONFIG == null || !SoundPhysicsMod.EAP_CONFIG.debugRays.get()) {
            return;
        }

        renderExcitationSources(camX, camY, camZ);
        renderReflectionTaps(camX, camY, camZ);
<<<<<<< ours
<<<<<<< ours
        renderEmitterMarkers(camX, camY, camZ);
        renderHyperrealityFeatures(camX, camY, camZ);
=======
>>>>>>> theirs
=======
        renderEmitterMarkers(camX, camY, camZ);
        renderSpatialFieldClusters(camX, camY, camZ);
>>>>>>> theirs
    }

    private void renderExcitationSources(double camX, double camY, double camZ) {
        ExcitationSourceManager excitation = this.excitationRef;
        if (excitation == null) {
            return;
        }

        List<ExcitationSourceManager.SourceInfo> activeInfo = excitation.getActiveSourceInfo();
        for (ExcitationSourceManager.SourceInfo info : activeInfo) {
            Vec3 sourcePos = new Vec3(info.posX(), info.posY(), info.posZ());

            // Skip sources too far from camera
            Vec3 cam = new Vec3(camX, camY, camZ);
            if (cam.distanceTo(sourcePos) > 64.0) {
                continue;
            }

            int color = getColorForType(info.type().ordinal());

            // Render a line from camera to source position
            Vec3 playerPos = mc.gameRenderer.getMainCamera().position();
            GizmoProperties line = Gizmos.line(playerPos, sourcePos, color);
            line.persistForMillis(100);
            line.fadeOut();
        }
    }

    private void renderReflectionTaps(double camX, double camY, double camZ) {
        EnvironmentProfile profile = this.profileRef;
        if (profile == null) {
            return;
        }

        List<ReflectionTap> taps = profile.taps();
        Vec3 cam = new Vec3(camX, camY, camZ);
        Vec3 playerPos = mc.gameRenderer.getMainCamera().position();

        for (ReflectionTap tap : taps) {
            Vec3 tapPos = tap.position();

            if (cam.distanceTo(tapPos) > 64.0) {
                continue;
            }

            // Color intensity based on energy
            int color = tap.energy() > 0.5 ? COLOR_TAP : COLOR_TAP_DIM;

            GizmoProperties line = Gizmos.line(playerPos, tapPos, color);
            line.persistForMillis(100);
            line.fadeOut();
        }
    }

    private static int getColorForType(int typeOrdinal) {
        return switch (typeOrdinal) {
            case 0 -> COLOR_WIND;
            case 1 -> COLOR_FOLIAGE;
            case 2 -> COLOR_GRASS;
            case 3 -> COLOR_WATER;
            case 4 -> COLOR_LAVA;
            default -> 0xFFFFFFFF;
        };
    }

<<<<<<< ours
<<<<<<< ours
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
    // ── Emitter markers ────────────────────────────────────────────────

    /**
     * Renders lines from player to active emitter positions.
     * Color by category.
     */
    private void renderEmitterMarkers(double camX, double camY, double camZ) {
        EmitterManager emitters = this.emitterManagerRef;
        if (emitters == null) return;

        Vec3 cam = new Vec3(camX, camY, camZ);
        Vec3 playerPos = mc.gameRenderer.getMainCamera().position();

        for (Emitter e : emitters.getPool().getActiveEmitters()) {
            if (e.currentGain < 0.001f) continue;

            Vec3 emitterPos = new Vec3(e.blockX + 0.5, e.blockY + 0.5, e.blockZ + 0.5);
            if (cam.distanceTo(emitterPos) > 48.0) continue;

            int color = getColorForEmitterCategory(e.category);
            GizmoProperties line = Gizmos.line(playerPos, emitterPos, color);
            line.persistForMillis(100);
            line.fadeOut();
        }
    }

<<<<<<< ours
    private void renderHyperrealityFeatures(double camX, double camY, double camZ) {
        HyperrealitySystem system = this.hyperrealityRef;
        if (system == null || system.getActiveCount() == 0) return;

        HyperrealityPool pool = system.getPool();
        if (pool == null) return;
=======
    // ── Spatial field clusters ────────────────────────────────────────

    /**
     * Renders lines from player to spatial field cluster centroids.
     * Color intensity proportional to cluster energy.
     */
    private void renderSpatialFieldClusters(double camX, double camY, double camZ) {
        SpatialFieldProcessor field = this.spatialFieldRef;
        EnvironmentProfile profile = this.profileRef;
        if (field == null || profile == null || field.getActiveCount() == 0) return;
>>>>>>> theirs

        Vec3 cam = new Vec3(camX, camY, camZ);
        Vec3 playerPos = mc.gameRenderer.getMainCamera().position();

<<<<<<< ours
        for (HyperrealitySource src : pool.getSources()) {
            if (!src.isActive() || src.getCurrentGain() < 0.001f) continue;

            Vec3 featurePos = new Vec3(src.getCurrentX(), src.getCurrentY(), src.getCurrentZ());
            if (cam.distanceTo(featurePos) > 64.0) continue;

            int baseColor = src.getType() != null
                    ? HyperrealityDebugRenderer.colorForType(src.getType())
                    : 0xFFFFFFFF;

            int alpha = (int) (Math.min(src.getCurrentGain() * 5f, 1f) * 255);
            int color = (alpha << 24) | (baseColor & 0x00FFFFFF);

            GizmoProperties line = Gizmos.line(playerPos, featurePos, color);
=======
        List<SurfaceCluster> clusters = com.sonicether.soundphysics.eap.spatial.ClusterDetector.detect(
                profile.taps(), playerPos);

        for (int i = 0; i < Math.min(clusters.size(), field.getSourceCount()); i++) {
            SurfaceCluster c = clusters.get(i);
            float gain = c.computeGain(0.5f);
            if (gain < 0.0001f) continue;
            if (cam.distanceTo(c.centroid()) > 64.0) continue;

            int alpha = (int) (Math.min(gain * 5f, 1f) * 255);
            int color = (alpha << 24) | (COLOR_SPATIAL & 0x00FFFFFF);

            GizmoProperties line = Gizmos.line(playerPos, c.centroid(), color);
>>>>>>> theirs
            line.persistForMillis(100);
            line.fadeOut();
        }
    }

<<<<<<< ours
    // ── Source budget ─────────────────────────────────────────────────

    public String getSourceBudgetText(EmitterManager emitters, HyperrealitySystem hyperreality,
=======
=======
>>>>>>> theirs
    // ── Source budget ─────────────────────────────────────────────────

    /**
     * Returns a formatted string showing the OpenAL source budget across all EAP subsystems.
     * Format: "Sources: excitation=5 refl=3/8 emit=12/64 spatial=4/8 total=24"
     *
     * @param emitters  the emitter manager (Layer 1)
     * @param spatial   the spatial field processor (Layer 3)
     * @param reflections the early reflection processor
     * @return formatted budget string
     */
    public String getSourceBudgetText(EmitterManager emitters, SpatialFieldProcessor spatial,
>>>>>>> theirs
                                       EarlyReflectionProcessor reflections) {
        ExcitationSourceManager excitation = this.excitationRef;
        int excitationCount = (excitation != null) ? excitation.getActiveSourceInfo().size() : 0;
        int reflActive = reflections.getActiveSlotCount();
        int reflTotal = EarlyReflectionProcessor.POOL_SIZE;
        int emitActive = emitters.getActiveCount();
        int emitTotal = emitters.getTotalTracked();
<<<<<<< ours
        int hyperActive = hyperreality.getActiveCount();
        int hyperTotal = hyperreality.getSourceCount();
        int totalActive = excitationCount + reflActive + emitActive + hyperActive;

        String breakdown = "";
        if (hyperActive > 0) {
            int[] counts = new int[TerrainFeatureType.values().length];
            HyperrealityPool pool = hyperreality.getPool();
            if (pool != null) {
                for (HyperrealitySource src : pool.getSources()) {
                    if (src.isActive() && src.getType() != null) {
                        counts[src.getType().ordinal()]++;
                    }
                }
            }
            breakdown = String.format(" [E:%d S:%d D:%d W:%d C:%d P:%d O:%d]",
                    counts[TerrainFeatureType.EDGE.ordinal()],
                    counts[TerrainFeatureType.STEP.ordinal()],
                    counts[TerrainFeatureType.DROP.ordinal()],
                    counts[TerrainFeatureType.WALL.ordinal()],
                    counts[TerrainFeatureType.CEILING.ordinal()],
                    counts[TerrainFeatureType.PASSAGE.ordinal()],
                    counts[TerrainFeatureType.SOLID_OBJECT.ordinal()]);
        }

        return String.format("Sources: excite=%d refl=%d/%d emit=%d/%d hyper=%d/%d%s total=%d",
                excitationCount, reflActive, reflTotal, emitActive, emitTotal,
                hyperActive, hyperTotal, breakdown, totalActive);
=======
        int spatialActive = spatial.getActiveCount();
        int spatialTotal = spatial.getSourceCount();
        int totalActive = excitationCount + reflActive + emitActive + spatialActive;

        return String.format("Sources: excite=%d refl=%d/%d emit=%d/%d spatial=%d/%d total=%d",
                excitationCount, reflActive, reflTotal, emitActive, emitTotal,
                spatialActive, spatialTotal, totalActive);
>>>>>>> theirs
    }

    /**
     * Returns an ARGB color for a given emitter category, suitable for debug visualization.
     *
     * @param category the emitter category
     * @return ARGB color int
     */
    public static int getColorForEmitterCategory(EmitterCategory category) {
        return switch (category) {
            case WIND_LEAF, WIND_GRASS, WIND_WHISTLE -> COLOR_EMITTER_WIND;
<<<<<<< ours
            case WATER_FLOW, WATER_DRIP, WATER_RAIN, WATER_STILL -> COLOR_EMITTER_WATER;
            case LAVA -> COLOR_EMITTER_LAVA;
            case BIRD, INSECT, FROG, BAT -> COLOR_EMITTER_FAUNA;
            case CAVE_AMBIENT, CAVE_DRONE -> COLOR_EMITTER_CAVE;
=======
            case WATER_FLOW, WATER_DRIP, WATER_RAIN -> COLOR_EMITTER_WATER;
            case LAVA -> COLOR_EMITTER_LAVA;
            case BIRD, INSECT, FROG, BAT -> COLOR_EMITTER_FAUNA;
            case CAVE_AMBIENT -> COLOR_EMITTER_CAVE;
>>>>>>> theirs
            case MECHANICAL -> COLOR_EMITTER_MECH;
        };
    }

<<<<<<< ours
    // ── Environment conditions text ──────────────────────────────────

    /**
     * Returns formatted text lines showing the current EnvironmentConditions,
     * or null if no conditions have been set.
     */
    public String getConditionsText() {
        EnvironmentConditions cond = this.conditionsRef;
        if (cond == null) return null;
        return String.format("Env: day=%.2f dawn=%.2f dusk=%.2f temp=%.2f wind=%.2f",
                        cond.daylight(), cond.dawnChorus(), cond.duskFactor(),
                        cond.temperature(), cond.windExposure())
                + String.format("\n     rain=%b forest=%b swamp=%b enc=%.2f room=%.1f",
                        cond.isRaining(), cond.isForest(), cond.isSwamp(),
                        cond.enclosure(), cond.roomSize());
    }

=======
>>>>>>> theirs
=======
>>>>>>> theirs
    // ── HUD overlay rendering ───────────────────────────────────────

    /**
     * Renders the EAP debug HUD overlay with text lines on screen.
     * Called from the Fabric HudRenderCallback.
     *
     * @param gui the GuiGraphics context for rendering
     */
    public void renderHudOverlay(GuiGraphics gui) {
        if (SoundPhysicsMod.EAP_CONFIG == null || !SoundPhysicsMod.EAP_CONFIG.debugOverlay.get()) {
            return;
        }
        EapSystem eap = EapSystem.getInstanceOrNull();
        if (eap == null) return;

        String hudText = eap.getDebugHudText();
        if (hudText == null) return;

        // Split into lines and render at (4, 4) with background
        String[] lines = hudText.split("\n");
        Font font = mc.font;
        int y = 4;
        for (String line : lines) {
            int width = font.width(line);
            gui.fill(2, y - 1, 4 + width + 2, y + font.lineHeight, 0x80000000);
            gui.drawString(font, line, 4, y, 0xFFFFFFFF);
            y += font.lineHeight + 1;
        }
    }

<<<<<<< ours
=======
>>>>>>> theirs
=======
>>>>>>> theirs
}
