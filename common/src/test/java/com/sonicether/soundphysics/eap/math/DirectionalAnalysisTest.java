package com.sonicether.soundphysics.eap.math;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectionalAnalysisTest {

    @Test void isotropicDirections_balanceNearOne() {
        Vec3[] dirs = goldenSpiral(200);
        float balance = DirectionalAnalysis.computeDirectionalBalance(dirs);
        assertTrue(balance > 0.85f, "Isotropic should be near 1.0, got " + balance);
    }

    @Test void collinearDirections_balanceNearZero() {
        Vec3[] dirs = new Vec3[50];
        for (int i = 0; i < 50; i++) dirs[i] = new Vec3(1, 0, 0);
        assertEquals(0.0f, DirectionalAnalysis.computeDirectionalBalance(dirs), 0.01f);
    }

    @Test void planarDirections_balanceNearZero() {
        Vec3[] dirs = new Vec3[100];
        for (int i = 0; i < 100; i++) {
            double a = 2.0 * Math.PI * i / 100;
            dirs[i] = new Vec3(Math.cos(a), Math.sin(a), 0);
        }
        assertTrue(DirectionalAnalysis.computeDirectionalBalance(dirs) < 0.05f);
    }

    @Test void fewerThan3_returnsZero() {
        assertEquals(0.0f, DirectionalAnalysis.computeDirectionalBalance(
                new Vec3[]{new Vec3(1,0,0), new Vec3(0,1,0)}));
    }

    @Test void empty_returnsZero() {
        assertEquals(0.0f, DirectionalAnalysis.computeDirectionalBalance(new Vec3[0]));
    }

    @Test void canopy_allUpperHit_returnsOne() {
        Vec3[] d = {new Vec3(0,1,0), new Vec3(0.3,0.9,0.1), new Vec3(0,-1,0)};
        assertEquals(1.0f, DirectionalAnalysis.computeCanopyCoverage(d, new boolean[]{true,true,false}), 0.001f);
    }

    @Test void canopy_noneHit_returnsZero() {
        Vec3[] d = {new Vec3(0,1,0), new Vec3(0.1,0.5,0.2), new Vec3(0,-1,0)};
        assertEquals(0.0f, DirectionalAnalysis.computeCanopyCoverage(d, new boolean[]{false,false,true}), 0.001f);
    }

    @Test void skyExposure_allPassThrough_returnsOne() {
        Vec3[] d = {new Vec3(0,1,0), new Vec3(0.2,0.8,0.1), new Vec3(0,-1,0)};
        assertEquals(1.0f, DirectionalAnalysis.computeSkyExposure(d, new boolean[]{false,false,true}), 0.001f);
    }

    @Test void skyExposure_allBlocked_returnsZero() {
        Vec3[] d = {new Vec3(0,1,0), new Vec3(0.1,0.6,0.2), new Vec3(0,-1,0)};
        assertEquals(0.0f, DirectionalAnalysis.computeSkyExposure(d, new boolean[]{true,true,false}), 0.001f);
    }

    @Test void eigenvalues_identityMatrix() {
        double[] eigs = DirectionalAnalysis.symmetricEigenvalues3x3(1,0,0,1,0,1);
        for (double e : eigs) assertEquals(1.0, e, 1e-10);
    }

    @Test void eigenvalues_diagonalMatrix() {
        double[] eigs = DirectionalAnalysis.symmetricEigenvalues3x3(3,0,0,2,0,1);
        java.util.Arrays.sort(eigs);
        assertEquals(1.0, eigs[0], 1e-10);
        assertEquals(2.0, eigs[1], 1e-10);
        assertEquals(3.0, eigs[2], 1e-10);
    }

<<<<<<< ours
    @Test void eigenvalues_nearDegenerate() {
        // Matrix with eigenvalues approximately 10, 0, 0
        // Near-degenerate case that exercises the Cardano fallback path
        double[] eigs = DirectionalAnalysis.symmetricEigenvalues3x3(10, 0, 0, 0, 0, 0);
        java.util.Arrays.sort(eigs);
        assertEquals(0.0, eigs[0], 0.01);
        assertEquals(0.0, eigs[1], 0.01);
        assertEquals(10.0, eigs[2], 0.01);
    }

=======
>>>>>>> theirs
    private static Vec3[] goldenSpiral(int n) {
        Vec3[] dirs = new Vec3[n];
        double gr = (1 + Math.sqrt(5)) / 2;
        for (int i = 0; i < n; i++) {
            double theta = Math.acos(1 - 2.0*(i+0.5)/n);
            double phi = 2*Math.PI*i/gr;
            dirs[i] = new Vec3(Math.sin(theta)*Math.cos(phi), Math.sin(theta)*Math.sin(phi), Math.cos(theta));
        }
        return dirs;
    }
}
