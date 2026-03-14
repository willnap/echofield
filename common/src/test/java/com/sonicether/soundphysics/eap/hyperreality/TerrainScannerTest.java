// $TEST/TerrainScannerTest.java
package com.sonicether.soundphysics.eap.hyperreality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonicether.soundphysics.eap.SpectralCategory;

<<<<<<< ours
=======
import java.util.List;

>>>>>>> theirs
=======
>>>>>>> theirs
import static org.junit.jupiter.api.Assertions.*;

class TerrainScannerTest {

    // ── Task 4: lifecycle / state-machine tests ──────────────────────────

    @Nested
    class Lifecycle {

        private TerrainScanner scanner;

        @BeforeEach
        void setUp() {
            scanner = new TerrainScanner();
        }

        @Test
        void defaultRange() {
            assertEquals(24, scanner.getScanRadius());
            assertEquals(49, scanner.getGridSize());
        }

        @Test
        void setRangeUpdatesGridSize() {
            scanner.setRange(10);
            assertEquals(10, scanner.getScanRadius());
            assertEquals(21, scanner.getGridSize());
        }

        @Test
        void setRangeClampsMinimum() {
            scanner.setRange(2);
            assertEquals(4, scanner.getScanRadius());
            assertEquals(9, scanner.getGridSize());
        }

        @Test
        void setRangeClampsMaximum() {
            scanner.setRange(100);
            assertEquals(48, scanner.getScanRadius());
            assertEquals(97, scanner.getGridSize());
        }

        @Test
        void beginCycleResetsSliceCounter() {
            scanner.beginCycle(0, 64, 0);
            // After beginCycle, scanner should be ready to scan from slice 0
            assertFalse(scanner.isCycleComplete());
            assertEquals(0, scanner.getCurrentSlice());
        }

        @Test
        void shouldRescanReturnsTrueInitially() {
            // No cycle has ever completed, so any position should trigger a scan
            assertTrue(scanner.shouldRescan(0, 64, 0));
        }

        @Test
        void shouldRescanReturnsFalseAfterRecentCycle() {
            scanner.beginCycle(100, 64, 200);
            scanner.markCycleComplete();
            // Same position, 0 ticks elapsed
            assertFalse(scanner.shouldRescan(100, 64, 200));
        }

        @Test
        void shouldRescanReturnsTrueWhenPlayerMovedFar() {
            scanner.beginCycle(100, 64, 200);
            scanner.markCycleComplete();
            // Moved 5 blocks (> 4.0 threshold)
            assertTrue(scanner.shouldRescan(105, 64, 200));
        }

        @Test
        void shouldRescanReturnsTrueAfterRescanInterval() {
            scanner.beginCycle(100, 64, 200);
            scanner.markCycleComplete();
            // Simulate 40 ticks passing
            for (int i = 0; i < 40; i++) {
                scanner.incrementTickCounter();
            }
            assertTrue(scanner.shouldRescan(100, 64, 200));
        }

        @Test
        void shouldRescanReturnsTrueAfterForceRescan() {
            scanner.beginCycle(100, 64, 200);
            scanner.markCycleComplete();
            assertFalse(scanner.shouldRescan(100, 64, 200));
            scanner.forceRescan();
            assertTrue(scanner.shouldRescan(100, 64, 200));
        }

        @Test
        void slicesPerCycleCoversFullGrid() {
            // Default grid = 49, 7 slices per tick → ceil(49/7) = 7 ticks to complete
            scanner.beginCycle(0, 64, 0);
            int ticks = 0;
            while (!scanner.isCycleComplete() && ticks < 20) {
                scanner.advanceSlice();
                ticks++;
            }
            assertTrue(scanner.isCycleComplete());
            assertEquals(7, ticks);
        }

        @Test
        void slicesPerCycleWithCustomRange() {
            scanner.setRange(10); // grid = 21, ceil(21/7) = 3 ticks
            scanner.beginCycle(0, 64, 0);
            int ticks = 0;
            while (!scanner.isCycleComplete() && ticks < 20) {
                scanner.advanceSlice();
                ticks++;
            }
            assertTrue(scanner.isCycleComplete());
            assertEquals(3, ticks);
        }

        @Test
        void getFeaturesEmptyBeforeAnyCycle() {
            assertTrue(scanner.getFeatures().isEmpty());
        }

        @Test
        void gridsInitializedToUnprobed() {
            scanner.beginCycle(0, 64, 0);
            int[][] floor = scanner.getFloorArray();
            int[][] ceiling = scanner.getCeilingArray();
            for (int x = 0; x < scanner.getGridSize(); x++) {
                for (int z = 0; z < scanner.getGridSize(); z++) {
                    assertEquals(TerrainScanner.UNPROBED, floor[x][z],
                            "floor[" + x + "][" + z + "] should be UNPROBED");
                    assertEquals(TerrainScanner.UNPROBED, ceiling[x][z],
                            "ceiling[" + x + "][" + z + "] should be UNPROBED");
                }
            }
        }

        @Test
        void scanCenterWorldCoordinates() {
            scanner.beginCycle(100, 64, -200);
            assertEquals(100, scanner.getScanCenterX());
            assertEquals(64, scanner.getScanCenterY());
            assertEquals(-200, scanner.getScanCenterZ());
        }

        @Test
        void worldToGridConversion() {
            scanner.beginCycle(100, 64, 200);
            // The grid center corresponds to world (100, ?, 200)
            // Grid index 0 corresponds to world x = 100 - 24 = 76
            // Grid index 24 corresponds to world x = 100
            assertEquals(24, scanner.worldToGridX(100));
            assertEquals(24, scanner.worldToGridZ(200));
            assertEquals(0, scanner.worldToGridX(76));
            assertEquals(48, scanner.worldToGridX(124));
        }

        @Test
        void gridToWorldConversion() {
            scanner.beginCycle(100, 64, 200);
            assertEquals(76, scanner.gridToWorldX(0));
            assertEquals(100, scanner.gridToWorldX(24));
            assertEquals(124, scanner.gridToWorldX(48));
            assertEquals(176, scanner.gridToWorldZ(0));
            assertEquals(200, scanner.gridToWorldZ(24));
        }
    }
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs

    // ── Task 5: height feature detection ─────────────────────────────────

    @Nested
    class HeightFeatures {

        private static final int SIZE = 11;
        private static final int ORIGIN_X = 100;
        private static final int ORIGIN_Z = 200;
        private static final int PLAYER_Y = 64;

        private int[][] floor;
        private int[][] ceiling;
        private SpectralCategory[][] materials;

        @BeforeEach
        void setUp() {
            floor = new int[SIZE][SIZE];
            ceiling = new int[SIZE][SIZE];
            materials = new SpectralCategory[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                Arrays.fill(floor[x], 64);
                Arrays.fill(ceiling[x], TerrainScanner.UNPROBED);
                Arrays.fill(materials[x], SpectralCategory.HARD);
            }
        }

        @Test
        void flatTerrainProducesNoFeatures() {
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertTrue(features.isEmpty());
        }

        @Test
        void threeBlockDropProducesEdge() {
            // East half is 3 blocks lower
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = 61;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            List<TerrainFeature> edges = features.stream()
                    .filter(f -> f.type() == TerrainFeatureType.EDGE)
                    .toList();
            assertFalse(edges.isEmpty(), "Should detect EDGE features at the cliff");
            // All edges should be near x-grid=5.5 → world x = 105.5
            for (TerrainFeature e : edges) {
                assertEquals(3.0f, e.magnitude(), 0.01f);
                // The edge should be at the boundary between grid x=5 and x=6
                float worldX = ORIGIN_X + 5.5f;
                assertEquals(worldX, e.x(), 0.6f);
            }
        }

        @Test
        void threeBlockDropAlsoProducesDropFeature() {
            // East half is 3 blocks lower → qualifies for depth probe (drop ≥ 3, fall ≥ 4)
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = 60; // 4 blocks lower → fall_depth = 4
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            List<TerrainFeature> drops = features.stream()
                    .filter(f -> f.type() == TerrainFeatureType.DROP)
                    .toList();
            assertFalse(drops.isEmpty(), "Should detect DROP features for 4-block fall");
            for (TerrainFeature d : drops) {
                assertEquals(4.0f, d.magnitude(), 0.01f);
            }
        }

        @Test
        void oneBlockRiseProducesStep() {
            // East half is 1 block higher
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = 65;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            List<TerrainFeature> steps = features.stream()
                    .filter(f -> f.type() == TerrainFeatureType.STEP)
                    .toList();
            assertFalse(steps.isEmpty(), "Should detect STEP for 1-block height change");
            for (TerrainFeature s : steps) {
                assertEquals(1.0f, s.magnitude(), 0.01f);
            }
        }

        @Test
        void twoBlockRiseProducesStep() {
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = 66;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            List<TerrainFeature> steps = features.stream()
                    .filter(f -> f.type() == TerrainFeatureType.STEP)
                    .toList();
            assertFalse(steps.isEmpty(), "Should detect STEP for 2-block height change");
        }

        @Test
        void unprobedCellsAreSkipped() {
            // Mark entire east side as unprobed
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = TerrainScanner.UNPROBED;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertTrue(features.isEmpty(), "UNPROBED cells should not produce features");
        }

        @Test
        void singleBlockPitProducesEdgesAroundIt() {
            // A single 4-block pit at grid (5,5)
            floor[5][5] = 60;
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            List<TerrainFeature> edges = features.stream()
                    .filter(f -> f.type() == TerrainFeatureType.EDGE)
                    .toList();
            // The pit has 4 cardinal neighbors, each should detect an edge
            assertTrue(edges.size() >= 4,
                    "Single pit should produce edges on all 4 sides, got " + edges.size());
        }

        @Test
        void edgeNormalPointsTowardDrop() {
            // East half is lower — edge normal should point east (+x direction)
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = 61;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            List<TerrainFeature> edges = features.stream()
                    .filter(f -> f.type() == TerrainFeatureType.EDGE)
                    .toList();
            // From grid x=5 looking east to x=6 (lower): normal should be (1, 0, 0)
            // From grid x=6 looking west to x=5 (higher): edge is placed from x=5's perspective
            // Edges from x=5→x=6 direction
            boolean foundEastFacing = edges.stream()
                    .anyMatch(e -> e.nx() > 0.5f && Math.abs(e.nz()) < 0.1f);
            assertTrue(foundEastFacing, "Should have east-facing edge normals toward the drop");
        }

        @Test
        void materialFromFloorIsPreserved() {
            for (int x = 6; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    floor[x][z] = 61;
                    materials[x][z] = SpectralCategory.WOOD;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectHeightFeatures(
                    floor, ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            // Features at the boundary should use the material of the lower side
            boolean hasWood = features.stream()
                    .anyMatch(f -> f.material() == SpectralCategory.WOOD);
            boolean hasStone = features.stream()
                    .anyMatch(f -> f.material() == SpectralCategory.HARD);
            assertTrue(hasWood || hasStone, "Features should have material from floor");
        }
    }
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs

    // ── Task 6: wall, solid object, ceiling detection ────────────────────

    @Nested
    class WallDetection {

        private static final int SIZE = 11;
        private static final int ORIGIN_X = 100;
        private static final int ORIGIN_Z = 200;
        private static final int PLAYER_Y = 64;

        private boolean[][] solid;
        private int[][] wallHeights;
        private SpectralCategory[][] materials;

        @BeforeEach
        void setUp() {
            solid = new boolean[SIZE][SIZE];
            wallHeights = new int[SIZE][SIZE];
            materials = new SpectralCategory[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                Arrays.fill(materials[x], SpectralCategory.HARD);
                Arrays.fill(wallHeights[x], 4);
            }
        }

        @Test
        void allAirProducesNoWalls() {
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            assertTrue(features.isEmpty());
        }

        @Test
        void allSolidProducesNoWalls() {
            for (int x = 0; x < SIZE; x++) {
                Arrays.fill(solid[x], true);
            }
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            assertTrue(features.isEmpty(), "Fully solid grid has no air boundaries");
        }

        @Test
        void solidLineProducesWalls() {
            // A vertical wall along x=5 (all z)
            for (int z = 0; z < SIZE; z++) {
                solid[5][z] = true;
            }
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            assertFalse(features.isEmpty(), "Wall along x=5 should produce WALL features");
            for (TerrainFeature f : features) {
                assertEquals(TerrainFeatureType.WALL, f.type());
            }
        }

        @Test
        void wallNormalPointsTowardAir() {
            // Solid block at (5,5), air everywhere else
            solid[5][5] = true;
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            // With 4 air neighbors, should produce walls facing each direction
            assertEquals(4, features.size(), "Isolated solid block → 4 wall faces");
            // Each normal should be unit length in a cardinal direction
            for (TerrainFeature f : features) {
                float len = (float) Math.sqrt(f.nx() * f.nx() + f.nz() * f.nz());
                assertEquals(1.0f, len, 0.01f, "Wall normal should be unit length");
            }
        }

        @Test
        void wallHeightBelowTwoIsNotEmitted() {
            solid[5][5] = true;
            // Set wall height to 1 (below threshold)
            for (int x = 0; x < SIZE; x++) {
                Arrays.fill(wallHeights[x], 1);
            }
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            assertTrue(features.isEmpty(), "Wall height < 2 should not produce WALL");
        }

        @Test
        void wallMagnitudeEqualsHeight() {
            solid[5][5] = true;
            wallHeights[5][5] = 7;
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            for (TerrainFeature f : features) {
                assertEquals(7.0f, f.magnitude(), 0.01f);
            }
        }

        @Test
        void wallPositionIsAtSolidAirBoundary() {
            // Solid at grid (5,5), air at (6,5)
            solid[5][5] = true;
            List<TerrainFeature> features = TerrainScanner.detectWalls(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, wallHeights, materials);
            // Should have a wall facing +x at boundary (5.5, ?, 5)
            boolean found = features.stream().anyMatch(f ->
                    Math.abs(f.x() - (ORIGIN_X + 5.5f)) < 0.1f
                    && f.nx() > 0.5f);
            assertTrue(found, "Should place wall at solid/air boundary facing +x");
        }
    }

    @Nested
    class SolidObjectDetection {

        private static final int SIZE = 11;
        private static final int ORIGIN_X = 100;
        private static final int ORIGIN_Z = 200;
        private static final int PLAYER_Y = 64;

        private boolean[][] solid;
        private int[][] objectHeights;
        private SpectralCategory[][] materials;

        @BeforeEach
        void setUp() {
            solid = new boolean[SIZE][SIZE];
            objectHeights = new int[SIZE][SIZE];
            materials = new SpectralCategory[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                Arrays.fill(materials[x], SpectralCategory.HARD);
                Arrays.fill(objectHeights[x], 2);
            }
        }

        @Test
        void isolatedSolidBlockIsSolidObject() {
            // Single solid block surrounded by air on all 4 sides
            solid[5][5] = true;
            List<TerrainFeature> features = TerrainScanner.detectSolidObjects(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, objectHeights, materials);
            assertEquals(1, features.size());
            assertEquals(TerrainFeatureType.SOLID_OBJECT, features.get(0).type());
            assertEquals(ORIGIN_X + 5.0f, features.get(0).x(), 0.5f);
            assertEquals(ORIGIN_Z + 5.0f, features.get(0).z(), 0.5f);
        }

        @Test
        void solidBlockWithOnlyOneAirNeighborIsNotObject() {
            // Solid block at (5,5) with 3 solid neighbors
            solid[5][5] = true;
            solid[4][5] = true;
            solid[6][5] = true;
            solid[5][4] = true;
            // Only (5,6) is air → only 1 air neighbor, not ≥ 3
            List<TerrainFeature> features = TerrainScanner.detectSolidObjects(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, objectHeights, materials);
            assertTrue(features.isEmpty(), "Needs 3+ air neighbors to be SOLID_OBJECT");
        }

        @Test
        void pillarOfTwoSolidBlocksIsObject() {
            // Two adjacent solid blocks along x, each with 3 air neighbors
            solid[5][5] = true;
            solid[6][5] = true;
            // (5,5) has air at (4,5), (5,4), (5,6) → 3 air neighbors
            // (6,5) has air at (7,5), (6,4), (6,6) → 3 air neighbors
            List<TerrainFeature> features = TerrainScanner.detectSolidObjects(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, objectHeights, materials);
            assertEquals(2, features.size(), "Both blocks qualify as SOLID_OBJECT");
        }

        @Test
        void objectAtGridEdgeCountsBoundaryAsAir() {
            // Solid at grid (0,0) — 2 neighbors out of bounds (treated as air) + 2 in-bounds
            solid[0][0] = true;
            List<TerrainFeature> features = TerrainScanner.detectSolidObjects(
                    solid, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, objectHeights, materials);
            // Out of bounds = air, so (0,0) has 4 air neighbors (2 OOB + 2 in-bounds air)
            assertEquals(1, features.size());
        }
    }

    @Nested
    class CeilingDetection {

        private static final int SIZE = 11;
        private static final int ORIGIN_X = 100;
        private static final int ORIGIN_Z = 200;
        private static final int PLAYER_Y = 64;

        private int[][] ceiling;
        private SpectralCategory[][] materials;

        @BeforeEach
        void setUp() {
            ceiling = new int[SIZE][SIZE];
            materials = new SpectralCategory[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                Arrays.fill(ceiling[x], TerrainScanner.UNPROBED);
                Arrays.fill(materials[x], SpectralCategory.HARD);
            }
        }

        @Test
        void noCeilingProducesNoFeatures() {
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertTrue(features.isEmpty());
        }

        @Test
        void ceilingWithinRangeProducesFeature() {
            // Ceiling at y=68, player at y=64 → distance=4, within [1,16]
            ceiling[5][5] = 68;
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertEquals(1, features.size());
            TerrainFeature f = features.get(0);
            assertEquals(TerrainFeatureType.CEILING, f.type());
            assertEquals(68.0f, f.y(), 0.01f);
            assertEquals(0.0f, f.nx(), 0.01f);
            assertEquals(-1.0f, f.ny(), 0.01f); // facing down
            assertEquals(0.0f, f.nz(), 0.01f);
        }

        @Test
        void ceilingTooFarAwayIsNotDetected() {
            // Ceiling at y=81, distance = 17 > 16
            ceiling[5][5] = 81;
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertTrue(features.isEmpty());
        }

        @Test
        void ceilingAtPlayerLevelIsNotDetected() {
            // Ceiling at y=64, distance = 0 < 1
            ceiling[5][5] = 64;
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertTrue(features.isEmpty());
        }

        @Test
        void netherStyleLowCeilingProducesManyFeatures() {
            // Nether: ceiling at y=67 everywhere → distance=3
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    ceiling[x][z] = 67;
                }
            }
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertEquals(SIZE * SIZE, features.size(),
                    "Every cell with a close ceiling should produce a feature");
        }

        @Test
        void ceilingMagnitudeIsDistance() {
            ceiling[5][5] = 70;
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertEquals(1, features.size());
            assertEquals(6.0f, features.get(0).magnitude(), 0.01f);
        }

        @Test
        void ceilingBelowPlayerIsNotDetected() {
            // Ceiling below player (y=60) should not produce a feature
            ceiling[5][5] = 60;
            List<TerrainFeature> features = TerrainScanner.detectCeilings(
                    ceiling, SIZE, ORIGIN_X, ORIGIN_Z, PLAYER_Y, materials);
            assertTrue(features.isEmpty());
        }
    }
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs

    // ── Task 7: passage detection ────────────────────────────────────────

    @Nested
    class ChebyshevDistanceTransform {

        @Test
        void allAirGridHasLargeDistances() {
            boolean[][] solid = new boolean[11][11];
            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, 11, 11);
            // With no solid cells, all distances remain at INF = w + h = 22
            int INF = 11 + 11;
            assertEquals(INF, dist[5][5]);
            assertEquals(INF, dist[0][0]);
            assertEquals(INF, dist[5][0]);
        }

        @Test
        void allSolidGridHasZeroDistances() {
            boolean[][] solid = new boolean[11][11];
            for (int x = 0; x < 11; x++) {
                Arrays.fill(solid[x], true);
            }
            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, 11, 11);
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 11; z++) {
                    assertEquals(0, dist[x][z]);
                }
            }
        }

        @Test
        void singleSolidBlockInCenter() {
            boolean[][] solid = new boolean[11][11];
            solid[5][5] = true;
            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, 11, 11);
            assertEquals(0, dist[5][5]);
            // Adjacent cells should have distance 1
            assertEquals(1, dist[4][5]);
            assertEquals(1, dist[6][5]);
            assertEquals(1, dist[5][4]);
            assertEquals(1, dist[5][6]);
            // Diagonally adjacent should also be 1 (Chebyshev)
            assertEquals(1, dist[4][4]);
            assertEquals(1, dist[6][6]);
            // Two steps away
            assertEquals(2, dist[3][5]);
            assertEquals(2, dist[7][5]);
        }

        @Test
        void corridorHasCorrectDistances() {
            // 11-wide grid, solid walls at x=0 and x=10, open in between
            boolean[][] solid = new boolean[11][11];
            for (int z = 0; z < 11; z++) {
                solid[0][z] = true;
                solid[10][z] = true;
            }
            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, 11, 11);
            // Center of corridor at x=5 → distance to nearest wall = 4
            // But also bounded by z-edges: at z=5, distance = min(4, 5+1) = 4...
            // Actually dist is distance to nearest SOLID, not edge.
            // x=5 is 4 blocks from x=1 (adjacent to solid x=0) → distance = 4
            // But Chebyshev distance to solid[0][5] is 5, and to solid[10][5] is 5
            // Wait: dist to solid[0][z] for cell (5,z) = max(|5-0|, |z-z|) = 5
            // That's Chebyshev distance in the other sense. Let me reconsider.
            //
            // The Chebyshev distance transform computes for each cell the distance
            // to the nearest solid cell. For (5,5) the nearest solid is (0,5) or (10,5),
            // both at Chebyshev distance 5.
            assertEquals(5, dist[5][5]);
            // (1,5) is adjacent to solid (0,5) → distance 1
            assertEquals(1, dist[1][5]);
            // (2,5) → distance 2
            assertEquals(2, dist[2][5]);
        }

        @Test
        void narrowPassageHasSmallDistanceInMiddle() {
            // Two solid walls with a 3-wide gap:
            // Solid from x=0..3 and x=7..10, open from x=4..6
            boolean[][] solid = new boolean[11][11];
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z < 11; z++) {
                    solid[x][z] = true;
                }
            }
            for (int x = 7; x <= 10; x++) {
                for (int z = 0; z < 11; z++) {
                    solid[x][z] = true;
                }
            }
            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, 11, 11);
            // Center of gap at x=5, z=5: nearest solid is at x=3 (distance 2) or x=7 (distance 2)
            assertEquals(2, dist[5][5]);
            // x=4, z=5: nearest solid is x=3 → distance 1
            assertEquals(1, dist[4][5]);
        }
    }

    @Nested
    class PassageDetection {

        private static final int ORIGIN_X = 100;
        private static final int ORIGIN_Z = 200;
        private static final int PLAYER_Y = 64;

        @Test
        void openAreaProducesNoPassages() {
            boolean[][] solid = new boolean[21][21];
            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, 21, 21);
            List<TerrainFeature> features = TerrainScanner.detectPassages(
                    dist, 21, ORIGIN_X, ORIGIN_Z, PLAYER_Y);
            assertTrue(features.isEmpty(),
                    "Wide open area should not produce passage features");
        }

        @Test
        void narrowCorridorBetweenWideRoomsProducesPassage() {
            // 31-wide grid: rooms on left and right, narrow corridor in middle
            int size = 31;
            boolean[][] solid = new boolean[size][size];

            // Build two walls with a narrow gap (width ~2) in the middle
            // Wall from z=0 to z=30 at x=13 and x=17, with gap only at z=14..16
            // Actually let's do: solid walls making a corridor along z-axis
            // Left room: x=0..30, z=0..10 → all air
            // Corridor: x=13..17, z=11..19 → air, rest solid at z=11..19
            // Right room: x=0..30, z=20..30 → all air
            for (int x = 0; x < size; x++) {
                for (int z = 11; z <= 19; z++) {
                    solid[x][z] = true;
                }
            }
            // Carve out narrow corridor (3 blocks wide)
            for (int x = 14; x <= 16; x++) {
                for (int z = 11; z <= 19; z++) {
                    solid[x][z] = false;
                }
            }

            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, size, size);
            List<TerrainFeature> features = TerrainScanner.detectPassages(
                    dist, size, ORIGIN_X, ORIGIN_Z, PLAYER_Y);
            assertFalse(features.isEmpty(),
                    "Narrow corridor between open rooms should produce PASSAGE features");
            for (TerrainFeature f : features) {
                assertEquals(TerrainFeatureType.PASSAGE, f.type());
            }
        }

        @Test
        void uniformCorridorDoesNotProducePassage() {
            // A corridor of uniform width — no constriction
            int size = 21;
            boolean[][] solid = new boolean[size][size];
            // Walls at x=0..7 and x=13..20 for all z → uniform 5-wide corridor
            for (int x = 0; x <= 7; x++) {
                for (int z = 0; z < size; z++) {
                    solid[x][z] = true;
                }
            }
            for (int x = 13; x <= 20; x++) {
                for (int z = 0; z < size; z++) {
                    solid[x][z] = true;
                }
            }

            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, size, size);
            List<TerrainFeature> features = TerrainScanner.detectPassages(
                    dist, size, ORIGIN_X, ORIGIN_Z, PLAYER_Y);
            // Uniform corridor: no constriction (distance is ~2 everywhere in corridor,
            // with no wider area within 6 blocks on either side)
            assertTrue(features.isEmpty(),
                    "Uniform corridor should not produce passage (no constriction)");
        }

        @Test
        void passageMagnitudeReflectsWidth() {
            int size = 31;
            boolean[][] solid = new boolean[size][size];
            // Same corridor setup as narrowCorridorBetweenWideRoomsProducesPassage
            for (int x = 0; x < size; x++) {
                for (int z = 11; z <= 19; z++) {
                    solid[x][z] = true;
                }
            }
            for (int x = 14; x <= 16; x++) {
                for (int z = 11; z <= 19; z++) {
                    solid[x][z] = false;
                }
            }

            int[][] dist = TerrainScanner.chebyshevDistanceTransform(solid, size, size);
            List<TerrainFeature> features = TerrainScanner.detectPassages(
                    dist, size, ORIGIN_X, ORIGIN_Z, PLAYER_Y);
            assertFalse(features.isEmpty(),
                    "Expected passage features from narrow corridor between rooms");
            // Magnitude = dist * 2, corridor is ~1 block from wall → magnitude ≈ 2
            for (TerrainFeature f : features) {
                assertTrue(f.magnitude() > 0 && f.magnitude() <= 6,
                        "Passage magnitude should reflect narrow width, got " + f.magnitude());
            }
        }
    }
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs

    // ── Task 8: wall coalescing, deduplication, density caps ─────────────

    @Nested
    class Deduplication {

        @Test
        void identicalFeaturesAreMerged() {
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.5f, 64.0f, 200.5f,
                    1, 0, 0,
                    3.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.5f, 64.0f, 200.5f,
                    1, 0, 0,
                    2.0f, 0.4f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.deduplicateFeatures(
                    new ArrayList<>(List.of(a, b)));
            assertEquals(1, result.size(), "Identical-position same-type features should merge");
            assertEquals(3.0f, result.get(0).magnitude(), 0.01f,
                    "Should keep the larger magnitude");
        }

        @Test
        void featuresWithinOneBlockAreMerged() {
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.0f, 64.0f, 200.0f,
                    1, 0, 0,
                    3.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.8f, 64.0f, 200.0f,
                    1, 0, 0,
                    2.0f, 0.4f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.deduplicateFeatures(
                    new ArrayList<>(List.of(a, b)));
            assertEquals(1, result.size(), "Features within 1.0 block should merge");
        }

        @Test
        void featuresFarApartAreNotMerged() {
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.0f, 64.0f, 200.0f,
                    1, 0, 0,
                    3.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    102.0f, 64.0f, 200.0f,
                    1, 0, 0,
                    2.0f, 0.4f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.deduplicateFeatures(
                    new ArrayList<>(List.of(a, b)));
            assertEquals(2, result.size(), "Features > 1.0 block apart should not merge");
        }

        @Test
        void differentTypesAreNotMerged() {
            TerrainFeature edge = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.0f, 64.0f, 200.0f,
                    1, 0, 0,
                    3.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature step = new TerrainFeature(
                    TerrainFeatureType.STEP,
                    100.0f, 64.0f, 200.0f,
                    1, 0, 0,
                    1.0f, 0.3f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.deduplicateFeatures(
                    new ArrayList<>(List.of(edge, step)));
            assertEquals(2, result.size(), "Different types at same position should not merge");
        }

        @Test
        void chainOfCloseFeaturesMergesCorrectly() {
            // Three features at 0.4 block intervals — all within 1.0 of each other
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.0f, 64.0f, 200.0f,
                    1, 0, 0, 5.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.4f, 64.0f, 200.0f,
                    1, 0, 0, 3.0f, 0.4f, SpectralCategory.HARD);
            TerrainFeature c = new TerrainFeature(
                    TerrainFeatureType.EDGE,
                    100.8f, 64.0f, 200.0f,
                    1, 0, 0, 4.0f, 0.6f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.deduplicateFeatures(
                    new ArrayList<>(List.of(a, b, c)));
            // a and b merge (distance 0.4), result and c merge (distance ≤ 0.8 from a)
            assertEquals(1, result.size(), "Chain of close features should merge to one");
            assertEquals(5.0f, result.get(0).magnitude(), 0.01f, "Largest magnitude kept");
        }
    }

    @Nested
    class WallCoalescing {

        @Test
        void singleWallIsUnchanged() {
            TerrainFeature wall = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.5f, 64.0f, 200.5f,
                    1, 0, 0,
                    4.0f, 0.5f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.coalesceWalls(List.of(wall));
            assertEquals(1, result.size());
        }

        @Test
        void twoAdjacentWallsSameFacingCoalesce() {
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.5f, 64.0f, 200.0f,
                    1, 0, 0,
                    4.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.5f, 64.0f, 201.0f,
                    1, 0, 0,
                    4.0f, 0.5f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.coalesceWalls(List.of(a, b));
            // 2 walls → 2 endpoints = 2 features
            assertEquals(2, result.size(), "Two adjacent walls coalesce to 2 endpoints");
        }

        @Test
        void longWallCoalescesToEndpointsAndMidpoints() {
            // 30 wall segments, 1 block apart, all facing +x
            List<TerrainFeature> walls = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                walls.add(new TerrainFeature(
                        TerrainFeatureType.WALL,
                        100.5f, 64.0f, 200.0f + i,
                        1, 0, 0,
                        4.0f, 0.5f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.coalesceWalls(walls);
            // 30-block wall → ~7 features (2 ends + midpoints at ~6 block intervals)
            assertTrue(result.size() >= 5 && result.size() <= 9,
                    "30-block wall should coalesce to ~7 features, got " + result.size());
        }

        @Test
        void wallsFacingDifferentDirectionsDoNotCoalesce() {
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.5f, 64.0f, 200.0f,
                    1, 0, 0, // facing +x
                    4.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.0f, 64.0f, 200.5f,
                    0, 0, 1, // facing +z
                    4.0f, 0.5f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.coalesceWalls(List.of(a, b));
            assertEquals(2, result.size(), "Differently-facing walls should not coalesce");
        }

        @Test
        void nonAdjacentWallsSameFacingDoNotCoalesce() {
            TerrainFeature a = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.5f, 64.0f, 200.0f,
                    1, 0, 0,
                    4.0f, 0.5f, SpectralCategory.HARD);
            TerrainFeature b = new TerrainFeature(
                    TerrainFeatureType.WALL,
                    100.5f, 64.0f, 210.0f, // 10 blocks away
                    1, 0, 0,
                    4.0f, 0.5f, SpectralCategory.HARD);
            List<TerrainFeature> result = TerrainScanner.coalesceWalls(List.of(a, b));
            assertEquals(2, result.size(), "Non-adjacent walls should not coalesce");
        }

        @Test
        void lShapedWallSplitsIntoTwoGroups() {
            List<TerrainFeature> walls = new ArrayList<>();
            // Horizontal segment along z, facing +x
            for (int i = 0; i < 10; i++) {
                walls.add(new TerrainFeature(
                        TerrainFeatureType.WALL,
                        100.5f, 64.0f, 200.0f + i,
                        1, 0, 0,
                        4.0f, 0.5f, SpectralCategory.HARD));
            }
            // Vertical segment along x, facing +z
            for (int i = 0; i < 10; i++) {
                walls.add(new TerrainFeature(
                        TerrainFeatureType.WALL,
                        100.0f + i, 64.0f, 209.5f,
                        0, 0, 1,
                        4.0f, 0.5f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.coalesceWalls(walls);
            // Each segment of 10 → ~2 endpoints + ~1 midpoint = ~3 each, total ~6
            assertTrue(result.size() >= 4 && result.size() <= 8,
                    "L-shaped wall should split into two groups, got " + result.size());
        }
    }

    @Nested
    class DensityCaps {

        @Test
        void underCapReturnsAll() {
            List<TerrainFeature> features = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.EDGE, // family = VOID
                        100.0f + i * 3, 64.0f, 200.0f,
                        1, 0, 0,
                        3.0f, 0.5f - i * 0.05f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.applyDensityCaps(features);
            assertEquals(5, result.size(), "Under cap should return all features");
        }

        @Test
        void voidFamilyCappedAtSixteen() {
            List<TerrainFeature> features = new ArrayList<>();
            // EDGE is in VOID family — cap = 16
            for (int i = 0; i < 25; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.EDGE,
                        100.0f + i * 3, 64.0f, 200.0f,
                        1, 0, 0,
                        3.0f, 1.0f - i * 0.01f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.applyDensityCaps(features);
            long voidCount = result.stream()
                    .filter(f -> f.type().family() == TerrainFeatureType.Family.VOID)
                    .count();
            assertEquals(16, voidCount, "VOID family should be capped at 16");
        }

        @Test
        void surfaceFamilyCappedAtTwenty() {
            List<TerrainFeature> features = new ArrayList<>();
            // WALL is in SURFACE family — cap = 20
            for (int i = 0; i < 30; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.WALL,
                        100.0f + i * 3, 64.0f, 200.0f,
                        1, 0, 0,
                        4.0f, 1.0f - i * 0.01f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.applyDensityCaps(features);
            long surfaceCount = result.stream()
                    .filter(f -> f.type().family() == TerrainFeatureType.Family.SURFACE)
                    .count();
            assertEquals(20, surfaceCount, "SURFACE family should be capped at 20");
        }

        @Test
        void groundFamilyCappedAtTwelve() {
            List<TerrainFeature> features = new ArrayList<>();
            // STEP is in GROUND family — cap = 12
            for (int i = 0; i < 20; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.STEP,
                        100.0f + i * 3, 64.0f, 200.0f,
                        1, 0, 0,
                        1.0f, 1.0f - i * 0.01f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.applyDensityCaps(features);
            long groundCount = result.stream()
                    .filter(f -> f.type().family() == TerrainFeatureType.Family.GROUND)
                    .count();
            assertEquals(12, groundCount, "GROUND family should be capped at 12");
        }

        @Test
        void highestSaliencyFeaturesAreKept() {
            List<TerrainFeature> features = new ArrayList<>();
            // 20 EDGE features with decreasing saliency — cap at 16 should keep top 16
            for (int i = 0; i < 20; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.EDGE,
                        100.0f + i * 3, 64.0f, 200.0f,
                        1, 0, 0,
                        3.0f, 1.0f - i * 0.04f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.applyDensityCaps(features);
            // The 4 lowest-saliency features (indices 16-19) should be removed
            float minSaliency = result.stream()
                    .map(TerrainFeature::saliency)
                    .min(Float::compareTo)
                    .orElse(0f);
            // Index 15 has saliency 1.0 - 15*0.04 = 0.4
            // Index 16 has saliency 1.0 - 16*0.04 = 0.36
            assertTrue(minSaliency >= 0.39f,
                    "Minimum saliency should be from index 15 (0.4), got " + minSaliency);
        }

        @Test
        void mixedFamiliesEachCappedIndependently() {
            List<TerrainFeature> features = new ArrayList<>();
            // 20 EDGE (VOID, cap 16) + 25 WALL (SURFACE, cap 20) + 15 STEP (GROUND, cap 12)
            for (int i = 0; i < 20; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.EDGE,
                        i * 3.0f, 64.0f, 0.0f,
                        1, 0, 0, 3.0f, 1.0f - i * 0.01f, SpectralCategory.HARD));
            }
            for (int i = 0; i < 25; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.WALL,
                        i * 3.0f, 64.0f, 100.0f,
                        1, 0, 0, 4.0f, 1.0f - i * 0.01f, SpectralCategory.HARD));
            }
            for (int i = 0; i < 15; i++) {
                features.add(new TerrainFeature(
                        TerrainFeatureType.STEP,
                        i * 3.0f, 64.0f, 200.0f,
                        1, 0, 0, 1.0f, 1.0f - i * 0.01f, SpectralCategory.HARD));
            }
            List<TerrainFeature> result = TerrainScanner.applyDensityCaps(features);
            long voidCount = result.stream()
                    .filter(f -> f.type().family() == TerrainFeatureType.Family.VOID).count();
            long surfaceCount = result.stream()
                    .filter(f -> f.type().family() == TerrainFeatureType.Family.SURFACE).count();
            long groundCount = result.stream()
                    .filter(f -> f.type().family() == TerrainFeatureType.Family.GROUND).count();
            assertEquals(16, voidCount);
            assertEquals(20, surfaceCount);
            assertEquals(12, groundCount);
            assertEquals(48, result.size());
        }
    }
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs

    @Nested
    class AnalyzeAndProduceFeaturesPipeline {

        @Test
        void fullPipelineProducesExpectedFeatureTypes() {
            TerrainScanner scanner = new TerrainScanner();
            scanner.setRange(10);
            // Player at Y=62, midway between cliff levels 64 and 58
            scanner.beginCycle(0, 62, 0);

            int size = scanner.getGridSize();
            int[][] floor = scanner.getFloorArray();
            int[][] ceiling = scanner.getCeilingArray();
            boolean[][] solid = scanner.getSolidArray();
            SpectralCategory[][] floorMats = scanner.getFloorMaterials();

            // Create a cliff at z=10: floor=64 for z<10, floor=58 for z>=10.
            // Drop of 6 blocks triggers EDGE (>=3) and DROP (>=4).
            // Place a solid wall along one edge for WALL detection (row of blocks,
            // not isolated pillars, to avoid producing spurious passages).
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (z < 10) {
                        floor[x][z] = 64;
                    } else {
                        floor[x][z] = 58;
                    }
                    ceiling[x][z] = TerrainScanner.UNPROBED;
                    solid[x][z] = false;
                    floorMats[x][z] = SpectralCategory.HARD;
                }
            }
            // Build a solid wall along x=0 for all z: contiguous wall, no passages
            for (int z = 0; z < size; z++) {
                solid[0][z] = true;
            }

            for (int i = 0; i < 20; i++) {
                scanner.advanceSlice();
                if (scanner.isCycleComplete()) break;
            }

            scanner.analyzeAndProduceFeatures();
            List<TerrainFeature> features = scanner.getFeatures();

            assertTrue(features.stream().anyMatch(f -> f.type() == TerrainFeatureType.EDGE),
                    "Pipeline should produce EDGE features from 6-block cliff");
            assertTrue(features.stream().anyMatch(f -> f.type() == TerrainFeatureType.DROP),
                    "Pipeline should produce DROP features from 6-block fall");
            assertTrue(features.stream().anyMatch(f -> f.type() == TerrainFeatureType.WALL),
                    "Pipeline should produce WALL features from solid wall");

            for (TerrainFeature f : features) {
                assertTrue(f.saliency() >= 0f,
                        "Feature saliency should be non-negative: " + f);
                assertTrue(f.magnitude() > 0f,
                        "Feature magnitude should be positive: " + f);
            }

            assertTrue(features.size() <= 48,
                    "Density caps should limit total features to 48, got " + features.size());
        }
    }
<<<<<<< ours
=======
>>>>>>> theirs
=======
>>>>>>> theirs
=======
>>>>>>> theirs
=======
>>>>>>> theirs
=======
>>>>>>> theirs
=======
>>>>>>> theirs
}
