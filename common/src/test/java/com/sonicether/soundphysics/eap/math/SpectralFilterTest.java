package com.sonicether.soundphysics.eap.math;

import com.sonicether.soundphysics.eap.SpectralCategory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpectralFilterTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void stone_lowAbsorption() {
        float[] a = SpectralFilter.computeAbsorption(SpectralCategory.HARD);
        assertArrayEquals(new float[]{0.02f, 0.03f, 0.05f}, a, 0.001f);
    }

    @Test void wool_highAbsorption() {
        float[] a = SpectralFilter.computeAbsorption(SpectralCategory.SOFT);
        assertArrayEquals(new float[]{0.25f, 0.40f, 0.55f}, a, 0.001f);
    }

    @Test void wood_moderateAbsorption() {
        float[] a = SpectralFilter.computeAbsorption(SpectralCategory.WOOD);
        assertArrayEquals(new float[]{0.10f, 0.08f, 0.12f}, a, 0.001f);
    }

    @Test void foliage_lfTransparent() {
        float[] a = SpectralFilter.computeAbsorption(SpectralCategory.FOLIAGE);
        assertArrayEquals(new float[]{0.10f, 0.20f, 0.35f}, a, 0.001f);
    }

    @Test void defaultCategory() {
        float[] a = SpectralFilter.computeAbsorption(SpectralCategory.DEFAULT);
        assertArrayEquals(new float[]{0.05f, 0.05f, 0.05f}, a, 0.001f);
    }

    @Test void hardAndDefaultDiffer() {
        // HARD and DEFAULT now have different absorption values
        float[] hard = SpectralFilter.computeAbsorption(SpectralCategory.HARD);
        float[] def = SpectralFilter.computeAbsorption(SpectralCategory.DEFAULT);
        assertFalse(java.util.Arrays.equals(hard, def));
    }
}
