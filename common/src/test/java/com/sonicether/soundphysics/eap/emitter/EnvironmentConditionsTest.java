package com.sonicether.soundphysics.eap.emitter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentConditionsTest {

    private static final float TOLERANCE = 0.01f;

    // ---- computeDaylight ----

    @Test
    void daylight_noon_isPeak() {
        // Tick 6000 = noon, should be 1.0
        assertEquals(1.0f, EnvironmentConditions.computeDaylight(6000), TOLERANCE);
    }

    @Test
    void daylight_midnight_isTrough() {
        // Tick 18000 = midnight, should be 0.0
        assertEquals(0.0f, EnvironmentConditions.computeDaylight(18000), TOLERANCE);
    }

    @Test
    void daylight_sunrise_isHalf() {
        // Tick 0 = 6:00 AM (sunrise), should be ~0.5
        assertEquals(0.5f, EnvironmentConditions.computeDaylight(0), TOLERANCE);
    }

    @Test
    void daylight_sunset_isHalf() {
        // Tick 12000 = 6:00 PM (sunset), should be ~0.5
        assertEquals(0.5f, EnvironmentConditions.computeDaylight(12000), TOLERANCE);
    }

    @Test
    void daylight_alwaysInRange() {
        for (long t = 0; t < 24000; t += 100) {
            float val = EnvironmentConditions.computeDaylight(t);
            assertTrue(val >= 0f && val <= 1f,
                    "daylight out of [0,1] at tick " + t + ": " + val);
        }
    }

    // ---- computeDawnChorus ----

    @Test
    void dawnChorus_atDawn_isPeak() {
        // Tick 0 = 6:00 AM (dawn center), should be 1.0
        assertEquals(1.0f, EnvironmentConditions.computeDawnChorus(0), TOLERANCE);
    }

    @Test
    void dawnChorus_atNoon_isZero() {
        // Tick 6000 = noon, well outside dawn window
        assertEquals(0.0f, EnvironmentConditions.computeDawnChorus(6000), TOLERANCE);
    }

    @Test
    void dawnChorus_atMidnight_isZero() {
        // Tick 18000 = midnight, well outside dawn window
        assertEquals(0.0f, EnvironmentConditions.computeDawnChorus(18000), TOLERANCE);
    }

    @Test
    void dawnChorus_justBeforeDawnWindow_isZero() {
        // Tick 23000 = 1000 ticks before dawn center (wrap), should be at edge = 0.0
        assertEquals(0.0f, EnvironmentConditions.computeDawnChorus(23000), TOLERANCE);
    }

    @Test
    void dawnChorus_withinDawnWindow_isPositive() {
        // Tick 500 = 500 ticks after dawn center, should be positive
        float val = EnvironmentConditions.computeDawnChorus(500);
        assertTrue(val > 0f && val < 1f,
                "dawnChorus should be positive but <1 at tick 500: " + val);
    }

    @Test
    void dawnChorus_wrapAround_isPositive() {
        // Tick 23500 = 500 ticks before dawn center (wrap), should be positive
        float val = EnvironmentConditions.computeDawnChorus(23500);
        assertTrue(val > 0f && val < 1f,
                "dawnChorus should be positive at tick 23500 (near dawn via wrap): " + val);
    }

    @Test
    void dawnChorus_alwaysInRange() {
        for (long t = 0; t < 24000; t += 100) {
            float val = EnvironmentConditions.computeDawnChorus(t);
            assertTrue(val >= 0f && val <= 1f,
                    "dawnChorus out of [0,1] at tick " + t + ": " + val);
        }
    }

    // ---- computeDuskFactor ----

    @Test
    void duskFactor_atSunset_isPeak() {
        // Tick 12000 = sunset, should be 1.0
        assertEquals(1.0f, EnvironmentConditions.computeDuskFactor(12000), TOLERANCE);
    }

    @Test
    void duskFactor_atDawn_isZero() {
        // Tick 0 = dawn, well outside dusk window
        assertEquals(0.0f, EnvironmentConditions.computeDuskFactor(0), TOLERANCE);
    }

    @Test
    void duskFactor_atMidnight_isZero() {
        // Tick 18000 = midnight, well outside dusk window
        assertEquals(0.0f, EnvironmentConditions.computeDuskFactor(18000), TOLERANCE);
    }

    @Test
    void duskFactor_atEdge_isZero() {
        // Tick 11000 = 1000 ticks before sunset, should be at edge = 0.0
        assertEquals(0.0f, EnvironmentConditions.computeDuskFactor(11000), TOLERANCE);
    }

    @Test
    void duskFactor_withinDuskWindow_isPositive() {
        // Tick 12500 = 500 ticks after sunset center, should be positive
        float val = EnvironmentConditions.computeDuskFactor(12500);
        assertTrue(val > 0f && val < 1f,
                "duskFactor should be positive but <1 at tick 12500: " + val);
    }

    @Test
    void duskFactor_alwaysInRange() {
        for (long t = 0; t < 24000; t += 100) {
            float val = EnvironmentConditions.computeDuskFactor(t);
            assertTrue(val >= 0f && val <= 1f,
                    "duskFactor out of [0,1] at tick " + t + ": " + val);
        }
    }

    // ---- Boundary / wrap-around tests ----

    @Test
    void boundary_tick23999_allInRange() {
        float dl = EnvironmentConditions.computeDaylight(23999);
        float dc = EnvironmentConditions.computeDawnChorus(23999);
        float df = EnvironmentConditions.computeDuskFactor(23999);
        assertTrue(dl >= 0f && dl <= 1f, "daylight at 23999: " + dl);
        assertTrue(dc >= 0f && dc <= 1f, "dawnChorus at 23999: " + dc);
        assertTrue(df >= 0f && df <= 1f, "duskFactor at 23999: " + df);
    }

    @Test
    void boundary_tick0_matchesCycleStart() {
        // Tick 0 and tick 24000 should give identical results (mod 24000 in compute())
        float dl0 = EnvironmentConditions.computeDaylight(0);
        float dl24k = EnvironmentConditions.computeDaylight(24000);
        assertEquals(dl0, dl24k, TOLERANCE, "daylight should wrap at 24000");

        float dc0 = EnvironmentConditions.computeDawnChorus(0);
        float dc24k = EnvironmentConditions.computeDawnChorus(24000);
        assertEquals(dc0, dc24k, TOLERANCE, "dawnChorus should wrap at 24000");

        float df0 = EnvironmentConditions.computeDuskFactor(0);
        float df24k = EnvironmentConditions.computeDuskFactor(24000);
        assertEquals(df0, df24k, TOLERANCE, "duskFactor should wrap at 24000");
    }

    // ---- Record constructor sanity ----

    @Test
    void record_accessors_returnConstructorValues() {
        EnvironmentConditions ec = new EnvironmentConditions(
            0.8f, 0.3f, 0.1f,
            true, false,
            0.7f, true, false, true,
            0.5f, 0.6f, 10.0f
        );

        assertEquals(0.8f, ec.daylight(), TOLERANCE);
        assertEquals(0.3f, ec.dawnChorus(), TOLERANCE);
        assertEquals(0.1f, ec.duskFactor(), TOLERANCE);
        assertTrue(ec.isRaining());
        assertFalse(ec.isThundering());
        assertEquals(0.7f, ec.temperature(), TOLERANCE);
        assertTrue(ec.isForest());
        assertFalse(ec.isSwamp());
        assertTrue(ec.isDesert());
        assertEquals(0.5f, ec.windExposure(), TOLERANCE);
        assertEquals(0.6f, ec.enclosure(), TOLERANCE);
        assertEquals(10.0f, ec.roomSize(), TOLERANCE);
    }
}
