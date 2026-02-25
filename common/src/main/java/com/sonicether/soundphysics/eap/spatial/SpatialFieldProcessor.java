package com.sonicether.soundphysics.eap.spatial;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Layer 3: Spatial Field Processor.
 * Maintains a pool of 8 OpenAL sources playing a shared broadband noise buffer
 * (slightly pink, crossfade-looped). Each source is positioned at a detected
 * surface cluster centroid and gain-modulated by cluster energy, distance,
 * and material reflectivity.
 *
 * <p>The field is energy-gated: if the scene is silent (no emitters, no excitation),
 * the spatial field fades to silence. This prevents the field from playing in
 * truly quiet environments.
 *
 * <p>All OpenAL calls run on the render thread (same thread as EapSystem.onClientTick).
 */
public final class SpatialFieldProcessor {

    /** Number of OpenAL sources in the spatial field pool. */
    private static final int SOURCE_COUNT = 8;

    /** Field buffer: slightly pink broadband noise, 4 seconds at 44100 Hz mono. */
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SECONDS = 4;
    private static final int BUFFER_SAMPLES = SAMPLE_RATE * BUFFER_SECONDS;

    /** Crossfade region in samples for seamless looping. */
    private static final int CROSSFADE_SAMPLES = 4410; // 100ms

    /** Distance beyond which precedence attenuation kicks in (blocks). */
    private static final float PRECEDENCE_DISTANCE = 17.0f;

    /** Exponential smoothing factor per tick for gain and position. */
    private static final float SMOOTH_FACTOR = 0.1f;

    /** Energy gate threshold — below this, the field is silent. */
    private static final float ENERGY_GATE_THRESHOLD = 0.001f;

    // OpenAL resources
    private final int[] sourceIds = new int[SOURCE_COUNT];
    private final int[] filterIds = new int[SOURCE_COUNT];
    private int bufferId;

    // Per-source current state (smoothed)
    private final float[] currentGain = new float[SOURCE_COUNT];
    private final float[] currentX = new float[SOURCE_COUNT];
    private final float[] currentY = new float[SOURCE_COUNT];
    private final float[] currentZ = new float[SOURCE_COUNT];
    private final float[] currentFilterHF = new float[SOURCE_COUNT];

    // Per-source target state
    private final float[] targetGain = new float[SOURCE_COUNT];
    private final float[] targetX = new float[SOURCE_COUNT];
    private final float[] targetY = new float[SOURCE_COUNT];
    private final float[] targetZ = new float[SOURCE_COUNT];
    private final float[] targetFilterHF = new float[SOURCE_COUNT];

    private boolean enabled = true;
    private float intensity = 0.5f;
    private int activeCount = 0;

    /**
     * Creates the spatial field processor: generates the noise buffer,
     * allocates 8 sources with lowpass filters, binds the buffer, and
     * starts all sources at zero gain.
     */
    public SpatialFieldProcessor() {
        // Generate the shared field buffer
        bufferId = generateFieldBuffer();

        // Create sources and filters
        for (int i = 0; i < SOURCE_COUNT; i++) {
            sourceIds[i] = AL10.alGenSources();
            filterIds[i] = EXTEfx.alGenFilters();

            // Configure filter as lowpass
            EXTEfx.alFilteri(filterIds[i], EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(filterIds[i], EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(filterIds[i], EXTEfx.AL_LOWPASS_GAINHF, 1.0f);

            // Bind buffer and set looping
            AL10.alSourcei(sourceIds[i], AL10.AL_BUFFER, bufferId);
            AL10.alSourcei(sourceIds[i], AL10.AL_LOOPING, AL10.AL_TRUE);

            // Start at zero gain (silent)
            AL10.alSourcef(sourceIds[i], AL10.AL_GAIN, 0f);

            // Set distance model parameters
            AL10.alSourcef(sourceIds[i], AL10.AL_REFERENCE_DISTANCE, 1.0f);
            AL10.alSourcef(sourceIds[i], AL10.AL_MAX_DISTANCE, 64.0f);
            AL10.alSourcef(sourceIds[i], AL10.AL_ROLLOFF_FACTOR, 0f); // We handle attenuation manually

            // Apply direct filter
            AL10.alSourcei(sourceIds[i], EXTEfx.AL_DIRECT_FILTER, filterIds[i]);

            // Stagger playback start to avoid phase correlation
            int offset = (i * BUFFER_SAMPLES / SOURCE_COUNT);
            AL10.alSourcei(sourceIds[i], AL11.AL_SAMPLE_OFFSET, offset);

            // Start playing (at zero gain)
            AL10.alSourcePlay(sourceIds[i]);

            // Initialize state
            currentGain[i] = 0f;
            targetGain[i] = 0f;
            currentFilterHF[i] = 1.0f;
            targetFilterHF[i] = 1.0f;
        }

        Loggers.log("SpatialFieldProcessor: initialized {} sources with {}s noise buffer",
                SOURCE_COUNT, BUFFER_SECONDS);
    }

    /**
     * Main per-tick update. Detects surface clusters from the current profile,
     * assigns them to sources, applies precedence attenuation, spectral filtering,
     * and smooths all parameters.
     *
     * @param profile      current environment profile
     * @param masterGain   global master gain (0-1)
     * @param sceneEnergy  aggregate scene energy from emitters + excitation
     * @param listenerPos  current listener (player) position
     */
    public void tick(EnvironmentProfile profile, float masterGain, float sceneEnergy, Vec3 listenerPos) {
        if (!enabled) {
            silenceAll();
            return;
        }

        // Energy gate: silent world = silent field
        if (sceneEnergy < ENERGY_GATE_THRESHOLD) {
            for (int i = 0; i < SOURCE_COUNT; i++) {
                targetGain[i] = 0f;
            }
            smoothAndApply();
            return;
        }

        // Detect clusters from profile taps
        List<SurfaceCluster> clusters = ClusterDetector.detect(profile.taps(), listenerPos);

        // Assign clusters to sources (1:1, excess clusters dropped)
        int assigned = Math.min(clusters.size(), SOURCE_COUNT);
        activeCount = 0;

        for (int i = 0; i < SOURCE_COUNT; i++) {
            if (i < assigned) {
                SurfaceCluster cluster = clusters.get(i);
                float gain = cluster.computeGain(intensity);

                // Precedence attenuation: reduce gain beyond 17 blocks
                if (cluster.averageDistance() > PRECEDENCE_DISTANCE) {
                    float excess = cluster.averageDistance() - PRECEDENCE_DISTANCE;
                    float attenuation = 1.0f / (1.0f + excess * 0.3f);
                    gain *= attenuation;
                }

                // Apply master gain
                gain *= masterGain;

                // Clamp
                gain = Math.max(0f, Math.min(1.0f, gain));

                targetGain[i] = gain;
                targetX[i] = (float) cluster.centroid().x;
                targetY[i] = (float) cluster.centroid().y;
                targetZ[i] = (float) cluster.centroid().z;

                // Material-based spectral filter: more HF absorption = darker
                float hfAbsorption = cluster.spectralHigh();
                targetFilterHF[i] = Math.max(0.1f, 1.0f - hfAbsorption * 0.7f);

                if (gain > 0.001f) {
                    activeCount++;
                }
            } else {
                // No cluster for this source — fade to silence
                targetGain[i] = 0f;
            }
        }

        smoothAndApply();
    }

    /**
     * Smooths current values toward targets and applies to OpenAL sources.
     */
    private void smoothAndApply() {
        for (int i = 0; i < SOURCE_COUNT; i++) {
            // Smooth gain
            currentGain[i] += (targetGain[i] - currentGain[i]) * SMOOTH_FACTOR;
            if (currentGain[i] < 0.0001f) {
                currentGain[i] = 0f;
            }

            // Smooth position
            currentX[i] += (targetX[i] - currentX[i]) * SMOOTH_FACTOR;
            currentY[i] += (targetY[i] - currentY[i]) * SMOOTH_FACTOR;
            currentZ[i] += (targetZ[i] - currentZ[i]) * SMOOTH_FACTOR;

            // Smooth filter
            currentFilterHF[i] += (targetFilterHF[i] - currentFilterHF[i]) * SMOOTH_FACTOR;

            // Apply to OpenAL
            AL10.alSourcef(sourceIds[i], AL10.AL_GAIN, currentGain[i]);
            AL10.alSource3f(sourceIds[i], AL10.AL_POSITION, currentX[i], currentY[i], currentZ[i]);

            // Apply lowpass filter
            EXTEfx.alFilterf(filterIds[i], EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(filterIds[i], EXTEfx.AL_LOWPASS_GAINHF, currentFilterHF[i]);
            AL10.alSourcei(sourceIds[i], EXTEfx.AL_DIRECT_FILTER, filterIds[i]);

            // Ensure source is still playing (OpenAL can stop sources on error)
            int state = AL10.alGetSourcei(sourceIds[i], AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING) {
                AL10.alSourcePlay(sourceIds[i]);
            }
        }
    }

    /**
     * Immediately silences all sources by setting gains to zero.
     */
    public void silenceAll() {
        for (int i = 0; i < SOURCE_COUNT; i++) {
            targetGain[i] = 0f;
            currentGain[i] = 0f;
            AL10.alSourcef(sourceIds[i], AL10.AL_GAIN, 0f);
        }
        activeCount = 0;
    }

    /**
     * Enables or disables the spatial field. When disabled, all sources are silenced.
     */
    public void setEnabled(boolean enabled) {
        if (!enabled && this.enabled) {
            silenceAll();
        }
        this.enabled = enabled;
    }

    /**
     * Sets the spatial field intensity (0-1). Higher = louder field response.
     */
    public void setIntensity(float intensity) {
        this.intensity = Math.max(0f, Math.min(1.0f, intensity));
    }

    /**
     * Returns how many sources are actively producing audible output.
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * Returns the total number of sources in the pool.
     */
    public int getSourceCount() {
        return SOURCE_COUNT;
    }

    /**
     * Frees all OpenAL resources.
     */
    public void shutdown() {
        for (int i = 0; i < SOURCE_COUNT; i++) {
            AL10.alSourceStop(sourceIds[i]);
            AL10.alDeleteSources(sourceIds[i]);
            EXTEfx.alDeleteFilters(filterIds[i]);
        }
        if (bufferId != 0) {
            AL10.alDeleteBuffers(bufferId);
            bufferId = 0;
        }
        Loggers.log("SpatialFieldProcessor: shutdown, freed {} sources", SOURCE_COUNT);
    }

    /**
     * Generates a slightly pink broadband noise buffer suitable for spatial field playback.
     * Uses a simple 1-pole lowpass on white noise to roll off highs, then applies a
     * crossfade at buffer boundaries for seamless looping.
     *
     * @return OpenAL buffer ID
     */
    private static int generateFieldBuffer() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float[] samples = new float[BUFFER_SAMPLES];

        // Generate white noise and apply 1-pole pink filter
        // y[n] = 0.95 * y[n-1] + 0.05 * x[n]  — gentle HF rolloff
        float prev = 0f;
        float pinkCoeff = 0.95f;
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            float white = rng.nextFloat() * 2.0f - 1.0f;
            prev = pinkCoeff * prev + (1.0f - pinkCoeff) * white;
            samples[i] = prev;
        }

        // Normalize to [-0.8, 0.8] to leave headroom
        float maxAbs = 0f;
        for (float s : samples) {
            float abs = Math.abs(s);
            if (abs > maxAbs) maxAbs = abs;
        }
        if (maxAbs > 0f) {
            float scale = 0.8f / maxAbs;
            for (int i = 0; i < BUFFER_SAMPLES; i++) {
                samples[i] *= scale;
            }
        }

        // Crossfade ends for seamless looping
        for (int i = 0; i < CROSSFADE_SAMPLES; i++) {
            float t = (float) i / CROSSFADE_SAMPLES;
            // Fade in at start
            samples[i] *= t;
            // Blend with end at the same time
            int endIdx = BUFFER_SAMPLES - CROSSFADE_SAMPLES + i;
            samples[endIdx] = samples[endIdx] * (1.0f - t) + samples[i] * t;
        }

        // Convert to 16-bit PCM
        ByteBuffer pcm = ByteBuffer.allocateDirect(BUFFER_SAMPLES * 2).order(ByteOrder.nativeOrder());
        for (int i = 0; i < BUFFER_SAMPLES; i++) {
            short s = (short) (samples[i] * 32767f);
            pcm.putShort(s);
        }
        pcm.flip();

        int buf = AL10.alGenBuffers();
        AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
        return buf;
    }
}
