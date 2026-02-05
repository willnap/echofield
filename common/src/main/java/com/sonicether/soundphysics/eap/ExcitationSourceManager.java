package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.Loggers;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
<<<<<<< ours
import java.util.concurrent.ThreadLocalRandom;
=======
>>>>>>> theirs

/**
 * Manages five looping OpenAL sources that produce environmental ambience
 * (wind, foliage, grass, water, lava). Each source plays the same white-noise
 * buffer; spectral character comes from a per-source lowpass filter.
 *
 * <p>Sources never stop playing — gain is set to 0 when inactive.
 * Target parameters are recomputed from each {@link EnvironmentProfile} and
 * smoothed toward current values with exponential interpolation (factor 0.1).
 */
public final class ExcitationSourceManager {

<<<<<<< ours
    /** Length of the shared noise buffer in samples (mono, 44100 Hz, 2 seconds). */
    private static final int NOISE_SAMPLES = 88200; // 2 seconds at 44.1 kHz — long enough to avoid audible looping
=======
    /** Length of the shared noise buffer in samples (mono, 44100 Hz). */
    private static final int NOISE_SAMPLES = 44100; // 1 second at 44.1 kHz
>>>>>>> theirs
    private static final int SAMPLE_RATE = 44100;

    /** Exponential smoothing factor per tick. */
    private static final float SMOOTH = 0.1f;
    /** Below this gain, treat as silent. */
    private static final float SILENCE = 0.001f;

    private final ExcitationSource[] sources;
<<<<<<< ours
    private final int[] buffers; // one per ExcitationType

    private final java.util.Map<ExcitationType, Boolean> typeEnabled = new java.util.EnumMap<>(ExcitationType.class);

    private float masterGain = 1.0f;
    private float excitationVolume = 0.2f;
=======
    private int noiseBuffer;

    private float masterGain = 1.0f;
    private float excitationVolume = 1.0f;
>>>>>>> theirs
    private boolean enabled = true;
    private double playerX, playerY, playerZ;

    // ── Construction ──────────────────────────────────────────────────

    public ExcitationSourceManager() {
<<<<<<< ours
        ExcitationType[] types = ExcitationType.values();
        sources = new ExcitationSource[types.length];
        buffers = new int[types.length];

        // Generate a unique procedural buffer per source type
        for (int i = 0; i < types.length; i++) {
            buffers[i] = generateProceduralBuffer(types[i]);
        }
=======
        noiseBuffer = generateNoiseBuffer();

        ExcitationType[] types = ExcitationType.values();
        sources = new ExcitationSource[types.length];
>>>>>>> theirs

        for (int i = 0; i < types.length; i++) {
            ExcitationSource s = new ExcitationSource(types[i]);

            s.sourceId = AL10.alGenSources();
            Loggers.logALError("ExcitationSourceManager: alGenSources for " + types[i]);

            s.filterId = EXTEfx.alGenFilters();
            EXTEfx.alFilteri(s.filterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(s.filterId, EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(s.filterId, EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
            Loggers.logALError("ExcitationSourceManager: alGenFilters for " + types[i]);

<<<<<<< ours
            // Create per-source send filters for aux reverb sends
            for (int j = 0; j < 4; j++) {
                s.sendFilterIds[j] = EXTEfx.alGenFilters();
                EXTEfx.alFilteri(s.sendFilterIds[j], EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                EXTEfx.alFilterf(s.sendFilterIds[j], EXTEfx.AL_LOWPASS_GAIN, 0.0f);
                EXTEfx.alFilterf(s.sendFilterIds[j], EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
            }
            Loggers.logALError("ExcitationSourceManager: send filters for " + types[i]);

            // Bind type-specific procedural buffer, set looping, start at gain=0
            AL10.alSourcei(s.sourceId, AL10.AL_BUFFER, buffers[i]);
            AL10.alSourcei(s.sourceId, AL10.AL_LOOPING, AL10.AL_TRUE);
            AL10.alSourcef(s.sourceId, AL10.AL_GAIN, 0.0f);
            AL10.alSourcef(s.sourceId, AL10.AL_ROLLOFF_FACTOR, 1.0f);
            AL10.alSourcef(s.sourceId, AL10.AL_REFERENCE_DISTANCE, 8.0f);
            AL10.alSourcef(s.sourceId, AL10.AL_MAX_DISTANCE, 64.0f);
=======
            // Bind buffer, set looping, start at gain=0
            AL10.alSourcei(s.sourceId, AL10.AL_BUFFER, noiseBuffer);
            AL10.alSourcei(s.sourceId, AL10.AL_LOOPING, AL10.AL_TRUE);
            AL10.alSourcef(s.sourceId, AL10.AL_GAIN, 0.0f);
            AL10.alSourcef(s.sourceId, AL10.AL_ROLLOFF_FACTOR, 0.0f);
>>>>>>> theirs
            AL10.alSourcei(s.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL11.alSourcei(s.sourceId, EXTEfx.AL_DIRECT_FILTER, s.filterId);
            Loggers.logALError("ExcitationSourceManager: source setup for " + types[i]);

            AL10.alSourcePlay(s.sourceId);
            Loggers.logALError("ExcitationSourceManager: alSourcePlay for " + types[i]);

<<<<<<< ours
            // Initialize filter state — procedural buffers are already shaped,
            // so filters are for environmental fine-tuning, not primary shaping
            s.currentFilterGain = 0.5f;
            s.currentFilterGainHF = 0.3f;
            s.targetFilterGain = 0.5f;
            s.targetFilterGainHF = 0.3f;
=======
            // Initialize filter state
            s.currentFilterGain = 1.0f;
            s.currentFilterGainHF = 1.0f;
            s.targetFilterGain = 1.0f;
            s.targetFilterGainHF = 1.0f;
>>>>>>> theirs

            sources[i] = s;
        }

        Loggers.log("ExcitationSourceManager: initialized {} excitation sources", types.length);
    }

    // ── Noise buffer ──────────────────────────────────────────────────

    /**
<<<<<<< ours
     * Generates a procedural audio buffer tailored to the given excitation type.
     * Each type uses a different synthesis model:
     * <ul>
     *   <li>WIND: Low-frequency sine waves with gust modulation + sub-bass noise</li>
     *   <li>FOLIAGE: Granular synthesis — short bandpass noise bursts (leaf rustle)</li>
     *   <li>GRASS: Softer granular synthesis at lower frequencies (gentle swish)</li>
     *   <li>WATER: Stochastic bubble model — damped sinusoidal bubble-pop events</li>
     *   <li>LAVA: Deep bubble model with low frequencies and slow pacing</li>
     * </ul>
     */
    static int generateProceduralBuffer(ExcitationType type) {
        float[] samples = new float[NOISE_SAMPLES];
        Random rng = new Random(42 + type.ordinal());

        switch (type) {
            case WIND -> generateWind(samples, rng);
            case FOLIAGE -> generateFoliage(samples, rng);
            case GRASS -> generateGrass(samples, rng);
            case WATER -> generateWater(samples, rng);
            case LAVA -> generateLava(samples, rng);
        }

        // Normalize to prevent clipping
        float peak = 0.001f;
        for (float s : samples) peak = Math.max(peak, Math.abs(s));
        float norm = 0.85f / peak;

        ByteBuffer data = ByteBuffer.allocateDirect(NOISE_SAMPLES * 2)
                .order(ByteOrder.nativeOrder());
        for (float s : samples) {
            data.putShort((short) (s * norm * Short.MAX_VALUE));
=======
     * Generates a mono 16-bit white-noise buffer at 44100 Hz.
     *
     * @return OpenAL buffer handle
     */
    static int generateNoiseBuffer() {
        Random rng = new Random(42); // deterministic seed for reproducibility
        ByteBuffer data = ByteBuffer.allocateDirect(NOISE_SAMPLES * 2)
                .order(ByteOrder.nativeOrder());
        for (int i = 0; i < NOISE_SAMPLES; i++) {
            short sample = (short) ((rng.nextFloat() * 2.0f - 1.0f) * Short.MAX_VALUE);
            data.putShort(sample);
>>>>>>> theirs
        }
        data.flip();

        int buf = AL10.alGenBuffers();
        AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, data, SAMPLE_RATE);
<<<<<<< ours
        Loggers.logALError("ExcitationSourceManager: generateProceduralBuffer " + type);
        return buf;
    }

    /**
     * Wind: sum of detuned low-frequency sines with slow gust modulation
     * and integrated noise for sub-bass texture.
     */
    private static void generateWind(float[] out, Random rng) {
        float brownState = 0;
        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;

            // Sub-bass drone: sum of detuned sines (40-180 Hz)
            float drone = 0;
            drone += 0.30f * (float) Math.sin(2 * Math.PI * 47 * t);
            drone += 0.25f * (float) Math.sin(2 * Math.PI * 73 * t);
            drone += 0.20f * (float) Math.sin(2 * Math.PI * 113 * t);
            drone += 0.15f * (float) Math.sin(2 * Math.PI * 167 * t);
            drone += 0.10f * (float) Math.sin(2 * Math.PI * 41 * t);

            // Integrated noise for texture (brown noise component)
            float white = rng.nextFloat() * 2f - 1f;
            brownState += white * 0.015f;
            brownState *= 0.997f;
            brownState = Math.max(-1f, Math.min(1f, brownState));

            // Gust modulation: slow amplitude envelope (0.3-1.2 Hz)
            float gust = 0.5f + 0.3f * (float) Math.sin(2 * Math.PI * 0.37 * t)
                    + 0.2f * (float) Math.sin(2 * Math.PI * 0.83 * t);

            out[i] = (drone * 0.6f + brownState * 0.4f) * gust;
        }
    }

    /**
     * Foliage: granular synthesis — short bandpass noise bursts creating
     * natural leaf-rustle texture (1.5-5 kHz, 2-6ms grains).
     */
    private static void generateFoliage(float[] out, Random rng) {
        int numGrains = 4000; // ~2000 per second
        for (int g = 0; g < numGrains; g++) {
            int startSample = rng.nextInt(out.length);
            int grainLen = 88 + rng.nextInt(176); // 2-6ms at 44100 Hz
            float amplitude = 0.01f + rng.nextFloat() * 0.025f;
            float freq = 1500 + rng.nextFloat() * 3500; // 1.5-5 kHz

            for (int i = 0; i < grainLen && startSample + i < out.length; i++) {
                float t = (float) i / SAMPLE_RATE;
                // Hanning window
                float window = 0.5f * (1f - (float) Math.cos(2 * Math.PI * i / grainLen));
                float noise = rng.nextFloat() * 2f - 1f;
                // Bandpass: multiply noise by sine at center freq
                out[startSample + i] += amplitude * window * noise
                        * (float) Math.sin(2 * Math.PI * freq * t);
            }
        }
    }

    /**
     * Grass: softer granular synthesis at lower frequencies (500-2 kHz)
     * with longer, gentler grains for a swishing character.
     */
    private static void generateGrass(float[] out, Random rng) {
        int numGrains = 2500; // ~1250 per second (sparser than foliage)
        for (int g = 0; g < numGrains; g++) {
            int startSample = rng.nextInt(out.length);
            int grainLen = 132 + rng.nextInt(264); // 3-9ms
            float amplitude = 0.005f + rng.nextFloat() * 0.012f;
            float freq = 500 + rng.nextFloat() * 1500; // 500-2000 Hz

            for (int i = 0; i < grainLen && startSample + i < out.length; i++) {
                float t = (float) i / SAMPLE_RATE;
                float window = 0.5f * (1f - (float) Math.cos(2 * Math.PI * i / grainLen));
                float noise = rng.nextFloat() * 2f - 1f;
                out[startSample + i] += amplitude * window * noise
                        * (float) Math.sin(2 * Math.PI * freq * t);
            }
        }
    }

    /**
     * Water: stochastic bubble model — hundreds of damped sinusoidal events
     * at random frequencies (80-3000 Hz) simulating bubble formation and
     * surface turbulence.
     */
    private static void generateWater(float[] out, Random rng) {
        int numBubbles = 3000; // ~1500 per second
        for (int b = 0; b < numBubbles; b++) {
            int startSample = rng.nextInt(out.length);
            // Bubble frequency inversely related to size (small=high, large=low)
            float freq = 80 + rng.nextFloat() * rng.nextFloat() * 2900; // skewed toward low
            float decay = 30 + rng.nextFloat() * 120; // decay rate (Hz)
            float amplitude = 0.008f + rng.nextFloat() * 0.02f;
            int duration = Math.min((int) (5f / decay * SAMPLE_RATE), 4410); // max 100ms

            for (int i = 0; i < duration && startSample + i < out.length; i++) {
                float t = (float) i / SAMPLE_RATE;
                float env = amplitude * (float) Math.exp(-decay * t);
                // Slight frequency drop as bubble rises (chirp)
                float chirpFreq = freq * (1f - 0.15f * t * decay / 50f);
                out[startSample + i] += env * (float) Math.sin(2 * Math.PI * chirpFreq * t);
            }
        }
    }

    /**
     * Lava: deep bubble model — slow, large, low-frequency bubble events
     * (20-200 Hz) with violent pops and sub-bass rumble.
     */
    private static void generateLava(float[] out, Random rng) {
        // Deep sub-bass drone
        float brownState = 0;
        for (int i = 0; i < out.length; i++) {
            float t = (float) i / SAMPLE_RATE;
            float white = rng.nextFloat() * 2f - 1f;
            brownState += white * 0.01f;
            brownState *= 0.999f;
            brownState = Math.max(-1f, Math.min(1f, brownState));
            out[i] = brownState * 0.3f;

            // Low drone
            out[i] += 0.15f * (float) Math.sin(2 * Math.PI * 25 * t);
            out[i] += 0.10f * (float) Math.sin(2 * Math.PI * 38 * t);
        }

        // Large, slow bubbles
        int numBubbles = 600; // ~300 per second (sparse, heavy)
        for (int b = 0; b < numBubbles; b++) {
            int startSample = rng.nextInt(out.length);
            float freq = 20 + rng.nextFloat() * 180; // 20-200 Hz
            float decay = 8 + rng.nextFloat() * 30; // slow decay
            float amplitude = 0.03f + rng.nextFloat() * 0.06f;
            int duration = Math.min((int) (5f / decay * SAMPLE_RATE), 22050); // max 500ms

            for (int i = 0; i < duration && startSample + i < out.length; i++) {
                float t = (float) i / SAMPLE_RATE;
                float env = amplitude * (float) Math.exp(-decay * t);
                out[startSample + i] += env * (float) Math.sin(2 * Math.PI * freq * t);
            }
        }
    }

=======
        Loggers.logALError("ExcitationSourceManager: generateNoiseBuffer");
        return buf;
    }

>>>>>>> theirs
    // ── Shutdown ──────────────────────────────────────────────────────

    public void shutdown() {
        for (ExcitationSource s : sources) {
            AL10.alSourceStop(s.sourceId);
<<<<<<< ours
            AL10.alSourcei(s.sourceId, AL10.AL_BUFFER, 0);
            AL10.alDeleteSources(s.sourceId);
            EXTEfx.alDeleteFilters(s.filterId);
            for (int fid : s.sendFilterIds) {
                if (fid != 0) EXTEfx.alDeleteFilters(fid);
            }
        }
        for (int buf : buffers) {
            if (buf != 0) AL10.alDeleteBuffers(buf);
        }
=======
            AL10.alDeleteSources(s.sourceId);
            EXTEfx.alDeleteFilters(s.filterId);
        }
        AL10.alDeleteBuffers(noiseBuffer);
        noiseBuffer = 0;
>>>>>>> theirs
        Loggers.log("ExcitationSourceManager: shut down");
    }

    // ── Setters ──────────────────────────────────────────────────────

    public void setMasterGain(float gain) {
        this.masterGain = gain;
    }

    public void setExcitationVolume(float volume) {
        this.excitationVolume = volume;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

<<<<<<< ours
    public void setTypeEnabled(ExcitationType type, boolean enabled) {
        typeEnabled.put(type, enabled);
    }

=======
>>>>>>> theirs
    public void setPlayerPosition(double x, double y, double z) {
        this.playerX = x;
        this.playerY = y;
        this.playerZ = z;
    }

    // ── Accessors ────────────────────────────────────────────────────

    public ExcitationSource[] getSources() {
        return sources;
    }

    // ── Update loop ──────────────────────────────────────────────────

    /**
     * Recomputes target parameters from the current profile and smooths
     * all source values toward their targets. Call once per tick.
     */
    public void update(EnvironmentProfile profile) {
<<<<<<< ours
        if (profile == null || !enabled) {
=======
        if (!enabled) {
>>>>>>> theirs
            silenceAll();
            return;
        }

        // Compute targets for each excitation type
        computeWindTargets(profile);
        computeFoliageTargets(profile);
        computeGrassTargets(profile);
        computeWaterTargets(profile);
        computeLavaTargets(profile);

<<<<<<< ours
        // Gate disabled excitation types
        for (ExcitationSource s : sources) {
            if (Boolean.FALSE.equals(typeEnabled.get(s.type))) {
                s.targetGain = 0f;
            }
        }

=======
>>>>>>> theirs
        // Smooth and apply
        for (ExcitationSource s : sources) {
            // Scale target gain by master and excitation volume
            float scaledTarget = s.targetGain * masterGain * excitationVolume;

            s.currentGain += (scaledTarget - s.currentGain) * SMOOTH;
            s.currentX += (s.targetX - s.currentX) * SMOOTH;
            s.currentY += (s.targetY - s.currentY) * SMOOTH;
            s.currentZ += (s.targetZ - s.currentZ) * SMOOTH;
            s.currentFilterGain += (s.targetFilterGain - s.currentFilterGain) * SMOOTH;
            s.currentFilterGainHF += (s.targetFilterGainHF - s.currentFilterGainHF) * SMOOTH;

            // Snap to zero below silence threshold
            if (s.currentGain < SILENCE) {
                s.currentGain = 0.0f;
            }

<<<<<<< ours
            // Apply amplitude modulation if depth > 0 (foliage rustle per spec)
            float appliedGain = s.currentGain;
            if (s.modulationDepth > 0.001f) {
                s.modulationPhase += 0.3f + ThreadLocalRandom.current().nextFloat() * 0.4f;
                float mod = 1.0f - s.modulationDepth
                        * 0.5f * (1.0f + (float) Math.sin(s.modulationPhase));
                appliedGain *= Math.max(0.0f, mod);
            }

            // Apply to OpenAL
            AL10.alSourcef(s.sourceId, AL10.AL_GAIN, appliedGain);
=======
            // Apply to OpenAL
            AL10.alSourcef(s.sourceId, AL10.AL_GAIN, s.currentGain);
>>>>>>> theirs
            AL10.alSource3f(s.sourceId, AL10.AL_POSITION,
                    s.currentX, s.currentY, s.currentZ);

            EXTEfx.alFilterf(s.filterId, EXTEfx.AL_LOWPASS_GAIN,
                    Math.max(0.0f, Math.min(1.0f, s.currentFilterGain)));
            EXTEfx.alFilterf(s.filterId, EXTEfx.AL_LOWPASS_GAINHF,
                    Math.max(0.0f, Math.min(1.0f, s.currentFilterGainHF)));
<<<<<<< ours
            // Re-bind filter to source — OpenAL requires re-binding after parameter changes
            AL11.alSourcei(s.sourceId, EXTEfx.AL_DIRECT_FILTER, s.filterId);

            // Safety: ensure source is still playing (could be stopped by OpenAL error)
            int state = AL10.alGetSourcei(s.sourceId, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING) {
                Loggers.log("ExcitationSourceManager: Source {} not playing (state={}), restarting",
                        s.type, state);
                AL10.alSourcePlay(s.sourceId);
            }
        }
    }

    public void silenceAll() {
        for (ExcitationSource s : sources) {
            s.targetGain = 0.0f;
            s.currentGain = 0.0f;
            AL10.alSourcef(s.sourceId, AL10.AL_GAIN, 0.0f);
            AL10.alSourceStop(s.sourceId);
        }
    }

    /**
     * Silence the 4 categories that overlap with the Layer 1 emitter system
     * (FOLIAGE, GRASS, WATER, LAVA). Keeps WIND active as it provides a
     * unique sub-bass drone with no emitter equivalent.
     */
    public void silenceOverlappingWithEmitters() {
        for (ExcitationType type : new ExcitationType[]{
                ExcitationType.FOLIAGE, ExcitationType.GRASS,
                ExcitationType.WATER, ExcitationType.LAVA}) {
            ExcitationSource s = sources[type.ordinal()];
            s.targetGain = 0.0f;
=======
            AL11.alSourcei(s.sourceId, EXTEfx.AL_DIRECT_FILTER, s.filterId);
        }
    }

    private void silenceAll() {
        for (ExcitationSource s : sources) {
            s.currentGain *= (1.0f - SMOOTH);
            if (s.currentGain < SILENCE) s.currentGain = 0.0f;
            AL10.alSourcef(s.sourceId, AL10.AL_GAIN, s.currentGain);
>>>>>>> theirs
        }
    }

    // ── Wind ─────────────────────────────────────────────────────────

    private void computeWindTargets(EnvironmentProfile profile) {
        ExcitationSource s = sources[ExcitationType.WIND.ordinal()];
        float wind = profile.windExposure();

        s.targetGain = wind;

        // Position: 20 blocks from player toward mostOpenSkyDirection
        Vec3 dir = profile.mostOpenSkyDirection();
        s.targetX = (float) (playerX + dir.x * 20.0);
        s.targetY = (float) (playerY + dir.y * 20.0);
        s.targetZ = (float) (playerZ + dir.z * 20.0);

        // Filter varies with geometry:
        //   open/broadband: gain=1.0, gainHF=0.6
        //   channeled (high enclosure, low scatter): gain=0.8, gainHF=0.3
        //   draft (moderate enclosure): gain=0.6, gainHF=0.4
        float enclosure = profile.enclosureFactor();
        float scatter = profile.scatteringDensity();

        if (enclosure > 0.7f && scatter < 0.3f) {
<<<<<<< ours
            // Channeled wind (tunnels, corridors) — darker, more resonant
            s.targetFilterGain = 0.8f;
            s.targetFilterGainHF = 0.3f;
        } else if (enclosure > 0.4f) {
            // Draft (partial enclosure) — slightly muffled
            s.targetFilterGain = 0.7f;
            s.targetFilterGainHF = 0.5f;
        } else {
            // Open wind — full character
            s.targetFilterGain = 0.9f;
            s.targetFilterGainHF = 0.8f;
=======
            // Channeled wind (tunnels, corridors)
            s.targetFilterGain = 0.8f;
            s.targetFilterGainHF = 0.3f;
        } else if (enclosure > 0.4f) {
            // Draft (partial enclosure)
            s.targetFilterGain = 0.6f;
            s.targetFilterGainHF = 0.4f;
        } else {
            // Broadband open wind
            s.targetFilterGain = 1.0f;
            s.targetFilterGainHF = 0.6f;
>>>>>>> theirs
        }
    }

    // ── Foliage ──────────────────────────────────────────────────────

    private void computeFoliageTargets(EnvironmentProfile profile) {
        ExcitationSource s = sources[ExcitationType.FOLIAGE.ordinal()];
        float wind = profile.windExposure();
        List<ReflectionTap> taps = profile.taps();

        int leafCount = countMatchingTaps(taps, ExcitationSourceManager::isLeafMaterial);
        if (leafCount == 0 || wind < SILENCE) {
            s.targetGain = 0.0f;
            return;
        }

        // Gain scales with leaf density and wind
        float density = Math.min(leafCount / 10.0f, 1.0f);
        s.targetGain = density * wind;

        // Position at average leaf tap location
        Vec3 avg = averagePositionOfMatchingTaps(taps, ExcitationSourceManager::isLeafMaterial);
        s.targetX = (float) avg.x;
        s.targetY = (float) avg.y;
        s.targetZ = (float) avg.z;

<<<<<<< ours
        // Leaf rustle: let the granular synthesis speak
        s.targetFilterGain = 0.8f;
        s.targetFilterGainHF = 0.7f;

        // Amplitude modulation: organic rustle character scales with wind
        s.modulationDepth = 0.4f * wind;
=======
        // Highpass-like: let HF through, attenuate broadband
        s.targetFilterGain = 0.3f;
        s.targetFilterGainHF = 1.0f;
>>>>>>> theirs
    }

    // ── Grass ────────────────────────────────────────────────────────

    private void computeGrassTargets(EnvironmentProfile profile) {
        ExcitationSource s = sources[ExcitationType.GRASS.ordinal()];
        float wind = profile.windExposure();
        List<ReflectionTap> taps = profile.taps();

        int grassCount = countMatchingTaps(taps, ExcitationSourceManager::isGrassMaterial);
        if (grassCount == 0 || wind < SILENCE) {
            s.targetGain = 0.0f;
            return;
        }

        float density = Math.min(grassCount / 10.0f, 1.0f);
        s.targetGain = density * wind;

        Vec3 avg = averagePositionOfMatchingTaps(taps, ExcitationSourceManager::isGrassMaterial);
        s.targetX = (float) avg.x;
        s.targetY = (float) avg.y;
        s.targetZ = (float) avg.z;

<<<<<<< ours
        // Grass swish: gentle, let synthesis character through
        s.targetFilterGain = 0.7f;
        s.targetFilterGainHF = 0.6f;
=======
        // Very high frequency — quiet overall, all HF
        s.targetFilterGain = 0.15f;
        s.targetFilterGainHF = 1.0f;
>>>>>>> theirs
    }

    // ── Water ────────────────────────────────────────────────────────

    private void computeWaterTargets(EnvironmentProfile profile) {
        ExcitationSource s = sources[ExcitationType.WATER.ordinal()];
        List<ReflectionTap> taps = profile.taps();

        int waterCount = countMatchingTaps(taps, ExcitationSourceManager::isWaterBlock);
        if (waterCount == 0) {
            s.targetGain = 0.0f;
            return;
        }

<<<<<<< ours
        // Self-excited — NOT wind-dependent, capped low
        float density = Math.min(waterCount / 8.0f, 1.0f);
        s.targetGain = density * 0.2f;
=======
        // Self-excited — NOT wind-dependent
        float density = Math.min(waterCount / 8.0f, 1.0f);
        s.targetGain = density;
>>>>>>> theirs

        Vec3 avg = averagePositionOfMatchingTaps(taps, ExcitationSourceManager::isWaterBlock);
        s.targetX = (float) avg.x;
        s.targetY = (float) avg.y;
        s.targetZ = (float) avg.z;

<<<<<<< ours
        // Water: let bubble synthesis speak, slight HF rolloff
        s.targetFilterGain = 0.8f;
=======
        // Broadband with moderate HF rolloff
        s.targetFilterGain = 0.6f;
>>>>>>> theirs
        s.targetFilterGainHF = 0.5f;
    }

    // ── Lava ─────────────────────────────────────────────────────────

    private void computeLavaTargets(EnvironmentProfile profile) {
        ExcitationSource s = sources[ExcitationType.LAVA.ordinal()];
        List<ReflectionTap> taps = profile.taps();

        int lavaCount = countMatchingTaps(taps, ExcitationSourceManager::isLavaBlock);
        if (lavaCount == 0) {
            s.targetGain = 0.0f;
            return;
        }

<<<<<<< ours
        // Self-excited — NOT wind-dependent, capped
        float density = Math.min(lavaCount / 6.0f, 1.0f);
        s.targetGain = density * 0.3f;
=======
        // Self-excited — NOT wind-dependent
        float density = Math.min(lavaCount / 6.0f, 1.0f);
        s.targetGain = density;
>>>>>>> theirs

        Vec3 avg = averagePositionOfMatchingTaps(taps, ExcitationSourceManager::isLavaBlock);
        s.targetX = (float) avg.x;
        s.targetY = (float) avg.y;
        s.targetZ = (float) avg.z;

<<<<<<< ours
        // Lava: let deep synthesis speak
        s.targetFilterGain = 0.9f;
        s.targetFilterGainHF = 0.4f;
=======
        // Low rumble
        s.targetFilterGain = 0.8f;
        s.targetFilterGainHF = 0.1f;
>>>>>>> theirs
    }

    // ── Material checks ──────────────────────────────────────────────

    static boolean isLeafMaterial(BlockState state) {
        return SpectralCategory.fromBlockState(state) == SpectralCategory.FOLIAGE;
    }

    static boolean isGrassMaterial(BlockState state) {
        String id = state.getBlock().getDescriptionId();
        return id.contains("grass") || id.contains("fern") || id.contains("tallgrass");
    }

    static boolean isWaterBlock(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    static boolean isLavaBlock(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    @FunctionalInterface
    interface MaterialPredicate {
        boolean test(BlockState state);
    }

    static int countMatchingTaps(List<ReflectionTap> taps, MaterialPredicate pred) {
        int count = 0;
        for (ReflectionTap tap : taps) {
            if (pred.test(tap.material())) count++;
        }
        return count;
    }

    static Vec3 averagePositionOfMatchingTaps(List<ReflectionTap> taps, MaterialPredicate pred) {
        double ax = 0, ay = 0, az = 0;
        int count = 0;
        for (ReflectionTap tap : taps) {
            if (pred.test(tap.material())) {
                Vec3 pos = tap.position();
                ax += pos.x;
                ay += pos.y;
                az += pos.z;
                count++;
            }
        }
        if (count == 0) return new Vec3(0, 0, 0);
        return new Vec3(ax / count, ay / count, az / count);
    }

    // ── Diagnostics ──────────────────────────────────────────────────

    /** Diagnostic record describing one excitation source's live state. */
    public record SourceInfo(
            ExcitationType type,
            int sourceId,
            float currentGain,
            float targetGain,
            float filterGain,
            float filterGainHF,
            float posX, float posY, float posZ
    ) {}

    /**
     * Returns the raw OpenAL source IDs for all excitation sources.
     * Order matches {@link ExcitationType#values()}.
     */
    public int[] getSourceIds() {
        int[] ids = new int[sources.length];
        for (int i = 0; i < sources.length; i++) {
            ids[i] = sources[i].sourceId;
        }
        return ids;
    }

    /**
     * Returns diagnostic info for all sources whose current gain exceeds
     * the silence threshold.
     */
    public List<SourceInfo> getActiveSourceInfo() {
        List<SourceInfo> active = new ArrayList<>();
        for (ExcitationSource s : sources) {
            if (s.currentGain >= SILENCE) {
                active.add(new SourceInfo(
                        s.type, s.sourceId,
                        s.currentGain, s.targetGain,
                        s.currentFilterGain, s.currentFilterGainHF,
                        s.currentX, s.currentY, s.currentZ
                ));
            }
        }
        return active;
    }
}
