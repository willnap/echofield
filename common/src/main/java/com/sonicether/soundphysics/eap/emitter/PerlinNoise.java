package com.sonicether.soundphysics.eap.emitter;

/**
 * Lightweight 1D gradient (Perlin) noise for audio envelope generation.
 * Deterministic: same seed produces same output.
 */
public final class PerlinNoise {
    private final int[] perm;  // permutation table (256 entries)

    public PerlinNoise(long seed) {
        // Initialize permutation table from seed using Fisher-Yates shuffle
        perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;
        // Shuffle using seed
        java.util.Random rng = new java.util.Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
        }
    }

    /** Sample 1D gradient noise at position x. Returns value in [-1, 1]. */
    public float sample(float x) {
        int xi = (int) Math.floor(x) & 255;
        float xf = x - (float) Math.floor(x);
        float u = fade(xf);
        float g0 = grad(perm[xi], xf);
        float g1 = grad(perm[(xi + 1) & 255], xf - 1f);
        // Raw 1D Perlin with ±1 gradients produces [-0.5, 0.5]; scale to [-1, 1]
        return lerp(u, g0, g1) * 2f;
    }

    /** Fractal Brownian Motion -- multi-octave Perlin noise. */
    public float fBm(float x, int octaves, float lacunarity, float persistence) {
        float sum = 0f, amp = 1f, freq = 1f, maxAmp = 0f;
        for (int i = 0; i < octaves; i++) {
            sum += sample(x * freq) * amp;
            maxAmp += amp;
            freq *= lacunarity;
            amp *= persistence;
        }
        return sum / maxAmp;  // Normalize to [-1, 1]
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);  // Improved Perlin smoothstep
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x) {
        return (hash & 1) == 0 ? x : -x;  // 1D gradient: +1 or -1
    }
}
