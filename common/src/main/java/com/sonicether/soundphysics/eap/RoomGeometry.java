package com.sonicether.soundphysics.eap;

public final class RoomGeometry {

    private RoomGeometry() {}

    public static float estimateVolume(float enclosure, float avgReturnDist) {
        if (enclosure < 0.01f) {
            return 100_000f;
        }
        float side = 2.0f * avgReturnDist;
        float rawVolume = side * side * side;
        return rawVolume / enclosure;
    }

    public static float criticalDistance(float volumeM3, float rt60) {
        float clampedRT60 = Math.max(0.05f, rt60);
        return 0.057f * (float) Math.sqrt(volumeM3 / clampedRT60);
    }

    public static float reverbSendMultiplier(float sourceDistance, float criticalDist) {
        if (criticalDist <= 0.01f) return 1.0f;
        float ratio = sourceDistance / criticalDist;
        float clamped = Math.min(ratio, 1.0f);
        return (float) Math.sqrt(clamped);
    }
}
