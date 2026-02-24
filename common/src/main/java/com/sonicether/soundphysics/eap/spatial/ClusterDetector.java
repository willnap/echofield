package com.sonicether.soundphysics.eap.spatial;

import com.sonicether.soundphysics.eap.ReflectionTap;
import com.sonicether.soundphysics.eap.math.SpectralFilter;
import com.sonicether.soundphysics.SoundPhysicsMod;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups ReflectionTaps into SurfaceClusters using directional binning.
 */
public final class ClusterDetector {

    public static final int MAX_CLUSTERS = 12;

    private static final Vec3[] BIN_DIRS = {
            new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
            new Vec3(0, 1, 0), new Vec3(0, -1, 0),
            new Vec3(0, 0, 1), new Vec3(0, 0, -1),
            new Vec3(1, 1, 0).normalize(), new Vec3(1, -1, 0).normalize(),
            new Vec3(-1, 1, 0).normalize(), new Vec3(-1, -1, 0).normalize(),
            new Vec3(0, 1, 1).normalize(), new Vec3(0, 1, -1).normalize(),
            new Vec3(0, -1, 1).normalize(), new Vec3(0, -1, -1).normalize(),
    };

    public static List<SurfaceCluster> detect(List<ReflectionTap> taps, Vec3 listenerPos) {
        if (taps.isEmpty()) return List.of();

        @SuppressWarnings("unchecked")
        List<ReflectionTap>[] bins = new List[BIN_DIRS.length];
        for (int i = 0; i < bins.length; i++) bins[i] = new ArrayList<>();

        for (ReflectionTap tap : taps) {
            int bestBin = 0;
            double bestDot = -2;
            Vec3 tapDir = tap.position().subtract(listenerPos).normalize();
            for (int i = 0; i < BIN_DIRS.length; i++) {
                double dot = tapDir.dot(BIN_DIRS[i]);
                if (dot > bestDot) {
                    bestDot = dot;
                    bestBin = i;
                }
            }
            bins[bestBin].add(tap);
        }

        List<SurfaceCluster> clusters = new ArrayList<>();
        for (int i = 0; i < bins.length; i++) {
            if (bins[i].isEmpty()) continue;
            clusters.add(buildCluster(bins[i], BIN_DIRS[i]));
        }

        clusters.sort((a, b) -> Float.compare(b.totalEnergy(), a.totalEnergy()));

        if (clusters.size() > MAX_CLUSTERS) {
            clusters = new ArrayList<>(clusters.subList(0, MAX_CLUSTERS));
        }

        return clusters;
    }

    /** Overload for cases without explicit listener position (uses origin). */
    public static List<SurfaceCluster> detect(List<ReflectionTap> taps) {
        return detect(taps, Vec3.ZERO);
    }

    private static SurfaceCluster buildCluster(List<ReflectionTap> taps, Vec3 binDir) {
        double cx = 0, cy = 0, cz = 0;
        double totalEnergy = 0;
        double totalDist = 0;
        float totalReflectivity = 0;
        float specLow = 0, specMid = 0, specHigh = 0;

        for (ReflectionTap tap : taps) {
            cx += tap.position().x;
            cy += tap.position().y;
            cz += tap.position().z;
            totalEnergy += tap.energy();
            totalDist += tap.distance();

            float[] abs = SpectralFilter.computeAbsorption(
                    tap.material(), SoundPhysicsMod.REFLECTIVITY_CONFIG);
            specLow += abs[0];
            specMid += abs[1];
            specHigh += abs[2];
            totalReflectivity += (1.0f - (abs[0] + abs[1] + abs[2]) / 3.0f);
        }

        int n = taps.size();
        return new SurfaceCluster(
                new Vec3(cx / n, cy / n, cz / n),
                binDir,
                (float) (totalDist / n),
                (float) totalEnergy,
                totalReflectivity / n,
                n,
                specLow / n, specMid / n, specHigh / n
        );
    }
}
