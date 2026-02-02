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

<<<<<<< ours
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
=======
    @Test void stone_clamped_zeroAbsorption() {
        float[] a = SpectralFilter.computeAbsorption(1.5f, SpectralCategory.HARD);
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f}, a, 0.001f);
    }

    @Test void wool_softHighFreqBias() {
        float[] a = SpectralFilter.computeAbsorption(0.1f, SpectralCategory.SOFT);
        assertEquals(0.45f, a[0], 0.001f);
        assertEquals(0.9f, a[1], 0.001f);
        assertEquals(1.0f, a[2], 0.001f);
    }

    @Test void wood_slightHighAbsorption() {
        float[] a = SpectralFilter.computeAbsorption(0.4f, SpectralCategory.WOOD);
        assertEquals(0.42f, a[0], 0.001f);
        assertEquals(0.6f, a[1], 0.001f);
        assertEquals(0.78f, a[2], 0.001f);
    }

    @Test void negativeReflectivity_fullAbsorption() {
        float[] a = SpectralFilter.computeAbsorption(-0.5f, SpectralCategory.HARD);
        assertArrayEquals(new float[]{1.0f, 1.0f, 1.0f}, a, 0.001f);
    }

    @Test void foliage_midHighBias() {
        float[] a = SpectralFilter.computeAbsorption(0.3f, SpectralCategory.FOLIAGE);
        assertEquals(0.21f, a[0], 0.001f);
        assertEquals(0.84f, a[1], 0.001f);
        assertEquals(1.0f, a[2], 0.001f);
    }

    @Test void defaultSameAsHard() {
        assertArrayEquals(
                SpectralFilter.computeAbsorption(0.5f, SpectralCategory.HARD),
                SpectralFilter.computeAbsorption(0.5f, SpectralCategory.DEFAULT), 0.001f);
>>>>>>> theirs
    }
}
