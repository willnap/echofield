package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

class HyperrealitySourceTest {

    private HyperrealitySource source;

    @BeforeEach
    void setUp() {
        source = new HyperrealitySource(0);
    }

    @Nested
    class MaterialHFMapping {

        @Test
        void hardMaterialReturns085() {
            assertEquals(0.85f, HyperrealitySource.materialHFGain(SpectralCategory.HARD), 1e-6f);
        }

        @Test
        void woodMaterialReturns055() {
            assertEquals(0.55f, HyperrealitySource.materialHFGain(SpectralCategory.WOOD), 1e-6f);
        }

        @Test
        void softMaterialReturns030() {
            assertEquals(0.30f, HyperrealitySource.materialHFGain(SpectralCategory.SOFT), 1e-6f);
        }

        @Test
        void foliageMaterialReturns035() {
            assertEquals(0.35f, HyperrealitySource.materialHFGain(SpectralCategory.FOLIAGE), 1e-6f);
        }

        @Test
        void defaultMaterialReturns050() {
            assertEquals(0.50f, HyperrealitySource.materialHFGain(SpectralCategory.DEFAULT), 1e-6f);
        }
    }

    @Nested
    class PriorityComputation {

        @Test
        void priorityAtZeroDistance() {
            source.saliency = 0.8f;
            source.distance = 0.0f;
            source.type = TerrainFeatureType.PASSAGE;
            assertEquals(1.2f, source.computePriority(), 1e-5f);
        }

        @Test
        void priorityAtTenBlocks() {
            source.saliency = 1.0f;
            source.distance = 10.0f;
            source.type = TerrainFeatureType.WALL;
            float expected = 1.0f * (1.0f / 11.0f) * 1.0f;
            assertEquals(expected, source.computePriority(), 1e-5f);
        }

        @Test
        void priorityIncorporatesFamilyWeight() {
            source.saliency = 0.5f;
            source.distance = 5.0f;

            source.type = TerrainFeatureType.PASSAGE;
            float voidPriority = source.computePriority();

            source.type = TerrainFeatureType.WALL;
            float surfacePriority = source.computePriority();

            source.type = TerrainFeatureType.STEP;
            float groundPriority = source.computePriority();

            assertTrue(voidPriority > surfacePriority);
            assertTrue(surfacePriority > groundPriority);
            assertEquals(voidPriority / surfacePriority, 1.5f, 0.01f);
        }

        @Test
        void priorityIsZeroWhenSaliencyIsZero() {
            source.saliency = 0.0f;
            source.distance = 5.0f;
            source.type = TerrainFeatureType.WALL;
            assertEquals(0.0f, source.computePriority(), 1e-6f);
        }
    }

    @Nested
    class SmoothStep {

        @Test
        void gainMovesTowardTarget() {
            source.currentGain = 0.0f;
            source.targetGain = 1.0f;
            source.smoothStep();
            assertEquals(0.12f, source.currentGain, 1e-5f);
        }

        @Test
        void gainConvergesOverManySteps() {
            source.currentGain = 0.0f;
            source.targetGain = 1.0f;
            for (int i = 0; i < 100; i++) {
                source.smoothStep();
            }
            assertEquals(1.0f, source.currentGain, 0.001f);
        }

        @Test
        void positionMovesTowardTarget() {
            source.currentX = 0.0f;
            source.currentY = 0.0f;
            source.currentZ = 0.0f;
            source.targetX = 10.0f;
            source.targetY = 20.0f;
            source.targetZ = 30.0f;
            source.smoothStep();
            assertEquals(0.8f, source.currentX, 1e-5f);
            assertEquals(1.6f, source.currentY, 1e-5f);
            assertEquals(2.4f, source.currentZ, 1e-5f);
        }

        @Test
        void filterHFMovesTowardTarget() {
            source.currentFilterHF = 1.0f;
            source.targetFilterHF = 0.3f;
            source.smoothStep();
            assertEquals(0.93f, source.currentFilterHF, 1e-5f);
        }

        @Test
        void noMovementWhenAtTarget() {
            source.currentGain = 0.5f;
            source.targetGain = 0.5f;
            source.currentX = 3.0f;
            source.targetX = 3.0f;
            source.currentFilterHF = 0.85f;
            source.targetFilterHF = 0.85f;
            source.smoothStep();
            assertEquals(0.5f, source.currentGain, 1e-6f);
            assertEquals(3.0f, source.currentX, 1e-6f);
            assertEquals(0.85f, source.currentFilterHF, 1e-6f);
        }
    }

    @Nested
    class FadeLifecycle {

        @Test
        void fadeInDecrementsTicksAndScalesGain() {
            source.fadeTicks = HyperrealitySource.FADE_DURATION;
            source.targetGain = 0.8f;
            float fadeScale = source.computeFadeScale();
            assertEquals(0.25f, fadeScale, 1e-5f);

            source.fadeTicks = 3;
            fadeScale = source.computeFadeScale();
            assertEquals(0.5f, fadeScale, 1e-5f);

            source.fadeTicks = 1;
            fadeScale = source.computeFadeScale();
            assertEquals(1.0f, fadeScale, 1e-5f);
        }

        @Test
        void fadeOutUsesNegativeTicks() {
            source.fadeTicks = -HyperrealitySource.FADE_DURATION;
            float fadeScale = source.computeFadeScale();
            assertEquals(0.75f, fadeScale, 1e-5f);

            source.fadeTicks = -1;
            fadeScale = source.computeFadeScale();
            assertEquals(0.0f, fadeScale, 1e-5f);
        }

        @Test
        void steadyStateReturnsFullScale() {
            source.fadeTicks = 0;
            assertEquals(1.0f, source.computeFadeScale(), 1e-6f);
        }
    }

    @Nested
    class Reset {

        @Test
        void resetClearsAllState() {
            source.type = TerrainFeatureType.WALL;
            source.featureX = 10.0f;
            source.featureY = 20.0f;
            source.featureZ = 30.0f;
            source.material = SpectralCategory.HARD;
            source.saliency = 0.9f;
            source.currentGain = 0.5f;
            source.targetGain = 0.8f;
            source.currentX = 5.0f;
            source.currentY = 6.0f;
            source.currentZ = 7.0f;
            source.targetX = 8.0f;
            source.targetY = 9.0f;
            source.targetZ = 10.0f;
            source.currentFilterHF = 0.85f;
            source.targetFilterHF = 0.5f;
            source.active = true;
            source.fadeTicks = 3;
            source.distance = 12.0f;

            source.reset();

            assertNull(source.type);
            assertEquals(0.0f, source.featureX, 1e-6f);
            assertEquals(0.0f, source.featureY, 1e-6f);
            assertEquals(0.0f, source.featureZ, 1e-6f);
            assertNull(source.material);
            assertEquals(0.0f, source.saliency, 1e-6f);
            assertEquals(0.0f, source.currentGain, 1e-6f);
            assertEquals(0.0f, source.targetGain, 1e-6f);
            assertEquals(0.0f, source.currentX, 1e-6f);
            assertEquals(0.0f, source.currentY, 1e-6f);
            assertEquals(0.0f, source.currentZ, 1e-6f);
            assertEquals(0.0f, source.targetX, 1e-6f);
            assertEquals(0.0f, source.targetY, 1e-6f);
            assertEquals(0.0f, source.targetZ, 1e-6f);
            assertEquals(1.0f, source.currentFilterHF, 1e-6f);
            assertEquals(1.0f, source.targetFilterHF, 1e-6f);
            assertFalse(source.active);
            assertEquals(0, source.fadeTicks);
            assertEquals(0.0f, source.distance, 1e-6f);
        }
    }

    @Nested
    class SlotIndex {

        @Test
        void slotIndexIsSetAtConstruction() {
            HyperrealitySource s = new HyperrealitySource(7);
            assertEquals(7, s.getSlotIndex());
        }
    }
}
