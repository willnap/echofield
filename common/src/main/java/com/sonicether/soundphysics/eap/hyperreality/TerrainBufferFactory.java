package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.emitter.PerlinNoise;

<<<<<<< ours
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
=======
>>>>>>> theirs
import java.util.Random;

/**
 * Generates procedural audio buffers for the hyperreality terrain layer.
 * 3 families (VOID, SURFACE, GROUND) x 4 variants = 12 mono 16-bit PCM buffers
 * at 44,100 Hz, 6 seconds each (264,600 samples). Total memory: ~6.35 MB.
 *
 * <p>Each buffer uses seeded RNG for deterministic generation. Buffers are
 * equal-power crossfaded over the final 100ms for seamless looping.
 *
 * <p>All static synthesis methods operate on float[] for testability without OpenAL.
 * The {@link #init()} and {@link #shutdown()} methods manage OpenAL buffer upload/deletion.
 */
public final class TerrainBufferFactory {

    public static final int SAMPLE_RATE = 44100;
    public static final int BUFFER_SECONDS = 6;
    public static final int BUFFER_SAMPLES = SAMPLE_RATE * BUFFER_SECONDS; // 264600
    public static final int CROSSFADE_SAMPLES = 4410; // 100ms
    public static final int VARIANTS_PER_FAMILY = 4;

<<<<<<< ours
    /** Family count — matches {@link TerrainFeatureType.Family} ordinals: VOID=0, SURFACE=1, GROUND=2. */
    private static final int FAMILY_COUNT = 3;

    /** Family indices matching TerrainFeatureType.Family ordinal values. */
    private static final int FAMILY_VOID = 0;
    private static final int FAMILY_SURFACE = 1;
    private static final int FAMILY_GROUND = 2;

    /** [3 families][4 variants] — OpenAL buffer IDs, populated by {@link #init()}. */
    private final int[][] bufferIds = new int[FAMILY_COUNT][VARIANTS_PER_FAMILY];

    private boolean initialized = false;

    // ---- Pure DSP utilities (static, testable without OpenAL) ----
=======
    /** [3 families][4 variants] — OpenAL buffer IDs, populated by {@link #init()}. */
    private final int[][] bufferIds = new int[3][VARIANTS_PER_FAMILY];
>>>>>>> theirs

    /**
     * Normalizes samples so the peak absolute value equals {@code ceiling}.
     * If all samples are zero, no scaling is applied.
     *
     * @param samples the sample buffer to normalize in-place
     * @param ceiling the target peak amplitude (e.g., 0.7)
     */
    static void normalize(float[] samples, float ceiling) {
        float maxAbs = 0f;
        for (float s : samples) {
            float abs = Math.abs(s);
            if (abs > maxAbs) maxAbs = abs;
        }
        if (maxAbs > 0f) {
            float scale = ceiling / maxAbs;
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= scale;
            }
        }
    }

    /**
     * Applies an equal-power crossfade over the final {@code crossfadeSamples} of the buffer,
     * blending the end into the start for seamless looping.
     *
     * <p>Uses cos/sin curves so that power (amplitude squared) remains constant across the fade:
     * {@code fadeOut = cos(t * pi/2)}, {@code fadeIn = sin(t * pi/2)}.
     *
     * @param samples          the sample buffer to crossfade in-place
     * @param crossfadeSamples number of samples in the crossfade region
     */
    static void crossfade(float[] samples, int crossfadeSamples) {
        for (int i = 0; i < crossfadeSamples; i++) {
            float t = (float) i / crossfadeSamples;
            float fadeOut = (float) Math.cos(t * Math.PI * 0.5);
            float fadeIn = (float) Math.sin(t * Math.PI * 0.5);
            int endIdx = samples.length - crossfadeSamples + i;
            samples[endIdx] = samples[endIdx] * fadeOut + samples[i] * fadeIn;
        }
    }

    /**
     * Applies a biquad bandpass filter (Audio EQ Cookbook) in-place.
     *
     * <p>Transfer function: peak gain = 1 at center frequency, -3 dB bandwidth
     * determined by Q factor. Uses Direct Form I implementation.
     *
     * @param samples    sample buffer to filter in-place
     * @param centerHz   center frequency in Hz
     * @param q          quality factor (higher = narrower bandwidth)
     * @param sampleRate sample rate in Hz
     */
    static void applyBiquadBandpass(float[] samples, float centerHz, float q, int sampleRate) {
        float w0 = (float) (2.0 * Math.PI * centerHz / sampleRate);
        float sinW0 = (float) Math.sin(w0);
        float cosW0 = (float) Math.cos(w0);
        float alpha = sinW0 / (2.0f * q);

        // Bandpass coefficients (constant-0-dB-peak gain)
        float b0 = alpha;
        float b1 = 0f;
        float b2 = -alpha;
        float a0 = 1f + alpha;
        float a1 = -2f * cosW0;
        float a2 = 1f - alpha;

        // Normalize by a0
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;

        // Direct Form I
        float x1 = 0f, x2 = 0f;
        float y1 = 0f, y2 = 0f;
        for (int i = 0; i < samples.length; i++) {
            float x0 = samples[i];
            float y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
            samples[i] = y0;
        }
    }

    /**
     * Applies a 4-pole (24 dB/oct) lowpass filter using two cascaded 2-pole sections.
<<<<<<< ours
     * Uses a one-pole approximation pattern for efficient real-time filtering.
=======
     * Uses the same one-pole approximation pattern as {@code SpatialFieldProcessor}.
>>>>>>> theirs
     *
     * @param samples    sample buffer to filter in-place
     * @param cutoffHz   cutoff frequency in Hz
     * @param sampleRate sample rate in Hz
     */
    static void apply4PoleLowpass(float[] samples, float cutoffHz, int sampleRate) {
        float lpA = cutoffHz / (cutoffHz + sampleRate / (2f * (float) Math.PI));
        float lp1 = 0f, lp2 = 0f, lp3 = 0f, lp4 = 0f;
        for (int i = 0; i < samples.length; i++) {
            // First 2-pole section
            lp1 += (samples[i] - lp1) * lpA;
            lp2 += (lp1 - lp2) * lpA;
            // Second 2-pole section (cascade)
            lp3 += (lp2 - lp3) * lpA;
            lp4 += (lp3 - lp4) * lpA;
            samples[i] = lp4;
        }
    }

<<<<<<< ours
    // ---- Family-specific synthesis methods ----

=======
>>>>>>> theirs
    /**
     * Generates void family samples (for EDGE, DROP, PASSAGE features).
     *
     * <p>White noise -> biquad bandpass at 400 Hz (Q=2.0 for variants 0-1, Q=4.0 for 2-3)
     * -> Perlin-based amplitude modulation at 0.5 Hz +/-3 dB -> normalize to 0.7 -> crossfade.
     *
     * @param variantIndex 0-3, determines seed and Q factor
     * @return 264,600 float samples
     */
    static float[] generateVoidSamples(int variantIndex) {
<<<<<<< ours
        long seed = (long) FAMILY_VOID * 100003L + (long) variantIndex * 7919L;
=======
        // Deterministic seed: same formula used across all families
        int familyIndex = 0; // VOID
        long seed = (long) familyIndex * 100003L + (long) variantIndex * 7919L;
>>>>>>> theirs
        Random rng = new Random(seed);

        // Generate white noise
        float[] samples = new float[BUFFER_SAMPLES];
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            samples[i] = rng.nextFloat() * 2f - 1f;
        }

        // Biquad bandpass: center=400Hz, Q depends on variant
        float q = (variantIndex < 2) ? 2.0f : 4.0f;
        applyBiquadBandpass(samples, 400f, q, SAMPLE_RATE);

        // Amplitude modulation: Perlin noise at 0.5 Hz, +/-3 dB
<<<<<<< ours
        // +/-3 dB = factor of 10^(+/-3/20) = 0.708 to 1.413 around unity
=======
        // +/-3 dB = factor of 0.708 to 1.413 around unity
        // Using: gain = 10^(modDb/20) where modDb = noise * 3.0
>>>>>>> theirs
        PerlinNoise amNoise = new PerlinNoise(seed + 99991L);
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            float t = (float) i / SAMPLE_RATE;
            float noiseVal = amNoise.sample(t * 0.5f); // 0.5 Hz
            float modDb = noiseVal * 3.0f; // +/-3 dB
            float modGain = (float) Math.pow(10.0, modDb / 20.0);
            samples[i] *= modGain;
        }

        normalize(samples, 0.7f);
        crossfade(samples, CROSSFADE_SAMPLES);

        return samples;
    }

<<<<<<< ours
    /**
     * Generates surface family samples (for WALL, CEILING, SOLID_OBJECT features).
     *
     * <p>White noise -> 4-pole lowpass at 1000 Hz -> mix in 110 Hz sine at -18 dB
     * -> Perlin AM at 0.3 Hz +/-2 dB -> normalize to 0.7 -> crossfade.
     *
     * @param variantIndex 0-3, determines seed
     * @return 264,600 float samples
     */
    static float[] generateSurfaceSamples(int variantIndex) {
        long seed = (long) FAMILY_SURFACE * 100003L + (long) variantIndex * 7919L;
        Random rng = new Random(seed);

        // Generate white noise
        float[] samples = new float[BUFFER_SAMPLES];
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            samples[i] = rng.nextFloat() * 2f - 1f;
        }

        // 4-pole lowpass at 1000 Hz
        apply4PoleLowpass(samples, 1000f, SAMPLE_RATE);

        // Mix in sine tone at 110 Hz, -18 dB relative
        // -18 dB = 10^(-18/20) ≈ 0.126
        float toneAmp = (float) Math.pow(10.0, -18.0 / 20.0); // ≈ 0.126
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            float t = (float) i / SAMPLE_RATE;
            samples[i] += toneAmp * (float) Math.sin(2.0 * Math.PI * 110.0 * t);
        }

        // Amplitude modulation: Perlin noise at 0.3 Hz, +/-2 dB
        PerlinNoise amNoise = new PerlinNoise(seed + 77773L);
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            float t = (float) i / SAMPLE_RATE;
            float noiseVal = amNoise.sample(t * 0.3f); // 0.3 Hz
            float modDb = noiseVal * 2.0f; // +/-2 dB
            float modGain = (float) Math.pow(10.0, modDb / 20.0);
            samples[i] *= modGain;
        }

        normalize(samples, 0.7f);
        crossfade(samples, CROSSFADE_SAMPLES);

        return samples;
    }

    /**
     * Generates ground family samples (for STEP features).
     *
     * <p>White noise -> biquad bandpass at 280 Hz Q=1.4 (200-400 Hz band)
     * -> Perlin AM at 0.2 Hz +/-1.5 dB -> normalize to 0.6 (quietest family) -> crossfade.
     *
     * @param variantIndex 0-3, determines seed
     * @return 264,600 float samples
     */
    static float[] generateGroundSamples(int variantIndex) {
        long seed = (long) FAMILY_GROUND * 100003L + (long) variantIndex * 7919L;
        Random rng = new Random(seed);

        // Generate white noise
        float[] samples = new float[BUFFER_SAMPLES];
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            samples[i] = rng.nextFloat() * 2f - 1f;
        }

        // Bandpass 200-400 Hz: center 280 Hz, Q ≈ 1.4
        applyBiquadBandpass(samples, 280f, 1.4f, SAMPLE_RATE);

        // Amplitude modulation: Perlin noise at 0.2 Hz, +/-1.5 dB
        PerlinNoise amNoise = new PerlinNoise(seed + 55547L);
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            float t = (float) i / SAMPLE_RATE;
            float noiseVal = amNoise.sample(t * 0.2f); // 0.2 Hz
            float modDb = noiseVal * 1.5f; // +/-1.5 dB
            float modGain = (float) Math.pow(10.0, modDb / 20.0);
            samples[i] *= modGain;
        }

        // Lower headroom — quietest family
        normalize(samples, 0.6f);
        crossfade(samples, CROSSFADE_SAMPLES);

        return samples;
    }

    // ---- Variant hashing ----

    /**
     * Computes the variant index (0-3) for a world block position.
     * Uses a hash with good spatial distribution for decorrelation.
     *
     * @param blockX world X coordinate
     * @param blockZ world Z coordinate
     * @return variant index in [0, 3]
     */
    public static int variantForPosition(int blockX, int blockZ) {
        // Stafford-style integer hash for spatial distribution
        return (blockX * 374761393 + blockZ * 668265263) >>> 30; // top 2 bits -> 0-3
    }

    // ---- OpenAL lifecycle ----
=======
    // ---- OpenAL lifecycle (not tested in unit tests) ----
>>>>>>> theirs

    /**
     * Generates all 12 buffers and uploads them to OpenAL.
     * Called once at system initialization on the render thread.
     */
    public void init() {
<<<<<<< ours
        if (initialized) return;

        for (int variant = 0; variant < VARIANTS_PER_FAMILY; variant++) {
            bufferIds[FAMILY_VOID][variant] = uploadToOpenAL(generateVoidSamples(variant));
            bufferIds[FAMILY_SURFACE][variant] = uploadToOpenAL(generateSurfaceSamples(variant));
            bufferIds[FAMILY_GROUND][variant] = uploadToOpenAL(generateGroundSamples(variant));
        }

        initialized = true;
=======
        // Stub — implemented in Task 10
>>>>>>> theirs
    }

    /**
     * Returns the OpenAL buffer ID for a given family and world position.
     * Variant is deterministically chosen by hashing block coordinates.
     *
     * @param family the terrain feature family
     * @param blockX world X coordinate
     * @param blockZ world Z coordinate
<<<<<<< ours
     * @return OpenAL buffer ID, or 0 if not initialized
     */
    public int getBufferId(TerrainFeatureType.Family family, int blockX, int blockZ) {
        if (!initialized) return 0;
        int variant = variantForPosition(blockX, blockZ);
        return bufferIds[family.ordinal()][variant];
=======
     * @return OpenAL buffer ID
     */
    public int getBufferId(TerrainFeatureType.Family family, int blockX, int blockZ) {
        // Stub — implemented in Task 10
        return 0;
>>>>>>> theirs
    }

    /**
     * Deletes all OpenAL buffers. Called on system shutdown.
     */
    public void shutdown() {
<<<<<<< ours
        if (!initialized) return;
        for (int f = 0; f < FAMILY_COUNT; f++) {
            for (int v = 0; v < VARIANTS_PER_FAMILY; v++) {
                if (bufferIds[f][v] != 0) {
                    org.lwjgl.openal.AL10.alDeleteBuffers(bufferIds[f][v]);
                    bufferIds[f][v] = 0;
                }
            }
        }
        initialized = false;
    }

    /**
     * Converts float samples to 16-bit PCM and uploads to an OpenAL buffer.
     *
     * @param samples float sample data [-1, 1]
     * @return OpenAL buffer ID
     */
    private static int uploadToOpenAL(float[] samples) {
        ByteBuffer pcm = ByteBuffer.allocateDirect(samples.length * 2).order(ByteOrder.nativeOrder());
        for (float sample : samples) {
            short s = (short) (sample * 32767f);
            pcm.putShort(s);
        }
        pcm.flip();

        int buf = org.lwjgl.openal.AL10.alGenBuffers();
        org.lwjgl.openal.AL10.alBufferData(buf, org.lwjgl.openal.AL10.AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
        return buf;
=======
        // Stub — implemented in Task 10
>>>>>>> theirs
    }
}
