package com.sonicether.soundphysics.eap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomGeometryTest {

    @Test
    void estimateVolume_fullyEnclosed_returnsReasonableVolume() {
        float volume = RoomGeometry.estimateVolume(1.0f, 5.0f);
        assertTrue(volume > 100f && volume < 2000f,
                "Volume should be reasonable for 5-block avg distance: " + volume);
    }

    @Test
    void estimateVolume_halfEnclosed_increasesEffectiveVolume() {
        float full = RoomGeometry.estimateVolume(1.0f, 5.0f);
        float half = RoomGeometry.estimateVolume(0.5f, 5.0f);
        assertTrue(half > full,
                "Lower enclosure means larger effective volume (more open): half=" + half + " full=" + full);
    }

    @Test
    void estimateVolume_openSpace_returnsLargeVolume() {
        float volume = RoomGeometry.estimateVolume(0.1f, 100.0f);
        assertTrue(volume > 5000f, "Open space should give large volume");
    }

    @Test
    void criticalDistance_smallHardRoom_isShort() {
        float dc = RoomGeometry.criticalDistance(500f, 2.0f);
        assertTrue(dc > 0.5f && dc < 1.5f,
                "Critical distance for small stone room: " + dc);
    }

    @Test
    void criticalDistance_largeRoom_isLonger() {
        float dc = RoomGeometry.criticalDistance(4000f, 1.5f);
        assertTrue(dc > 2.0f && dc < 4.0f,
                "Critical distance for large mixed room: " + dc);
    }

    @Test
    void criticalDistance_zeroRT60_clampsSafely() {
        float dc = RoomGeometry.criticalDistance(500f, 0.0f);
        assertTrue(dc > 0f, "Should not produce zero or negative");
    }
}
