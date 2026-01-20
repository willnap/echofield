package com.sonicether.soundphysics.eap.emitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans blocks near the player to find positions for Layer 1 emitters.
 * Runs incrementally — scans a slice per tick to spread the work.
 */
public final class EmitterScanner {

    private static final int SCAN_RADIUS = 24;
    private static final int SLICES_PER_CYCLE = 8;

    private final List<EmitterCandidate> candidates = new ArrayList<>();
    private int currentSlice = 0;
    private int scanCenterX, scanCenterY, scanCenterZ;
    private boolean cycleComplete = false;

    public record EmitterCandidate(EmitterCategory category, int x, int y, int z) {}

    public void beginCycle(int centerX, int centerY, int centerZ) {
        candidates.clear();
        currentSlice = -SCAN_RADIUS;
        scanCenterX = centerX;
        scanCenterY = centerY;
        scanCenterZ = centerZ;
        cycleComplete = false;
    }

    public boolean tickScan(Level level) {
        if (cycleComplete) return true;

        int endSlice = Math.min(currentSlice + SLICES_PER_CYCLE, SCAN_RADIUS + 1);

        for (int dx = currentSlice; dx < endSlice; dx++) {
            int wx = scanCenterX + dx;
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                int wz = scanCenterZ + dz;
                int minY = Math.max(level.getMinY(), scanCenterY - 16);
                int maxY = Math.min(level.getMinY() + level.getHeight(), scanCenterY + 16);

                for (int wy = minY; wy < maxY; wy++) {
                    BlockPos pos = new BlockPos(wx, wy, wz);
                    BlockState state = level.getBlockState(pos);
                    EmitterCategory cat = classifyBlock(state, level, pos);
                    if (cat != null) {
                        candidates.add(new EmitterCandidate(cat, wx, wy, wz));
                    }
                }
            }
        }

        currentSlice = endSlice;
        if (currentSlice > SCAN_RADIUS) {
            cycleComplete = true;
        }
        return cycleComplete;
    }

    private EmitterCategory classifyBlock(BlockState state, Level level, BlockPos pos) {
        // Avalanche-quality hash: murmurhash3 finalizer ensures uniform distribution
        // regardless of coordinate region. The old linear hash (x*73+z*37+y*11)
        // produced non-uniform distributions for specific coordinate ranges.
        int h = pos.getX() * 374761393 + pos.getZ() * 668265263 + pos.getY() * 1274126177;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        int hash = h & 0xFF;

        if (state.is(BlockTags.LEAVES)) {
            BlockState above = level.getBlockState(pos.above());
            if (above.isAir()) {
                if (hash < 8) return EmitterCategory.BIRD;   // ~3% of canopy leaves
                if (hash < 34) return EmitterCategory.WIND_LEAF; // ~10% of canopy leaves
                return null;
            }
            if (above.is(BlockTags.LEAVES)) {
                if (hash < 20) return EmitterCategory.WIND_LEAF; // ~8% of interior leaves
                return null;
            }
            return null;
        }

        if (state.is(Blocks.GRASS_BLOCK) && level.getBlockState(pos.above()).isAir()
                && level.getBrightness(LightLayer.SKY, pos.above()) > 8) {
            if (hash < 8) return EmitterCategory.INSECT;     // ~3% of outdoor grass
            if (hash >= 8 && hash < 18) return EmitterCategory.WIND_GRASS; // ~4% of outdoor grass
        }

        String blockId = state.getBlock().getDescriptionId();
        if ((blockId.contains("grass") && !blockId.contains("grass_block"))
                || blockId.contains("fern") || blockId.contains("tall_grass")) {
            return EmitterCategory.WIND_GRASS;
        }

        if (state.getFluidState().is(FluidTags.WATER)
                && level.getBlockState(pos.above()).isAir()) {
            if (hasAdjacentSolid(level, pos) && hash < 4) {
                return EmitterCategory.FROG;                  // ~1.5% of shoreline water
            }
            // Still water: source blocks with no adjacent flow (~1.5-3% hash-gated)
            if (state.getFluidState().isSource() && !hasAdjacentFlow(level, pos)) {
                if (hash < 8) return EmitterCategory.WATER_STILL; // ~3% of still water surfaces
                return null;
            }
            return EmitterCategory.WATER_FLOW;
        }

        if (state.isAir()) {
            BlockState above = level.getBlockState(pos.above());
            if (above.getFluidState().is(FluidTags.WATER)
                    || (!above.isAir() && hasAdjacentWater(level, pos.above()))) {
                if (level.getBrightness(LightLayer.SKY, pos) < 4) {
                    return EmitterCategory.WATER_DRIP;
                }
            }
        }

        if (state.getFluidState().is(FluidTags.LAVA)
                && level.getBlockState(pos.above()).isAir()) {
            return EmitterCategory.LAVA;
        }

        if (state.isAir()
                && level.getBrightness(LightLayer.SKY, pos) == 0
                && level.getBrightness(LightLayer.BLOCK, pos) < 8) {
            if (hash < 4) {
                return EmitterCategory.CAVE_AMBIENT;
            }
            // Continuous cave drone at ~2% of dark air blocks (separate from triggered events)
            if (hash >= 4 && hash < 9) {
                return EmitterCategory.CAVE_DRONE;
            }
        }

        if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)) {
            if (state.hasProperty(net.minecraft.world.level.block.FurnaceBlock.LIT)
                    && state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)) {
                return EmitterCategory.MECHANICAL;
            }
        }

        return null;
    }

    private static boolean hasAdjacentFlow(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            FluidState adjacent = level.getFluidState(pos.relative(dir));
            if (!adjacent.isEmpty() && !adjacent.isSource()) return true;
        }
        return false;
    }

    private boolean hasAdjacentSolid(Level level, BlockPos pos) {
        return level.getBlockState(pos.north()).isSolidRender()
                || level.getBlockState(pos.south()).isSolidRender()
                || level.getBlockState(pos.east()).isSolidRender()
                || level.getBlockState(pos.west()).isSolidRender();
    }

    private boolean hasAdjacentWater(Level level, BlockPos pos) {
        return level.getBlockState(pos.north()).getFluidState().is(FluidTags.WATER)
                || level.getBlockState(pos.south()).getFluidState().is(FluidTags.WATER)
                || level.getBlockState(pos.east()).getFluidState().is(FluidTags.WATER)
                || level.getBlockState(pos.west()).getFluidState().is(FluidTags.WATER);
    }

    public List<EmitterCandidate> getCandidates() {
        return candidates;
    }

    public boolean isCycleComplete() {
        return cycleComplete;
    }
}
