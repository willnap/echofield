package com.sonicether.soundphysics.eap;

import org.lwjgl.openal.EXTEfx;

/**
 * ISO 9613-1 inspired multi-band air absorption model.
 * Computes frequency-dependent attenuation over distance based on
 * humidity, replacing SPR's single-coefficient approximation.
 *
 * <p>Three bands are modelled: low (~250 Hz), mid (~2 kHz), high (~8 kHz).
 * Absorption coefficients are interpolated between reference values at
 * 30% and 70% relative humidity (RH). The 3-band result is mapped to
 * OpenAL's lowpass filter parameters for real-time application.
 */
public final class AirAbsorptionProcessor {

    /** Absorption coefficients at 30% RH in dB/m for [low, mid, high]. */
    private static final float[] ALPHA_30RH = {0.1f, 2.0f, 15.0f};

    /** Absorption coefficients at 70% RH in dB/m for [low, mid, high]. */
    private static final float[] ALPHA_70RH = {0.05f, 1.0f, 8.0f};

    private boolean enabled = true;

    /**
     * Computes per-band linear gain factors for air absorption over a given distance.
     *
     * @param distanceM   distance in metres (blocks)
     * @param humidityRH  relative humidity in percent (0-100)
     * @return float[3] with linear gain [low, mid, high], each in [0, 1]
     */
    public static float[] computeBandGains(float distanceM, float humidityRH) {
        float[] gains = new float[3];

        // Clamp humidity to interpolation range
        float t = Math.max(0f, Math.min(1f, (humidityRH - 30f) / 40f));

        for (int i = 0; i < 3; i++) {
            // Linearly interpolate absorption coefficient between 30% and 70% RH
            float alpha = ALPHA_30RH[i] + (ALPHA_70RH[i] - ALPHA_30RH[i]) * t;

            // Convert dB/m to linear gain over distance
            // attenuationDb = alpha * distance / 100 (alpha is dB per 100m scaled)
            float attenuationDb = alpha * distanceM / 100f;
            gains[i] = (float) Math.pow(10.0, -attenuationDb / 20.0);
            gains[i] = Math.max(0f, Math.min(1f, gains[i]));
        }

        return gains;
    }

    /**
     * Applies 3-band air absorption to an OpenAL lowpass filter.
     * Maps the 3-band gains to the 2-parameter lowpass model:
     * <ul>
     *   <li>AL_LOWPASS_GAIN = mean of all three bands</li>
     *   <li>AL_LOWPASS_GAINHF = high / low ratio (clamped to [0,1])</li>
     * </ul>
     *
     * @param filterId   OpenAL filter ID (must be AL_FILTER_LOWPASS)
     * @param distanceM  distance in metres
     * @param humidityRH relative humidity in percent
     */
    public void applyToFilter(int filterId, float distanceM, float humidityRH) {
        if (!enabled) return;

        float[] gains = computeBandGains(distanceM, humidityRH);

        float lpGain = (gains[0] + gains[1] + gains[2]) / 3f;
        lpGain = Math.max(0f, Math.min(1f, lpGain));

        float lpGainHF;
        if (gains[0] < 0.001f) {
            lpGainHF = 0f;
        } else {
            lpGainHF = gains[2] / gains[0];
            lpGainHF = Math.max(0f, Math.min(1f, lpGainHF));
        }

        EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAIN, lpGain);
        EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAINHF, lpGainHF);
    }

    /**
     * Returns an estimated humidity value for a biome based on its registry key.
     * Uses simple name matching for common biome types.
     *
     * @param biomeKey the biome's resource location string (e.g. "minecraft:desert")
     * @return humidity in percent (0-100)
     */
    public static float biomeHumidity(String biomeKey) {
        if (biomeKey == null) return 50f;

        String lower = biomeKey.toLowerCase();

        if (lower.contains("desert") || lower.contains("badlands")
                || lower.contains("mesa")) {
            return 15f;
        }
        if (lower.contains("savanna")) {
            return 25f;
        }
        if (lower.contains("taiga") || lower.contains("snowy")
                || lower.contains("frozen") || lower.contains("ice")) {
            return 35f;
        }
        if (lower.contains("plains") || lower.contains("forest")
                || lower.contains("birch") || lower.contains("mountain")
                || lower.contains("meadow")) {
            return 50f;
        }
        if (lower.contains("jungle") || lower.contains("mangrove")) {
            return 85f;
        }
        if (lower.contains("swamp") || lower.contains("mushroom")) {
            return 90f;
        }
        if (lower.contains("ocean") || lower.contains("river")
                || lower.contains("beach")) {
            return 75f;
        }

        return 50f; // default
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
