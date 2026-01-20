package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.SoundPhysics;
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import com.sonicether.soundphysics.eap.SpectralCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class HyperrealitySystem {

    private TerrainScanner scanner;
    private TerrainBufferFactory bufferFactory;
    private HyperrealityPool pool;

    private boolean initialized = false;
    private float intensity = 0.5f;
    private int pendingRange = -1;

    public HyperrealitySystem() {
        this.scanner = new TerrainScanner();
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(0f, Math.min(1f, intensity));
    }

    public void setRange(int range) {
        this.pendingRange = range;
    }

    private void ensureInitialized() {
        if (initialized) return;

        bufferFactory = new TerrainBufferFactory();
        bufferFactory.init();

        pool = new HyperrealityPool(bufferFactory);
        pool.init();

        initialized = true;
        Loggers.log("HyperrealitySystem: initialized ({} sources, {} buffers)",
                pool.getPoolSize(), 12);
    }

    private int debugTickCounter = 0;

    public void tick(EnvironmentProfile profile, float masterGain,
                     float sceneEnergy, Vec3 playerPos) {
        ensureInitialized();

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;

        float px = (float) playerPos.x;
        float py = (float) playerPos.y;
        float pz = (float) playerPos.z;

        int playerBlockX = (int) Math.floor(px);
        int playerBlockY = (int) Math.floor(py);
        int playerBlockZ = (int) Math.floor(pz);

        scanner.incrementTickCounter();

        if (pendingRange >= 0 && scanner.isCycleComplete()) {
            scanner.setRange(pendingRange);
            pendingRange = -1;
        }

        if (scanner.isCycleComplete() && scanner.shouldRescan(px, py, pz)) {
            scanner.beginCycle(playerBlockX, playerBlockY, playerBlockZ);
            Loggers.log("HYPER-SCAN: beginCycle at {},{},{} gridSize={}", playerBlockX, playerBlockY, playerBlockZ, scanner.getGridSize());
        }

        if (!scanner.isCycleComplete()) {
            scanCurrentSlice(level, scanner);
            scanner.advanceSlice();

            if (scanner.isCycleComplete()) {
                // Log raw grid data sample before analysis
                int[][] floorArr = scanner.getFloorArray();
                int probed = 0, unprobed = 0;
                int gs = scanner.getGridSize();
                for (int gx = 0; gx < gs; gx++) {
                    for (int gz = 0; gz < gs; gz++) {
                        if (floorArr[gx][gz] == TerrainScanner.UNPROBED) unprobed++;
                        else probed++;
                    }
                }
                Loggers.log("HYPER-SCAN: cycle complete. Floor grid: probed={} unprobed={} total={}", probed, unprobed, gs*gs);

                scanner.analyzeAndProduceFeatures();
                List<TerrainFeature> features = scanner.getFeatures();
                Loggers.log("HYPER-SCAN: analyzeAndProduceFeatures() returned {} features", features.size());
                if (!features.isEmpty()) {
                    TerrainFeature f0 = features.get(0);
                    Loggers.log("HYPER-SCAN: first feature: type={} pos=({},{},{}) mag={} sal={}",
                            f0.type(), f0.x(), f0.y(), f0.z(), f0.magnitude(), f0.saliency());
                }

                scanner.markCycleComplete();

                pool.reconcile(features, px, py, pz, scanner.getScanRadius());
            }
        }

        pool.tick(masterGain, intensity, px, py, pz);

        int maxSends = SoundPhysics.getMaxAuxSends();
        if (maxSends > 0) {
            float hfAbsorption = profile.spectralProfile()[2];
            pool.applyReverb(profile.enclosureFactor(), profile.estimatedRT60(),
                    hfAbsorption, maxSends);
        }

        debugTickCounter++;
        if (debugTickCounter % 100 == 0) {
            int active = pool.getActiveCount();
            float maxGain = 0f;
            for (HyperrealitySource src : pool.getSources()) {
                if (src.isActive() && src.getCurrentGain() > maxGain) {
                    maxGain = src.getCurrentGain();
                }
            }
            List<TerrainFeature> feats = scanner.getFeatures();
            Loggers.log("HYPER-DEBUG tick={} features={} active={} maxGain={} masterGain={} intensity={}",
                    debugTickCounter, feats.size(), active, maxGain, masterGain, intensity);
        }
    }

    private static void scanCurrentSlice(Level level, TerrainScanner scanner) {
        int gridSize = scanner.getGridSize();
        int slicesPerTick = Math.max(1, (int) Math.ceil(gridSize / 7.0));
        int startZ = scanner.getCurrentSlice() * slicesPerTick;
        int endZ = Math.min(startZ + slicesPerTick, gridSize);

        int[][] floor = scanner.getFloorArray();
        int[][] ceiling = scanner.getCeilingArray();
        boolean[][] solid = scanner.getSolidArray();
        SpectralCategory[][] floorMats = scanner.getFloorMaterials();
        SpectralCategory[][] ceilingMats = scanner.getCeilingMaterials();
        SpectralCategory[][] wallMats = scanner.getWallMaterials();

        int centerY = scanner.getScanCenterY();

        for (int gz = startZ; gz < endZ; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int wx = scanner.gridToWorldX(gx);
                int wz = scanner.gridToWorldZ(gz);

                if (!level.hasChunkAt(new BlockPos(wx, centerY, wz))) {
                    floor[gx][gz] = TerrainScanner.UNPROBED;
                    ceiling[gx][gz] = TerrainScanner.UNPROBED;
                    continue;
                }

                int floorY = probeFloor(level, wx, centerY, wz);
                floor[gx][gz] = floorY;

                if (floorY != TerrainScanner.UNPROBED) {
                    BlockState floorState = level.getBlockState(new BlockPos(wx, floorY, wz));
                    floorMats[gx][gz] = SpectralCategory.fromBlockState(floorState);
                }

                int ceilingY = probeCeiling(level, wx, centerY, wz);
                ceiling[gx][gz] = ceilingY;

                if (ceilingY != TerrainScanner.UNPROBED) {
                    BlockState ceilingState = level.getBlockState(new BlockPos(wx, ceilingY, wz));
                    ceilingMats[gx][gz] = SpectralCategory.fromBlockState(ceilingState);
                }

                BlockState atY = level.getBlockState(new BlockPos(wx, centerY, wz));
                BlockState atY1 = level.getBlockState(new BlockPos(wx, centerY + 1, wz));
                boolean isSolid = atY.isSolidRender() || atY1.isSolidRender();
                solid[gx][gz] = isSolid;

                if (isSolid) {
                    BlockState wallState = atY.isSolidRender() ? atY : atY1;
                    wallMats[gx][gz] = SpectralCategory.fromBlockState(wallState);
                }
            }
        }
    }

    private static int probeFloor(Level level, int x, int startY, int z) {
        for (int y = startY; y >= startY - TerrainScanner.VERTICAL_PROBE_RANGE && y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isSolidRender()) {
                return y;
            }
        }
        return TerrainScanner.UNPROBED;
    }

    private static int probeCeiling(Level level, int x, int startY, int z) {
        for (int y = startY + 1; y <= startY + TerrainScanner.VERTICAL_PROBE_RANGE && y <= level.getMinY() + level.getHeight(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isSolidRender()) {
                return y;
            }
        }
        return TerrainScanner.UNPROBED;
    }

    public void silenceAll() {
        if (pool != null) {
            pool.silenceAll();
        }
    }

    public void forceRescan() {
        scanner.forceRescan();
    }

    public void shutdown() {
        if (pool != null) {
            pool.shutdown();
        }
        if (bufferFactory != null) {
            bufferFactory.shutdown();
        }
        initialized = false;
        Loggers.log("HyperrealitySystem: shutdown complete");
    }

    public int getActiveCount() {
        return pool != null ? pool.getActiveCount() : 0;
    }

    public int getSourceCount() {
        return pool != null ? pool.getPoolSize() : 0;
    }

    public TerrainScanner getScanner() {
        return scanner;
    }

    public HyperrealityPool getPool() {
        return pool;
    }
}
