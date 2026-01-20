package com.sonicether.soundphysics.eap.emitter;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

/**
 * Mutable state for a single positioned environmental emitter.
 * Owns one OpenAL source (allocated from EmitterPool) and one lowpass filter.
 */
public final class Emitter {

    // Identity
    public final EmitterCategory category;
    public final int blockX, blockY, blockZ;

    // OpenAL handles (0 = not allocated)
    int sourceId;
    int filterId;

    // Current state (smoothed toward targets each tick)
    public float currentGain;
    float targetGain;
    float currentFilterGainHF = 1.0f;
    float targetFilterGainHF = 1.0f;

    // Lifecycle
    boolean active;          // currently has an OpenAL source
    long lastActiveTick;    // for fade-out timing
    float fadeProgress = 1.0f; // 0=fading out, 1=fully on

    // Distance from listener (updated each tick; MAX_VALUE until first cull pass)
    float distanceToListener = Float.MAX_VALUE;

    // Triggered emitters and fauna behavior
    float pitch = 1.0f;           // OpenAL AL_PITCH value
    float eagerness = 0f;         // Frog chorus: Felix Hess eagerness integrator
    long lastCallTick = 0;        // When this emitter last played a sound
    int speciesVariant = 0;       // Position-derived variant index (0-7)

    // Sample-based audio: subcategory and no-repeat history
    SampleSubcategory subcategory;
    final int[] sampleHistory = new int[]{-1, -1, -1};

    public Emitter(EmitterCategory category, int blockX, int blockY, int blockZ) {
        this.category = category;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.speciesVariant = ((blockX * 73 + blockZ * 37 + blockY * 11) >>> 4) & 7; // 0-7
    }

    /**
     * Returns the priority score for pool allocation.
     * Higher = more important to keep alive.
     * Combines category priority, distance (closer = higher), and facing direction.
     */
    public float priorityScore() {
        if (distanceToListener <= 0.01f) return category.priority * 100f;
        return category.priority / distanceToListener;
    }

    /**
     * Applies current gain and position to the OpenAL source.
     */
    public void applyToOpenAL(float masterGain) {
        if (sourceId == 0) return;

        float appliedGain = currentGain * masterGain * fadeProgress;
        AL10.alSourcef(sourceId, AL10.AL_GAIN, appliedGain);
        AL10.alSourcef(sourceId, AL10.AL_PITCH, pitch);
        // Position at block center
        AL10.alSource3f(sourceId, AL10.AL_POSITION,
                blockX + 0.5f, blockY + 0.5f, blockZ + 0.5f);

        if (filterId != 0) {
            EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAINHF, currentFilterGainHF);
            AL11.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, filterId);
        }
    }

    /**
     * Smooth interpolation toward targets. Call once per tick.
     */
    public void smoothStep(float factor) {
        currentGain += (targetGain - currentGain) * factor;
        currentFilterGainHF += (targetFilterGainHF - currentFilterGainHF) * factor;
        if (currentGain < 0.001f) currentGain = 0f;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * blockX + blockY) + blockZ) + category.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Emitter e)) return false;
        return blockX == e.blockX && blockY == e.blockY && blockZ == e.blockZ
                && category == e.category;
    }
}
