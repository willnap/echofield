package com.sonicether.soundphysics.eap;

/**
 * Mutable state for a single environmental excitation slot.
 * Each instance owns one OpenAL source and one lowpass filter.
 * Current and target values are interpolated per tick via exponential smoothing.
 */
public final class ExcitationSource {

    final ExcitationType type;

    // OpenAL resource handles (set once at construction, freed at shutdown)
    int sourceId;
    int filterId;
<<<<<<< ours
    int[] sendFilterIds = new int[4]; // per-source aux send filters (avoids shared filter conflicts)
=======
>>>>>>> theirs

    // Current interpolated values (applied to OpenAL each tick)
    float currentGain;
    float currentX, currentY, currentZ;
    float currentFilterGain;
    float currentFilterGainHF;

    // Target values (recomputed each profile update)
    float targetGain;
    float targetX, targetY, targetZ;
    float targetFilterGain;
    float targetFilterGainHF;

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
    // Amplitude modulation for transient character (foliage rustle)
    float modulationDepth;
    float modulationPhase;

<<<<<<< ours
=======
>>>>>>> theirs
=======
>>>>>>> theirs
    ExcitationSource(ExcitationType type) {
        this.type = type;
    }
}
