package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;

/**
 * Mutable per-source state for one hyperreality pool slot.
 * One instance per slot; recycled across allocations via {@link #reset()}.
 */
public final class HyperrealitySource {

    static final float GAIN_SMOOTH = 0.12f;
    static final float POS_SMOOTH = 0.08f;
    static final float FILTER_SMOOTH = 0.10f;
    static final int FADE_DURATION = 4;

    TerrainFeatureType type;
    float featureX;
    float featureY;
    float featureZ;
    float featureNX;
    float featureNY;
    float featureNZ;
    SpectralCategory material;
    float saliency;
    float magnitude;

    int sourceId;
    int filterId;
    final int[] sendFilterIds = new int[4];

    float currentGain;
    float currentX;
    float currentY;
    float currentZ;
    float currentFilterHF;

    float targetGain;
    float targetX;
    float targetY;
    float targetZ;
    float targetFilterHF;

    boolean active;
    int fadeTicks;
    float distance;
    int cyclesSinceActivation;

    private final int slotIndex;

    public HyperrealitySource(int slotIndex) {
        this.slotIndex = slotIndex;
        this.currentFilterHF = 1.0f;
        this.targetFilterHF = 1.0f;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
    public boolean isActive() { return active; }
    public TerrainFeatureType getType() { return type; }
    public float getCurrentGain() { return currentGain; }
    public float getCurrentX() { return currentX; }
    public float getCurrentY() { return currentY; }
    public float getCurrentZ() { return currentZ; }

<<<<<<< ours
=======
>>>>>>> theirs
=======
>>>>>>> theirs
    public static float materialHFGain(SpectralCategory category) {
        return switch (category) {
            case HARD -> 0.85f;
            case WOOD -> 0.55f;
            case SOFT -> 0.30f;
            case FOLIAGE -> 0.35f;
            case DEFAULT -> 0.50f;
        };
    }

    public float computeNoveltyBoost() {
        return 1.0f + 0.5f * Math.max(0f, 1.0f - cyclesSinceActivation / 3.0f);
    }

    public float computePriority() {
        if (type == null) {
            return 0.0f;
        }
        return saliency * (1.0f / (distance + 1.0f)) * type.familyWeight();
    }

    public void smoothStep() {
        currentGain += (targetGain - currentGain) * GAIN_SMOOTH;
        currentX += (targetX - currentX) * POS_SMOOTH;
        currentY += (targetY - currentY) * POS_SMOOTH;
        currentZ += (targetZ - currentZ) * POS_SMOOTH;
        currentFilterHF += (targetFilterHF - currentFilterHF) * FILTER_SMOOTH;
    }

    public float computeFadeScale() {
        if (fadeTicks == 0) {
            return 1.0f;
        }
        int absTicks = Math.abs(fadeTicks);
        float progress = (float) (FADE_DURATION - absTicks + 1) / (float) FADE_DURATION;
        if (fadeTicks > 0) {
            return progress;
        } else {
            return 1.0f - progress;
        }
    }

    public boolean advanceFade() {
        if (fadeTicks > 0) {
            fadeTicks--;
        } else if (fadeTicks < 0) {
            fadeTicks++;
            if (fadeTicks == 0) {
                return true;
            }
        }
        return false;
    }

    public void applyToOpenAL(float fadeScale) {
        if (sourceId == 0) {
            return;
        }
        float effectiveGain = currentGain * fadeScale;
        org.lwjgl.openal.AL11.alSourcef(sourceId, org.lwjgl.openal.AL11.AL_GAIN, effectiveGain);
        org.lwjgl.openal.AL11.alSource3f(sourceId, org.lwjgl.openal.AL11.AL_POSITION,
                currentX, currentY, currentZ);
        if (filterId != 0) {
            org.lwjgl.openal.EXTEfx.alFilterf(filterId,
                    org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF, currentFilterHF);
            org.lwjgl.openal.AL11.alSourcei(sourceId,
                    org.lwjgl.openal.EXTEfx.AL_DIRECT_FILTER, filterId);
        }
    }

    public void reset() {
        type = null;
        featureX = 0.0f;
        featureY = 0.0f;
        featureZ = 0.0f;
        featureNX = 0.0f;
        featureNY = 0.0f;
        featureNZ = 0.0f;
        material = null;
        saliency = 0.0f;
        magnitude = 0.0f;

        currentGain = 0.0f;
        currentX = 0.0f;
        currentY = 0.0f;
        currentZ = 0.0f;
        currentFilterHF = 1.0f;

        targetGain = 0.0f;
        targetX = 0.0f;
        targetY = 0.0f;
        targetZ = 0.0f;
        targetFilterHF = 1.0f;

        active = false;
        fadeTicks = 0;
        distance = 0.0f;
        cyclesSinceActivation = 0;
    }

    public void silence() {
        targetGain = 0.0f;
        currentGain = 0.0f;
        if (sourceId != 0) {
            org.lwjgl.openal.AL11.alSourceStop(sourceId);
            org.lwjgl.openal.AL11.alSourcef(sourceId, org.lwjgl.openal.AL11.AL_GAIN, 0.0f);
            org.lwjgl.openal.AL11.alSourcei(sourceId,
                    org.lwjgl.openal.AL11.AL_BUFFER, 0);
        }
    }
}
