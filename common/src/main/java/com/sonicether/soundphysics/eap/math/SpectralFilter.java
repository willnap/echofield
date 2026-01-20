package com.sonicether.soundphysics.eap.math;

import com.sonicether.soundphysics.config.blocksound.BlockSoundConfigBase;
import com.sonicether.soundphysics.eap.SpectralCategory;
import net.minecraft.world.level.block.state.BlockState;

/** Derives 3-band absorption from SpectralCategory physical coefficients. */
public final class SpectralFilter {
    private SpectralFilter() {}

    /** Computes {low, mid, high} absorption for a block using physical absorption coefficients. */
    public static float[] computeAbsorption(BlockState block, BlockSoundConfigBase config) {
        SpectralCategory category = SpectralCategory.fromSoundType(block.getSoundType());
        return computeAbsorption(category);
    }

    /**
     * Returns the physical absorption coefficients for the given spectral category.
     * These are based on real-world measurements (ISO 354) rather than SPR's game
     * reflectivity values, which are multipliers that can exceed 1.0.
     * Package-private for unit testing without Minecraft BlockState.
     */
    static float[] computeAbsorption(SpectralCategory category) {
        return new float[]{category.lowAbs(), category.midAbs(), category.highAbs()};
    }

    /** @deprecated Use computeAbsorption(SpectralCategory) instead. */
    @Deprecated
    static float[] computeAbsorption(float reflectivity, SpectralCategory category) {
        return computeAbsorption(category);
    }
}
