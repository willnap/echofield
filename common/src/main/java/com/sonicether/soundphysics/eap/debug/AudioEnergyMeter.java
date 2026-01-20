package com.sonicether.soundphysics.eap.debug;

import com.sonicether.soundphysics.eap.EarlyReflectionProcessor;
import com.sonicether.soundphysics.eap.ExcitationSourceManager;
import org.lwjgl.openal.AL10;

/**
 * Computes aggregate audio energy levels from EAP sources for debug HUD display.
 * Energy is read from OpenAL source gain values. Maintains a circular history
 * buffer of 100 entries (5 seconds at 20 tps).
 */
public final class AudioEnergyMeter {

    private static final int HISTORY_SIZE = 100; // 5 seconds at 20 tps

    private final float[] history = new float[HISTORY_SIZE];
    private int historyIndex = 0;
    private float latestEnergy = 0f;

    /**
     * Computes total energy across all excitation and reflection sources.
     * Stores the result in the history buffer.
     *
     * @param excitation  the excitation source manager
     * @param reflections the early reflection processor
     * @return total energy (sum of gains)
     */
    public float computeTotalEnergy(ExcitationSourceManager excitation, EarlyReflectionProcessor reflections) {
        float total = 0f;

        // Excitation sources
        int[] excIds = excitation.getSourceIds();
        for (int id : excIds) {
            total += Math.max(0f, AL10.alGetSourcef(id, AL10.AL_GAIN));
        }

        // Reflection pool sources
        int[] refIds = reflections.getPoolSourceIds();
        for (int id : refIds) {
            total += Math.max(0f, AL10.alGetSourcef(id, AL10.AL_GAIN));
        }

        latestEnergy = total;
        history[historyIndex] = total;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        return total;
    }

    /**
     * Returns the last computed energy value.
     */
    public float getLatestEnergy() {
        return latestEnergy;
    }

    /**
     * Returns a formatted energy string suitable for HUD display.
     * Shows energy in dB, or "-inf dB" if energy is negligible.
     */
    public String getFormattedEnergy() {
        if (latestEnergy <= 0.0001f) {
            return "EAP Energy: -inf dB";
        }
        float db = 20f * (float) Math.log10(latestEnergy);
        return String.format("EAP Energy: %.1f dB", db);
    }

    /**
     * Returns the energy history buffer (circular, length {@value HISTORY_SIZE}).
     */
    public float[] getEnergyHistory() {
        return history;
    }

    /**
     * Returns the current write index into the history buffer.
     */
    public int getHistoryIndex() {
        return historyIndex;
    }

    // ── Static convenience methods for backward compatibility ────────

    /**
     * Computes the total energy (sum of gains) across all excitation sources.
     *
     * @param excitation the excitation source manager
     * @return total gain sum
     */
    public static float totalExcitationEnergy(ExcitationSourceManager excitation) {
        int[] ids = excitation.getSourceIds();
        float total = 0f;
        for (int id : ids) {
            total += Math.max(0f, AL10.alGetSourcef(id, AL10.AL_GAIN));
        }
        return total;
    }

    /**
     * Returns the peak energy (maximum gain) across all excitation sources.
     *
     * @param excitation the excitation source manager
     * @return the maximum gain value
     */
    public static float peakExcitationEnergy(ExcitationSourceManager excitation) {
        int[] ids = excitation.getSourceIds();
        float peak = 0f;
        for (int id : ids) {
            float gain = AL10.alGetSourcef(id, AL10.AL_GAIN);
            if (gain > peak) peak = gain;
        }
        return peak;
    }
}
