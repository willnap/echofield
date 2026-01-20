package com.sonicether.soundphysics.eap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirAbsorptionTest {

    @Test
    void atZeroDistance_allBandsUnattenuated() {
        float[] gains = AirAbsorptionProcessor.computeBandGains(0f, 50f);
        assertEquals(1.0f, gains[0], 0.001f, "Low band at 0m should be 1.0");
        assertEquals(1.0f, gains[1], 0.001f, "Mid band at 0m should be 1.0");
        assertEquals(1.0f, gains[2], 0.001f, "High band at 0m should be 1.0");
    }

    @Test
    void highFrequency_attenuatesFasterThanLow() {
        float distance = 50f;
        float humidity = 50f;
        float[] gains = AirAbsorptionProcessor.computeBandGains(distance, humidity);
        assertTrue(gains[2] < gains[0],
                "High-freq gain (" + gains[2] + ") should be less than low-freq ("
                        + gains[0] + ") at 50m");
    }

    @Test
    void dryAir_attenuatesMoreThanHumid() {
        float distance = 40f;
        float[] dryGains = AirAbsorptionProcessor.computeBandGains(distance, 20f);
        float[] wetGains = AirAbsorptionProcessor.computeBandGains(distance, 80f);

        float dryMean = (dryGains[0] + dryGains[1] + dryGains[2]) / 3f;
        float wetMean = (wetGains[0] + wetGains[1] + wetGains[2]) / 3f;

        assertTrue(dryMean < wetMean,
                "Dry air mean gain (" + dryMean + ") should be less than humid ("
                        + wetMean + ") — dry air absorbs more");
    }

    @Test
    void biomeHumidity_returnsExpectedValues() {
        float desert = AirAbsorptionProcessor.biomeHumidity("minecraft:desert");
        float jungle = AirAbsorptionProcessor.biomeHumidity("minecraft:jungle");
        float ocean = AirAbsorptionProcessor.biomeHumidity("minecraft:ocean");
        float unknown = AirAbsorptionProcessor.biomeHumidity("modded:alien_world");

        assertTrue(desert < 30f, "Desert should be dry: " + desert);
        assertTrue(jungle > 70f, "Jungle should be humid: " + jungle);
        assertTrue(ocean > 60f, "Ocean should be humid: " + ocean);
        assertEquals(50f, unknown, 0.001f, "Unknown biome should default to 50%");
    }
}
