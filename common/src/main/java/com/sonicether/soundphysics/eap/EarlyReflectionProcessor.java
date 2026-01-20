package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.SoundPhysicsMod;
import com.sonicether.soundphysics.eap.math.SpectralFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages an 8-source OpenAL pool for delayed, spectrally-filtered early reflections.
 *
 * <p>When a game sound is played, the processor receives the original source's buffer ID
 * and the current {@link EnvironmentProfile}. It sorts the profile's {@link ReflectionTap}s
 * by energy (descending) and assigns the top taps to pool slots. Each slot is positioned at
 * the tap's world-space location, gain-scaled by the tap's energy, and played after a delay
 * corresponding to the acoustic propagation time.
 *
 * <p>Delayed playback is implemented via tick-based scheduling rather than a separate thread,
 * because all OpenAL calls must be made from a thread with a current OpenAL context (LWJGL
 * binds ALCapabilities per thread). The tick() method checks pending plays each tick (~50ms
 * resolution), which is acceptable for environmental reflection delays.
 *
 * <p>A priority-stealing system allows high-energy taps to reclaim slots from weaker ones.
 * Per-tap spectral filtering is applied via OpenAL lowpass filters derived from the tap's
 * surface material absorption coefficients.
 */
public final class EarlyReflectionProcessor {

    /** Number of OpenAL sources in the reflection pool. */
    public static final int POOL_SIZE = 8;

    /** Conservative upper bound on reflection playback duration in ticks (2 seconds at 20 tps). */
    private static final int MAX_PLAYBACK_TICKS = 100;

    /** Minimum tap energy to allocate a pool slot. */
    private static final double ENERGY_THRESHOLD = 0.001;

    // ── Inner class ──────────────────────────────────────────────────

    /** Mutable state for a single reflection pool slot. */
    static final class ReflectionSlot {
        int sourceId;
        int filterId;
        volatile double energy;
        volatile long playbackEndTick;
        volatile boolean inUse;

        // Tick-based delayed playback: nanoTime at which alSourcePlay should be called.
        // -1 means no pending play (already playing or not configured).
        volatile long scheduledPlayNanos = -1;
    }

    // ── Fields ───────────────────────────────────────────────────────

    private final ReflectionSlot[] slots;

    private float reflectionIntensity = 1.0f;
    private float masterGain = 1.0f;
    private boolean muted = false;
    private volatile boolean initialized = false;

    // ── Construction (Task 15) ───────────────────────────────────────

    /**
     * Allocates {@value POOL_SIZE} OpenAL sources and lowpass filters.
     * Sources are non-looping with initial gain 0.
     */
    public EarlyReflectionProcessor() {
        slots = new ReflectionSlot[POOL_SIZE];

        for (int i = 0; i < POOL_SIZE; i++) {
            ReflectionSlot slot = new ReflectionSlot();

            slot.sourceId = AL10.alGenSources();
            Loggers.logALError("EarlyReflectionProcessor: alGenSources slot " + i);

            slot.filterId = EXTEfx.alGenFilters();
            EXTEfx.alFilteri(slot.filterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(slot.filterId, EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(slot.filterId, EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
            Loggers.logALError("EarlyReflectionProcessor: alGenFilters slot " + i);

            // Non-looping, gain=0, world-space positioning
            AL10.alSourcei(slot.sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcef(slot.sourceId, AL10.AL_GAIN, 0.0f);
            AL10.alSourcef(slot.sourceId, AL10.AL_ROLLOFF_FACTOR, 1.0f);
            AL10.alSourcef(slot.sourceId, AL10.AL_REFERENCE_DISTANCE, 4.0f);
            AL10.alSourcef(slot.sourceId, AL10.AL_MAX_DISTANCE, 48.0f);
            AL10.alSourcei(slot.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL11.alSourcei(slot.sourceId, EXTEfx.AL_DIRECT_FILTER, slot.filterId);
            Loggers.logALError("EarlyReflectionProcessor: source setup slot " + i);

            slot.energy = 0.0;
            slot.playbackEndTick = 0;
            slot.inUse = false;
            slots[i] = slot;
        }

        initialized = true;
        Loggers.log("EarlyReflectionProcessor: initialized {} reflection slots", POOL_SIZE);
    }

    // ── Shutdown (Task 15) ───────────────────────────────────────────

    /**
     * Stops all sources, unbinds buffers and filters, deletes OpenAL resources.
     */
    public void shutdown() {
        initialized = false;

        for (ReflectionSlot slot : slots) {
            slot.scheduledPlayNanos = -1;
            AL10.alSourceStop(slot.sourceId);
            AL10.alSourcei(slot.sourceId, AL10.AL_BUFFER, 0);
            AL11.alSourcei(slot.sourceId, EXTEfx.AL_DIRECT_FILTER, 0);
            AL10.alDeleteSources(slot.sourceId);
            EXTEfx.alDeleteFilters(slot.filterId);
            slot.inUse = false;
        }

        Loggers.log("EarlyReflectionProcessor: shut down");
    }

    // ── Slot lookup (Task 16) ────────────────────────────────────────

    /**
     * Returns the index of the first free slot, or -1 if all are in use.
     */
    int findAvailableSlot() {
        for (int i = 0; i < POOL_SIZE; i++) {
            if (!slots[i].inUse) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the in-use slot with the lowest energy, or -1 if none are in use.
     */
    int findWeakestSlot() {
        int weakest = -1;
        double weakestEnergy = Double.MAX_VALUE;
        for (int i = 0; i < POOL_SIZE; i++) {
            if (slots[i].inUse && slots[i].energy < weakestEnergy) {
                weakestEnergy = slots[i].energy;
                weakest = i;
            }
        }
        return weakest;
    }

    // ── Slot configuration (Task 16) ─────────────────────────────────

    /**
     * Configures a pool slot for delayed reflection playback.
     *
     * <p>Stops any current playback on the slot, binds the given buffer, positions the source
     * at the tap's world-space position, sets gain based on energy and master/intensity scaling,
     * applies spectral filtering from the tap's material, and schedules delayed playback via
     * tick-based timing (checked each tick in {@link #tick}).
     *
     * @param slotIdx        index into the pool
     * @param bufferId       OpenAL buffer to bind (shared from original sound source)
     * @param tap            the reflection tap with position, energy, delay, and material
     * @param reflectionGain per-reflection gain modifier
     * @param currentTick    current game tick for tracking playback end
     */
    void configureSlot(int slotIdx, int bufferId, ReflectionTap tap,
                       float reflectionGain, long currentTick) {
        ReflectionSlot slot = slots[slotIdx];

        // Cancel any pending delayed play
        slot.scheduledPlayNanos = -1;

        // Stop any current playback
        AL10.alSourceStop(slot.sourceId);

        // Unbind previous buffer before binding new one
        AL10.alSourcei(slot.sourceId, AL10.AL_BUFFER, 0);

        // Bind new buffer
        AL10.alSourcei(slot.sourceId, AL10.AL_BUFFER, bufferId);
        Loggers.logALError("EarlyReflectionProcessor: bind buffer slot " + slotIdx);

        // Position at tap world-space position
        Vec3 pos = tap.position();
        AL10.alSource3f(slot.sourceId, AL10.AL_POSITION,
                (float) pos.x, (float) pos.y, (float) pos.z);

        // Set gain = energy * reflectionGain * masterGain
        float gain = (float) tap.energy() * reflectionGain * masterGain;
        if (muted) {
            gain = 0.0f;
        }
        AL10.alSourcef(slot.sourceId, AL10.AL_GAIN, gain);

        // Apply spectral filter based on tap material
        applySpectralFilter(slot, tap);

        // Mark in-use: write playbackEndTick and energy BEFORE inUse (volatile publish)
        slot.energy = tap.energy();
        slot.playbackEndTick = currentTick + MAX_PLAYBACK_TICKS;
        slot.inUse = true; // volatile write — publishes energy and playbackEndTick

        // Schedule delayed playback via tick-based timing
        long delayNanos = (long) (tap.delay() * 1_000_000_000.0);
        if (delayNanos <= 0) {
            // No delay — play immediately
            AL10.alSourcePlay(slot.sourceId);
        } else {
            // Store the nanoTime at which this source should start playing.
            // tick() will call alSourcePlay when System.nanoTime() >= this value.
            slot.scheduledPlayNanos = System.nanoTime() + delayNanos;
        }
    }

    // ── Sound event handler (Task 16) ────────────────────────────────

    /**
     * Called when a game sound is played. Extracts the buffer from the original source,
     * sorts taps by energy (descending), and assigns the top {@value POOL_SIZE} taps to
     * pool slots using available slots or priority stealing.
     *
     * @param originalSourceId OpenAL source ID of the original game sound
     * @param soundPos         world-space position of the original sound
     * @param profile          current environment profile with reflection taps
     * @param currentTick      current game tick
     */
    public void onSoundPlay(int originalSourceId, Vec3 soundPos,
                            EnvironmentProfile profile, long currentTick) {
        if (!initialized || profile == null || muted) {
            return;
        }

        List<ReflectionTap> taps = profile.taps();
        if (taps.isEmpty()) {
            return;
        }

        // Extract buffer from original source
        int bufferId = AL10.alGetSourcei(originalSourceId, AL10.AL_BUFFER);
        Loggers.logALError("EarlyReflectionProcessor: get buffer from original source");
        if (bufferId <= 0) {
            return;
        }

        // Sort taps by energy descending
        List<ReflectionTap> sorted = new ArrayList<>(taps);
        sorted.sort(Comparator.comparingDouble(ReflectionTap::energy).reversed());

        // Precedence window: filter to 1-50ms
        sorted.removeIf(tap -> {
            double delayMs = tap.delay() * 1000.0;
            return delayMs < 1.0 || delayMs > 50.0;
        });

        // Re-sort with lateral fraction weighting for spatial clarity
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Vec3 fwd = mc.player.getLookAngle();
            Vec3 listenerPos = mc.player.position();
            sorted.sort((a, b) -> {
                double lateralA = lateralFraction(a.position(), listenerPos, fwd);
                double lateralB = lateralFraction(b.position(), listenerPos, fwd);
                double scoreA = a.energy() * 0.7 + lateralA * 0.3;
                double scoreB = b.energy() * 0.7 + lateralB * 0.3;
                return Double.compare(scoreB, scoreA);
            });
        }

        // Assign top POOL_SIZE taps to slots
        int assigned = 0;
        for (int i = 0; i < sorted.size() && assigned < POOL_SIZE; i++) {
            ReflectionTap tap = sorted.get(i);

            // Skip taps with negligible energy (sorted desc, so all remaining are also below)
            if (tap.energy() < ENERGY_THRESHOLD) {
                break;
            }

            int slotIdx = findAvailableSlot();
            if (slotIdx < 0) {
                slotIdx = tryStealSlot(tap);
            }
            if (slotIdx < 0) {
                break; // No slots available, even via stealing
            }

            configureSlot(slotIdx, bufferId, tap, reflectionIntensity, currentTick);
            assigned++;
        }

        Loggers.logDebug("EarlyReflectionProcessor: assigned {} reflections for source {}",
                assigned, originalSourceId);
    }

    // ── Priority stealing (Task 17) ─────────────────────────────────

    /**
     * Attempts to steal a slot for a new tap. If the weakest in-use slot has less energy
     * than the new tap, stops and reclaims it.
     *
     * @param newTap the tap requesting a slot
     * @return index of the stolen slot, or -1 if stealing was not justified
     */
    int tryStealSlot(ReflectionTap newTap) {
        int weakest = findWeakestSlot();
        if (weakest < 0) {
            return -1;
        }

        if (newTap.energy() > slots[weakest].energy) {
            // Stop the weakest slot and reclaim it
            double oldEnergy = slots[weakest].energy;
            slots[weakest].scheduledPlayNanos = -1; // Cancel pending play
            AL10.alSourceStop(slots[weakest].sourceId);
            AL10.alSourcei(slots[weakest].sourceId, AL10.AL_BUFFER, 0);
            slots[weakest].inUse = false;
            slots[weakest].energy = 0.0;
            Loggers.logDebug("EarlyReflectionProcessor: stole slot {} (energy {}) for new tap (energy {})",
                    weakest, oldEnergy, newTap.energy());
            return weakest;
        }

        return -1;
    }

    // ── Tick update (Task 17) ────────────────────────────────────────

    /**
     * Called once per game tick. Performs two duties:
     * <ol>
     *   <li>Fires any pending delayed plays whose scheduled time has elapsed</li>
     *   <li>Recycles slots whose playback has completed (STOPPED/INITIAL or expired)</li>
     * </ol>
     *
     * @param currentTick the current game tick
     */
    public void tick(long currentTick) {
        if (!initialized) {
            return;
        }

        long now = System.nanoTime();

        for (int i = 0; i < POOL_SIZE; i++) {
            ReflectionSlot slot = slots[i];
            if (!slot.inUse) {
                continue;
            }

            // Check for pending delayed plays
            long playAt = slot.scheduledPlayNanos;
            if (playAt >= 0 && now >= playAt) {
                AL10.alSourcePlay(slot.sourceId);
                slot.scheduledPlayNanos = -1;
            }

            // Skip recycling if we haven't even started playing yet
            if (playAt >= 0) {
                continue;
            }

            // Check if playback has finished
            int state = AL10.alGetSourcei(slot.sourceId, AL10.AL_SOURCE_STATE);
            boolean expired = currentTick >= slot.playbackEndTick;
            boolean stopped = (state == AL10.AL_STOPPED || state == AL10.AL_INITIAL);

            if (expired || stopped) {
                // Recycle the slot
                AL10.alSourceStop(slot.sourceId);
                AL10.alSourcei(slot.sourceId, AL10.AL_BUFFER, 0);
                AL10.alSourcef(slot.sourceId, AL10.AL_GAIN, 0.0f);
                slot.inUse = false;
                slot.energy = 0.0;
            }
        }
    }

    // ── Per-tap spectral filtering (Task 18) ─────────────────────────

    /**
     * Applies material-based spectral filtering to a reflection slot's lowpass filter.
     *
     * <p>Computes 3-band (low, mid, high) absorption from the tap's surface material
     * using {@link SpectralFilter#computeAbsorption}, then maps to OpenAL lowpass parameters:
     * <ul>
     *   <li>{@code AL_LOWPASS_GAIN} = 1.0 - mean(low, mid, high)
     *   <li>{@code AL_LOWPASS_GAINHF} = (1.0 - high) / (1.0 - low), clamped to [0,1]
     * </ul>
     *
     * @param slot the reflection slot whose filter to configure
     * @param tap  the reflection tap providing material information
     */
    private void applySpectralFilter(ReflectionSlot slot, ReflectionTap tap) {
        float[] absorption = SpectralFilter.computeAbsorption(
                tap.material(), SoundPhysicsMod.REFLECTIVITY_CONFIG);

        float low = absorption[0];
        float mid = absorption[1];
        float high = absorption[2];

        // Overall gain reduction: mean absorption across all bands
        float meanAbsorption = (low + mid + high) / 3.0f;
        float lpGain = Math.max(0.0f, Math.min(1.0f, 1.0f - meanAbsorption));

        // HF ratio: how much high-frequency content survives relative to low
        float lpGainHF;
        if (low >= 1.0f) {
            // Fully absorbed at low frequencies — clamp to 0
            lpGainHF = 0.0f;
        } else {
            lpGainHF = (1.0f - high) / (1.0f - low);
            lpGainHF = Math.max(0.0f, Math.min(1.0f, lpGainHF));
        }

        EXTEfx.alFilterf(slot.filterId, EXTEfx.AL_LOWPASS_GAIN, lpGain);
        EXTEfx.alFilterf(slot.filterId, EXTEfx.AL_LOWPASS_GAINHF, lpGainHF);
        AL11.alSourcei(slot.sourceId, EXTEfx.AL_DIRECT_FILTER, slot.filterId);
        Loggers.logALError("EarlyReflectionProcessor: applySpectralFilter");
    }

    // ── Lateral fraction helper ──────────────────────────────────────

    /**
     * Computes the lateral fraction of a reflection direction relative to
     * the listener's forward vector. Returns 0 for sounds directly ahead/behind,
     * 1 for sounds arriving from the side. Used to prefer lateral reflections
     * which improve spatial impression (per psychoacoustic precedence effect).
     */
    private static double lateralFraction(Vec3 reflectionPos, Vec3 listenerPos, Vec3 forward) {
        Vec3 toReflection = reflectionPos.subtract(listenerPos).normalize();
        Vec3 horizForward = new Vec3(forward.x, 0, forward.z).normalize();
        if (horizForward.lengthSqr() < 0.001) return 0.5;
        double dot = Math.abs(toReflection.x * horizForward.x + toReflection.z * horizForward.z);
        return 1.0 - dot;
    }

    // ── Setters (Task 16) ────────────────────────────────────────────

    public void setReflectionIntensity(float intensity) {
        this.reflectionIntensity = intensity;
    }

    public void setMasterGain(float gain) {
        this.masterGain = gain;
    }

    public void muteAll() {
        this.muted = true;
        for (ReflectionSlot slot : slots) {
            slot.scheduledPlayNanos = -1;
            AL10.alSourcef(slot.sourceId, AL10.AL_GAIN, 0.0f);
        }
    }

    public void unmuteAll() {
        this.muted = false;
    }

    // ── Diagnostics (Task 17) ────────────────────────────────────────

    public int[] getPoolSourceIds() {
        int[] ids = new int[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            ids[i] = slots[i].sourceId;
        }
        return ids;
    }

    public int getActiveSlotCount() {
        int count = 0;
        for (ReflectionSlot slot : slots) {
            if (slot.inUse) count++;
        }
        return count;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Diagnostic snapshot of a single reflection slot. */
    public record SlotSnapshot(
            int index, int sourceId, boolean inUse,
            double energy, long playbackEndTick, int alState
    ) {}

    public List<SlotSnapshot> getDiagnosticSnapshot() {
        List<SlotSnapshot> snapshots = new ArrayList<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            ReflectionSlot slot = slots[i];
            int alState = AL10.alGetSourcei(slot.sourceId, AL10.AL_SOURCE_STATE);
            snapshots.add(new SlotSnapshot(
                    i, slot.sourceId, slot.inUse,
                    slot.energy, slot.playbackEndTick, alState));
        }
        return snapshots;
    }

    // ── Spectral filter diagnostics (Task 18) ────────────────────────

    public void logSpectralFilterDiagnostics() {
        Loggers.logDebug("EarlyReflectionProcessor: spectral filter diagnostics ({} slots)", POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            ReflectionSlot slot = slots[i];
            if (!slot.inUse) {
                continue;
            }

            float lpGain = EXTEfx.alGetFilterf(slot.filterId, EXTEfx.AL_LOWPASS_GAIN);
            float lpGainHF = EXTEfx.alGetFilterf(slot.filterId, EXTEfx.AL_LOWPASS_GAINHF);
            int alState = AL10.alGetSourcei(slot.sourceId, AL10.AL_SOURCE_STATE);
            float srcGain = AL10.alGetSourcef(slot.sourceId, AL10.AL_GAIN);

            Loggers.logDebug("  Slot {}: energy={}, gain={}, lpGain={}, lpGainHF={}, alState={}",
                    i, slot.energy, srcGain, lpGain, lpGainHF, alState);
        }
    }
}
