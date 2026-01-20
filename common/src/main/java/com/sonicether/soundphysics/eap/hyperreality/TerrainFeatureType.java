package com.sonicether.soundphysics.eap.hyperreality;

public enum TerrainFeatureType {

    EDGE(Family.VOID, 24f, 5),
    DROP(Family.VOID, 24f, 5),
    PASSAGE(Family.VOID, 24f, 4),

    WALL(Family.SURFACE, 24f, 3),
    CEILING(Family.SURFACE, 24f, 2),
    SOLID_OBJECT(Family.SURFACE, 24f, 3),

    STEP(Family.GROUND, 12f, 2);

    private final Family family;
    private final float maxRange;
    private final int priority;

    TerrainFeatureType(Family family, float maxRange, int priority) {
        this.family = family;
        this.maxRange = maxRange;
        this.priority = priority;
    }

    public Family family() { return family; }
    public float maxRange() { return maxRange; }
    public int priority() { return priority; }
    public float familyWeight() { return family.priorityWeight(); }

    public enum Family {
        VOID(16, 0.08f, 1.5f),
        SURFACE(20, 0.06f, 1.0f),
        GROUND(12, 0.03f, 0.7f);

        private final int densityCap;
        private final float baseGain;
        private final float priorityWeight;

        Family(int densityCap, float baseGain, float priorityWeight) {
            this.densityCap = densityCap;
            this.baseGain = baseGain;
            this.priorityWeight = priorityWeight;
        }

        public int densityCap() { return densityCap; }
        public float baseGain() { return baseGain; }
        public float priorityWeight() { return priorityWeight; }
    }
}
