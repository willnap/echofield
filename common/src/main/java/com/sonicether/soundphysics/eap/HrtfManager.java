package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.Loggers;
import org.lwjgl.openal.ALC10;

/**
 * Manages HRTF activation and verification on the OpenAL device.
 * HRTF is REQUIRED — without it, no binaural cues are delivered and
 * the system MUST refuse to process audio (spec 4.1).
 *
 * Does not attempt programmatic activation via alcResetDeviceSOFT()
 * to avoid interfering with user's audio device settings. Activation
 * is delegated to OpenAL Soft configuration (alsoft.conf / alsoftrc).
 */
public final class HrtfManager {

    private static final int RECHECK_INTERVAL_TICKS = 1200; // 60 seconds at 20 TPS

    // ALC_SOFT_HRTF constants — defined inline to avoid dependency on SOFTHrtf class
    // which may not be available in all LWJGL builds bundled with Minecraft.
    private static final int ALC_HRTF_SOFT = 0x1992;
    private static final int ALC_HRTF_SPECIFIER_SOFT = 0x1994;

    private boolean hrtfActive = false;
    private String hrtfName = "none";
    private long lastCheckDevice = 0L;
    private int ticksSinceLastCheck = 0;

    /**
     * Queries the current OpenAL device for HRTF status.
     *
     * @param device the ALC device handle
     * @return true if HRTF is active
     */
    public boolean verifyHrtf(long device) {
        if (device == 0L) {
            Loggers.log("HrtfManager: FATAL — no device handle, cannot verify HRTF");
            hrtfActive = false;
            return false;
        }

        lastCheckDevice = device;
        int hrtfState = ALC10.alcGetInteger(device, ALC_HRTF_SOFT);
        hrtfActive = (hrtfState == ALC10.ALC_TRUE);

        if (hrtfActive) {
            String specifier = ALC10.alcGetString(device, ALC_HRTF_SPECIFIER_SOFT);
            hrtfName = specifier != null ? specifier : "unknown";
            Loggers.log("HrtfManager: HRTF active — dataset: {}", hrtfName);
        } else {
            Loggers.log("HrtfManager: FATAL — HRTF is NOT active.");
            Loggers.log("HrtfManager: EchoField REFUSES TO PROCESS — binaural cues require HRTF.");
            Loggers.log("HrtfManager: Fix: set 'hrtf = true' in ~/.alsoftrc and restart.");
        }

        ticksSinceLastCheck = 0;
        return hrtfActive;
    }

    /**
     * Periodic re-check. Call once per tick. Re-verifies HRTF every 60 seconds
     * to detect runtime deactivation (e.g., audio device change).
     */
    public void tick() {
        ticksSinceLastCheck++;
        if (ticksSinceLastCheck >= RECHECK_INTERVAL_TICKS && lastCheckDevice != 0L) {
            verifyHrtf(lastCheckDevice);
        }
    }

    /**
     * Returns true if HRTF is active. When false, ALL EchoField audio
     * processing must be skipped — the system refuses to run without HRTF.
     */
    public boolean isHrtfActive() {
        return hrtfActive;
    }

    public String getHrtfName() {
        return hrtfName;
    }

    /**
     * Returns a persistent warning string when HRTF is off.
     * Displayed on-screen every frame — cannot be dismissed.
     */
    public String getStatusText() {
        return hrtfActive
                ? "HRTF: ON (" + hrtfName + ")"
                : "HRTF: OFF — EchoField DISABLED. Set hrtf=true in ~/.alsoftrc";
    }
}
