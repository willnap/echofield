package com.sonicether.soundphysics.eap.hyperreality;

public final class HyperrealityDebugRenderer {

    public static final int COLOR_EDGE = 0xFFFF6600;
    public static final int COLOR_STEP = 0xFFFFCC00;
    public static final int COLOR_DROP = 0xFFFF0000;
    public static final int COLOR_WALL = 0xFF00AAFF;
    public static final int COLOR_CEILING = 0xFF9966FF;
    public static final int COLOR_PASSAGE = 0xFF00FF88;
    public static final int COLOR_SOLID_OBJECT = 0xFFBBBBBB;

    private HyperrealityDebugRenderer() {}

    public static int colorForType(TerrainFeatureType type) {
        return switch (type) {
            case EDGE -> COLOR_EDGE;
            case STEP -> COLOR_STEP;
            case DROP -> COLOR_DROP;
            case WALL -> COLOR_WALL;
            case CEILING -> COLOR_CEILING;
            case PASSAGE -> COLOR_PASSAGE;
            case SOLID_OBJECT -> COLOR_SOLID_OBJECT;
        };
    }
}
