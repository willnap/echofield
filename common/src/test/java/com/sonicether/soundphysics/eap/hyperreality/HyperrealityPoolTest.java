package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HyperrealityPoolTest {

    private static final int TEST_POOL_SIZE = 6;
    private HyperrealityPool pool;

    @BeforeEach
    void setUp() {
        pool = new HyperrealityPool(TEST_POOL_SIZE, null);
    }

    private static TerrainFeature feature(TerrainFeatureType type, float x, float y, float z,
                                          float magnitude, float saliency, SpectralCategory mat) {
        return new TerrainFeature(type, x, y, z, 0.0f, 1.0f, 0.0f, magnitude, saliency, mat);
    }

    private static TerrainFeature feature(TerrainFeatureType type, float x, float y, float z,
                                          float magnitude, float saliency) {
        return feature(type, x, y, z, magnitude, saliency, SpectralCategory.DEFAULT);
    }

    @Nested
    class Reconciliation {

        @Test
        void emptyFeaturesResultsInNoActiveSources() {
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), Collections.emptyList(), 0, 0, 0, 24);
            assertTrue(result.matched().isEmpty());
            assertTrue(result.orphaned().isEmpty());
            assertTrue(result.toAllocate().isEmpty());
            assertTrue(result.toSteal().isEmpty());
        }

        @Test
        void newFeaturesAreAllocated() {
            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 5, 10, 5, 1.0f, 0.8f),
                    feature(TerrainFeatureType.CEILING, 10, 20, 10, 0.5f, 0.6f)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            assertTrue(result.matched().isEmpty());
            assertTrue(result.orphaned().isEmpty());
            assertEquals(2, result.toAllocate().size());
            assertTrue(result.toSteal().isEmpty());
        }

        @Test
        void existingSourceMatchesByTypeAndProximity() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 5.0f;
            src.featureY = 10.0f;
            src.featureZ = 5.0f;

            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 5.5f, 10.0f, 5.5f, 1.0f, 0.8f)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            assertEquals(1, result.matched().size());
            assertEquals(0, result.matched().get(0).sourceIndex());
            assertTrue(result.orphaned().isEmpty());
            assertTrue(result.toAllocate().isEmpty());
        }

        @Test
        void sourceNotMatchedWhenTypeDiffers() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 5.0f;
            src.featureY = 10.0f;
            src.featureZ = 5.0f;

            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.CEILING, 5.0f, 10.0f, 5.0f, 1.0f, 0.8f)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            assertTrue(result.matched().isEmpty());
            assertEquals(1, result.orphaned().size());
            assertEquals(1, result.toAllocate().size());
        }

        @Test
        void sourceNotMatchedWhenTooFar() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 5.0f;
            src.featureY = 10.0f;
            src.featureZ = 5.0f;

            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 8.0f, 10.0f, 5.0f, 1.0f, 0.8f)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            assertTrue(result.matched().isEmpty());
            assertEquals(1, result.orphaned().size());
            assertEquals(1, result.toAllocate().size());
        }

        @Test
        void unmatchedActiveSourcesAreOrphaned() {
            HyperrealitySource src0 = pool.getSources()[0];
            src0.active = true;
            src0.type = TerrainFeatureType.WALL;
            src0.featureX = 100.0f;
            src0.featureY = 100.0f;
            src0.featureZ = 100.0f;

            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 0, 0, 0, 1.0f, 0.8f)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            assertEquals(1, result.orphaned().size());
            assertEquals(0, result.orphaned().get(0).intValue());
        }

        @Test
        void newFeaturesAreSortedBySaliencyDescending() {
            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 1, 0, 0, 1.0f, 0.3f),
                    feature(TerrainFeatureType.CEILING, 2, 0, 0, 1.0f, 0.9f),
                    feature(TerrainFeatureType.STEP, 3, 0, 0, 1.0f, 0.6f)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            List<TerrainFeature> alloc = result.toAllocate();
            assertEquals(3, alloc.size());
            assertEquals(0.9f, alloc.get(0).saliency(), 1e-6f);
            assertEquals(0.6f, alloc.get(1).saliency(), 1e-6f);
            assertEquals(0.3f, alloc.get(2).saliency(), 1e-6f);
        }
    }

    @Nested
    class PriorityStealing {

        @Test
        void stealWhenPoolIsFull() {
            for (int i = 0; i < TEST_POOL_SIZE; i++) {
                HyperrealitySource src = pool.getSources()[i];
                src.active = true;
                src.type = TerrainFeatureType.STEP;
                src.featureX = (float) (i * 10);
                src.featureY = 0.0f;
                src.featureZ = 0.0f;
                src.saliency = 0.1f;
                src.distance = 20.0f;
            }

            List<TerrainFeature> features = new ArrayList<>();
            for (int i = 0; i < TEST_POOL_SIZE; i++) {
                features.add(feature(TerrainFeatureType.STEP,
                        (float) (i * 10), 0, 0, 0.1f, 0.1f));
            }
            features.add(feature(TerrainFeatureType.PASSAGE, 1, 1, 1, 2.0f, 1.0f));

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);

            assertEquals(1, result.toSteal().size());
            HyperrealityPool.StealDecision steal = result.toSteal().get(0);
            assertEquals(TerrainFeatureType.PASSAGE, steal.feature().type());
        }

        @Test
        void noStealWhenNewFeatureHasLowerPriority() {
            for (int i = 0; i < TEST_POOL_SIZE; i++) {
                HyperrealitySource src = pool.getSources()[i];
                src.active = true;
                src.type = TerrainFeatureType.PASSAGE;
                src.featureX = (float) (i * 5);
                src.featureY = 0.0f;
                src.featureZ = 0.0f;
                src.saliency = 1.0f;
                src.distance = 2.0f;
            }

            List<TerrainFeature> features = new ArrayList<>();
            for (int i = 0; i < TEST_POOL_SIZE; i++) {
                features.add(feature(TerrainFeatureType.PASSAGE,
                        (float) (i * 5), 0, 0, 1.0f, 1.0f));
            }
            features.add(feature(TerrainFeatureType.STEP, 50, 0, 0, 0.1f, 0.01f));

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);

            assertTrue(result.toSteal().isEmpty());
        }

        @Test
        void stealTargetsLowestPrioritySource() {
            for (int i = 0; i < TEST_POOL_SIZE; i++) {
                HyperrealitySource src = pool.getSources()[i];
                src.active = true;
                src.type = TerrainFeatureType.WALL;
                src.featureX = (float) (i * 10);
                src.featureY = 0.0f;
                src.featureZ = 0.0f;
                src.saliency = (float) (i + 1) * 0.1f;
                src.distance = 5.0f;
            }

            List<TerrainFeature> features = new ArrayList<>();
            for (int i = 0; i < TEST_POOL_SIZE; i++) {
                features.add(feature(TerrainFeatureType.WALL,
                        (float) (i * 10), 0, 0, 1.0f, (float) (i + 1) * 0.1f));
            }
            features.add(feature(TerrainFeatureType.PASSAGE, 2, 2, 2, 2.0f, 1.0f));

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);

            assertEquals(1, result.toSteal().size());
            assertEquals(0, result.toSteal().get(0).victimIndex());
        }
    }

    @Nested
    class Hysteresis {

        @Test
        void featureNearScanBoundaryKeptAlive() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 23.5f;
            src.featureY = 0.0f;
            src.featureZ = 0.0f;

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), Collections.emptyList(), 0, 0, 0, 24);

            assertTrue(result.orphaned().isEmpty());
            assertTrue(result.hysteresisKept().contains(0));
        }

        @Test
        void featureWellInsideBoundaryIsOrphanedNormally() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 15.0f;
            src.featureY = 0.0f;
            src.featureZ = 0.0f;

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), Collections.emptyList(), 0, 0, 0, 24);

            assertEquals(1, result.orphaned().size());
            assertTrue(result.hysteresisKept().isEmpty());
        }

        @Test
        void featureBeyondBoundaryIsOrphaned() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 26.0f;
            src.featureY = 0.0f;
            src.featureZ = 0.0f;

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), Collections.emptyList(), 0, 0, 0, 24);

            assertEquals(1, result.orphaned().size());
            assertTrue(result.hysteresisKept().isEmpty());
        }
    }

    @Nested
    class ApplyReconciliation {

        @Test
        void applyAllocatesNewFeatures() {
            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 5, 10, 5, 1.0f, 0.8f, SpectralCategory.HARD)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);

            pool.applyReconciliation(result, 0, 0, 0);

            assertEquals(1, pool.getActiveCount());
            HyperrealitySource src = findActiveSource(pool);
            assertNotNull(src);
            assertEquals(TerrainFeatureType.WALL, src.type);
            assertEquals(5.0f, src.featureX, 1e-6f);
            assertEquals(10.0f, src.featureY, 1e-6f);
            assertEquals(5.0f, src.featureZ, 1e-6f);
            assertEquals(SpectralCategory.HARD, src.material);
            assertTrue(src.active);
            assertEquals(HyperrealitySource.FADE_DURATION, src.fadeTicks);
        }

        @Test
        void applyOrphansUnmatchedSources() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 100.0f;
            src.featureY = 0.0f;
            src.featureZ = 0.0f;
            src.fadeTicks = 0;

            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), Collections.emptyList(), 0, 0, 0, 24);
            pool.applyReconciliation(result, 0, 0, 0);

            assertEquals(-HyperrealitySource.FADE_DURATION, src.fadeTicks);
        }

        @Test
        void applyUpdatesMatchedSourceTargets() {
            HyperrealitySource src = pool.getSources()[0];
            src.active = true;
            src.type = TerrainFeatureType.WALL;
            src.featureX = 5.0f;
            src.featureY = 10.0f;
            src.featureZ = 5.0f;
            src.material = SpectralCategory.HARD;
            src.saliency = 0.5f;
            src.magnitude = 0.5f;

            List<TerrainFeature> features = List.of(
                    feature(TerrainFeatureType.WALL, 5.5f, 10.0f, 5.5f, 0.8f, 0.9f, SpectralCategory.WOOD)
            );
            HyperrealityPool.ReconciliationResult result =
                    HyperrealityPool.computeReconciliation(
                            pool.getSources(), features, 0, 0, 0, 24);
            pool.applyReconciliation(result, 0, 0, 0);

            assertEquals(5.5f, src.targetX, 1e-6f);
            assertEquals(10.0f, src.targetY, 1e-6f);
            assertEquals(5.5f, src.targetZ, 1e-6f);
            assertEquals(5.5f, src.featureX, 1e-6f);
            assertEquals(0.9f, src.saliency, 1e-6f);
            assertEquals(0.8f, src.magnitude, 1e-6f);
            assertEquals(SpectralCategory.WOOD, src.material);
        }
    }

    private static HyperrealitySource findActiveSource(HyperrealityPool pool) {
        for (HyperrealitySource src : pool.getSources()) {
            if (src.active) {
                return src;
            }
        }
        return null;
    }
}
