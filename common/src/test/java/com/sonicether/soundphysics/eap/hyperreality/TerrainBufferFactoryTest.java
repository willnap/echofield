package com.sonicether.soundphysics.eap.hyperreality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainBufferFactoryTest {

    // ---- Constants ----

    private static final int SAMPLES = TerrainBufferFactory.BUFFER_SAMPLES;       // 264600
    private static final int CROSSFADE = TerrainBufferFactory.CROSSFADE_SAMPLES;   // 4410

    // ---- normalize ----

    @Test
    void normalize_peakDoesNotExceedCeiling() {
        float[] samples = {-1.0f, 0.5f, 0.8f, -0.3f, 1.2f};
        TerrainBufferFactory.normalize(samples, 0.7f);
        for (float s : samples) {
            assertTrue(Math.abs(s) <= 0.7f + 1e-6f,
                    "Sample " + s + " exceeds ceiling 0.7");
        }
    }

    @Test
    void normalize_peakEqualsExactCeiling() {
        float[] samples = {-1.0f, 0.5f, 0.8f, -0.3f, 1.2f};
        TerrainBufferFactory.normalize(samples, 0.7f);
        float peak = 0f;
        for (float s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        assertEquals(0.7f, peak, 1e-5f, "Peak should exactly equal ceiling after normalize");
    }

    @Test
    void normalize_preservesRelativeAmplitudes() {
        float[] samples = {-1.0f, 0.5f};
        TerrainBufferFactory.normalize(samples, 0.7f);
        // Ratio should be preserved: samples[1] / |samples[0]| = 0.5
        assertEquals(0.5f, Math.abs(samples[1] / samples[0]), 1e-5f);
    }

    @Test
    void normalize_allZero_remainsZero() {
        float[] samples = {0f, 0f, 0f};
        TerrainBufferFactory.normalize(samples, 0.7f);
        for (float s : samples) {
            assertEquals(0f, s);
        }
    }

    // ---- crossfade ----

    @Test
    void crossfade_endMatchesStart_loopContinuity() {
        // Generate a non-trivial signal
        float[] samples = new float[SAMPLES];
        java.util.Random rng = new java.util.Random(42L);
        for (int i = 0; i < SAMPLES; i++) {
            samples[i] = rng.nextFloat() * 2f - 1f;
        }
        // Save original start region
        float[] startRegion = new float[CROSSFADE];
        System.arraycopy(samples, 0, startRegion, 0, CROSSFADE);

        TerrainBufferFactory.crossfade(samples, CROSSFADE);

        // At the very end of the buffer, the signal should blend toward the start
        // The last sample should be close to samples[0] (within crossfade tolerance)
        // Check that RMS of the crossfade region is reasonable (not zero, not clipped)
        float rms = 0f;
        for (int i = SAMPLES - CROSSFADE; i < SAMPLES; i++) {
            rms += samples[i] * samples[i];
        }
        rms = (float) Math.sqrt(rms / CROSSFADE);
        assertTrue(rms > 0.01f, "Crossfade region should not be silent, RMS=" + rms);
        assertTrue(rms < 1.5f, "Crossfade region should not be clipped, RMS=" + rms);
    }

    @Test
    void crossfade_equalPower_noEnergyDip() {
        // Equal-power crossfade: fadeOut = cos(t*PI/2), fadeIn = sin(t*PI/2)
        // On an all-ones buffer: sample[mid] = cos(PI/4) + sin(PI/4) ≈ 1.414
        // Linear crossfade would give: (1-0.5) + 0.5 = 1.0
        // Test distinguishes the two by requiring > 1.1 at midpoint
        float[] ones = new float[SAMPLES];
        java.util.Arrays.fill(ones, 1.0f);
        TerrainBufferFactory.crossfade(ones, CROSSFADE);

        int midIdx = SAMPLES - CROSSFADE + CROSSFADE / 2;
        float midValue = ones[midIdx];
        // Equal-power gives ~1.414 at midpoint; linear gives exactly 1.0
        assertTrue(midValue > 1.1f,
                "Equal-power crossfade midpoint should exceed linear (1.0), got " + midValue);
        // Should be close to sqrt(2) ≈ 1.414
        assertEquals(Math.sqrt(2.0), midValue, 0.05f,
                "Equal-power crossfade midpoint should be ~sqrt(2)");
    }

    // ---- Void buffer generation ----

    @Test
    void voidSamples_correctLength() {
        float[] samples = TerrainBufferFactory.generateVoidSamples(0);
        assertEquals(SAMPLES, samples.length);
    }

    @Test
    void voidSamples_deterministic_sameSeed() {
        float[] a = TerrainBufferFactory.generateVoidSamples(0);
        float[] b = TerrainBufferFactory.generateVoidSamples(0);
        assertArrayEquals(a, b, "Same variantIndex must produce identical samples");
    }

    @Test
    void voidSamples_decorrelated_differentVariants() {
        float[] a = TerrainBufferFactory.generateVoidSamples(0);
        float[] b = TerrainBufferFactory.generateVoidSamples(1);
        // Compute Pearson correlation on first 10000 samples
        float correlation = pearsonCorrelation(a, b, 10000);
        assertTrue(Math.abs(correlation) < 0.5f,
                "Different variants should be decorrelated, correlation=" + correlation);
    }

    @Test
    void voidSamples_normalized_peakBelowCeiling() {
        float[] samples = TerrainBufferFactory.generateVoidSamples(0);
        float peak = 0f;
        for (float s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        assertTrue(peak <= 0.7f + 1e-5f,
                "Void buffer peak should not exceed 0.7 ceiling, got " + peak);
        assertTrue(peak >= 0.5f,
                "Void buffer should have significant energy, peak=" + peak);
    }

    @Test
    void voidSamples_hasAmplitudeModulation() {
        float[] samples = TerrainBufferFactory.generateVoidSamples(0);
        // Compute RMS in 1-second windows and check variance across windows
        int windowSize = TerrainBufferFactory.SAMPLE_RATE; // 1 second
        int numWindows = SAMPLES / windowSize;
        float[] windowRms = new float[numWindows];
        for (int w = 0; w < numWindows; w++) {
            float sum = 0f;
            for (int i = 0; i < windowSize; i++) {
                float s = samples[w * windowSize + i];
                sum += s * s;
            }
            windowRms[w] = (float) Math.sqrt(sum / windowSize);
        }
        // Compute variance of window RMS — AM should create measurable variation
        float mean = 0f;
        for (float r : windowRms) mean += r;
        mean /= numWindows;
        float variance = 0f;
        for (float r : windowRms) variance += (r - mean) * (r - mean);
        variance /= numWindows;
        assertTrue(variance > 1e-6f,
                "Void buffer should have AM-induced RMS variance, got " + variance);
    }

    @Test
    void voidSamples_bandpassShape_energyConcentratedAroundCenter() {
        float[] samples = TerrainBufferFactory.generateVoidSamples(0);
        // Check that low-frequency energy (below 100 Hz) is less than
        // mid-frequency energy (300-500 Hz band around center 400 Hz)
        float lowEnergy = bandEnergy(samples, 20f, 100f);
        float midEnergy = bandEnergy(samples, 300f, 500f);
        float highEnergy = bandEnergy(samples, 2000f, 4000f);
        assertTrue(midEnergy > lowEnergy,
                "Void bandpass: mid energy (" + midEnergy + ") should exceed low (" + lowEnergy + ")");
        assertTrue(midEnergy > highEnergy,
                "Void bandpass: mid energy (" + midEnergy + ") should exceed high (" + highEnergy + ")");
    }

    @Test
    void voidSamples_qDifference_variant0vs2() {
        // Variants 0-1 use Q=2.0, variants 2-3 use Q=4.0
        // Higher Q = narrower peak = less energy in the tails
        float[] v0 = TerrainBufferFactory.generateVoidSamples(0);
        float[] v2 = TerrainBufferFactory.generateVoidSamples(2);
        // Measure energy ratio of tails vs center for each
        float v0center = bandEnergy(v0, 350f, 450f);
        float v0tails = bandEnergy(v0, 100f, 200f) + bandEnergy(v0, 600f, 800f);
        float v2center = bandEnergy(v2, 350f, 450f);
        float v2tails = bandEnergy(v2, 100f, 200f) + bandEnergy(v2, 600f, 800f);
        float v0ratio = v0tails / (v0center + 1e-12f);
        float v2ratio = v2tails / (v2center + 1e-12f);
        // Higher Q (variant 2) should have lower tail-to-center ratio
        assertTrue(v2ratio < v0ratio,
                "Higher Q variant should have narrower band: v0 ratio=" + v0ratio + ", v2 ratio=" + v2ratio);
    }

    // ---- Surface buffer generation ----

    @Test
    void surfaceSamples_correctLength() {
        float[] samples = TerrainBufferFactory.generateSurfaceSamples(0);
        assertEquals(SAMPLES, samples.length);
    }

    @Test
    void surfaceSamples_deterministic_sameSeed() {
        float[] a = TerrainBufferFactory.generateSurfaceSamples(1);
        float[] b = TerrainBufferFactory.generateSurfaceSamples(1);
        assertArrayEquals(a, b, "Same variantIndex must produce identical samples");
    }

    @Test
    void surfaceSamples_decorrelated_differentVariants() {
        float[] a = TerrainBufferFactory.generateSurfaceSamples(0);
        float[] b = TerrainBufferFactory.generateSurfaceSamples(1);
        float correlation = pearsonCorrelation(a, b, 10000);
        assertTrue(Math.abs(correlation) < 0.6f,
                "Different surface variants should be decorrelated, correlation=" + correlation);
    }

    @Test
    void surfaceSamples_normalized_peakBelowCeiling() {
        float[] samples = TerrainBufferFactory.generateSurfaceSamples(0);
        float peak = 0f;
        for (float s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        assertTrue(peak <= 0.7f + 1e-5f,
                "Surface buffer peak should not exceed 0.7, got " + peak);
        assertTrue(peak >= 0.5f,
                "Surface buffer should have significant energy, peak=" + peak);
    }

    @Test
    void surfaceSamples_lowpassShape_highFreqRolloff() {
        float[] samples = TerrainBufferFactory.generateSurfaceSamples(0);
        float lowEnergy = bandEnergy(samples, 100f, 500f);
        float highEnergy = bandEnergy(samples, 3000f, 6000f);
        assertTrue(lowEnergy > highEnergy * 2f,
                "Surface lowpass: low energy (" + lowEnergy + ") should dominate high (" + highEnergy + ")");
    }

    @Test
    void surfaceSamples_contains110HzTone() {
        float[] samples = TerrainBufferFactory.generateSurfaceSamples(0);
        // Energy around 110 Hz should be elevated compared to neighboring bands
        float toneEnergy = bandEnergy(samples, 100f, 120f);
        float neighborEnergy = bandEnergy(samples, 150f, 170f);
        assertTrue(toneEnergy > neighborEnergy * 0.5f,
                "Surface should have 110Hz tone: tone energy (" + toneEnergy +
                ") vs neighbor (" + neighborEnergy + ")");
    }

    @Test
    void surfaceSamples_hasAmplitudeModulation() {
        float[] samples = TerrainBufferFactory.generateSurfaceSamples(0);
        int windowSize = TerrainBufferFactory.SAMPLE_RATE;
        int numWindows = SAMPLES / windowSize;
        float[] windowRms = new float[numWindows];
        for (int w = 0; w < numWindows; w++) {
            float sum = 0f;
            for (int i = 0; i < windowSize; i++) {
                float s = samples[w * windowSize + i];
                sum += s * s;
            }
            windowRms[w] = (float) Math.sqrt(sum / windowSize);
        }
        float mean = 0f;
        for (float r : windowRms) mean += r;
        mean /= numWindows;
        float variance = 0f;
        for (float r : windowRms) variance += (r - mean) * (r - mean);
        variance /= numWindows;
        assertTrue(variance > 1e-7f,
                "Surface buffer should have AM-induced RMS variance, got " + variance);
    }

    // ---- Ground buffer generation ----

    @Test
    void groundSamples_correctLength() {
        float[] samples = TerrainBufferFactory.generateGroundSamples(0);
        assertEquals(SAMPLES, samples.length);
    }

    @Test
    void groundSamples_deterministic_sameSeed() {
        float[] a = TerrainBufferFactory.generateGroundSamples(3);
        float[] b = TerrainBufferFactory.generateGroundSamples(3);
        assertArrayEquals(a, b, "Same variantIndex must produce identical samples");
    }

    @Test
    void groundSamples_decorrelated_differentVariants() {
        float[] a = TerrainBufferFactory.generateGroundSamples(0);
        float[] b = TerrainBufferFactory.generateGroundSamples(1);
        float correlation = pearsonCorrelation(a, b, 10000);
        assertTrue(Math.abs(correlation) < 0.5f,
                "Different ground variants should be decorrelated, correlation=" + correlation);
    }

    @Test
    void groundSamples_normalized_lowerCeiling() {
        float[] samples = TerrainBufferFactory.generateGroundSamples(0);
        float peak = 0f;
        for (float s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        // Ground uses 0.6 ceiling (lower headroom — quietest family)
        assertTrue(peak <= 0.6f + 1e-5f,
                "Ground buffer peak should not exceed 0.6, got " + peak);
        assertTrue(peak >= 0.4f,
                "Ground buffer should have significant energy, peak=" + peak);
    }

    @Test
    void groundSamples_bandpassShape_narrowBand() {
        float[] samples = TerrainBufferFactory.generateGroundSamples(0);
        // Center is 280 Hz, Q≈1.4 → bandwidth ~200 Hz (200-400 Hz range)
        float centerEnergy = bandEnergy(samples, 220f, 340f);
        float lowEnergy = bandEnergy(samples, 50f, 120f);
        float highEnergy = bandEnergy(samples, 800f, 1500f);
        assertTrue(centerEnergy > lowEnergy,
                "Ground bandpass: center (" + centerEnergy + ") should exceed low (" + lowEnergy + ")");
        assertTrue(centerEnergy > highEnergy,
                "Ground bandpass: center (" + centerEnergy + ") should exceed high (" + highEnergy + ")");
    }

    @Test
    void groundSamples_hasAmplitudeModulation() {
        float[] samples = TerrainBufferFactory.generateGroundSamples(0);
        int windowSize = TerrainBufferFactory.SAMPLE_RATE;
        int numWindows = SAMPLES / windowSize;
        float[] windowRms = new float[numWindows];
        for (int w = 0; w < numWindows; w++) {
            float sum = 0f;
            for (int i = 0; i < windowSize; i++) {
                float s = samples[w * windowSize + i];
                sum += s * s;
            }
            windowRms[w] = (float) Math.sqrt(sum / windowSize);
        }
        float mean = 0f;
        for (float r : windowRms) mean += r;
        mean /= numWindows;
        float variance = 0f;
        for (float r : windowRms) variance += (r - mean) * (r - mean);
        variance /= numWindows;
        assertTrue(variance > 1e-7f,
                "Ground buffer should have AM-induced RMS variance, got " + variance);
    }

    // ---- Cross-family decorrelation ----

    @Test
    void families_decorrelated_voidVsSurface() {
        float[] v = TerrainBufferFactory.generateVoidSamples(0);
        float[] s = TerrainBufferFactory.generateSurfaceSamples(0);
        float correlation = pearsonCorrelation(v, s, 10000);
        assertTrue(Math.abs(correlation) < 0.5f,
                "Void and Surface should be decorrelated, correlation=" + correlation);
    }

    @Test
    void families_decorrelated_voidVsGround() {
        float[] v = TerrainBufferFactory.generateVoidSamples(0);
        float[] g = TerrainBufferFactory.generateGroundSamples(0);
        float correlation = pearsonCorrelation(v, g, 10000);
        assertTrue(Math.abs(correlation) < 0.5f,
                "Void and Ground should be decorrelated, correlation=" + correlation);
    }

    // ---- getBufferId variant hashing ----

    @Test
    void getBufferId_variantHash_deterministic() {
        // Verify same coords always produce same variant
        int a = TerrainBufferFactory.variantForPosition(10, 20);
        int b = TerrainBufferFactory.variantForPosition(10, 20);
        assertEquals(a, b, "Same position must produce same variant");
    }

    @Test
    void getBufferId_variantHash_range0to3() {
        // Verify all results are in range [0, 3]
        for (int x = -1000; x < 1000; x += 7) {
            for (int z = -1000; z < 1000; z += 11) {
                int variant = TerrainBufferFactory.variantForPosition(x, z);
                assertTrue(variant >= 0 && variant <= 3,
                        "Variant out of range at (" + x + "," + z + "): " + variant);
            }
        }
    }

    @Test
    void getBufferId_variantHash_distributionReasonablyUniform() {
        // Over many positions, each variant should appear roughly 25%
        int[] counts = new int[4];
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                counts[TerrainBufferFactory.variantForPosition(x, z)]++;
            }
        }
        int total = 200 * 200;
        for (int v = 0; v < 4; v++) {
            float ratio = (float) counts[v] / total;
            assertTrue(ratio > 0.15f && ratio < 0.35f,
                    "Variant " + v + " has uneven distribution: " + ratio + " (" + counts[v] + "/" + total + ")");
        }
    }

    // ---- Helpers ----

    /**
     * Pearson correlation coefficient between first n samples of two arrays.
     */
    static float pearsonCorrelation(float[] a, float[] b, int n) {
        float sumA = 0, sumB = 0, sumAB = 0, sumA2 = 0, sumB2 = 0;
        for (int i = 0; i < n; i++) {
            sumA += a[i];
            sumB += b[i];
            sumAB += a[i] * b[i];
            sumA2 += a[i] * a[i];
            sumB2 += b[i] * b[i];
        }
        float num = n * sumAB - sumA * sumB;
        float den = (float) Math.sqrt((n * sumA2 - sumA * sumA) * (n * sumB2 - sumB * sumB));
        if (den < 1e-12f) return 0f;
        return num / den;
    }

    /**
     * Estimate energy in a frequency band by applying a 2-pole bandpass filter
     * and computing RMS. This is a simple spectral test — not FFT-precise,
     * but sufficient to verify gross spectral shape.
     */
    static float bandEnergy(float[] samples, float lowHz, float highHz) {
        float centerHz = (lowHz + highHz) / 2f;
        float bw = highHz - lowHz;
        float q = centerHz / bw;
        // Apply biquad bandpass and measure RMS
        float[] filtered = samples.clone();
        TerrainBufferFactory.applyBiquadBandpass(filtered, centerHz, q, TerrainBufferFactory.SAMPLE_RATE);
        float sum = 0f;
        for (float s : filtered) sum += s * s;
        return (float) Math.sqrt(sum / filtered.length);
    }
}
