package com.sonicether.soundphysics.eap.emitter;

import com.sonicether.soundphysics.Loggers;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates and caches procedural OpenAL buffers for continuous emitter categories.
 * Each category gets a unique mono buffer with appropriate spectral character.
 * Buffers are created once and shared across all emitters of the same category.
 */
public final class ProceduralBufferFactory {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SECONDS = 6;
    private static final int BUFFER_SAMPLES = SAMPLE_RATE * BUFFER_SECONDS;

    private static final int BUFFER_VARIANTS = 4;
    private final Map<EmitterCategory, int[]> buffers = new EnumMap<>(EmitterCategory.class);
    // Reusable direct ByteBuffer for triggered emitters — avoids per-trigger allocateDirect
    // which creates off-heap memory that is only freed by GC finalization.
    private ByteBuffer reusableBuf;
    private int reusableBufCapacity;

    public int getBuffer(EmitterCategory category, int variant) {
        if (category.sampleBased) return 0;
        int[] variants = buffers.computeIfAbsent(category, this::generateVariants);
        return variants[Math.abs(variant) % BUFFER_VARIANTS];
    }

    private int[] generateVariants(EmitterCategory category) {
        int[] ids = new int[BUFFER_VARIANTS];
        for (int v = 0; v < BUFFER_VARIANTS; v++) {
            float[] samples = new float[BUFFER_SAMPLES];
            Random rng = new Random(System.nanoTime() ^ ((long) category.ordinal() << 32) ^ ((long) v << 48));

            switch (category) {
                case WIND_LEAF -> generateLeafRustle(samples, rng);
                case WIND_GRASS -> generateGrassSwish(samples, rng);
                case WIND_WHISTLE -> generateWhistle(samples, rng);
                case WATER_FLOW -> generateWaterFlow(samples, rng);
                case LAVA -> generateLava(samples, rng);
                case CAVE_DRONE -> generateCaveDrone(samples, rng);
                default -> generateWhiteNoise(samples, rng);
            }

            applyCrossfade(samples);
            ids[v] = createBuffer(samples, category.name() + "_v" + v);
        }
        return ids;
    }

    /**
     * Compute Perlin wind speed envelope for the entire buffer. Values in [0, 1].
     * Emphasizes slow gusting (0.15-0.4 Hz) with minimal jitter for natural dynamics.
     * The envelope has 15-20 dB of dynamic range — from near-silence lulls to full gusts.
     */
    private float[] computeWindEnvelope(Random rng) {
        PerlinNoise noise = new PerlinNoise(rng.nextLong());
        float[] envelope = new float[BUFFER_SAMPLES];
        // 4 octaves — slow gusting dominates, minimal high-freq jitter
        float[] freqs = {0.08f, 0.25f, 0.6f, 1.5f};
        float[] weights = {0.5f, 1.0f, 0.6f, 0.12f};
        float normFactor = 0.40f;
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            float t = (float) i / SAMPLE_RATE;
            float value = 0;
            for (int o = 0; o < 4; o++) {
                value += noise.sample(t * freqs[o]) * weights[o];
            }
            // Center around 0.35 (shifted down so lulls are more common, gusts are events)
            float raw = value * normFactor + 0.35f;
            raw = Math.max(0f, Math.min(1f, raw));
            // Cube the envelope for dramatic dynamic range: lulls near-silent, peaks intense
            // This gives ~30 dB of dynamic range (0.1³=0.001 vs 1.0³=1.0)
            envelope[i] = raw * raw * raw;
        }
        return envelope;
    }

    private void generateLeafRustle(float[] out, Random rng) {
        float[] wind = computeWindEnvelope(rng);

        // Two-pole dynamic bandpass for broadband wind noise (12 dB/oct slopes)
        // This is the continuous "whoosh" layer — the foundation of the wind sound
        float hp1 = 0, hp2 = 0; // 2-pole highpass state
        float lp1 = 0, lp2 = 0; // 2-pole lowpass state

        // Smoothed wind envelope (20ms smoothing for natural tracking)
        float smoothWind = 0;
        float smoothAlpha = 1f / (1f + SAMPLE_RATE * 0.020f);

        for (int i = 0; i < out.length; i++) {
            smoothWind += (wind[i] - smoothWind) * smoothAlpha;

            // Dynamic cutoffs: wider band at higher wind
            float hpFreq = 150f - smoothWind * 80f;   // 70-150 Hz (lower than before for LF rumble)
            float lpFreq = 800f + smoothWind * 1400f;  // 800-2200 Hz (tighter than before)
            float hpA = hpFreq / (hpFreq + SAMPLE_RATE / (2f * (float) Math.PI));
            float lpA = lpFreq / (lpFreq + SAMPLE_RATE / (2f * (float) Math.PI));

            float white = rng.nextFloat() * 2f - 1f;
            // Two-pole highpass (12 dB/oct)
            hp1 += (white - hp1) * hpA;
            float hp = white - hp1;
            hp2 += (hp - hp2) * hpA;
            hp = hp - hp2;
            // Two-pole lowpass (12 dB/oct)
            lp1 += (hp - lp1) * lpA;
            lp2 += (lp1 - lp2) * lpA;

            out[i] = lp2 * smoothWind * 0.55f;  // Boosted broadband component
        }

        // Granular rustling: bandlimited noise bursts (NOT ring-modulated)
        // Each grain is a short, lowpass-filtered noise burst — simulates individual
        // leaf contacts. Density driven by LOCAL wind per 250ms window.
        int windowSamples = SAMPLE_RATE / 4; // 250ms
        int numWindows = BUFFER_SAMPLES / windowSamples;
        int baseGrainsPerWindow = 120;
        int maxExtraPerWindow = 500;

        for (int w = 0; w < numWindows; w++) {
            int windowStart = w * windowSamples;
            float localWind = 0;
            for (int i = windowStart; i < windowStart + windowSamples && i < wind.length; i++) {
                localWind += wind[i];
            }
            localWind /= windowSamples;

            int numGrains = baseGrainsPerWindow + (int) (maxExtraPerWindow * localWind);
            for (int g = 0; g < numGrains; g++) {
                int start = windowStart + rng.nextInt(windowSamples);
                if (start >= out.length) continue;
                float lw = wind[Math.min(start, wind.length - 1)];
                int len = (int) (440 * (1f - lw * 0.5f)) + 50;
                float amp = (0.006f + rng.nextFloat() * 0.010f) * (0.3f + lw * 0.7f);

                // Grain cutoff frequency: 600-1800 Hz (bandlimited, no ring modulation)
                float grainCutoff = 600f + lw * 1200f;
                float grainLpA = grainCutoff / (grainCutoff + SAMPLE_RATE / (2f * (float) Math.PI));
                float grainLp1 = 0, grainLp2 = 0;

                for (int i = 0; i < len && start + i < out.length; i++) {
                    float window2 = 0.5f * (1f - (float) Math.cos(2 * Math.PI * i / len));
                    float noise = rng.nextFloat() * 2f - 1f;
                    // Two-pole lowpass on the noise grain itself (12 dB/oct)
                    grainLp1 += (noise - grainLp1) * grainLpA;
                    grainLp2 += (grainLp1 - grainLp2) * grainLpA;
                    out[start + i] += amp * window2 * grainLp2;
                }
            }
        }
    }

    private void generateGrassSwish(float[] out, Random rng) {
        float[] wind = computeWindEnvelope(rng);

        // Two-pole dynamic bandpass (12 dB/oct slopes) — softer than leaves
        float hp1 = 0, hp2 = 0;
        float lp1 = 0, lp2 = 0;
        float smoothWind = 0;
        float smoothAlpha = 1f / (1f + SAMPLE_RATE * 0.025f); // 25ms smoothing (slower than leaves)

        for (int i = 0; i < out.length; i++) {
            smoothWind += (wind[i] - smoothWind) * smoothAlpha;
            // Lower frequency range than leaves — grass is softer, more swishy
            float hpFreq = 80f - smoothWind * 40f;    // 40-80 Hz
            float lpFreq = 600f + smoothWind * 800f;   // 600-1400 Hz (lower than leaves)
            float hpA = hpFreq / (hpFreq + SAMPLE_RATE / (2f * (float) Math.PI));
            float lpA = lpFreq / (lpFreq + SAMPLE_RATE / (2f * (float) Math.PI));

            float white = rng.nextFloat() * 2f - 1f;
            hp1 += (white - hp1) * hpA;
            float hp = white - hp1;
            hp2 += (hp - hp2) * hpA;
            hp = hp - hp2;
            lp1 += (hp - lp1) * lpA;
            lp2 += (lp1 - lp2) * lpA;

            out[i] = lp2 * smoothWind * 0.45f;
        }

        // Bandlimited swish grains — longer than leaf grains (grass sways, doesn't snap)
        // Uses windowed windows to avoid ring modulation artifacts
        int windowSamples = SAMPLE_RATE / 3; // 333ms windows (slower than leaves)
        int numWindows = BUFFER_SAMPLES / windowSamples;
        int baseGrainsPerWindow = 60;   // fewer, longer grains than leaves
        int maxExtraPerWindow = 250;

        for (int w = 0; w < numWindows; w++) {
            int windowStart = w * windowSamples;
            float localWind = 0;
            for (int i = windowStart; i < windowStart + windowSamples && i < wind.length; i++) {
                localWind += wind[i];
            }
            localWind /= windowSamples;

            int numGrains = baseGrainsPerWindow + (int) (maxExtraPerWindow * localWind);
            for (int g = 0; g < numGrains; g++) {
                int start = windowStart + rng.nextInt(windowSamples);
                if (start >= out.length) continue;
                float lw = wind[Math.min(start, wind.length - 1)];
                // Longer grains: 15-35ms (grass sways)
                int len = (int) (660 * (1f - lw * 0.3f)) + 220;
                float amp = (0.004f + rng.nextFloat() * 0.008f) * (0.2f + lw * 0.8f);

                // Grain cutoff: lower than leaves (400-1000 Hz)
                float grainCutoff = 400f + lw * 600f;
                float grainLpA = grainCutoff / (grainCutoff + SAMPLE_RATE / (2f * (float) Math.PI));
                float grainLp1 = 0, grainLp2 = 0;

                for (int i = 0; i < len && start + i < out.length; i++) {
                    // Asymmetric window: slow attack (40%), faster release (grass sway character)
                    float phase = (float) i / len;
                    float window2 = phase < 0.4f
                            ? 0.5f * (1f - (float) Math.cos(Math.PI * phase / 0.4f))
                            : 0.5f * (1f + (float) Math.cos(Math.PI * (phase - 0.4f) / 0.6f));
                    float noise = rng.nextFloat() * 2f - 1f;
                    grainLp1 += (noise - grainLp1) * grainLpA;
                    grainLp2 += (grainLp1 - grainLp2) * grainLpA;
                    out[start + i] += amp * window2 * grainLp2;
                }
            }
        }
    }

    private void generateWhistle(float[] out, Random rng) {
        float[] wind = computeWindEnvelope(rng);

        // Aeolian tone physical model
        // Strouhal number: f = 0.2 * windSpeed / diameter
        // Model thin objects: diameter 5-20mm
        // At normalized wind speed 1.0 ~ 20 m/s: freq range 200-800 Hz
        // We pick a random "object diameter" for this buffer
        float diameter = 0.005f + rng.nextFloat() * 0.015f; // 5-20mm
        float referenceWindSpeed = 20f; // m/s at envelope=1.0
        float strouhalNumber = 0.2f;

        // Onset threshold: whistle only above wind speed > 0.6
        float onsetThreshold = 0.6f;

        // Turbulence noise for frequency modulation (separate Perlin instance)
        PerlinNoise turbulence = new PerlinNoise(rng.nextLong());

        // High-Q resonant bandpass filter state
        float bp0 = 0, bp1 = 0; // filter delay elements

        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float windSpeed = wind[i];

            // Abrupt onset: no whistle below threshold
            if (windSpeed <= onsetThreshold) {
                out[i] = 0;
                continue;
            }

            // Effective wind speed above threshold, normalized
            float effectiveWind = (windSpeed - onsetThreshold) / (1f - onsetThreshold);

            // Strouhal frequency
            float vPhysical = windSpeed * referenceWindSpeed;
            float baseFreq = strouhalNumber * vPhysical / diameter;

            // Pitch wobble from turbulence: +/-5% frequency modulation
            float wobble = turbulence.sample(t * 3.5f) * 0.05f;
            float freq = baseFreq * (1f + wobble);

            // Clamp frequency to audible range
            freq = Math.max(100f, Math.min(freq, 4000f));

            // High-Q resonant bandpass filter coefficients (update per sample for FM)
            float w0 = (float) (2 * Math.PI * freq / SAMPLE_RATE);
            float Q = 15f + effectiveWind * 10f; // Q increases with wind
            float alpha = (float) (Math.sin(w0) / (2 * Q));
            float cosW0 = (float) Math.cos(w0);

            // Bandpass filter coefficients (peak gain = 1)
            float b0 = alpha;
            float a1 = -2f * cosW0;
            float a2 = 1f - alpha;
            float a0 = 1f + alpha;

            // Normalize
            b0 /= a0;
            a1 /= a0;
            a2 /= a0;

            // Turbulent noise excitation
            float white = rng.nextFloat() * 2f - 1f;
            float excitation = white * effectiveWind;

            // Apply resonant bandpass
            float filtered = b0 * excitation - a1 * bp0 - a2 * bp1;
            bp1 = bp0;
            bp0 = filtered;

            // Clamp to prevent filter blowup
            bp0 = Math.max(-2f, Math.min(2f, bp0));
            bp1 = Math.max(-2f, Math.min(2f, bp1));

            out[i] = filtered * effectiveWind * 0.7f;
        }
    }

    private void generateWaterFlow(float[] out, Random rng) {
        // Perlin-modulated flow envelope for temporal variation
        PerlinNoise flowNoise = new PerlinNoise(rng.nextLong());

        // Layer 1: Sub-bass turbulence (30-250 Hz) — 2-pole filters for proper rolloff
        float turbHp1 = 0, turbHp2 = 0;
        float turbLp1 = 0, turbLp2 = 0;
        float turbHpAlpha = 30f / (30f + SAMPLE_RATE / (2f * (float) Math.PI));
        float turbLpAlpha = 250f / (250f + SAMPLE_RATE / (2f * (float) Math.PI));

        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float flowMod = 0.6f + 0.4f * flowNoise.sample(t * 1.5f);

            float white = rng.nextFloat() * 2f - 1f;
            // 2-pole HP at 30 Hz
            turbHp1 += (white - turbHp1) * turbHpAlpha;
            float hp = white - turbHp1;
            turbHp2 += (hp - turbHp2) * turbHpAlpha;
            hp = hp - turbHp2;
            // 2-pole LP at 250 Hz
            turbLp1 += (hp - turbLp1) * turbLpAlpha;
            turbLp2 += (turbLp1 - turbLp2) * turbLpAlpha;
            out[i] = turbLp2 * 0.45f * flowMod; // Boosted from 0.25
        }

        // Layer 1b: Mid-frequency turbulence (200-800 Hz) for body
        float midHp1 = 0, midHp2 = 0, midLp1 = 0, midLp2 = 0;
        float midHpA = 200f / (200f + SAMPLE_RATE / (2f * (float) Math.PI));
        float midLpA = 800f / (800f + SAMPLE_RATE / (2f * (float) Math.PI));

        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float flowMod = 0.5f + 0.5f * flowNoise.sample(t * 2.5f);
            float white = rng.nextFloat() * 2f - 1f;
            midHp1 += (white - midHp1) * midHpA;
            float hp = white - midHp1;
            midHp2 += (hp - midHp2) * midHpA;
            hp = hp - midHp2;
            midLp1 += (hp - midLp1) * midLpA;
            midLp2 += (midLp1 - midLp2) * midLpA;
            out[i] += midLp2 * 0.20f * flowMod;
        }

        // Layer 2: Discrete Minnaert bubbles — 20-40/sec (NOT 500!)
        // Individual bubble pops should be clearly audible events
        int bubblesPerSecond = 30;
        int totalBubbles = BUFFER_SECONDS * bubblesPerSecond;
        for (int b = 0; b < totalBubbles; b++) {
            int start = rng.nextInt(out.length);
            // Wider radius range: 0.3-8mm for diverse frequencies (400-10000 Hz)
            float radiusMm = (float) Math.exp(Math.log(2.0) + 0.6 * rng.nextGaussian());
            radiusMm = Math.max(0.3f, Math.min(radiusMm, 8f));
            float decay = 40f + rng.nextFloat() * 100f;
            float amp = 0.2f + rng.nextFloat() * 0.3f; // Loud enough to pop above turbulence
            addMinnaertBubble(out, start, radiusMm, decay, amp, 0.05f, 998f);
        }

        // Layer 3: Sparse surface splashes (2-5/sec broadband bursts)
        int splashesPerSecond = 3;
        int totalSplashes = BUFFER_SECONDS * splashesPerSecond;
        for (int s = 0; s < totalSplashes; s++) {
            int start = rng.nextInt(out.length);
            int duration = (int) (SAMPLE_RATE * (0.005f + rng.nextFloat() * 0.015f)); // 5-20ms
            float amp = 0.05f + rng.nextFloat() * 0.1f;
            for (int i = 0; i < duration && start + i < out.length; i++) {
                float env = 1.0f - (float) i / duration; // linear decay
                out[start + i] += (rng.nextFloat() * 2f - 1f) * amp * env;
            }
        }
    }

    /**
     * Generate a single water drip using Minnaert bubble resonance.
     * For sample-based (triggered) emitter — called from EmitterManager.
     */
    void generateWaterDrip(float[] out, Random rng, int sampleCount) {
        // Single Minnaert bubble per trigger
        // Radius 1-4mm → freq 800-3260 Hz
        float radiusMm = 1f + rng.nextFloat() * 3f;
        // Damped sinusoid: decay 80-200
        float decay = 80f + rng.nextFloat() * 120f;
        float amp = 0.6f + rng.nextFloat() * 0.3f;
        // Slight pitch chirp: -5% over duration (bubble deformation)
        addMinnaertBubble(out, 0, radiusMm, decay, amp, 0.05f, 998f);
    }

    /**
     * Generate rain impact sounds using Marshall-Palmer stochastic model.
     * For sample-based (triggered) emitter — called from EmitterManager.
     * Marshall-Palmer: N(D) = N0 * exp(-Lambda * D) for raindrop size distribution.
     */
    void generateWaterRain(float[] out, Random rng, int sampleCount) {
        // ~200 drops/sec, buffer is sampleCount long
        float durationSec = (float) sampleCount / SAMPLE_RATE;
        int numDrops = (int) (200 * durationSec);

        for (int d = 0; d < numDrops; d++) {
            int start = rng.nextInt(sampleCount);

            // Marshall-Palmer drop size distribution: exponential
            // Lambda ~ 4.1 per mm for moderate rain; draw D from exponential
            float dropDiameterMm = (float) (-Math.log(1.0 - rng.nextDouble()) / 4.1);
            dropDiameterMm = Math.max(0.5f, Math.min(dropDiameterMm, 5f));

            // Minnaert bubble from splash: radius ~ 0.5-2mm
            float bubbleRadiusMm = 0.5f + rng.nextFloat() * 1.5f;
            float decay = 100f + rng.nextFloat() * 150f;
            float amp = 0.01f + dropDiameterMm * 0.008f; // larger drops = louder
            addMinnaertBubble(out, start, bubbleRadiusMm, decay, amp, 0.03f, 998f);

            // Impact noise burst: short broadband click for each drop
            int burstLen = (int) (SAMPLE_RATE * (0.001f + dropDiameterMm * 0.0005f)); // 1-3.5ms
            float burstAmp = 0.005f + dropDiameterMm * 0.004f;
            for (int i = 0; i < burstLen && start + i < out.length; i++) {
                float env = burstAmp * (1f - (float) i / burstLen);
                out[start + i] += env * (rng.nextFloat() * 2f - 1f);
            }
        }
    }

    private void generateLava(float[] out, Random rng) {
        // Brownian noise base for deep viscous rumble
        float brownState = 0;
        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float white = rng.nextFloat() * 2f - 1f;
            brownState += white * 0.01f;
            brownState *= 0.999f;
            brownState = Math.max(-1f, Math.min(1f, brownState));
            out[i] = brownState * 0.3f;
            // Sub-bass drone at 20-60 Hz (two detuned tones)
            out[i] += 0.15f * (float) Math.sin(2 * Math.PI * 25 * t);
            out[i] += 0.10f * (float) Math.sin(2 * Math.PI * 38 * t);
            out[i] += 0.06f * (float) Math.sin(2 * Math.PI * 55 * t);
        }

        // Minnaert bubbles through magma (rho=2500 kg/m^3)
        // Radius 5-50mm, longer durations (decay 5-20 → 50-200ms)
        int numBubbles = BUFFER_SECONDS * 300;
        for (int b = 0; b < numBubbles; b++) {
            int start = rng.nextInt(out.length);
            // Log-normal radius: mean ~15mm, sigma 0.6 for wide spread
            float radiusMm = (float) Math.exp(Math.log(15.0) + 0.6 * rng.nextGaussian());
            radiusMm = Math.max(8f, Math.min(radiusMm, 25f));
            // Slower decay for viscous magma: 5-20 (50-200ms duration)
            float decay = 5f + rng.nextFloat() * 15f;
            float amp = 0.03f + rng.nextFloat() * 0.06f;
            // Slight chirp from bubble deformation in viscous fluid
            addMinnaertBubble(out, start, radiusMm, decay, amp, 0.03f, 2500f);
        }
    }

    /**
     * Generate a single Minnaert bubble resonance into the output buffer.
     * Models the natural resonance frequency of a gas bubble in liquid,
     * based on the Minnaert equation: f = (1/2*pi*a) * sqrt(3*gamma*P0/rho).
     *
     * @param out       output array (samples are additively mixed)
     * @param start     sample offset where the bubble begins
     * @param radiusMm  bubble radius in mm (determines frequency via Minnaert equation)
     * @param decay     exponential decay rate (higher = shorter; 50-200 for water, 5-20 for lava)
     * @param amplitude peak amplitude
     * @param chirpRate pitch drop rate (0 = no chirp, 0.05 = ~5% over duration)
     * @param rho       fluid density in kg/m^3 (998 for water, 2500 for lava)
     */
    private void addMinnaertBubble(float[] out, int start, float radiusMm,
                                    float decay, float amplitude, float chirpRate, float rho) {
        // Minnaert equation: f = (1/(2*pi*a)) * sqrt(3 * gamma * P0 / rho)
        // gamma = 1.4 (air adiabatic index), P0 = 101325 Pa (atmospheric pressure)
        float a = radiusMm / 1000f;
        float freq = (float) (Math.sqrt(3.0 * 1.4 * 101325.0 / rho) / (2.0 * Math.PI * a));
        int dur = Math.min((int) (5f / decay * SAMPLE_RATE), SAMPLE_RATE); // cap at 1 second
        for (int i = 0; i < dur && start + i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = amplitude * (float) Math.exp(-decay * t);
            float f = freq * (1f - chirpRate * t * decay / 30f);
            out[start + i] += env * (float) Math.sin(2.0 * Math.PI * f * t);
        }
    }

    /**
     * Generate a unique bird call buffer. Called per trigger, not cached.
     * Each invocation produces a distinct call due to ODE sensitivity
     * and random variation in the syrinx model.
     *
     * @param out            output sample array (will be filled from index 0)
     * @param speciesVariant 0-7, selects bird species parameters
     * @param rng            random source for per-call variation
     */
    void generateBirdCall(float[] out, int speciesVariant, Random rng) {
        int written = SyrinxSynth.generate(out, speciesVariant, rng);
        // Silence any remaining samples
        for (int i = written; i < out.length; i++) out[i] = 0f;
    }

    /**
     * Generate a cricket chirp buffer using DDS sine oscillator with Dolbear's law
     * temperature coupling. Produces species-specific carrier frequency with AM
     * envelope creating square-ish pulse trains grouped into chirps.
     *
     * @param out         output sample array
     * @param rng         random source for per-call variation
     * @param temperature Minecraft biome temperature (0-2 range)
     * @param sampleCount number of samples to fill
     */
    void generateCricketChirp(float[] out, Random rng, float temperature, int sampleCount) {
        // Species-specific carrier frequency: 2-5 kHz
        float carrierFreq = 2000f + rng.nextFloat() * 3000f;
        int pulsesPerChirp = 3 + rng.nextInt(6); // 3-8 pulses

        // Dolbear's law: convert Minecraft temperature to chirp rate
        // MC temp 0-2 range -> approx Celsius: tempC = temperature * 30 - 5
        float tempC = temperature * 30f - 5f;
        float tempF = tempC * 9f / 5f + 32f;
        float chirpsPerMin = Math.max(0f, tempF - 40f);

        // At cold temperatures (< 0.25 MC temp), produce very sparse or silent buffer
        if (temperature < 0.25f) {
            for (int i = 0; i < sampleCount && i < out.length; i++) {
                out[i] = 0f;
            }
            return;
        }

        // Chirp interval in seconds
        float chirpInterval = chirpsPerMin > 0.5f ? 60f / chirpsPerMin : 999f;

        // Pulse parameters: ~17 pulses/sec at 25C, scaled by temperature
        float pulseRate = 10f + tempC * 0.28f; // roughly 17 at 25C
        pulseRate = Math.max(5f, Math.min(pulseRate, 25f));
        float pulsePeriod = 1f / pulseRate;
        float pulseDuration = pulsePeriod * 0.6f; // 60% duty cycle for square-ish pulses

        // Attack/release time for AM envelope: ~2ms
        float attackRelease = 0.002f;
        int attackReleaseSamples = (int) (attackRelease * SAMPLE_RATE);

        // DDS phase accumulator
        float phase = 0f;
        float phaseInc = carrierFreq / SAMPLE_RATE;

        // Generate chirp pattern
        float bufferDuration = (float) sampleCount / SAMPLE_RATE;
        float time = 0f;
        float nextChirpTime = rng.nextFloat() * 0.5f; // random initial offset
        float currentChirpAmp = 0.7f + rng.nextFloat() * 0.3f; // per-chirp amplitude

        // Slight frequency variation per individual (+/- 2%)
        float freqVariation = 1f + (rng.nextFloat() - 0.5f) * 0.04f;
        phaseInc *= freqVariation;

        // Perlin envelope for slow amplitude drift (simulates distance/orientation changes)
        PerlinNoise ampDrift = new PerlinNoise(rng.nextLong());

        for (int i = 0; i < sampleCount && i < out.length; i++) {
            time = (float) i / SAMPLE_RATE;
            float sample = 0f;

            // Slow amplitude modulation: 0.3-1.0 range, ~0.2 Hz drift
            float driftAmp = 0.3f + 0.7f * (0.5f + 0.5f * ampDrift.sample(time * 0.2f));

            // Determine if we're inside a chirp
            float timeSinceChirp = time - nextChirpTime;

            if (timeSinceChirp >= 0) {
                float chirpDuration = pulsesPerChirp * pulsePeriod;

                if (timeSinceChirp < chirpDuration) {
                    // Inside a chirp — determine which pulse we're in
                    float timeInChirp = timeSinceChirp;
                    float timeInPulse = timeInChirp % pulsePeriod;

                    if (timeInPulse < pulseDuration) {
                        // Inside a pulse — apply AM envelope
                        float env = 1f;
                        int sampleInPulse = (int) (timeInPulse * SAMPLE_RATE);
                        int pulseDurSamples = (int) (pulseDuration * SAMPLE_RATE);

                        // Attack ramp
                        if (sampleInPulse < attackReleaseSamples) {
                            env = (float) sampleInPulse / attackReleaseSamples;
                        }
                        // Release ramp
                        int samplesFromEnd = pulseDurSamples - sampleInPulse;
                        if (samplesFromEnd < attackReleaseSamples) {
                            env = Math.min(env, (float) samplesFromEnd / attackReleaseSamples);
                        }

                        // DDS sine oscillator with per-chirp amplitude variation
                        sample = (float) Math.sin(2.0 * Math.PI * phase) * env * currentChirpAmp;
                    }
                } else {
                    // Chirp finished — schedule next one with jitter and new amplitude
                    nextChirpTime += chirpInterval + (rng.nextFloat() - 0.5f) * chirpInterval * 0.15f;
                    currentChirpAmp = 0.4f + rng.nextFloat() * 0.6f; // 0.4-1.0 per chirp
                }
            }

            phase += phaseInc;
            if (phase > 1f) phase -= 1f;

            out[i] = sample * driftAmp * 0.75f;
        }
    }

    /**
     * Generate a cicada drone buffer using a simplified Smyth-Smith tymbal model.
     * Produces rapid impulse train excitation through a resonant bandpass filter
     * with characteristic AM tremolo.
     *
     * @param out         output sample array
     * @param rng         random source for per-call variation
     * @param temperature Minecraft biome temperature (0-2 range)
     * @param sampleCount number of samples to fill
     */
    void generateCicadaDrone(float[] out, Random rng, float temperature, int sampleCount) {
        // Cicadas only active in warm temperatures (> 0.6)
        if (temperature < 0.4f) {
            for (int i = 0; i < sampleCount && i < out.length; i++) {
                out[i] = 0f;
            }
            return;
        }

        // Volume ramp: fade in between 0.4 and 0.6
        float volumeScale;
        if (temperature < 0.6f) {
            volumeScale = ((temperature - 0.4f) / 0.2f) * 0.1f;
        } else {
            volumeScale = 0.1f + (Math.min(temperature, 1.5f) - 0.6f) / 0.9f * 0.9f;
        }

        // Species parameters
        float impulseRate = 100f + rng.nextFloat() * 100f;     // 100-200 Hz tymbal click rate
        float resonantFreq = 4000f + rng.nextFloat() * 6000f;  // 4-10 kHz body resonance
        float tremoloRate = 8f + rng.nextFloat() * 4f;          // 8-12 Hz AM tremolo
        float tremoloDepth = 0.4f + rng.nextFloat() * 0.3f;     // 40-70% modulation depth

        // Two-pole bandpass filter coefficients (Smyth-Smith body resonator)
        float w0 = (float) (2.0 * Math.PI * resonantFreq / SAMPLE_RATE);
        float Q = 8f + rng.nextFloat() * 7f; // Q = 8-15 for tymbal resonance
        float alpha = (float) (Math.sin(w0) / (2.0 * Q));
        float cosW0 = (float) Math.cos(w0);

        float b0 = alpha;
        float a1 = -2f * cosW0;
        float a2 = 1f - alpha;
        float a0 = 1f + alpha;

        // Normalize coefficients
        b0 /= a0;
        a1 /= a0;
        a2 /= a0;

        // Filter state
        float z1 = 0f, z2 = 0f;

        // Impulse train phase accumulator
        float impulsePhase = 0f;
        float impulsePhaseInc = impulseRate / SAMPLE_RATE;

        // Tremolo phase
        float tremoloPhase = rng.nextFloat(); // random start phase

        // Slight frequency drift over time (Perlin-driven)
        PerlinNoise drift = new PerlinNoise(rng.nextLong());

        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;

            // Generate impulse train excitation
            float excitation = 0f;
            float prevPhase = impulsePhase;
            impulsePhase += impulsePhaseInc;
            if (impulsePhase >= 1f) {
                impulsePhase -= 1f;
                // Sharp impulse with slight noise variation
                excitation = 0.8f + rng.nextFloat() * 0.4f;
            }

            // Add slight noise floor to excitation (tymbal membrane noise)
            excitation += (rng.nextFloat() * 2f - 1f) * 0.02f;

            // Apply two-pole bandpass filter (body resonator)
            float filtered = b0 * excitation - a1 * z1 - a2 * z2;
            z2 = z1;
            z1 = filtered;

            // Clamp to prevent filter blowup
            z1 = Math.max(-4f, Math.min(4f, z1));
            z2 = Math.max(-4f, Math.min(4f, z2));

            // AM tremolo: characteristic cicada pulsing
            tremoloPhase += tremoloRate / SAMPLE_RATE;
            if (tremoloPhase > 1f) tremoloPhase -= 1f;
            float tremolo = 1f - tremoloDepth
                    * (0.5f + 0.5f * (float) Math.sin(2.0 * Math.PI * tremoloPhase));

            // Slight resonant frequency drift (+/- 3%)
            float freqDrift = 1f + drift.sample(t * 0.5f) * 0.03f;
            // We approximate drift by modulating impulse rate slightly
            impulsePhaseInc = (impulseRate * freqDrift) / SAMPLE_RATE;

            out[i] = filtered * tremolo * volumeScale * 0.5f;
        }

        // Apply crossfade for seamless looping (first/last 100ms)
        int fadeSamples = Math.min((int) (0.1f * SAMPLE_RATE), sampleCount / 4);
        for (int i = 0; i < fadeSamples; i++) {
            float fade = (float) i / fadeSamples;
            // Fade in at start
            out[i] *= fade;
            // Fade out at end, blend with start
            int endIdx = sampleCount - fadeSamples + i;
            if (endIdx >= 0 && endIdx < out.length) {
                out[endIdx] = out[endIdx] * (1f - fade) + out[i] * fade;
            }
        }
    }

    /**
     * Generate a frog call using a sine bank with descending glissando (whine)
     * and optional harmonic burst (chuck). Models tungara frog vocal sac resonance.
     *
     * @param out         output sample array
     * @param rng         random source for per-call variation
     * @param sampleCount number of samples to fill
     */
    void generateFrogCall(float[] out, Random rng, int sampleCount) {
        float fundamental = 500f + rng.nextFloat() * 500f; // 500-1000 Hz
        float whineDuration = 0.3f + rng.nextFloat() * 0.2f; // 300-500ms
        boolean hasChuck = rng.nextFloat() < 0.3f;

        int whineSamples = (int) (whineDuration * SAMPLE_RATE);

        // Vocal sac resonance Q (consumed from rng for deterministic sequence;
        // reserved for future bandpass filter enhancement)
        @SuppressWarnings("unused")
        float resonanceQ = 5f + rng.nextFloat() * 5f; // Q = 5-10

        // Phase accumulators for harmonic sine bank (avoids phase discontinuity in glissando)
        float phase1 = 0f, phase2 = 0f, phase3 = 0f, phase4 = 0f;

        // --- Whine: descending glissando with sine bank ---
        for (int i = 0; i < whineSamples && i < sampleCount && i < out.length; i++) {
            float progress = (float) i / whineSamples;

            // Descending glissando: exponential pitch drop of ~20%
            float freq = fundamental * (1.0f - 0.2f * progress * progress);

            // Phase increments for each harmonic
            float inc1 = freq / SAMPLE_RATE;
            float inc2 = freq * 2f / SAMPLE_RATE;
            float inc3 = freq * 3f / SAMPLE_RATE;
            float inc4 = freq * 4f / SAMPLE_RATE;

            phase1 += inc1;
            phase2 += inc2;
            phase3 += inc3;
            phase4 += inc4;

            // Sine bank with harmonics (decreasing amplitude)
            float sample = 0f;
            sample += (float) Math.sin(2.0 * Math.PI * phase1) * 1.0f;   // fundamental
            sample += (float) Math.sin(2.0 * Math.PI * phase2) * 0.6f;   // 2nd harmonic
            sample += (float) Math.sin(2.0 * Math.PI * phase3) * 0.3f;   // 3rd harmonic
            sample += (float) Math.sin(2.0 * Math.PI * phase4) * 0.15f;  // 4th harmonic

            // Amplitude envelope: attack ~10ms, sustain, decay last 20%
            float env = 1f;
            if (progress < 0.03f) {
                env = progress / 0.03f;
            } else if (progress > 0.8f) {
                env = (1f - progress) / 0.2f;
            }

            // Simple vocal sac resonance: boost around fundamental
            // (The sine bank already emphasizes the fundamental; we just shape the envelope)
            out[i] = sample * env * 0.3f;
        }

        // --- Chuck: harmonic burst after whine (30% chance) ---
        if (hasChuck) {
            float chuckFreq = 200f + rng.nextFloat() * 50f; // 200-250 Hz
            int chuckStart = whineSamples;
            int chuckDur = (int) (0.05f * SAMPLE_RATE); // ~50ms

            // Phase accumulators for chuck harmonics
            float cPhase1 = 0f, cPhase2 = 0f, cPhase3 = 0f;

            for (int i = 0; i < chuckDur && chuckStart + i < sampleCount && chuckStart + i < out.length; i++) {
                float t = (float) i / SAMPLE_RATE;

                // Sharp exponential decay
                float env = (float) Math.exp(-40.0 * t);

                float cInc1 = chuckFreq / SAMPLE_RATE;
                float cInc2 = chuckFreq * 2f / SAMPLE_RATE;
                float cInc3 = chuckFreq * 3f / SAMPLE_RATE;

                cPhase1 += cInc1;
                cPhase2 += cInc2;
                cPhase3 += cInc3;

                float sample = 0f;
                sample += (float) Math.sin(2.0 * Math.PI * cPhase1);
                sample += 0.7f * (float) Math.sin(2.0 * Math.PI * cPhase2);
                sample += 0.4f * (float) Math.sin(2.0 * Math.PI * cPhase3);

                out[chuckStart + i] = sample * env * 0.4f;
            }
        }

        // Zero out remaining buffer
        int totalUsed = whineSamples + (hasChuck ? (int) (0.05f * SAMPLE_RATE) : 0);
        for (int i = totalUsed; i < sampleCount && i < out.length; i++) {
            out[i] = 0f;
        }
    }

    /**
     * Generate a cave ambient buffer with geometry-coupled resonance.
     * Models sub-bass room modes, sparse Minnaert drips, and distant wind moan.
     * Each invocation produces a unique buffer due to stochastic drip placement.
     *
     * @param out         output sample array
     * @param rng         random source for per-call variation
     * @param roomSize    estimated room size in blocks (meters), affects fundamental resonance
     * @param sampleCount number of samples to fill
     */
    void generateCaveAmbient(float[] out, Random rng, float roomSize, int sampleCount) {
        // Clamp room size to a sensible range
        roomSize = Math.max(3f, Math.min(roomSize, 64f));

        // --- Layer 1: Brown noise base with resonant peaks at room modes ---
        // Fundamental frequency from standing wave: f = speed_of_sound / (2 * roomSize)
        float speedOfSound = 343f;
        float fundamental = speedOfSound / (2f * roomSize);

        // Brown noise base — reduced level so resonant harmonics and drips aren't
        // buried. Previous 0.04 amplitude concentrated 85% of energy below 30Hz,
        // creating an inaudible sub-bass rumble. Reduced to 0.015 to shift spectral
        // balance toward audible mid-frequency content (drips, wind moan, presence).
        float brownState = 0f;
        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float white = rng.nextFloat() * 2f - 1f;
            brownState += white * 0.012f;
            brownState *= 0.9997f;
            brownState = Math.max(-1f, Math.min(1f, brownState));
            out[i] = brownState * 0.015f;
        }

        // Apply resonant bandpass harmonics (3-4 harmonics of the fundamental)
        // Each harmonic gets its own two-pole bandpass filter pass
        int numHarmonics = (fundamental < 20f) ? 4 : 3; // more harmonics for very low fundamentals
        for (int h = 1; h <= numHarmonics; h++) {
            float harmFreq = fundamental * h;
            // Skip harmonics above Nyquist / 2
            if (harmFreq > SAMPLE_RATE / 2f) break;

            // Decreasing Q and amplitude per harmonic
            float Q = (12f / h) + 2f; // Q: ~14, ~8, ~6, ~5
            float harmAmp = 0.08f / h; // reduced from 0.20/h — less sub-bass resonance dominance

            // Two-pole bandpass filter coefficients
            float w0 = (float) (2.0 * Math.PI * harmFreq / SAMPLE_RATE);
            float alpha = (float) (Math.sin(w0) / (2.0 * Q));
            float cosW0 = (float) Math.cos(w0);

            float b0 = alpha;
            float a1 = -2f * cosW0;
            float a2 = 1f - alpha;
            float a0 = 1f + alpha;
            b0 /= a0;
            a1 /= a0;
            a2 /= a0;

            float z1 = 0f, z2 = 0f;

            for (int i = 0; i < sampleCount && i < out.length; i++) {
                float in = out[i];
                float filtered = b0 * in - a1 * z1 - a2 * z2;
                z2 = z1;
                z1 = filtered;
                z1 = Math.max(-4f, Math.min(4f, z1));
                z2 = Math.max(-4f, Math.min(4f, z2));
                // Mix resonant peak back into output additively
                out[i] += filtered * harmAmp;
            }
        }

        // --- Layer 2: Sparse drip events using Minnaert bubbles ---
        // Rate: 0.5-2 drips per second
        float durationSec = (float) sampleCount / SAMPLE_RATE;
        float dripRate = 0.5f + rng.nextFloat() * 1.5f; // 0.5-2 per second
        int numDrips = Math.max(1, (int) (dripRate * durationSec));

        for (int d = 0; d < numDrips; d++) {
            int start = rng.nextInt(sampleCount);
            // Drip bubbles: radius 1-3mm -> frequency ~1-3 kHz
            // Boosted amplitude so drip transients pop above the spatial field
            // noise floor. Brief events don't raise IC significantly.
            float radiusMm = 1f + rng.nextFloat() * 2f;
            float decay = 60f + rng.nextFloat() * 140f; // 60-200 decay (fast, sharp drip)
            float amp = 0.50f + rng.nextFloat() * 0.30f;
            addMinnaertBubble(out, start, radiusMm, decay, amp, 0.04f, 998f);
        }

        // --- Layer 3: Distant wind moan (filtered noise with slow LFO) ---
        PerlinNoise windLfo = new PerlinNoise(rng.nextLong());
        float lfoPeriod = 2f + rng.nextFloat() * 2f; // 2-4 seconds
        float lfoFreq = 1f / lfoPeriod;

        // Wind moan bandpass: 80-300 Hz (eerie low moan)
        float moanCenterFreq = 80f + rng.nextFloat() * 120f; // 80-200 Hz center
        float moanW0 = (float) (2.0 * Math.PI * moanCenterFreq / SAMPLE_RATE);
        float moanQ = 3f + rng.nextFloat() * 2f;
        float moanAlpha = (float) (Math.sin(moanW0) / (2.0 * moanQ));
        float moanCosW0 = (float) Math.cos(moanW0);

        float mb0 = moanAlpha / (1f + moanAlpha);
        float ma1 = -2f * moanCosW0 / (1f + moanAlpha);
        float ma2 = (1f - moanAlpha) / (1f + moanAlpha);

        float mz1 = 0f, mz2 = 0f;

        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float white = rng.nextFloat() * 2f - 1f;

            // Bandpass filter the noise
            float filtered = mb0 * white - ma1 * mz1 - ma2 * mz2;
            mz2 = mz1;
            mz1 = filtered;
            mz1 = Math.max(-4f, Math.min(4f, mz1));
            mz2 = Math.max(-4f, Math.min(4f, mz2));

            // LFO amplitude modulation
            float lfo = 0.5f + 0.5f * windLfo.sample(t * lfoFreq);
            out[i] += filtered * lfo * 0.10f; // increased from 0.06 for audible wind moan
        }

        // --- Layer 4: Mid-frequency "cave presence" (800-3000 Hz) ---
        // Subtle bandlimited noise in the mid-frequency range where ILD works.
        // This gives the spatial processor something to decorrelate, since
        // low-frequency content (< 300 Hz) produces near-zero ILD (head too small
        // relative to wavelength). Without this, cave IC approaches 1.0 (mono).
        PerlinNoise midLfo = new PerlinNoise(rng.nextLong());
        float midCenter = 1200f + rng.nextFloat() * 800f; // 1200-2000 Hz
        float midW0 = (float) (2.0 * Math.PI * midCenter / SAMPLE_RATE);
        float midQ = 2f + rng.nextFloat() * 2f; // Q 2-4 (wide)
        float midAlpha = (float) (Math.sin(midW0) / (2.0 * midQ));
        float midCosW0 = (float) Math.cos(midW0);

        float midb0 = midAlpha / (1f + midAlpha);
        float mida1 = -2f * midCosW0 / (1f + midAlpha);
        float mida2 = (1f - midAlpha) / (1f + midAlpha);

        float midz1 = 0f, midz2 = 0f;

        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float white = rng.nextFloat() * 2f - 1f;
            float filtered = midb0 * white - mida1 * midz1 - mida2 * midz2;
            midz2 = midz1;
            midz1 = filtered;
            midz1 = Math.max(-4f, Math.min(4f, midz1));
            midz2 = Math.max(-4f, Math.min(4f, midz2));

            // Slow modulation (0.3-0.8 Hz) for natural variation
            float lfo = 0.4f + 0.6f * midLfo.sample(t * (0.3f + rng.nextFloat() * 0.5f));
            out[i] += filtered * lfo * 0.08f; // increased from 0.03 — needs to be audible for ILD-based spatialization
        }

        // DC blocker: brown noise random walk accumulates DC offset.
        // Single-pole highpass at ~10 Hz removes it without affecting audible content.
        float dcR = 1.0f - (2.0f * (float) Math.PI * 10f / SAMPLE_RATE);
        float dcPrevIn = 0f, dcPrevOut = 0f;
        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float in = out[i];
            dcPrevOut = in - dcPrevIn + dcR * dcPrevOut;
            dcPrevIn = in;
            out[i] = dcPrevOut;
        }

        // Zero out any remaining buffer
        for (int i = sampleCount; i < out.length; i++) {
            out[i] = 0f;
        }
    }

    /**
     * Generate a continuous cave drone buffer for room tone.
     * Brown noise filtered to sub-300Hz with slow Perlin modulation.
     * This provides the constant acoustic floor of an enclosed space —
     * air movement, structural resonance, distant geological activity.
     */
    private void generateCaveDrone(float[] out, Random rng) {
        PerlinNoise ampMod = new PerlinNoise(rng.nextLong());

        // Pink noise base — wider bandwidth than brown noise to include
        // mid-frequencies (800-3000Hz) where HRTF ILD cues work.
        // This is critical: sub-300Hz content is inherently mono (wavelength >> head size),
        // so a drone that's only low-freq causes IC collapse.
        // Real cave ambience spans 50Hz-4kHz (air currents, resonances, distant activity).

        // Voss-McCartney pink noise: sum of 8 octave-band random walks
        float[] octaveStates = new float[8];
        int[] octavePeriods = {1, 2, 4, 8, 16, 32, 64, 128};
        for (int o = 0; o < 8; o++) {
            octaveStates[o] = rng.nextFloat() * 2f - 1f;
        }

        // 4-pole (24 dB/oct) lowpass at ~3.5kHz — creates a visible spectral knee
        // that gives caves their characteristic dark, enclosed sound. Extends into the
        // 2-4kHz HRTF ILD region (ear canal resonance, up to 20dB ILD) so spatialization
        // still works, but rolls off steeply above to prevent the 8kHz artifact.
        float lpFreq = 3000f + rng.nextFloat() * 1500f; // 3000-4500 Hz
        float lpA = lpFreq / (lpFreq + SAMPLE_RATE / (2f * (float) Math.PI));
        float lp1 = 0f, lp2 = 0f, lp3 = 0f, lp4 = 0f;

        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;

            // Update octave-band random walks
            float sum = 0f;
            for (int o = 0; o < 8; o++) {
                if (i % octavePeriods[o] == 0) {
                    octaveStates[o] = rng.nextFloat() * 2f - 1f;
                }
                sum += octaveStates[o];
            }
            float pink = sum / 8f;

            // 4-pole cascade lowpass (24 dB/oct) — first 2-pole section
            lp1 += (pink - lp1) * lpA;
            lp2 += (lp1 - lp2) * lpA;
            // Second 2-pole section
            lp3 += (lp2 - lp3) * lpA;
            lp4 += (lp3 - lp4) * lpA;

            // Slow amplitude modulation (0.1-0.3 Hz) — like breathing air currents
            float amp = 0.5f + 0.5f * ampMod.sample(t * 0.15f);

            out[i] = lp4 * amp * 0.10f;
        }

        // DC blocker: single-pole highpass at ~10 Hz removes any DC offset
        // accumulated through the random walk and lowpass filtering.
        // Transfer function: y[n] = x[n] - x[n-1] + R*y[n-1], R ≈ 0.9993
        float R = 1.0f - (2.0f * (float) Math.PI * 10f / SAMPLE_RATE);
        float prevIn = 0f, prevOut = 0f;
        for (int i = 0; i < out.length; i++) {
            float in = out[i];
            prevOut = in - prevIn + R * prevOut;
            prevIn = in;
            out[i] = prevOut;
        }
    }

    /**
     * Generate fire crackle buffer using Andy Farnell's 3-component fire model:
     * crackle (Poisson impulses), hiss (HP filtered noise), and flame lap (broadband AM).
     * Suitable for furnace/campfire emitters.
     *
     * @param out         output sample array
     * @param rng         random source for per-call variation
     * @param sampleCount number of samples to fill
     */
    void generateFireCrackle(float[] out, Random rng, int sampleCount) {
        float durationSec = (float) sampleCount / SAMPLE_RATE;

        // === Component 1: Crackle — stochastic Poisson impulses ===
        // Each impulse is a short bandpass noise burst, 5-30ms, 100-1000 Hz
        float crackleRate = 5f + rng.nextFloat() * 10f; // 5-15 impulses/sec
        int numCrackles = Math.max(1, (int) (crackleRate * durationSec));

        for (int c = 0; c < numCrackles; c++) {
            int start = rng.nextInt(sampleCount);
            // Burst duration: 5-30ms
            float burstDurSec = 0.005f + rng.nextFloat() * 0.025f;
            int burstLen = (int) (burstDurSec * SAMPLE_RATE);
            // Random center frequency for this crackle: 100-1000 Hz
            float centerFreq = 100f + rng.nextFloat() * 900f;
            float burstAmp = 0.15f + rng.nextFloat() * 0.25f;

            // Simple two-pole bandpass for each burst
            float bpW0 = (float) (2.0 * Math.PI * centerFreq / SAMPLE_RATE);
            float bpQ = 3f + rng.nextFloat() * 4f; // Q = 3-7
            float bpAlpha = (float) (Math.sin(bpW0) / (2.0 * bpQ));
            float bpCosW0 = (float) Math.cos(bpW0);

            float cb0 = bpAlpha / (1f + bpAlpha);
            float ca1 = -2f * bpCosW0 / (1f + bpAlpha);
            float ca2 = (1f - bpAlpha) / (1f + bpAlpha);

            float cz1 = 0f, cz2 = 0f;

            for (int i = 0; i < burstLen && start + i < sampleCount && start + i < out.length; i++) {
                float progress = (float) i / burstLen;
                // Hann window for smooth burst envelope
                float window = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * progress));
                float white = rng.nextFloat() * 2f - 1f;

                // Bandpass filter the noise
                float filtered = cb0 * white - ca1 * cz1 - ca2 * cz2;
                cz2 = cz1;
                cz1 = filtered;
                cz1 = Math.max(-2f, Math.min(2f, cz1));
                cz2 = Math.max(-2f, Math.min(2f, cz2));

                out[start + i] += filtered * window * burstAmp;
            }
        }

        // === Component 2: Hiss — continuous high-pass filtered white noise ===
        // HP cutoff ~3000 Hz, low amplitude
        float hpCutoff = 3000f;
        float hpAlpha = hpCutoff / (hpCutoff + SAMPLE_RATE / (2f * (float) Math.PI));
        float hpState = 0f;

        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float white = rng.nextFloat() * 2f - 1f;
            hpState += (white - hpState) * hpAlpha;
            float hp = white - hpState; // high-pass output
            out[i] += hp * 0.04f; // low amplitude hiss
        }

        // === Component 3: Flame lap — slow broadband AM using Perlin noise ===
        // Modulation at 1-3 Hz, affects the entire mix
        PerlinNoise flameLfo = new PerlinNoise(rng.nextLong());
        float lapRate = 1f + rng.nextFloat() * 2f; // 1-3 Hz

        for (int i = 0; i < sampleCount && i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            // Perlin noise gives smooth, natural-sounding modulation (better than sine)
            float mod = 0.55f + 0.45f * flameLfo.sample(t * lapRate);
            out[i] *= mod;
        }

        // Zero out any remaining buffer
        for (int i = sampleCount; i < out.length; i++) {
            out[i] = 0f;
        }
    }

    private void generateWhiteNoise(float[] out, Random rng) {
        for (int i = 0; i < out.length; i++) {
            out[i] = (rng.nextFloat() * 2f - 1f) * 0.3f;
        }
    }

    private void applyCrossfade(float[] out) {
        int len = SAMPLE_RATE / 5; // 200ms for smoother loop
        for (int i = 0; i < len; i++) {
            float t = (float) i / len;
            // Equal-power: use sin/cos for constant energy
            float fadeOut = (float) Math.cos(t * Math.PI * 0.5);
            float fadeIn = (float) Math.sin(t * Math.PI * 0.5);
            out[out.length - len + i] =
                    out[out.length - len + i] * fadeOut + out[i] * fadeIn;
        }
    }

    private int createBuffer(float[] samples, String name) {
        float peak = 0.001f;
        for (float s : samples) peak = Math.max(peak, Math.abs(s));
        float norm = 0.75f / peak;

        ByteBuffer data = ByteBuffer.allocateDirect(BUFFER_SAMPLES * 2)
                .order(ByteOrder.nativeOrder());
        for (float s : samples) {
            data.putShort((short) (s * norm * Short.MAX_VALUE));
        }
        data.flip();

        int buf = AL10.alGenBuffers();
        AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, data, SAMPLE_RATE);
        Loggers.log("ProceduralBufferFactory: generated buffer for {}", name);
        return buf;
    }

    /**
     * Create a one-shot OpenAL buffer from pre-generated samples. Not cached.
     * Used by EmitterManager for triggered (sample-based) emitters where each
     * trigger produces a unique buffer.
     *
     * @param samples     audio data (may be longer than sampleCount)
     * @param sampleCount number of valid samples to use
     * @return OpenAL buffer ID
     */
    public int createTriggeredBuffer(float[] samples, int sampleCount) {
        // Normalize
        float peak = 0.001f;
        for (int i = 0; i < sampleCount; i++) peak = Math.max(peak, Math.abs(samples[i]));
        float norm = 0.75f / peak;

        // Reuse a single direct ByteBuffer to avoid off-heap memory pressure
        int needed = sampleCount * 2;
        if (reusableBuf == null || reusableBufCapacity < needed) {
            reusableBuf = ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder());
            reusableBufCapacity = needed;
        }
        reusableBuf.clear();
        for (int i = 0; i < sampleCount; i++) {
            reusableBuf.putShort((short) (samples[i] * norm * Short.MAX_VALUE));
        }
        reusableBuf.flip();

        int buf = AL10.alGenBuffers();
        AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, reusableBuf, SAMPLE_RATE);
        return buf;
    }

    public void shutdown() {
        for (int[] ids : buffers.values()) {
            for (int buf : ids) {
                if (buf != 0) AL10.alDeleteBuffers(buf);
            }
        }
        buffers.clear();
    }
}
