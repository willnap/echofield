package com.sonicether.soundphysics.eap;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Immutable per-ray hit data from the EnvironmentProfiler. */
public record ReflectionTap(
        Vec3 position, double distance, double delay, double energy,
        BlockState material, Vec3 direction, int order
) {
    private static final double SPEED_OF_SOUND = 343.0;

    /** Factory that computes delay = distance / 343.0 automatically. */
    public static ReflectionTap of(Vec3 position, double distance, double energy,
                                    BlockState material, Vec3 direction, int order) {
        return new ReflectionTap(position, distance, distance / SPEED_OF_SOUND,
                energy, material, direction, order);
    }
}
