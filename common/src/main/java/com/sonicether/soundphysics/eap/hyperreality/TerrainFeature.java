package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;

public record TerrainFeature(
        TerrainFeatureType type,
        float x, float y, float z,
        float nx, float ny, float nz,
        float magnitude,
        float saliency,
        SpectralCategory material
) {

    public TerrainFeatureType.Family family() {
        return type.family();
    }

    public float distanceTo(float px, float py, float pz) {
        float dx = x - px;
        float dy = y - py;
        float dz = z - pz;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float computeSaliency(
            float magnitude, float maxMagnitude,
            float distance, float maxRange,
            float nx, float ny, float nz,
            float dirToPlayerX, float dirToPlayerY, float dirToPlayerZ,
            float noveltyBoost
    ) {
        if (maxMagnitude <= 0f || maxRange <= 0f) return 0f;

        float magnitudeScore = (float) (Math.log(magnitude + 1.0) / Math.log(maxMagnitude + 1.0));
        float proximityScore = Math.max(0f, 1.0f - (distance / maxRange));
        float dot = nx * dirToPlayerX + ny * dirToPlayerY + nz * dirToPlayerZ;
        float facingScore = 0.6f + 0.4f * Math.max(0f, dot);

        return magnitudeScore * proximityScore * facingScore * noveltyBoost;
    }

    public static float computeDetectionSaliency(float magnitude, float maxMagnitude,
            float featureX, float featureY, float featureZ,
            float playerY, float scanRadius) {
        float dy = featureY - playerY;
        float roughDistance = (float) Math.sqrt(dy * dy);
        if (maxMagnitude <= 0f || scanRadius <= 0f) return 0f;
        float magnitudeScore = (float) (Math.log(magnitude + 1.0) / Math.log(maxMagnitude + 1.0));
        float proximityScore = Math.max(0f, 1.0f - (roughDistance / scanRadius));
        return magnitudeScore * proximityScore;
    }
}
