package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HyperrealityGainModelTest {

    @Nested
    class MagnitudeGain {

        @Test
        void zeroMagnitudeGivesZeroGain() {
            assertEquals(0.0f, HyperrealityPool.computeMagnitudeGain(0.0f), 1e-6f);
        }

        @Test
        void magnitudeOneGivesLog2Of2Over4() {
            assertEquals(0.25f, HyperrealityPool.computeMagnitudeGain(1.0f), 1e-5f);
        }

        @Test
        void magnitudeFifteenGivesLog2Of16Over4() {
            assertEquals(1.0f, HyperrealityPool.computeMagnitudeGain(15.0f), 1e-5f);
        }

        @Test
        void highMagnitudeClampedToOne() {
            assertEquals(1.0f, HyperrealityPool.computeMagnitudeGain(100.0f), 1e-6f);
        }
    }

    @Nested
    class DistanceRolloff {

        @Test
        void zeroDistanceGivesFullRolloff() {
            assertEquals(1.0f, HyperrealityPool.computeDistanceRolloff(0.0f, 24.0f), 1e-6f);
        }

        @Test
        void halfwayDistanceGivesHalfRolloff() {
            assertEquals(0.5f, HyperrealityPool.computeDistanceRolloff(12.0f, 24.0f), 1e-5f);
        }

        @Test
        void atMaxRangeGivesZero() {
            assertEquals(0.0f, HyperrealityPool.computeDistanceRolloff(24.0f, 24.0f), 1e-6f);
        }

        @Test
        void beyondMaxRangeClampedToZero() {
            assertEquals(0.0f, HyperrealityPool.computeDistanceRolloff(30.0f, 24.0f), 1e-6f);
        }
    }

    @Nested
    class FacingAttenuation {

        @Test
        void directlyFacingPlayerGivesFullGain() {
            assertEquals(1.0f,
                    HyperrealityPool.computeFacingAttenuation(0, 0, 1, 0, 0, 1),
                    1e-5f);
        }

        @Test
        void perpendicularGivesHalfGain() {
            assertEquals(0.5f,
                    HyperrealityPool.computeFacingAttenuation(1, 0, 0, 0, 0, 1),
                    1e-5f);
        }

        @Test
        void facingAwayFromPlayerGivesHalfGain() {
            assertEquals(0.5f,
                    HyperrealityPool.computeFacingAttenuation(0, 0, -1, 0, 0, 1),
                    1e-5f);
        }
    }

    @Nested
    class FamilyGain {

        @Test
        void voidFamilyGain() {
            assertEquals(0.08f, HyperrealityPool.familyGain(TerrainFeatureType.Family.VOID), 1e-6f);
        }

        @Test
        void surfaceFamilyGain() {
            assertEquals(0.06f, HyperrealityPool.familyGain(TerrainFeatureType.Family.SURFACE), 1e-6f);
        }

        @Test
        void groundFamilyGain() {
            assertEquals(0.03f, HyperrealityPool.familyGain(TerrainFeatureType.Family.GROUND), 1e-6f);
        }
    }

    @Nested
    class FullGainComputation {

        @Test
        void fullGainAtCloseRange() {
            float gain = HyperrealityPool.computeBaseGain(
                    4.0f, 2.0f, 24.0f,
                    0, 0, 1,
                    0, 0, 1,
                    TerrainFeatureType.Family.SURFACE);
            float magnitudeGain = (float) Math.min(1.0, Math.log(5.0) / Math.log(2.0) / 4.0);
            float distanceRolloff = 1.0f - 2.0f / 24.0f;
            float expected = magnitudeGain * distanceRolloff * 1.0f * 0.06f;
            assertEquals(expected, gain, 1e-5f);
        }

        @Test
        void fullGainAtMaxRange() {
            float gain = HyperrealityPool.computeBaseGain(
                    1.0f, 24.0f, 24.0f,
                    0, 1, 0,
                    0, 1, 0,
                    TerrainFeatureType.Family.SURFACE);
            assertEquals(0.0f, gain, 1e-6f);
        }

        @Test
        void voidFeatureHasHigherFamilyGain() {
            float voidGain = HyperrealityPool.computeBaseGain(
                    1.0f, 5.0f, 48.0f,
                    0, 1, 0, 0, 1, 0,
                    TerrainFeatureType.Family.VOID);
            float surfaceGain = HyperrealityPool.computeBaseGain(
                    1.0f, 5.0f, 24.0f,
                    0, 1, 0, 0, 1, 0,
                    TerrainFeatureType.Family.SURFACE);
            assertTrue(voidGain > surfaceGain);
        }
    }

    @Nested
    class TickLoop {

        private HyperrealityPool pool;

        @BeforeEach
        void setUp() {
            pool = new HyperrealityPool(4, null);
        }

        @Test
        void tickAdvancesFadeIn() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.fadeTicks = HyperrealitySource.FADE_DURATION;
            src.type = TerrainFeatureType.WALL;
            src.targetGain = 0.5f;
            src.magnitude = 1.0f;
            src.featureX = 5.0f;
            src.featureY = 0.0f;
            src.featureZ = 0.0f;

            pool.tick(1.0f, 1.0f, 0, 0, 0);

            assertEquals(3, src.fadeTicks);
        }

        @Test
        void tickCompleteFadeOutDeactivatesSource() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.fadeTicks = -1;
            src.type = TerrainFeatureType.WALL;
            src.saliency = 0.5f;

            pool.tick(1.0f, 1.0f, 0, 0, 0);

            assertFalse(src.active);
            assertNull(src.type);
        }

        @Test
        void tickAppliesSmoothStep() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.fadeTicks = 0;
            src.type = TerrainFeatureType.WALL;
            src.currentGain = 0.0f;
            src.targetGain = 1.0f;
            src.magnitude = 1.0f;
            src.featureX = 5.0f;

            pool.tick(1.0f, 1.0f, 0, 0, 0);

            assertTrue(src.currentGain > 0.0f, "Gain should have moved toward target");
        }

        @Test
        void tickAppliesMasterGainAndIntensity() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.fadeTicks = 0;
            src.type = TerrainFeatureType.WALL;
            src.magnitude = 4.0f;
            src.saliency = 1.0f;
            src.featureX = 5.0f;
            src.featureY = 0.0f;
            src.featureZ = 0.0f;
            src.featureNX = 0.0f;
            src.featureNY = 0.0f;
            src.featureNZ = -1.0f;
            src.material = SpectralCategory.HARD;

            pool.tick(0.5f, 0.5f, 0, 0, 0);

            assertTrue(src.targetGain > 0.0f);
            assertTrue(src.targetGain < 0.06f);
        }

        @Test
        void inactiveSourcesAreSkipped() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = false;
            src.targetGain = 1.0f;
            src.currentGain = 0.0f;

            pool.tick(1.0f, 1.0f, 0, 0, 0);

            assertEquals(0.0f, src.currentGain, 1e-6f);
        }
    }

    @Nested
    class AirAbsorption {

        @Test
        void airAbsorptionAppliedAtHalfStrength() {
            float baseHF = HyperrealitySource.materialHFGain(SpectralCategory.HARD);
            float fullAbsorption = HyperrealityPool.computeAirAbsorptionHF(20.0f, baseHF, 1.0f);
            float halfAbsorption = HyperrealityPool.computeAirAbsorptionHF(20.0f, baseHF, 0.5f);

            assertTrue(halfAbsorption > fullAbsorption);
            assertTrue(halfAbsorption < baseHF);
        }

        @Test
        void zeroDistanceNoAbsorption() {
            float hf = HyperrealityPool.computeAirAbsorptionHF(0.0f, 0.85f, 0.5f);
            assertEquals(0.85f, hf, 1e-6f);
        }

        @Test
        void absorptionNeverBelowMinimum() {
            float hf = HyperrealityPool.computeAirAbsorptionHF(1000.0f, 0.85f, 1.0f);
            assertTrue(hf >= 0.1f);
        }
    }

    @Nested
    class ReverbRouting {

        @Test
        void reverbSendGainComputesCorrectly() {
            float sendGain = HyperrealityPool.computeReverbSendGain(
                    0.8f, 1.5f, 0.7f);
            assertEquals(0.42f, sendGain, 1e-5f);
        }

        @Test
        void reverbSendGainClampsHighRT60() {
            float sendGain = HyperrealityPool.computeReverbSendGain(
                    1.0f, 5.0f, 1.0f);
            assertEquals(1.0f, sendGain, 1e-5f);
        }

        @Test
        void reverbHFCutoffComputation() {
            float hf = HyperrealityPool.computeReverbHFCutoff(0.7f);
            assertEquals(0.58f, hf, 1e-5f);
        }

        @Test
        void reverbHFCutoffClampsLow() {
            float hf = HyperrealityPool.computeReverbHFCutoff(2.0f);
            assertEquals(0.1f, hf, 1e-5f);
        }
    }
}
