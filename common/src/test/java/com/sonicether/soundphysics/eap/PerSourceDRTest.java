package com.sonicether.soundphysics.eap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PerSourceDRTest {

    @Test
    void nearSource_hasMuchLessReverb_thanDistantSource() {
        float critDist = 15.0f;
        float nearMult = RoomGeometry.reverbSendMultiplier(3.0f, critDist);
        float farMult = RoomGeometry.reverbSendMultiplier(20.0f, critDist);
        float diffDB = 20f * (float) Math.log10(farMult / nearMult);
        assertTrue(diffDB >= 6.0f,
                "Near vs far reverb difference must be >= 6dB, got: " + diffDB + "dB");
    }

    @Test
    void atZeroDistance_reverbIsMinimal() {
        float mult = RoomGeometry.reverbSendMultiplier(0f, 5.0f);
        assertEquals(0f, mult, 0.001f, "At distance 0, reverb should be 0");
    }

    @Test
    void beyondCriticalDistance_reverbIsFullyWet() {
        float mult = RoomGeometry.reverbSendMultiplier(100f, 5.0f);
        assertEquals(1.0f, mult, 0.001f, "Beyond critical distance, reverb = 1.0");
    }

    @Test
    void smoothing_limitsDeltaPerTick() {
        float maxDeltaLinear = (float) Math.pow(10.0, 3.0 / 20.0);
        float current = 0.2f;
        float target = 1.0f;
        float smoothed = PerSourceDRProcessor.smoothMultiplier(current, target);
        float ratio = smoothed / current;
        assertTrue(ratio <= maxDeltaLinear + 0.01f,
                "Smoothed step should not exceed 3dB: ratio=" + ratio);
    }
}
