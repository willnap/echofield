package com.sonicether.soundphysics.eap.emitter;

/**
 * Categories of environmental sound emitters.
 * Each category has a spectral character, priority, and audible range.
 */
public enum EmitterCategory {
<<<<<<< ours
    // Fauna — sample-based, triggered (HIGH priority — must steal from wind)
    BIRD(32.0f, 4, true),
    INSECT(28.0f, 3, true),
    FROG(24.0f, 4, true),
    BAT(20.0f, 2, true),

    // Wind-material — procedural continuous (LOW priority — abundant, expendable)
    WIND_LEAF(24.0f, 1, false),
    WIND_GRASS(20.0f, 1, false),
    WIND_WHISTLE(16.0f, 2, false),

    // Water — mix of procedural and sample
    WATER_FLOW(32.0f, 4, false),
    WATER_DRIP(24.0f, 3, true),
    WATER_RAIN(28.0f, 3, true),
    WATER_STILL(24.0f, 5, true),
=======
    // Fauna — sample-based, triggered
    BIRD(32.0f, 3, true),
    INSECT(16.0f, 1, true),
    FROG(24.0f, 2, true),
    BAT(20.0f, 2, true),

    // Wind-material — procedural continuous
    WIND_LEAF(24.0f, 4, false),
    WIND_GRASS(20.0f, 3, false),
    WIND_WHISTLE(16.0f, 5, false),

    // Water — mix of procedural and sample
    WATER_FLOW(32.0f, 5, false),
    WATER_DRIP(24.0f, 4, true),
    WATER_RAIN(28.0f, 3, true),
>>>>>>> theirs

    // Lava — procedural
    LAVA(40.0f, 5, false),

<<<<<<< ours
    // Cave — sample-based discrete events + continuous drone
    CAVE_AMBIENT(48.0f, 3, true),
    CAVE_DRONE(32.0f, 3, false),
=======
    // Cave — sample-based, sparse
    CAVE_AMBIENT(48.0f, 2, true),
>>>>>>> theirs

    // Mechanical — sample-based, triggered
    MECHANICAL(16.0f, 3, true);

    /** Maximum audible distance in blocks. Beyond this, emitter is culled. */
    public final float maxRange;

    /** Priority 1-5. Higher = harder to cull. */
    public final int priority;

    /** If true, plays a finite sample. If false, loops continuously. */
    public final boolean sampleBased;

    EmitterCategory(float maxRange, int priority, boolean sampleBased) {
        this.maxRange = maxRange;
        this.priority = priority;
        this.sampleBased = sampleBased;
    }
}
