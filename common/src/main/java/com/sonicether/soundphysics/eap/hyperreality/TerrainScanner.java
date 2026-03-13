package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Incrementally scans terrain around the player and produces {@link TerrainFeature}s.
 *
 * <p>Two complementary grids cover a square area centred on the player:
 * <ul>
 *   <li><b>Floor / ceiling probe grid</b> — for each (x, z) column, probes downward
 *       from player Y to find the floor and upward to find the ceiling.</li>
 *   <li><b>Horizontal slice grid</b> — at player Y and Y+1, classifies each column
 *       as AIR or SOLID.</li>
 * </ul>
 *
 * <p>Both grids are scanned incrementally: {@value SLICES_PER_TICK} x-columns per
 * tick, completing a full cycle in {@code ceil(gridSize / SLICES_PER_TICK)} ticks.
 * On cycle completion, pure detection algorithms extract features from the raw arrays.
 */
public final class TerrainScanner {

    // ── constants ────────────────────────────────────────────────────────

    private static final int MIN_RADIUS = 4;
    private static final int MAX_RADIUS = 48;
    private static final int DEFAULT_RADIUS = 24;
    static final int SLICES_PER_TICK = 7;
    static final int VERTICAL_PROBE_RANGE = 16;
    private static final int RESCAN_INTERVAL = 40;
    private static final float RESCAN_DISTANCE = 4.0f;
    static final int UNPROBED = Integer.MIN_VALUE;

    // ── density caps per family ──────────────────────────────────────────

    private static final int CAP_VOID = 16;
    private static final int CAP_SURFACE = 20;
    private static final int CAP_GROUND = 12;

    // ── mutable state ────────────────────────────────────────────────────

    private int scanRadius = DEFAULT_RADIUS;
    private int gridSize = 2 * DEFAULT_RADIUS + 1;

    private int[][] floor;
    private int[][] ceiling;
    private boolean[][] solid;
    private SpectralCategory[][] floorMaterials;
    private SpectralCategory[][] ceilingMaterials;
    private SpectralCategory[][] wallMaterials;

    private int currentSlice;
    private boolean cycleComplete;
    private int scanCenterX;
    private int scanCenterY;
    private int scanCenterZ;
    private int ticksSinceLastCycle;
    private boolean hasEverCompleted;

    private List<TerrainFeature> lastFeatures = List.of();

    // ── constructors ─────────────────────────────────────────────────────

    public TerrainScanner() {
<<<<<<< ours
        this.cycleComplete = true; // no cycle in progress initially
=======
>>>>>>> theirs
        allocateGrids();
    }

    // ── public API ───────────────────────────────────────────────────────

    /** Sets the scan radius (clamped to [{@value MIN_RADIUS}, {@value MAX_RADIUS}]). */
    public void setRange(int range) {
        this.scanRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, range));
        this.gridSize = 2 * this.scanRadius + 1;
        allocateGrids();
    }

    public int getScanRadius() {
        return scanRadius;
    }

    public int getGridSize() {
        return gridSize;
    }

    /**
     * Prepares a new scan cycle centred on the given world coordinates.
     * Resets slice counter and clears grids.
     */
    public void beginCycle(int cx, int cy, int cz) {
        this.scanCenterX = cx;
        this.scanCenterY = cy;
        this.scanCenterZ = cz;
        this.currentSlice = 0;
        this.cycleComplete = false;
        clearGrids();
    }

    /**
     * Advances the scan by one tick's worth of x-columns.
     * Intended for use by the tick driver that also calls into the Level.
     *
     * @return the x-column start index (inclusive) and end index (exclusive)
     *         for this tick's slice, as a two-element array.
     */
    public int[] getSliceRange() {
        int start = currentSlice * SLICES_PER_TICK;
        int end = Math.min(start + SLICES_PER_TICK, gridSize);
        return new int[]{start, end};
    }

    /**
     * Advances to the next slice. Called after the Level-based probing for
     * the current slice is complete.
     */
    public void advanceSlice() {
        currentSlice++;
        if (currentSlice * SLICES_PER_TICK >= gridSize) {
            cycleComplete = true;
        }
    }

    /**
     * Marks the cycle as complete (used by external code after analysis).
     * Resets tick counter and records that a cycle has completed.
     */
    public void markCycleComplete() {
        this.hasEverCompleted = true;
        this.ticksSinceLastCycle = 0;
    }

    /** Increments the tick counter for rescan timing. */
    public void incrementTickCounter() {
        ticksSinceLastCycle++;
    }

    /**
     * Returns {@code true} if a rescan should be initiated, either because:
     * <ul>
     *   <li>No cycle has ever completed, or</li>
     *   <li>The player has moved &ge; {@value RESCAN_DISTANCE} blocks, or</li>
     *   <li>{@value RESCAN_INTERVAL} ticks have elapsed since the last cycle, or</li>
     *   <li>{@link #forceRescan()} was called.</li>
     * </ul>
     */
    public boolean shouldRescan(float playerX, float playerY, float playerZ) {
        if (!hasEverCompleted) {
            return true;
        }
        if (ticksSinceLastCycle >= RESCAN_INTERVAL) {
            return true;
        }
        float dx = playerX - scanCenterX;
        float dy = playerY - scanCenterY;
        float dz = playerZ - scanCenterZ;
        return (dx * dx + dy * dy + dz * dz) >= RESCAN_DISTANCE * RESCAN_DISTANCE;
    }

    /** Forces a rescan on the next call to {@link #shouldRescan}. */
    public void forceRescan() {
        this.hasEverCompleted = false;
    }

    public boolean isCycleComplete() {
        return cycleComplete;
    }

    public int getCurrentSlice() {
        return currentSlice;
    }

    public List<TerrainFeature> getFeatures() {
        return lastFeatures;
    }

    // ── coordinate conversion ────────────────────────────────────────────

    public int worldToGridX(int worldX) {
        return worldX - (scanCenterX - scanRadius);
    }

    public int worldToGridZ(int worldZ) {
        return worldZ - (scanCenterZ - scanRadius);
    }

    public int gridToWorldX(int gridX) {
        return gridX + (scanCenterX - scanRadius);
    }

    public int gridToWorldZ(int gridZ) {
        return gridZ + (scanCenterZ - scanRadius);
    }

    // ── grid accessors (package-private, for tests) ──────────────────────

    int[][] getFloorArray() {
        return floor;
    }

    int[][] getCeilingArray() {
        return ceiling;
    }

    boolean[][] getSolidArray() {
        return solid;
    }

    SpectralCategory[][] getFloorMaterials() {
        return floorMaterials;
    }

    SpectralCategory[][] getCeilingMaterials() {
        return ceilingMaterials;
    }

    SpectralCategory[][] getWallMaterials() {
        return wallMaterials;
    }

    int getScanCenterX() {
        return scanCenterX;
    }

    int getScanCenterY() {
        return scanCenterY;
    }

    int getScanCenterZ() {
        return scanCenterZ;
    }

    // ── analysis entry point ─────────────────────────────────────────────

    /**
     * Runs all detection algorithms on the current grid state and stores
     * the resulting features. Called when a scan cycle completes.
     */
    void analyzeAndProduceFeatures() {
        // Probe wall heights from the solid grid
        int[][] wallHeights = probeWallHeights(solid, ceiling, gridSize, scanCenterY);

        List<TerrainFeature> all = new ArrayList<>();
        all.addAll(detectHeightFeatures(floor, ceiling, gridSize,
                gridToWorldX(0), gridToWorldZ(0), scanCenterY, floorMaterials));
        all.addAll(detectWalls(solid, gridSize,
                gridToWorldX(0), gridToWorldZ(0), scanCenterY, wallHeights, wallMaterials));
        all.addAll(detectSolidObjects(solid, gridSize,
                gridToWorldX(0), gridToWorldZ(0), scanCenterY,
                probeObjectHeights(solid, ceiling, gridSize, scanCenterY), wallMaterials));
        all.addAll(detectCeilings(ceiling, gridSize,
                gridToWorldX(0), gridToWorldZ(0), scanCenterY, ceilingMaterials));
        all.addAll(detectPassages(
                chebyshevDistanceTransform(solid, gridSize, gridSize),
                gridSize, gridToWorldX(0), gridToWorldZ(0), scanCenterY));

        all = deduplicateFeatures(all);

        // Separate walls and ceilings for coalescing
        List<TerrainFeature> walls = new ArrayList<>();
        List<TerrainFeature> ceilings = new ArrayList<>();
        List<TerrainFeature> rest = new ArrayList<>();
        for (TerrainFeature f : all) {
            if (f.type() == TerrainFeatureType.WALL) {
                walls.add(f);
            } else if (f.type() == TerrainFeatureType.CEILING) {
                ceilings.add(f);
            } else {
                rest.add(f);
            }
        }

        List<TerrainFeature> combined = new ArrayList<>(rest);
        combined.addAll(coalesceWalls(walls));
        combined.addAll(coalesceWalls(ceilings));

        lastFeatures = applyDensityCaps(combined);
    }

    // ── pure static detection algorithms ─────────────────────────────────
    // (implementations provided in Tasks 5–8; stubs here for compilation)

    static List<TerrainFeature> detectHeightFeatures(int[][] floor, int[][] ceiling,
            int scanSize, int worldOriginX, int worldOriginZ, int playerY,
            SpectralCategory[][] floorMaterials) {
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        List<TerrainFeature> features = new ArrayList<>();
        // Cardinal neighbor offsets: +x, -x, +z, -z
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        // Normal directions (point toward neighbor)
        float[] ndx = {1, -1, 0, 0};
        float[] ndz = {0, 0, 1, -1};

        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                int hCenter = floor[x][z];
                if (hCenter == UNPROBED) {
                    continue;
                }
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d];
                    int nz = z + dz[d];
                    if (nx < 0 || nx >= scanSize || nz < 0 || nz >= scanSize) {
                        continue;
                    }
                    int hNeighbor = floor[nx][nz];
                    if (hNeighbor == UNPROBED) {
                        continue;
                    }
                    int drop = hCenter - hNeighbor;
                    // Place feature at boundary midpoint
                    float worldX = worldOriginX + x + dx[d] * 0.5f;
                    float worldZ = worldOriginZ + z + dz[d] * 0.5f;

                    if (drop >= 3) {
                        // EDGE: the center is higher, neighbor is lower
                        // Normal points toward the drop (toward the neighbor)
                        SpectralCategory mat = floorMaterials[x][z] != null
                                ? floorMaterials[x][z] : SpectralCategory.HARD;
                        float saliency = TerrainFeature.computeDetectionSaliency(
                                drop, TerrainFeatureType.EDGE.maxRange(), worldX, hCenter, worldZ,
                                playerY, scanSize / 2.0f);
                        features.add(new TerrainFeature(
                                TerrainFeatureType.EDGE,
                                worldX, (float) hCenter, worldZ,
                                ndx[d], 0, ndz[d],
                                (float) drop, saliency, mat));

                        // Depth probe: if fall_depth >= 4, also emit DROP
                        int fallDepth = Math.abs(drop);
                        if (fallDepth >= 4) {
                            float landingY = hNeighbor;
                            SpectralCategory landingMat = floorMaterials[nx][nz] != null
                                    ? floorMaterials[nx][nz] : SpectralCategory.HARD;
                            float dropSaliency = TerrainFeature.computeDetectionSaliency(
                                    fallDepth, TerrainFeatureType.DROP.maxRange(),
                                    worldOriginX + nx, landingY, worldOriginZ + nz,
                                    playerY, scanSize / 2.0f);
                            features.add(new TerrainFeature(
                                    TerrainFeatureType.DROP,
                                    (float) (worldOriginX + nx), landingY,
                                    (float) (worldOriginZ + nz),
                                    0, 1, 0,  // faces upward per spec
                                    (float) fallDepth, dropSaliency, landingMat));
                        }
                    } else if (Math.abs(drop) >= 1 && Math.abs(drop) <= 2) {
                        // STEP
                        SpectralCategory mat = floorMaterials[x][z] != null
                                ? floorMaterials[x][z] : SpectralCategory.HARD;
                        float stepMagnitude = (float) Math.abs(drop);
                        float saliency = TerrainFeature.computeDetectionSaliency(
                                stepMagnitude, TerrainFeatureType.STEP.maxRange(),
                                worldX, hCenter, worldZ,
                                playerY, scanSize / 2.0f);
                        features.add(new TerrainFeature(
                                TerrainFeatureType.STEP,
                                worldX, (float) Math.min(hCenter, hNeighbor), worldZ,
                                ndx[d], 0, ndz[d],
                                stepMagnitude, saliency, mat));
                    }
                }
            }
        }
        return features;
<<<<<<< ours
=======
        return List.of(); // Task 5
>>>>>>> theirs
=======
>>>>>>> theirs
    }

    static List<TerrainFeature> detectWalls(boolean[][] solid, int scanSize,
            int worldOriginX, int worldOriginZ, int playerY,
            int[][] wallHeights, SpectralCategory[][] wallMaterials) {
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        List<TerrainFeature> features = new ArrayList<>();
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        float[] ndx = {1, -1, 0, 0};
        float[] ndz = {0, 0, 1, -1};

        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                if (!solid[x][z]) {
                    continue;
                }
                int height = wallHeights[x][z];
                if (height < 2) {
                    continue;
                }
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d];
                    int nz = z + dz[d];
                    // Skip out-of-bounds neighbors for walls
                    if (nx < 0 || nx >= scanSize || nz < 0 || nz >= scanSize) {
                        continue;
                    }
                    if (!solid[nx][nz]) {
                        float worldX = worldOriginX + x + dx[d] * 0.5f;
                        float worldZ = worldOriginZ + z + dz[d] * 0.5f;
                        SpectralCategory mat = wallMaterials[x][z] != null
                                ? wallMaterials[x][z] : SpectralCategory.HARD;
                        float saliency = TerrainFeature.computeDetectionSaliency(
                                height, TerrainFeatureType.WALL.maxRange(), worldX, playerY, worldZ,
                                playerY, scanSize / 2.0f);
                        features.add(new TerrainFeature(
                                TerrainFeatureType.WALL,
                                worldX, (float) playerY, worldZ,
                                ndx[d], 0, ndz[d],
                                (float) height, saliency, mat));
                    }
                }
            }
        }
        return features;
<<<<<<< ours
=======
        return List.of(); // Task 6
>>>>>>> theirs
=======
>>>>>>> theirs
    }

    static List<TerrainFeature> detectSolidObjects(boolean[][] solid, int scanSize,
            int worldOriginX, int worldOriginZ, int playerY,
            int[][] objectHeights, SpectralCategory[][] objectMaterials) {
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        List<TerrainFeature> features = new ArrayList<>();
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};

        // Label connected components of solid blocks
        int[][] labels = new int[scanSize][scanSize];
        int[] componentSizes = new int[scanSize * scanSize + 1];
        int nextLabel = 1;
        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                if (solid[x][z] && labels[x][z] == 0) {
                    int label = nextLabel++;
                    int size = floodFill(solid, labels, scanSize, x, z, label, dx, dz);
                    componentSizes[label] = size;
                }
            }
        }

        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                if (!solid[x][z]) {
                    continue;
                }
                // Only small clusters (size <= 2) qualify as solid objects
                if (componentSizes[labels[x][z]] > 2) {
                    continue;
                }
                int airNeighbors = 0;
                float aNx = 0, aNz = 0;
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d];
                    int nz = z + dz[d];
                    boolean isAir = (nx < 0 || nx >= scanSize || nz < 0 || nz >= scanSize)
                            || !solid[nx][nz];
                    if (isAir) {
                        airNeighbors++;
                        aNx += dx[d];
                        aNz += dz[d];
                    }
                }
                if (airNeighbors >= 3) {
                    float worldX = worldOriginX + x + 0.5f;
                    float worldZ = worldOriginZ + z + 0.5f;
                    int height = objectHeights[x][z];
                    SpectralCategory mat = objectMaterials[x][z] != null
                            ? objectMaterials[x][z] : SpectralCategory.HARD;
                    float saliency = TerrainFeature.computeDetectionSaliency(
                            height, TerrainFeatureType.SOLID_OBJECT.maxRange(),
                            worldX, playerY, worldZ,
                            playerY, scanSize / 2.0f);
                    // Normal points toward dominant air-neighbor direction
                    float nLen = (float) Math.sqrt(aNx * aNx + aNz * aNz);
                    float fnx = nLen > 0 ? aNx / nLen : 0;
                    float fny = nLen > 0 ? 0 : 1; // default upward if all sides are air
                    float fnz = nLen > 0 ? aNz / nLen : 0;
                    features.add(new TerrainFeature(
                            TerrainFeatureType.SOLID_OBJECT,
                            worldX, (float) playerY, worldZ,
                            fnx, fny, fnz,
                            (float) height, saliency, mat));
                }
            }
        }
        return features;
    }

    private static int floodFill(boolean[][] solid, int[][] labels, int scanSize,
            int startX, int startZ, int label, int[] dx, int[] dz) {
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{startX, startZ});
        labels[startX][startZ] = label;
        int size = 0;
        while (!stack.isEmpty()) {
            int[] cell = stack.remove(stack.size() - 1);
            size++;
            for (int d = 0; d < 4; d++) {
                int nx = cell[0] + dx[d];
                int nz = cell[1] + dz[d];
                if (nx >= 0 && nx < scanSize && nz >= 0 && nz < scanSize
                        && solid[nx][nz] && labels[nx][nz] == 0) {
                    labels[nx][nz] = label;
                    stack.add(new int[]{nx, nz});
                }
            }
        }
        return size;
<<<<<<< ours
=======
        return List.of(); // Task 6
>>>>>>> theirs
=======
>>>>>>> theirs
    }

    static List<TerrainFeature> detectCeilings(int[][] ceiling, int scanSize,
            int worldOriginX, int worldOriginZ, int playerY,
            SpectralCategory[][] ceilingMaterials) {
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        List<TerrainFeature> features = new ArrayList<>();
        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                int cy = ceiling[x][z];
                if (cy == UNPROBED) {
                    continue;
                }
                int distance = cy - playerY;
                if (distance < 1 || distance > 16) {
                    continue;
                }
                float worldX = worldOriginX + x + 0.5f;
                float worldZ = worldOriginZ + z + 0.5f;
                SpectralCategory mat = ceilingMaterials[x][z] != null
                        ? ceilingMaterials[x][z] : SpectralCategory.HARD;
                float saliency = TerrainFeature.computeDetectionSaliency(
                        distance, TerrainFeatureType.CEILING.maxRange(),
                        worldX, (float) cy, worldZ,
                        playerY, scanSize / 2.0f);
                features.add(new TerrainFeature(
                        TerrainFeatureType.CEILING,
                        worldX, (float) cy, worldZ,
                        0, -1, 0,
                        (float) distance, saliency, mat));
            }
        }
        return features;
<<<<<<< ours
    }

    static int[][] chebyshevDistanceTransform(boolean[][] solid, int w, int h) {
        int[][] dist = new int[w][h];
        int INF = w + h; // safe upper bound

        // Initialize: solid → 0, air → INF
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                dist[x][z] = solid[x][z] ? 0 : INF;
            }
        }

        // Forward pass (top-left to bottom-right)
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                if (dist[x][z] == 0) continue;
                int min = dist[x][z];
                if (x > 0) {
                    min = Math.min(min, dist[x - 1][z] + 1);
                    if (z > 0) min = Math.min(min, dist[x - 1][z - 1] + 1);
                }
                if (z > 0) {
                    min = Math.min(min, dist[x][z - 1] + 1);
                    if (x < w - 1) min = Math.min(min, dist[x + 1][z - 1] + 1);
                }
                dist[x][z] = min;
            }
        }

        // Backward pass (bottom-right to top-left)
        for (int x = w - 1; x >= 0; x--) {
            for (int z = h - 1; z >= 0; z--) {
                if (dist[x][z] == 0) continue;
                int min = dist[x][z];
                if (x < w - 1) {
                    min = Math.min(min, dist[x + 1][z] + 1);
                    if (z < h - 1) min = Math.min(min, dist[x + 1][z + 1] + 1);
                }
                if (z < h - 1) {
                    min = Math.min(min, dist[x][z + 1] + 1);
                    if (x > 0) min = Math.min(min, dist[x - 1][z + 1] + 1);
                }
                dist[x][z] = min;
            }
        }

        return dist;
=======
        return List.of(); // Task 6
=======
>>>>>>> theirs
    }

    static int[][] chebyshevDistanceTransform(boolean[][] solid, int w, int h) {
<<<<<<< ours
        return new int[w][h]; // Task 7
>>>>>>> theirs
=======
        int[][] dist = new int[w][h];
        int INF = w + h; // safe upper bound

        // Initialize: solid → 0, air → INF
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                dist[x][z] = solid[x][z] ? 0 : INF;
            }
        }

        // Forward pass (top-left to bottom-right)
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                if (dist[x][z] == 0) continue;
                int min = dist[x][z];
                if (x > 0) {
                    min = Math.min(min, dist[x - 1][z] + 1);
                    if (z > 0) min = Math.min(min, dist[x - 1][z - 1] + 1);
                }
                if (z > 0) {
                    min = Math.min(min, dist[x][z - 1] + 1);
                    if (x < w - 1) min = Math.min(min, dist[x + 1][z - 1] + 1);
                }
                dist[x][z] = min;
            }
        }

        // Backward pass (bottom-right to top-left)
        for (int x = w - 1; x >= 0; x--) {
            for (int z = h - 1; z >= 0; z--) {
                if (dist[x][z] == 0) continue;
                int min = dist[x][z];
                if (x < w - 1) {
                    min = Math.min(min, dist[x + 1][z] + 1);
                    if (z < h - 1) min = Math.min(min, dist[x + 1][z + 1] + 1);
                }
                if (z < h - 1) {
                    min = Math.min(min, dist[x][z + 1] + 1);
                    if (x > 0) min = Math.min(min, dist[x - 1][z + 1] + 1);
                }
                dist[x][z] = min;
            }
        }

        return dist;
>>>>>>> theirs
    }

    static List<TerrainFeature> detectPassages(int[][] dist, int scanSize,
            int worldOriginX, int worldOriginZ, int playerY) {
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        List<TerrainFeature> features = new ArrayList<>();
        float constrictionThreshold = 3.0f;
        float openThreshold = 4.0f;
        int lookAhead = 10;

        // Scan rows (along x-axis for each z)
        for (int z = 0; z < scanSize; z++) {
            for (int x = 0; x < scanSize; x++) {
                float d = dist[x][z];
                if (d < 1 || d > constrictionThreshold) continue;

                // Check for wider area within lookAhead blocks on the left (negative x)
                float maxLeft = 0;
                for (int i = 1; i <= lookAhead && x - i >= 0; i++) {
                    maxLeft = Math.max(maxLeft, dist[x - i][z]);
                }
                // Check for wider area within lookAhead blocks on the right (positive x)
                float maxRight = 0;
                for (int i = 1; i <= lookAhead && x + i < scanSize; i++) {
                    maxRight = Math.max(maxRight, dist[x + i][z]);
                }

                if (maxLeft >= openThreshold && maxRight >= openThreshold) {
                    float worldX = worldOriginX + x + 0.5f;
                    float worldZ = worldOriginZ + z + 0.5f;
                    float magnitude = d * 2;
                    float saliency = TerrainFeature.computeDetectionSaliency(
                            magnitude, TerrainFeatureType.PASSAGE.maxRange(),
                            worldX, playerY, worldZ,
                            playerY, scanSize / 2.0f);
                    features.add(new TerrainFeature(
                            TerrainFeatureType.PASSAGE,
                            worldX, (float) playerY, worldZ,
                            1, 0, 0,
                            magnitude, saliency, SpectralCategory.HARD));
                }
            }
        }

        // Scan columns (along z-axis for each x)
        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                float d = dist[x][z];
                if (d < 1 || d > constrictionThreshold) continue;

                float maxUp = 0;
                for (int i = 1; i <= lookAhead && z - i >= 0; i++) {
                    maxUp = Math.max(maxUp, dist[x][z - i]);
                }
                float maxDown = 0;
                for (int i = 1; i <= lookAhead && z + i < scanSize; i++) {
                    maxDown = Math.max(maxDown, dist[x][z + i]);
                }

                if (maxUp >= openThreshold && maxDown >= openThreshold) {
                    float worldX = worldOriginX + x + 0.5f;
                    float worldZ = worldOriginZ + z + 0.5f;
                    // Avoid duplicating if already placed by row scan at same position
                    float magnitude = d * 2;
                    float saliency = TerrainFeature.computeDetectionSaliency(
                            magnitude, TerrainFeatureType.PASSAGE.maxRange(),
                            worldX, playerY, worldZ,
                            playerY, scanSize / 2.0f);
                    features.add(new TerrainFeature(
                            TerrainFeatureType.PASSAGE,
                            worldX, (float) playerY, worldZ,
                            0, 0, 1,
                            magnitude, saliency, SpectralCategory.HARD));
                }
            }
        }

        return features;
<<<<<<< ours
    }

    static List<TerrainFeature> coalesceWalls(List<TerrainFeature> rawWalls) {
        if (rawWalls.size() <= 2) return new ArrayList<>(rawWalls);

        // Group walls by facing direction (quantized to 4 cardinal directions)
        // Direction key: 0=+x, 1=-x, 2=+z, 3=-z
        List<List<TerrainFeature>> groups = new ArrayList<>();
        boolean[] assigned = new boolean[rawWalls.size()];

        for (int i = 0; i < rawWalls.size(); i++) {
            if (assigned[i]) continue;
            TerrainFeature seed = rawWalls.get(i);
            int seedDir = cardinalDirection(seed.nx(), seed.nz());
            List<TerrainFeature> group = new ArrayList<>();
            group.add(seed);
            assigned[i] = true;

            // BFS: find all walls with same direction and within 2 blocks of any group member
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int j = 0; j < rawWalls.size(); j++) {
                    if (assigned[j]) continue;
                    TerrainFeature candidate = rawWalls.get(j);
                    if (cardinalDirection(candidate.nx(), candidate.nz()) != seedDir) continue;
                    // Check adjacency: within 2 blocks of any member
                    for (TerrainFeature member : group) {
                        if (member.distanceTo(candidate.x(), candidate.y(), candidate.z()) <= 2.0f) {
                            group.add(candidate);
                            assigned[j] = true;
                            changed = true;
                            break;
                        }
                    }
                }
            }
            groups.add(group);
        }

        // For each group, place features at ends, corners, and midpoints
        List<TerrainFeature> result = new ArrayList<>();
        for (List<TerrainFeature> group : groups) {
            if (group.size() <= 2) {
                result.addAll(group);
                continue;
            }
            result.addAll(placeCoalescedPoints(group));
        }
        return result;
    }

    private static List<TerrainFeature> placeCoalescedPoints(List<TerrainFeature> group) {
        // Find the two features furthest apart as the "spine"
        TerrainFeature endA = group.get(0);
        TerrainFeature endB = group.get(0);
        float maxDist = 0;
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                float d = group.get(i).distanceTo(
                        group.get(j).x(), group.get(j).y(), group.get(j).z());
                if (d > maxDist) {
                    maxDist = d;
                    endA = group.get(i);
                    endB = group.get(j);
                }
            }
        }

        List<TerrainFeature> result = new ArrayList<>();
        // Always place endpoints
        result.add(endA);
        if (maxDist > 0.5f) {
            result.add(endB);
        }

        // Place midpoints at ~6 block intervals along the spine
        if (maxDist > 6.0f) {
            float midpointInterval = 6.0f;
            int numMidpoints = (int) (maxDist / midpointInterval);
            TerrainFeature ref = endA;
            for (int i = 1; i <= numMidpoints; i++) {
                float t = i * midpointInterval / maxDist;
                if (t >= 1.0f) break;
                float mx = endA.x() + t * (endB.x() - endA.x());
                float my = endA.y() + t * (endB.y() - endA.y());
                float mz = endA.z() + t * (endB.z() - endA.z());
                // Find the closest original wall feature to this interpolated point
                TerrainFeature closest = group.get(0);
                float closestDist = Float.MAX_VALUE;
                for (TerrainFeature f : group) {
                    float d = f.distanceTo(mx, my, mz);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = f;
                    }
                }
                float saliency = TerrainFeature.computeDetectionSaliency(
                        ref.magnitude(), ref.type().maxRange(), mx, my, mz,
                        my, maxDist / 2.0f);
                result.add(new TerrainFeature(
                        ref.type(), mx, my, mz,
                        ref.nx(), ref.ny(), ref.nz(),
                        ref.magnitude(), saliency, closest.material()));
            }
        }

        return result;
    }

    private static int cardinalDirection(float nx, float nz) {
        if (Math.abs(nx) >= Math.abs(nz)) {
            return nx >= 0 ? 0 : 1; // +x or -x
        } else {
            return nz >= 0 ? 2 : 3; // +z or -z
        }
    }

    static List<TerrainFeature> deduplicateFeatures(List<TerrainFeature> features) {
        if (features.isEmpty()) return features;
        // Sort by magnitude descending so we keep the largest when merging
        List<TerrainFeature> sorted = new ArrayList<>(features);
        sorted.sort((a, b) -> Float.compare(b.magnitude(), a.magnitude()));

        List<TerrainFeature> result = new ArrayList<>();
        boolean[] merged = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (merged[i]) continue;
            TerrainFeature best = sorted.get(i);
            for (int j = i + 1; j < sorted.size(); j++) {
                if (merged[j]) continue;
                TerrainFeature other = sorted.get(j);
                if (best.type() != other.type()) continue;
                if (best.distanceTo(other.x(), other.y(), other.z()) <= 1.0f) {
                    merged[j] = true;
                    // Keep 'best' since it has larger magnitude (sorted)
                }
            }
            result.add(best);
        }
        return result;
    }

    static List<TerrainFeature> applyDensityCaps(List<TerrainFeature> features) {
        List<TerrainFeature> result = new ArrayList<>();

        // Group by family
        List<TerrainFeature> voidFeatures = new ArrayList<>();
        List<TerrainFeature> surfaceFeatures = new ArrayList<>();
        List<TerrainFeature> groundFeatures = new ArrayList<>();

        for (TerrainFeature f : features) {
            switch (f.type().family()) {
                case VOID -> voidFeatures.add(f);
                case SURFACE -> surfaceFeatures.add(f);
                case GROUND -> groundFeatures.add(f);
            }
        }

        // Sort each group by saliency descending, cap
        result.addAll(capByFamily(voidFeatures, CAP_VOID));
        result.addAll(capByFamily(surfaceFeatures, CAP_SURFACE));
        result.addAll(capByFamily(groundFeatures, CAP_GROUND));

        return result;
    }

    private static List<TerrainFeature> capByFamily(List<TerrainFeature> features, int cap) {
        if (features.size() <= cap) return features;
        features.sort((a, b) -> Float.compare(b.saliency(), a.saliency()));
        return new ArrayList<>(features.subList(0, cap));
=======
        return List.of(); // Task 7
=======
>>>>>>> theirs
    }

    static List<TerrainFeature> coalesceWalls(List<TerrainFeature> rawWalls) {
        return rawWalls; // Task 8
    }

    static List<TerrainFeature> deduplicateFeatures(List<TerrainFeature> features) {
        return features; // Task 8
    }

    static List<TerrainFeature> applyDensityCaps(List<TerrainFeature> features) {
        return features; // Task 8
>>>>>>> theirs
    }

    // ── internal helpers ─────────────────────────────────────────────────

    private void allocateGrids() {
        floor = new int[gridSize][gridSize];
        ceiling = new int[gridSize][gridSize];
        solid = new boolean[gridSize][gridSize];
        floorMaterials = new SpectralCategory[gridSize][gridSize];
        ceilingMaterials = new SpectralCategory[gridSize][gridSize];
        wallMaterials = new SpectralCategory[gridSize][gridSize];
    }

    private void clearGrids() {
        for (int x = 0; x < gridSize; x++) {
            Arrays.fill(floor[x], UNPROBED);
            Arrays.fill(ceiling[x], UNPROBED);
            Arrays.fill(solid[x], false);
            Arrays.fill(floorMaterials[x], null);
            Arrays.fill(ceilingMaterials[x], null);
            Arrays.fill(wallMaterials[x], null);
        }
    }

    /**
     * Derives wall heights from the solid and ceiling grids.
     * For each solid cell adjacent to air, the wall height is estimated
     * as the distance from playerY to the ceiling (or a default of 3).
     */
    static int[][] probeWallHeights(boolean[][] solid, int[][] ceiling,
            int scanSize, int playerY) {
        int[][] heights = new int[scanSize][scanSize];
        for (int x = 0; x < scanSize; x++) {
            for (int z = 0; z < scanSize; z++) {
                if (solid[x][z]) {
                    // Use ceiling info if available, otherwise default
                    if (ceiling[x][z] != UNPROBED && ceiling[x][z] > playerY) {
                        heights[x][z] = ceiling[x][z] - playerY;
                    } else {
                        heights[x][z] = 3; // reasonable default
                    }
                }
            }
        }
        return heights;
    }

    /**
     * Derives object heights similarly to wall heights.
     */
    static int[][] probeObjectHeights(boolean[][] solid, int[][] ceiling,
            int scanSize, int playerY) {
        return probeWallHeights(solid, ceiling, scanSize, playerY);
    }
}
