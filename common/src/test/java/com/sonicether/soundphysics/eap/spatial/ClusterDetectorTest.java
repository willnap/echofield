package com.sonicether.soundphysics.eap.spatial;

import com.sonicether.soundphysics.SoundPhysicsMod;
import com.sonicether.soundphysics.config.ReflectivityConfig;
import com.sonicether.soundphysics.config.SoundPhysicsConfig;
import com.sonicether.soundphysics.eap.ReflectionTap;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClusterDetectorTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SoundPhysicsMod.CONFIG = ConfigBuilder.builder(SoundPhysicsConfig::new)
                .path(tempDir.resolve("soundphysics.properties")).build();
        SoundPhysicsMod.REFLECTIVITY_CONFIG = new ReflectivityConfig(
                tempDir.resolve("reflectivity.properties"));
    }

    @Test
    void emptyTaps_returnsEmptyList() {
        List<SurfaceCluster> clusters = ClusterDetector.detect(List.of());
        assertTrue(clusters.isEmpty());
    }

    @Test
    void singleTap_producesSingleCluster() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        ReflectionTap tap = ReflectionTap.of(
                new Vec3(5, 0, 0), 5.0, 0.8, stone, new Vec3(1, 0, 0), 1);

        List<SurfaceCluster> clusters = ClusterDetector.detect(List.of(tap));

        assertEquals(1, clusters.size());
        SurfaceCluster c = clusters.get(0);
        assertEquals(1, c.tapCount());
        assertEquals(5.0f, c.averageDistance(), 0.001f);
        assertEquals(0.8f, c.totalEnergy(), 0.001f);
    }

    @Test
    void oppositeDirections_produceTwoClusters() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        ReflectionTap left = ReflectionTap.of(
                new Vec3(-5, 0, 0), 5.0, 0.6, stone, new Vec3(-1, 0, 0), 1);
        ReflectionTap right = ReflectionTap.of(
                new Vec3(5, 0, 0), 5.0, 0.4, stone, new Vec3(1, 0, 0), 1);

        List<SurfaceCluster> clusters = ClusterDetector.detect(List.of(left, right));

        assertEquals(2, clusters.size());
    }

    @Test
    void sameDirection_mergedIntoOneCluster() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        ReflectionTap t1 = ReflectionTap.of(
                new Vec3(4, 0, 0), 4.0, 0.5, stone, new Vec3(1, 0, 0), 1);
        ReflectionTap t2 = ReflectionTap.of(
                new Vec3(6, 0, 0), 6.0, 0.3, stone, new Vec3(1, 0, 0), 1);

        List<SurfaceCluster> clusters = ClusterDetector.detect(List.of(t1, t2));

        assertEquals(1, clusters.size());
        SurfaceCluster c = clusters.get(0);
        assertEquals(2, c.tapCount());
        assertEquals(5.0f, c.averageDistance(), 0.001f);
        assertEquals(5.0, c.centroid().x, 0.001);
    }

    @Test
    void clusters_sortedByTotalEnergyDescending() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        ReflectionTap low = ReflectionTap.of(
                new Vec3(5, 0, 0), 5.0, 0.1, stone, new Vec3(1, 0, 0), 1);
        ReflectionTap high = ReflectionTap.of(
                new Vec3(-5, 0, 0), 5.0, 0.9, stone, new Vec3(-1, 0, 0), 1);

        List<SurfaceCluster> clusters = ClusterDetector.detect(List.of(low, high));

        assertEquals(2, clusters.size());
        assertTrue(clusters.get(0).totalEnergy() >= clusters.get(1).totalEnergy());
    }

    @Test
    void maxClusters_limitEnforced() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        List<ReflectionTap> taps = new ArrayList<>();
        // Create taps in all 14 bin directions to fill all bins
        double[][] dirs = {
                {1, 0, 0}, {-1, 0, 0},
                {0, 1, 0}, {0, -1, 0},
                {0, 0, 1}, {0, 0, -1},
                {1, 1, 0}, {1, -1, 0},
                {-1, 1, 0}, {-1, -1, 0},
                {0, 1, 1}, {0, 1, -1},
                {0, -1, 1}, {0, -1, -1},
        };
        for (double[] d : dirs) {
            Vec3 pos = new Vec3(d[0] * 5, d[1] * 5, d[2] * 5);
            Vec3 dir = new Vec3(d[0], d[1], d[2]).normalize();
            taps.add(ReflectionTap.of(pos, 5.0, 0.5, stone, dir, 1));
        }

        List<SurfaceCluster> clusters = ClusterDetector.detect(taps);

        assertTrue(clusters.size() <= ClusterDetector.MAX_CLUSTERS);
    }

    @Test
    void listenerOffset_affectsBinning() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        // Tap at (10, 0, 0), listener at (8, 0, 0) => direction is (+1, 0, 0)
        // Same tap with listener at (12, 0, 0) => direction is (-1, 0, 0)
        ReflectionTap tap = ReflectionTap.of(
                new Vec3(10, 0, 0), 2.0, 0.5, stone, new Vec3(1, 0, 0), 1);

        List<SurfaceCluster> fromLeft = ClusterDetector.detect(List.of(tap), new Vec3(8, 0, 0));
        List<SurfaceCluster> fromRight = ClusterDetector.detect(List.of(tap), new Vec3(12, 0, 0));

        assertEquals(1, fromLeft.size());
        assertEquals(1, fromRight.size());
        // The normals should differ since the bin assignment depends on listener position
        assertNotEquals(fromLeft.get(0).normal(), fromRight.get(0).normal());
    }

    @Test
    void computeGain_zeroForCloseDistance() {
        SurfaceCluster close = new SurfaceCluster(
                Vec3.ZERO, new Vec3(1, 0, 0), 0.3f, 1.0f, 1.0f, 1,
                0.0f, 0.0f, 0.0f);
        assertEquals(0f, close.computeGain(1.0f));
    }

    @Test
    void computeGain_inverseSqrFalloff() {
        SurfaceCluster c = new SurfaceCluster(
                Vec3.ZERO, new Vec3(1, 0, 0), 2.0f, 1.0f, 1.0f, 1,
                0.0f, 0.0f, 0.0f);
        float gain = c.computeGain(1.0f);
        // expected: 1.0 * 1.0 * (1 / 4) * 1.0 * 0.1 = 0.025
        assertEquals(0.025f, gain, 0.001f);
    }
}
