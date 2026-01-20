package com.sonicether.soundphysics.eap.emitter;

import java.util.Random;

/**
 * Mindlin-Laje syrinx ODE bird song synthesizer.
 * Generates realistic bird calls by numerically integrating a physical model
 * of the avian vocal membrane.
 *
 * <p>The model consists of two coupled first-order ODEs:
 * <pre>
 *   dx/dt = y
 *   dy/dt = -omega0^2 * x - (beta1 + beta2*x^2 + beta3*y^2)*y + (p - kappa*x^2)*x
 * </pre>
 * where x is labial displacement (the output signal), y is velocity,
 * p is air sac pressure, and kappa is labial stiffness/tension.
 *
 * <p>Sweeping (p, kappa) along trajectories in parameter space produces
 * realistic birdsong syllables with natural pitch contours, amplitude
 * envelopes, and timbre variation.
 *
 * <p>Eight species templates with species-specific ODE coefficients, tracheal
 * models, and pressure ranges produce dramatically different calls.
 */
public final class SyrinxSynth {

    private static final int SAMPLE_RATE = 44100;

    // Maximum ODE state magnitude to prevent divergence
    private static final float STATE_CLAMP = 2.0f;

    private SyrinxSynth() {}

    /**
     * Generate a bird call into the provided buffer.
     *
     * @param out            output sample array (filled from index 0)
     * @param speciesVariant 0-7, selects bird species parameters
     * @param rng            random source for per-call variation
     * @return actual number of samples written
     */
    public static int generate(float[] out, int speciesVariant, Random rng) {
        SpeciesParams sp = getSpeciesParams(speciesVariant & 7, rng);

        // ODE state
        float x = 0.0001f; // small initial displacement to break symmetry
        float y = 0f;
        float dt = 1.0f;

        // Species-specific tracheal delay line — longer tubes create more coloration
        // Real bird tracheae: 10-500mm, delay 0.03-1.5ms (1-66 samples at 44.1kHz)
        int delayLen = sp.trachealDelay;
        float[] delayLine = new float[delayLen];
        int delayIdx = 0;
        float reflectionCoeff = sp.trachealReflection;

        // Second tracheal resonator for richer coloration
        int delay2Len = Math.max(1, delayLen / 3 + rng.nextInt(Math.max(1, delayLen / 4)));
        float[] delayLine2 = new float[delay2Len];
        int delay2Idx = 0;
        float reflection2 = reflectionCoeff * 0.4f + rng.nextFloat() * 0.2f;

        int sampleIdx = 0;

        for (int syl = 0; syl < sp.syllableCount && sampleIdx < out.length; syl++) {
            int syllableSamples = (int) (sp.syllableDuration * SAMPLE_RATE);
            syllableSamples = Math.min(syllableSamples, out.length - sampleIdx);

            for (int i = 0; i < syllableSamples && sampleIdx < out.length; i++) {
                float t = (float) i / syllableSamples; // 0 -> 1 within syllable

                // Sweep p and kappa along species-specific trajectory
                float p = sp.pressureTrajectory(t, syl);
                float kappa = sp.kappaTrajectory(t, syl);
                float omega0 = sp.omega0ForSyllable(t, syl);

                // RK4 integration with species-specific beta coefficients
                float k1x = y;
                float k1y = odeRHS(x, y, omega0, p, kappa, sp.beta1, sp.beta2, sp.beta3);

                float x2 = x + 0.5f * dt * k1x;
                float y2 = y + 0.5f * dt * k1y;
                float k2x = y2;
                float k2y = odeRHS(x2, y2, omega0, p, kappa, sp.beta1, sp.beta2, sp.beta3);

                float x3 = x + 0.5f * dt * k2x;
                float y3 = y + 0.5f * dt * k2y;
                float k3x = y3;
                float k3y = odeRHS(x3, y3, omega0, p, kappa, sp.beta1, sp.beta2, sp.beta3);

                float x4 = x + dt * k3x;
                float y4 = y + dt * k3y;
                float k4x = y4;
                float k4y = odeRHS(x4, y4, omega0, p, kappa, sp.beta1, sp.beta2, sp.beta3);

                x += dt / 6f * (k1x + 2 * k2x + 2 * k3x + k4x);
                y += dt / 6f * (k1y + 2 * k2y + 2 * k3y + k4y);

                // Clamp to prevent divergence
                x = Math.max(-STATE_CLAMP, Math.min(STATE_CLAMP, x));
                y = Math.max(-STATE_CLAMP, Math.min(STATE_CLAMP, y));

                // Two coupled tracheal resonators for richer timbre
                float trach1 = reflectionCoeff * delayLine[delayIdx];
                delayLine[delayIdx] = x;
                delayIdx = (delayIdx + 1) % delayLen;

                float trach2 = reflection2 * delayLine2[delay2Idx];
                delayLine2[delay2Idx] = x;
                delay2Idx = (delay2Idx + 1) % delay2Len;

                float trachealOutput = x + trach1 + trach2;

                // Amplitude envelope for syllable (smooth attack/decay)
                float env = syllableEnvelope(t);

                out[sampleIdx++] = trachealOutput * env;
            }

            // Inter-syllable gap with wider jitter
            float gapJitter = 0.6f + rng.nextFloat() * 0.8f; // ±40% gap variation
            int gapSamples = (int) (sp.gapDuration * SAMPLE_RATE * gapJitter);
            gapSamples = Math.min(gapSamples, out.length - sampleIdx);
            for (int i = 0; i < gapSamples && sampleIdx < out.length; i++) {
                x *= 0.999f;
                y *= 0.999f;
                out[sampleIdx++] = 0f;
            }
        }

        return sampleIdx;
    }

    /**
     * Right-hand side of the velocity ODE with species-specific beta coefficients.
     */
    private static float odeRHS(float x, float y, float omega0, float p, float kappa,
                                  float beta1, float beta2, float beta3) {
        float damping = (beta1 + beta2 * x * x + beta3 * y * y) * y;
        float forcing = (p - kappa * x * x) * x;
        return -omega0 * omega0 * x - damping + forcing;
    }

    /**
     * Smooth syllable envelope: 10% attack, 75% sustain, 15% decay.
     */
    private static float syllableEnvelope(float t) {
        if (t < 0.1f) return t / 0.1f;
        if (t > 0.85f) return (1f - t) / 0.15f;
        return 1f;
    }

    // -------------------------------------------------------------------------
    // Species parameter system — dramatically wider variation per call
    // -------------------------------------------------------------------------

    private static SpeciesParams getSpeciesParams(int variant, Random rng) {
        // Wide per-call randomization: each individual call sounds unique
        float pitchVar = 0.88f + rng.nextFloat() * 0.24f;  // ±12%
        float tempoVar = 0.75f + rng.nextFloat() * 0.50f;  // ±25%
        float pressureVar = 0.75f + rng.nextFloat() * 0.50f; // ±25%

        switch (variant) {
            case 0: return songSparrow(pitchVar, tempoVar, pressureVar, rng);
            case 1: return robin(pitchVar, tempoVar, pressureVar, rng);
            case 2: return wren(pitchVar, tempoVar, pressureVar, rng);
            case 3: return blackbird(pitchVar, tempoVar, pressureVar, rng);
            case 4: return finch(pitchVar, tempoVar, pressureVar, rng);
            case 5: return warbler(pitchVar, tempoVar, pressureVar, rng);
            case 6: return chickadee(pitchVar, tempoVar, pressureVar, rng);
            case 7: return thrush(pitchVar, tempoVar, pressureVar, rng);
            default: return songSparrow(pitchVar, tempoVar, pressureVar, rng);
        }
    }

    // Species 0: Song sparrow -- 3-7 descending syllables, medium gaps
    private static SpeciesParams songSparrow(float pv, float tv, float prv, Random rng) {
        return new SpeciesParams(
                2500f * pv, 5500f * pv,
                3 + rng.nextInt(5), // 3-7
                0.12f * tv, 0.08f * tv,
                TrajectoryShape.DESCENDING, prv,
                -0.12f, 0.12f, 0.10f, // energetic sparrow
                0.01f, 0.08f, 0.3f, 0.8f,
                8 + rng.nextInt(12), // tracheal delay 8-19
                0.25f + rng.nextFloat() * 0.3f
        );
    }

    // Species 1: Robin -- pairs of up-down syllables, wider register
    private static SpeciesParams robin(float pv, float tv, float prv, Random rng) {
        int pairs = 2 + rng.nextInt(4); // 2-5 pairs = 4-10 syllables
        return new SpeciesParams(
                2000f * pv, 6000f * pv,
                pairs * 2,
                0.15f * tv, 0.06f * tv,
                TrajectoryShape.UP_DOWN, prv,
                -0.10f, 0.10f, 0.11f, // balanced
                0.01f, 0.08f, 0.3f, 0.8f,
                12 + rng.nextInt(15), // longer trachea
                0.3f + rng.nextFloat() * 0.3f
        );
    }

    // Species 2: Wren -- rapid trill, very high frequency
    private static SpeciesParams wren(float pv, float tv, float prv, Random rng) {
        return new SpeciesParams(
                4500f * pv, 9000f * pv,
                12 + rng.nextInt(15), // 12-26 syllables
                0.04f * tv, 0.02f * tv,
                TrajectoryShape.SINUSOIDAL, prv,
                -0.15f, 0.15f, 0.08f, // sharp, buzzy oscillations
                0.02f, 0.10f, 0.2f, 0.7f,
                4 + rng.nextInt(6), // short trachea (small bird)
                0.15f + rng.nextFloat() * 0.25f
        );
    }

    // Species 3: Blackbird -- long flowing phrases, low register
    private static SpeciesParams blackbird(float pv, float tv, float prv, Random rng) {
        return new SpeciesParams(
                1500f * pv, 4500f * pv,
                3 + rng.nextInt(4), // 3-6
                0.30f * tv, 0.15f * tv,
                TrajectoryShape.U_SHAPE, prv,
                -0.09f, 0.09f, 0.12f, // rich, mellow
                0.008f, 0.06f, 0.35f, 0.85f,
                18 + rng.nextInt(20), // long trachea (large bird)
                0.35f + rng.nextFloat() * 0.25f
        );
    }

    // Species 4: Finch -- short buzzy phrases, zigzag
    private static SpeciesParams finch(float pv, float tv, float prv, Random rng) {
        return new SpeciesParams(
                3000f * pv, 7500f * pv,
                5 + rng.nextInt(6), // 5-10
                0.06f * tv, 0.03f * tv,
                TrajectoryShape.ZIGZAG, prv,
                -0.13f, 0.14f, 0.09f, // bright and buzzy
                0.015f, 0.09f, 0.25f, 0.75f,
                6 + rng.nextInt(8), // small bird
                0.2f + rng.nextFloat() * 0.3f
        );
    }

    // Species 5: Warbler -- complex multi-note, widest bandwidth
    private static SpeciesParams warbler(float pv, float tv, float prv, Random rng) {
        return new SpeciesParams(
                2500f * pv, 10000f * pv,
                6 + rng.nextInt(8), // 6-13
                0.08f * tv, 0.04f * tv,
                TrajectoryShape.COMPLEX, prv,
                -0.11f, 0.11f, 0.10f, // versatile
                0.01f, 0.10f, 0.2f, 0.8f,
                5 + rng.nextInt(10), // variable
                0.2f + rng.nextFloat() * 0.35f
        );
    }

    // Species 6: Chickadee -- distinctive two-note "fee-bee"
    private static SpeciesParams chickadee(float pv, float tv, float prv, Random rng) {
        int notes = rng.nextFloat() < 0.3f ? 3 : 2; // sometimes "fee-bee-bee"
        return new SpeciesParams(
                2500f * pv, 6500f * pv,
                notes,
                0.35f * tv, 0.10f * tv,
                TrajectoryShape.STEP, prv,
                -0.10f, 0.13f, 0.11f, // slightly buzzy
                0.01f, 0.07f, 0.3f, 0.7f,
                7 + rng.nextInt(8),
                0.25f + rng.nextFloat() * 0.25f
        );
    }

    // Species 7: Thrush -- clear flute-like notes, wide pauses, low register
    private static SpeciesParams thrush(float pv, float tv, float prv, Random rng) {
        return new SpeciesParams(
                1800f * pv, 5500f * pv,
                2 + rng.nextInt(4), // 2-5
                0.25f * tv, 0.20f * tv,
                TrajectoryShape.ASCENDING, prv,
                -0.08f, 0.08f, 0.13f, // smooth, flute-like
                0.005f, 0.05f, 0.4f, 0.85f,
                20 + rng.nextInt(25), // longest trachea
                0.4f + rng.nextFloat() * 0.25f
        );
    }

    // -------------------------------------------------------------------------
    // Trajectory shapes
    // -------------------------------------------------------------------------

    private enum TrajectoryShape {
        DESCENDING,
        ASCENDING,
        UP_DOWN,
        U_SHAPE,
        SINUSOIDAL,
        ZIGZAG,
        STEP,
        COMPLEX
    }

    // -------------------------------------------------------------------------
    // Species parameters inner class — now with per-species ODE and tracheal params
    // -------------------------------------------------------------------------

    private static final class SpeciesParams {
        final float omega0Low;
        final float omega0High;
        final int syllableCount;
        final float syllableDuration;
        final float gapDuration;
        final TrajectoryShape shape;
        final float pressureScale;

        // Species-specific ODE coefficients
        final float beta1; // Hopf bifurcation (energy injection, < 0)
        final float beta2; // Amplitude saturation
        final float beta3; // Velocity damping

        // Species-specific pressure/kappa ranges
        final float pMin, pMax;
        final float kappaMin, kappaMax;

        // Species-specific tracheal model
        final int trachealDelay;
        final float trachealReflection;

        SpeciesParams(float omega0Low, float omega0High, int syllableCount,
                      float syllableDuration, float gapDuration,
                      TrajectoryShape shape, float pressureScale,
                      float beta1, float beta2, float beta3,
                      float pMin, float pMax, float kappaMin, float kappaMax,
                      int trachealDelay, float trachealReflection) {
            this.omega0Low = omega0Low;
            this.omega0High = omega0High;
            this.syllableCount = syllableCount;
            this.syllableDuration = syllableDuration;
            this.gapDuration = gapDuration;
            this.shape = shape;
            this.pressureScale = pressureScale;
            this.beta1 = beta1;
            this.beta2 = beta2;
            this.beta3 = beta3;
            this.pMin = pMin;
            this.pMax = pMax;
            this.kappaMin = kappaMin;
            this.kappaMax = kappaMax;
            this.trachealDelay = Math.max(1, trachealDelay);
            this.trachealReflection = trachealReflection;
        }

        float omega0ForSyllable(float t, int syllableIndex) {
            float frac = (float) syllableIndex / Math.max(1, syllableCount - 1);
            float baseHz;

            switch (shape) {
                case DESCENDING:
                    baseHz = omega0High - (omega0High - omega0Low) * frac;
                    baseHz -= (omega0High - omega0Low) * 0.15f * t;
                    break;
                case ASCENDING:
                    baseHz = omega0Low + (omega0High - omega0Low) * frac;
                    baseHz += (omega0High - omega0Low) * 0.15f * t;
                    break;
                case UP_DOWN:
                    if (syllableIndex % 2 == 0) {
                        baseHz = omega0Low + (omega0High - omega0Low) * t;
                    } else {
                        baseHz = omega0High - (omega0High - omega0Low) * t;
                    }
                    break;
                case U_SHAPE:
                    baseHz = omega0High - (omega0High - omega0Low) * 2f * Math.abs(t - 0.5f);
                    baseHz = omega0Low + (omega0High - baseHz);
                    break;
                case SINUSOIDAL:
                    baseHz = (omega0Low + omega0High) * 0.5f
                            + (omega0High - omega0Low) * 0.5f
                            * (float) Math.sin(2 * Math.PI * t * 2);
                    break;
                case ZIGZAG:
                    float zigPhase = (t * 4) % 1.0f;
                    if (zigPhase < 0.5f) {
                        baseHz = omega0Low + (omega0High - omega0Low) * zigPhase * 2;
                    } else {
                        baseHz = omega0High - (omega0High - omega0Low) * (zigPhase - 0.5f) * 2;
                    }
                    break;
                case STEP:
                    baseHz = (syllableIndex == 0) ? omega0High : omega0Low;
                    break;
                case COMPLEX:
                    if (t < 0.3f) {
                        baseHz = omega0Low + (omega0High - omega0Low) * (t / 0.3f);
                    } else if (t < 0.6f) {
                        baseHz = omega0High;
                    } else {
                        float decay = (t - 0.6f) / 0.4f;
                        baseHz = omega0High - (omega0High - omega0Low) * 0.6f * decay;
                    }
                    break;
                default:
                    baseHz = (omega0Low + omega0High) * 0.5f;
                    break;
            }

            return baseHz * (float) (2 * Math.PI) / SAMPLE_RATE;
        }

        float pressureTrajectory(float t, int syllableIndex) {
            float pRange = (pMax - pMin) * pressureScale;
            float base;

            switch (shape) {
                case DESCENDING:
                    base = pMax * pressureScale - pRange * 0.3f * syllableIndex / Math.max(1, syllableCount - 1);
                    base *= (1f - 0.2f * t);
                    break;
                case ASCENDING:
                    base = pMin * pressureScale + pRange * 0.5f * syllableIndex / Math.max(1, syllableCount - 1);
                    base += pRange * 0.3f * t;
                    break;
                case UP_DOWN:
                    base = pMin * pressureScale + pRange * (float) Math.sin(Math.PI * t);
                    break;
                case U_SHAPE:
                    base = pMax * pressureScale - pRange * 0.5f * (float) Math.sin(Math.PI * t);
                    break;
                case SINUSOIDAL:
                    base = (pMin + pMax) * 0.5f * pressureScale
                            + pRange * 0.4f * (float) Math.sin(2 * Math.PI * t * 3);
                    break;
                case ZIGZAG:
                    float zigP = (t * 3) % 1.0f;
                    base = pMin * pressureScale + pRange * (zigP < 0.5f ? zigP * 2 : 2 - zigP * 2);
                    break;
                case STEP:
                    base = (syllableIndex == 0)
                            ? pMax * pressureScale * 0.8f
                            : pMax * pressureScale * 0.6f;
                    break;
                case COMPLEX:
                    base = (pMin + pMax) * 0.5f * pressureScale
                            + pRange * 0.4f * (float) Math.cos(2 * Math.PI * t);
                    break;
                default:
                    base = (pMin + pMax) * 0.5f * pressureScale;
                    break;
            }

            return Math.max(0f, base);
        }

        float kappaTrajectory(float t, int syllableIndex) {
            float kRange = kappaMax - kappaMin;

            switch (shape) {
                case DESCENDING:
                    return kappaMin + kRange * (0.3f + 0.4f * t);
                case ASCENDING:
                    return kappaMax - kRange * 0.4f * t;
                case UP_DOWN:
                    return kappaMin + kRange * (0.5f + 0.3f * (float) Math.cos(Math.PI * t));
                case U_SHAPE:
                    return kappaMin + kRange * (float) Math.sin(Math.PI * t);
                case SINUSOIDAL:
                    return kappaMin + kRange * (0.5f + 0.4f * (float) Math.sin(2 * Math.PI * t * 3 + 1.5f));
                case ZIGZAG:
                    float zigK = (t * 3 + 0.25f) % 1.0f;
                    return kappaMin + kRange * (zigK < 0.5f ? zigK * 2 : 2 - zigK * 2);
                case STEP:
                    return (syllableIndex == 0) ? kappaMin + kRange * 0.3f : kappaMin + kRange * 0.6f;
                case COMPLEX:
                    return kappaMin + kRange * (0.5f + 0.4f * (float) Math.sin(2 * Math.PI * t));
                default:
                    return (kappaMin + kappaMax) * 0.5f;
            }
        }
    }
}
