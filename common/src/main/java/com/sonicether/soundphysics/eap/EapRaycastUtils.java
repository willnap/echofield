package com.sonicether.soundphysics.eap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Set;

public class EapRaycastUtils {

    private static final Set<Block> ACOUSTIC_BLOCKLIST = Set.of(
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.LEVER,
            Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON,
            Blocks.BIRCH_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.ACACIA_BUTTON,
            Blocks.DARK_OAK_BUTTON, Blocks.CHERRY_BUTTON, Blocks.MANGROVE_BUTTON,
            Blocks.BAMBOO_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON,
            Blocks.POLISHED_BLACKSTONE_BUTTON,
            Blocks.STONE_PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE,
            Blocks.SPRUCE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE,
            Blocks.JUNGLE_PRESSURE_PLATE, Blocks.ACACIA_PRESSURE_PLATE,
            Blocks.DARK_OAK_PRESSURE_PLATE, Blocks.CHERRY_PRESSURE_PLATE,
            Blocks.MANGROVE_PRESSURE_PLATE, Blocks.BAMBOO_PRESSURE_PLATE,
            Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE,
            Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_SIGN, Blocks.SPRUCE_WALL_SIGN,
            Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN,
            Blocks.ACACIA_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.DARK_OAK_SIGN, Blocks.DARK_OAK_WALL_SIGN,
            Blocks.CHERRY_SIGN, Blocks.CHERRY_WALL_SIGN, Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN,
            Blocks.BAMBOO_SIGN, Blocks.BAMBOO_WALL_SIGN, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN,
            Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN,
            Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN,
            Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN,
            Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN,
            Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN,
            Blocks.ACACIA_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN,
            Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN,
            Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN,
            Blocks.MANGROVE_HANGING_SIGN, Blocks.MANGROVE_WALL_HANGING_SIGN,
            Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN,
            Blocks.CRIMSON_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN,
            Blocks.WARPED_HANGING_SIGN, Blocks.WARPED_WALL_HANGING_SIGN,
            Blocks.REDSTONE_WIRE, Blocks.REPEATER, Blocks.COMPARATOR,
            Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL,
            Blocks.FLOWER_POT, Blocks.LADDER
    );

    public static BlockHitResult rayCast(@Nullable BlockGetter blockGetter, Vec3 from, Vec3 to, @Nullable BlockPos ignore) {
        if (blockGetter == null) {
            return BlockHitResult.miss(to, Direction.getApproximateNearest(from.subtract(to)), BlockPos.containing(to));
        }
        return BlockGetter.traverseBlocks(from, to, blockGetter, (g, pos) -> {
            if (pos.equals(ignore)) {
                return null;
            }
            BlockState blockState = blockGetter.getBlockState(pos);

            if (ACOUSTIC_BLOCKLIST.contains(blockState.getBlock())) {
                return null;
            }

            VoxelShape shape = ClipContext.Block.OUTLINE.get(blockState, blockGetter, pos, CollisionContext.empty());
            BlockHitResult blockHitResult = blockGetter.clipWithInteractionOverride(from, to, pos, shape, blockState);
            FluidState fluidState = blockGetter.getFluidState(pos);
            VoxelShape fluidShape = fluidState.getShape(blockGetter, pos);
            BlockHitResult fluidHitResult = fluidShape.clip(from, to, pos);

            if (fluidHitResult == null) {
                return blockHitResult;
            }
            if (blockHitResult == null) {
                return fluidHitResult;
            }
            double blockDistance = from.distanceToSqr(blockHitResult.getLocation());
            double fluidDistance = from.distanceToSqr(fluidHitResult.getLocation());
            return blockDistance <= fluidDistance ? blockHitResult : fluidHitResult;
        }, (g) -> {
            return BlockHitResult.miss(to, Direction.getApproximateNearest(from.subtract(to)), BlockPos.containing(to));
        });
    }

    public static boolean isBlocklisted(Block block) {
        return ACOUSTIC_BLOCKLIST.contains(block);
    }
}
