package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.SoundPhysics;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Overrides SPR's per-environment reverb send gains with per-source
 * distance-scaled values. SPR controls reverb CHARACTER (RT60, density);
 * this controls how much each source sends to reverb based on distance
 * relative to critical distance.
 *
 * Each tracked source gets its own set of 4 lowpass filters (one per aux send)
 * to avoid cross-contamination when multiple sources are processed per tick.
 *
 * Smoothed with 100ms time constant, max 3dB change per tick (spec 2.4.1).
 * Called AFTER SPR's setEnvironment() on each sound play.
 */
public final class PerSourceDRProcessor {

    /** Max gain change per tick in linear units (3dB ≈ 1.413x). */
    private static final float MAX_DELTA_PER_TICK = 1.413f;
    /** Exponential smooth factor (~100ms at 20 TPS = 2 ticks). */
    private static final float SMOOTH_FACTOR = 0.5f;
    /** Cleanup: remove tracking for sources not seen in this many ticks. */
    private static final int STALE_TICKS = 100;
    /** Max tracked sources — prevents unbounded filter allocation. */
    private static final int MAX_TRACKED_SOURCES = 64;

    private boolean enabled = true;
    private boolean initialized = false;

    private final Map<Integer, SourceDRState> sourceStates = new HashMap<>();

    private static class SourceDRState {
        float currentMultiplier = 0.5f;
        long lastSeenTick;
        final int[] filterIds = new int[4];

        SourceDRState() {
            for (int i = 0; i < 4; i++) {
                filterIds[i] = EXTEfx.alGenFilters();
                EXTEfx.alFilteri(filterIds[i], EXTEfx.AL_FILTER_TYPE,
                        EXTEfx.AL_FILTER_LOWPASS);
                EXTEfx.alFilterf(filterIds[i], EXTEfx.AL_LOWPASS_GAIN, 1.0f);
                EXTEfx.alFilterf(filterIds[i], EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
            }
        }

        void deleteFilters() {
            for (int fid : filterIds) {
                if (fid != 0) EXTEfx.alDeleteFilters(fid);
            }
        }
    }

    public void init() {
        initialized = true;
        Loggers.log("PerSourceDRProcessor: initialized (per-source filter allocation)");
    }

    /**
     * Smooths a multiplier toward a target, clamping per-tick change to 3dB.
     */
    public static float smoothMultiplier(float current, float target) {
        float smoothed = current + (target - current) * SMOOTH_FACTOR;
        if (current > 0.001f) {
            float ratio = smoothed / current;
            if (ratio > MAX_DELTA_PER_TICK) {
                smoothed = current * MAX_DELTA_PER_TICK;
            } else if (ratio < 1.0f / MAX_DELTA_PER_TICK) {
                smoothed = current / MAX_DELTA_PER_TICK;
            }
        }
        return Math.max(0f, Math.min(1f, smoothed));
    }

    /**
     * Overrides reverb send gains for a game sound source.
     * Each source gets its own set of 4 OpenAL filters to avoid cross-contamination.
     */
    public void applyDR(int sourceId, float sourceX, float sourceY, float sourceZ,
                        float listenerX, float listenerY, float listenerZ,
                        float criticalDist, long currentTick) {
        if (!enabled || !initialized) return;

        int maxSends = SoundPhysics.getMaxAuxSends();
        if (maxSends < 1) return;

        float dx = sourceX - listenerX;
        float dy = sourceY - listenerY;
        float dz = sourceZ - listenerZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float targetMult = RoomGeometry.reverbSendMultiplier(distance, criticalDist);

        // Cap tracked sources to prevent unbounded filter allocation
        if (!sourceStates.containsKey(sourceId) && sourceStates.size() >= MAX_TRACKED_SOURCES) {
            return;
        }
        SourceDRState state = sourceStates.computeIfAbsent(sourceId, k -> new SourceDRState());
        state.currentMultiplier = smoothMultiplier(state.currentMultiplier, targetMult);
        state.lastSeenTick = currentTick;

        float multiplier = state.currentMultiplier;

        for (int i = 0; i < Math.min(4, maxSends); i++) {
            int auxSlot = SoundPhysics.getAuxFXSlot(i);
            if (auxSlot == 0) continue;

            EXTEfx.alFilterf(state.filterIds[i], EXTEfx.AL_LOWPASS_GAIN, multiplier);
            EXTEfx.alFilterf(state.filterIds[i], EXTEfx.AL_LOWPASS_GAINHF, 1.0f);

            AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER,
                    auxSlot, i, state.filterIds[i]);
        }
    }

    public void cleanupStale(long currentTick) {
        Iterator<Map.Entry<Integer, SourceDRState>> it = sourceStates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SourceDRState> entry = it.next();
            if (currentTick - entry.getValue().lastSeenTick > STALE_TICKS) {
                entry.getValue().deleteFilters();
                it.remove();
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        for (SourceDRState state : sourceStates.values()) {
            state.deleteFilters();
        }
        sourceStates.clear();
        initialized = false;
    }
}
