package com.sonicether.soundphysics.eap.spatial;

import net.minecraft.world.phys.Vec3;

/**
 * Immutable representation of a detected surface cluster.
 * A cluster groups nearby reflections from a similar surface into
 * a single acoustic entity — "the left wall 3 blocks away."
 */
public record SurfaceCluster(
        Vec3 centroid,
        Vec3 normal,
        float averageDistance,
        float totalEnergy,
        float reflectivity,
        int tapCount,
        float spectralLow,
        float spectralMid,
        float spectralHigh
) {
    public float computeGain(float intensityScale) {
        if (averageDistance < 0.5f) return 0f;
        float distFactor = 1.0f / (averageDistance * averageDistance);
        return totalEnergy * reflectivity * distFactor * intensityScale * 0.1f;
    }
}
