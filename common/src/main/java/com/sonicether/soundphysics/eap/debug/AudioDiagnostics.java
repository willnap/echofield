package com.sonicether.soundphysics.eap.debug;

import com.sonicether.soundphysics.eap.EarlyReflectionProcessor;
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import com.sonicether.soundphysics.eap.ExcitationSourceManager;
import com.sonicether.soundphysics.eap.ExcitationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

import java.util.List;

/**
 * Diagnostic logging utilities for the EAP audio system.
 * Logs OpenAL source state, environment profile data, and all EAP sources.
 */
public final class AudioDiagnostics {

<<<<<<< ours
    private static final Logger LOGGER = LogManager.getLogger("EchoField - EAP Diagnostics");
=======
    private static final Logger LOGGER = LogManager.getLogger("Sound Physics - EAP Diagnostics");
>>>>>>> theirs

    private AudioDiagnostics() {
    }

    /**
     * Logs the OpenAL state of a single source: AL state, gain, buffer, position, and filter parameters.
     *
     * @param label    a human-readable label for this source
     * @param sourceId the OpenAL source ID
     */
    public static void logSourceState(String label, int sourceId) {
        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        float gain = AL10.alGetSourcef(sourceId, AL10.AL_GAIN);
        int buffer = AL10.alGetSourcei(sourceId, AL10.AL_BUFFER);
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        float[] pos = new float[3];
        AL10.alGetSourcefv(sourceId, AL10.AL_POSITION, pos);
        float posX = pos[0];
        float posY = pos[1];
        float posZ = pos[2];
<<<<<<< ours
=======
        float posX = AL10.alGetSourcef(sourceId, AL10.AL_POSITION);
        float posY = AL10.alGetSourcef(sourceId, AL10.AL_POSITION + 1);
        float posZ = AL10.alGetSourcef(sourceId, AL10.AL_POSITION + 2);
>>>>>>> theirs
=======
>>>>>>> theirs

        String stateName = switch (state) {
            case AL10.AL_INITIAL -> "INITIAL";
            case AL10.AL_PLAYING -> "PLAYING";
            case AL10.AL_PAUSED -> "PAUSED";
            case AL10.AL_STOPPED -> "STOPPED";
            default -> "UNKNOWN(" + state + ")";
        };

        LOGGER.info("[{}] source={} state={} gain={} buffer={} pos=({},{},{})",
                label, sourceId, stateName, gain, buffer, posX, posY, posZ);
    }

    /**
     * Logs the state of all EAP sources: excitation sources and early reflection pool sources.
     *
     * @param excitation  the excitation source manager
     * @param reflections the early reflection processor
     */
    public static void logAllEapSources(ExcitationSourceManager excitation, EarlyReflectionProcessor reflections) {
        LOGGER.info("=== EAP Source Diagnostics ===");

        // Excitation sources
        int[] excitationIds = excitation.getSourceIds();
        ExcitationType[] types = ExcitationType.values();
        for (int i = 0; i < excitationIds.length && i < types.length; i++) {
            logSourceState("Excitation/" + types[i].name(), excitationIds[i]);
        }

        // Active excitation source info
        List<ExcitationSourceManager.SourceInfo> activeInfo = excitation.getActiveSourceInfo();
        LOGGER.info("Active excitation sources: {}", activeInfo.size());
        for (ExcitationSourceManager.SourceInfo info : activeInfo) {
            LOGGER.info("  {} src={} gain={}/{} filter={}/{} pos=({},{},{})",
                    info.type(), info.sourceId(),
                    info.currentGain(), info.targetGain(),
                    info.filterGain(), info.filterGainHF(),
                    info.posX(), info.posY(), info.posZ());
        }

        // Reflection pool sources
        int[] reflectionIds = reflections.getPoolSourceIds();
        for (int i = 0; i < reflectionIds.length; i++) {
            logSourceState("Reflection/slot" + i, reflectionIds[i]);
        }
        LOGGER.info("Active reflection slots: {}/{}", reflections.getActiveSlotCount(), EarlyReflectionProcessor.POOL_SIZE);

        LOGGER.info("=== End EAP Source Diagnostics ===");
    }

    /**
     * Logs a summary of the given environment profile.
     *
     * @param profile the environment profile to log
     */
    public static void logEnvironmentProfile(EnvironmentProfile profile) {
        if (profile == null) {
            LOGGER.info("EnvironmentProfile: null");
            return;
        }

        float[] spectral = profile.spectralProfile();
        LOGGER.info("EnvironmentProfile: enclosure={} avgDist={} scatter={} RT60={} avgAbsorption={}",
                profile.enclosureFactor(), profile.averageReturnDistance(),
                profile.scatteringDensity(), profile.estimatedRT60(), profile.averageAbsorption());
        LOGGER.info("  spectral=[{},{},{}] dirBalance={} wind={} canopy={} taps={}",
                spectral[0], spectral[1], spectral[2],
                profile.directionalBalance(), profile.windExposure(), profile.canopyCoverage(),
                profile.taps().size());
    }

}
