package com.sonicether.soundphysics.eap.hyperreality;

import com.sonicether.soundphysics.eap.SpectralCategory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainFeatureTest {

    private static final float TOL = 0.001f;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void construction_storesAllFields() {
        TerrainFeature f = new TerrainFeature(
                TerrainFeatureType.EDGE,
                10f, 65f, 20f, 0f, 1f, 0f,
                3.5f, 0.8f, SpectralCategory.HARD
        );
        assertEquals(TerrainFeatureType.EDGE, f.type());
        assertEquals(10f, f.x(), TOL);
        assertEquals(65f, f.y(), TOL);
        assertEquals(20f, f.z(), TOL);
        assertEquals(3.5f, f.magnitude(), TOL);
        assertEquals(0.8f, f.saliency(), TOL);
        assertEquals(SpectralCategory.HARD, f.material());
    }

    @Test
    void family_delegatesToType() {
        TerrainFeature f = new TerrainFeature(
                TerrainFeatureType.WALL, 0, 0, 0, 1, 0, 0,
                1f, 0.5f, SpectralCategory.HARD
        );
        assertEquals(TerrainFeatureType.Family.SURFACE, f.family());
    }

    @Test
    void distanceTo_samePoint_isZero() {
        TerrainFeature f = new TerrainFeature(
                TerrainFeatureType.STEP, 5f, 10f, 15f, 0, 1, 0,
                1f, 0.5f, SpectralCategory.SOFT
        );
        assertEquals(0f, f.distanceTo(5f, 10f, 15f), TOL);
    }

    @Test
    void distanceTo_knownDistance() {
        TerrainFeature f = new TerrainFeature(
                TerrainFeatureType.DROP, 0f, 0f, 0f, 0, 1, 0,
                5f, 0.9f, SpectralCategory.HARD
        );
        assertEquals(5f, f.distanceTo(3f, 4f, 0f), TOL);
    }

    @Test
    void computeSaliency_atMaxMagnitudeZeroDistance_isHigh() {
        float s = TerrainFeature.computeSaliency(
                10f, 10f, 0f, 24f,
                0f, 0f, 1f, 0f, 0f, 1f, 1.5f
        );
        assertEquals(1.5f, s, TOL);
    }

    @Test
    void computeSaliency_atZeroMagnitude_isZero() {
        float s = TerrainFeature.computeSaliency(
                0f, 10f, 5f, 24f,
                0f, 0f, 1f, 0f, 0f, 1f, 1.0f
        );
        assertEquals(0f, s, TOL);
    }

    @Test
    void computeSaliency_atMaxRange_isZero() {
        float s = TerrainFeature.computeSaliency(
                5f, 10f, 24f, 24f,
                0f, 0f, 1f, 0f, 0f, 1f, 1.0f
        );
        assertEquals(0f, s, TOL);
    }

    @Test
    void computeSaliency_facingAway_hasMinFacing() {
        float s = TerrainFeature.computeSaliency(
                10f, 10f, 0f, 24f,
                0f, 0f, -1f, 0f, 0f, 1f, 1.0f
        );
        assertEquals(0.6f, s, TOL);
    }

    @Test
    void computeSaliency_halfDistance_halfProximity() {
        float s = TerrainFeature.computeSaliency(
                10f, 10f, 12f, 24f,
                0f, 0f, 1f, 0f, 0f, 1f, 1.0f
        );
        assertEquals(0.5f, s, TOL);
    }

    @Test
    void record_equality_sameValues() {
        TerrainFeature a = new TerrainFeature(
                TerrainFeatureType.WALL, 1, 2, 3, 0, 1, 0,
                1f, 0.5f, SpectralCategory.HARD
        );
        TerrainFeature b = new TerrainFeature(
                TerrainFeatureType.WALL, 1, 2, 3, 0, 1, 0,
                1f, 0.5f, SpectralCategory.HARD
        );
        assertEquals(a, b);
    }

    @Test
    void computeDetectionSaliency_atMaxMagnitudeZeroVerticalDistance_isHigh() {
        float s = TerrainFeature.computeDetectionSaliency(
                10f, 10f, 5f, 64f, 5f, 64f, 24f);
        assertEquals(1.0f, s, TOL);
    }

    @Test
    void computeDetectionSaliency_atZeroMagnitude_isZero() {
        float s = TerrainFeature.computeDetectionSaliency(
                0f, 10f, 5f, 64f, 5f, 64f, 24f);
        assertEquals(0f, s, TOL);
    }

    @Test
    void computeDetectionSaliency_zeroMaxMagnitude_isZero() {
        float s = TerrainFeature.computeDetectionSaliency(
                5f, 0f, 5f, 64f, 5f, 64f, 24f);
        assertEquals(0f, s, TOL);
    }

    @Test
    void computeDetectionSaliency_zeroScanRadius_isZero() {
        float s = TerrainFeature.computeDetectionSaliency(
                5f, 10f, 5f, 64f, 5f, 64f, 0f);
        assertEquals(0f, s, TOL);
    }

    @Test
    void computeDetectionSaliency_verticalDistanceReducesProximity() {
        float s = TerrainFeature.computeDetectionSaliency(
                10f, 10f, 5f, 76f, 5f, 64f, 24f);
        assertEquals(0.5f, s, TOL);
    }
}
