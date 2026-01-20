package com.sonicether.soundphysics.eap.emitter;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PerlinNoiseTest {

    @Test
    void sample_outputInRange_forManyInputs() {
        PerlinNoise noise = new PerlinNoise(12345L);
        Random rng = new Random(99);
        for (int i = 0; i < 10000; i++) {
            float x = (rng.nextFloat() - 0.5f) * 2000f; // range [-1000, 1000]
            float val = noise.sample(x);
            assertTrue(val >= -1f && val <= 1f,
                    "sample(" + x + ") = " + val + " out of [-1, 1]");
        }
    }

    @Test
    void sample_deterministic_sameSeedSameOutput() {
        PerlinNoise a = new PerlinNoise(42L);
        PerlinNoise b = new PerlinNoise(42L);
        for (float x = -50f; x < 50f; x += 0.37f) {
            assertEquals(a.sample(x), b.sample(x),
                    "Determinism violated at x=" + x);
        }
    }

    @Test
    void fBm_outputInRange_forManyInputs() {
        PerlinNoise noise = new PerlinNoise(7777L);
        Random rng = new Random(42);
        for (int i = 0; i < 10000; i++) {
            float x = (rng.nextFloat() - 0.5f) * 2000f;
            float val = noise.fBm(x, 5, 2.0f, 0.5f);
            assertTrue(val >= -1f && val <= 1f,
                    "fBm(" + x + ") = " + val + " out of [-1, 1]");
        }
    }

    @Test
    void fBm_fiveOctaves_smootherThanOneOctave() {
        PerlinNoise noise = new PerlinNoise(1234L);
        int n = 5000;
        float step = 0.01f;

        // Compute variance of values for 1 octave
        float variance1 = computeVariance(noise, n, step, 1);

        // Compute variance of values for 5 octaves (normalization reduces spread)
        float variance5 = computeVariance(noise, n, step, 5);

        assertTrue(variance5 < variance1,
                "5-octave fBm (variance=" + variance5 +
                ") should have lower variance than 1-octave (variance=" + variance1 + ")");
    }

    @Test
    void fBm_deterministic_sameSeedSameOutput() {
        PerlinNoise a = new PerlinNoise(555L);
        PerlinNoise b = new PerlinNoise(555L);
        for (float x = 0f; x < 100f; x += 1.13f) {
            assertEquals(a.fBm(x, 5, 2.0f, 0.5f), b.fBm(x, 5, 2.0f, 0.5f),
                    "fBm determinism violated at x=" + x);
        }
    }

    /**
     * Computes the variance of sampled values.
     * Multi-octave fBm with normalization produces lower variance
     * as the independent octaves average out.
     */
    private float computeVariance(PerlinNoise noise, int n, float step, int octaves) {
        float sum = 0f, sumSq = 0f;
        for (int i = 0; i < n; i++) {
            float val = noise.fBm(i * step, octaves, 2.0f, 0.5f);
            sum += val;
            sumSq += val * val;
        }
        float mean = sum / n;
        return sumSq / n - mean * mean;
    }
}
