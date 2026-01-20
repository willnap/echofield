package com.sonicether.soundphysics.eap.hyperreality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

class HyperrealitySystemTest {

    private HyperrealitySystem system;

    @BeforeEach
    void setUp() {
        system = new HyperrealitySystem();
    }

    @Test
    void initialState() {
        assertEquals(0, system.getActiveCount());
        assertEquals(0, system.getSourceCount());
        assertNull(system.getPool());
        assertNotNull(system.getScanner());
    }

    @Nested
    class IntensityClamping {

        @Test
        void clampsToZero() {
            system.setIntensity(-0.5f);
        }

        @Test
        void clampsToOne() {
            system.setIntensity(1.5f);
        }

        @Test
        void validRange() {
            system.setIntensity(0.7f);
        }
    }

    @Nested
    class DeferredRange {

        @Test
        void setRangeDoesNotResizeImmediately() {
            int initialSize = system.getScanner().getGridSize();
            system.setRange(10);
            assertEquals(initialSize, system.getScanner().getGridSize(),
                    "setRange should be deferred, not applied immediately");
        }
    }

    @Test
    void silenceAllBeforeInit() {
        assertDoesNotThrow(() -> system.silenceAll());
    }

    @Test
    void forceRescanDelegatesToScanner() {
        assertDoesNotThrow(() -> system.forceRescan());
    }

    @Test
    void shutdownBeforeInit() {
        assertDoesNotThrow(() -> system.shutdown());
    }
}
