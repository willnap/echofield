package com.sonicether.soundphysics.eap.math;

import net.minecraft.world.phys.Vec3;

/** Directional analysis: covariance eigenvalues for isotropy, canopy/sky metrics. */
public final class DirectionalAnalysis {
    private DirectionalAnalysis() {}

    /**
     * Ratio of smallest to largest eigenvalue of direction covariance matrix.
     * 0 = collinear/planar, 1 = isotropic. Returns 0 for fewer than 3 directions.
     */
    public static float computeDirectionalBalance(Vec3[] directions) {
        if (directions.length < 3) return 0.0f;
        int n = directions.length;

        double mx = 0, my = 0, mz = 0;
        for (Vec3 d : directions) { mx += d.x; my += d.y; mz += d.z; }
        mx /= n; my /= n; mz /= n;

        double c00 = 0, c01 = 0, c02 = 0, c11 = 0, c12 = 0, c22 = 0;
        for (Vec3 d : directions) {
            double dx = d.x - mx, dy = d.y - my, dz = d.z - mz;
            c00 += dx*dx; c01 += dx*dy; c02 += dx*dz;
            c11 += dy*dy; c12 += dy*dz; c22 += dz*dz;
        }
        c00 /= n; c01 /= n; c02 /= n; c11 /= n; c12 /= n; c22 /= n;

        double[] eigs = symmetricEigenvalues3x3(c00, c01, c02, c11, c12, c22);
        double maxE = Math.max(eigs[0], Math.max(eigs[1], eigs[2]));
        double minE = Math.min(eigs[0], Math.min(eigs[1], eigs[2]));
        if (maxE < 1e-15) return 0.0f;
        return (float) Math.max(0.0, Math.min(1.0, minE / maxE));
    }

    /**
     * Eigenvalues of a 3x3 symmetric matrix via closed-form cubic (Cardano).
     * Parameters: upper triangle a00, a01, a02, a11, a12, a22.
     */
    static double[] symmetricEigenvalues3x3(double a00, double a01, double a02,
                                            double a11, double a12, double a22) {
        double p = a00 + a11 + a22; // trace
        double q = a00*a11 - a01*a01 + a00*a22 - a02*a02 + a11*a22 - a12*a12;
        double r = a00*(a11*a22 - a12*a12) - a01*(a01*a22 - a12*a02) + a02*(a01*a12 - a11*a02);

        double p3 = p / 3.0;
        double pp = (p*p - 3.0*q) / 9.0;
        double qq = (2.0*p*p*p - 9.0*p*q + 27.0*r) / 54.0;

        double[] result = new double[3];
        if (pp < 1e-30) {
            result[0] = result[1] = result[2] = p3;
        } else {
            double disc = qq*qq - pp*pp*pp;
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
            // For symmetric matrices all eigenvalues are real; disc > 0 only arises from
            // floating-point noise near degenerate roots. Use a relative epsilon guard so
            // near-degenerate cases fall through to the more accurate trigonometric branch.
            double eps = pp * pp * pp * 1e-10;
            if (disc <= eps) {
<<<<<<< ours
                double sqrtPP = Math.sqrt(pp);
                double theta = Math.acos(Math.max(-1.0, Math.min(1.0, -qq / (pp * sqrtPP))));
=======
            if (disc <= 0) {
=======
>>>>>>> theirs
                double sqrtPP = Math.sqrt(pp);
<<<<<<< ours
                double theta = Math.acos(Math.max(-1.0, Math.min(1.0, qq / (pp * sqrtPP))));
>>>>>>> theirs
=======
                double theta = Math.acos(Math.max(-1.0, Math.min(1.0, -qq / (pp * sqrtPP))));
>>>>>>> theirs
                result[0] = -2.0*sqrtPP*Math.cos(theta / 3.0) + p3;
                result[1] = -2.0*sqrtPP*Math.cos((theta + 2.0*Math.PI) / 3.0) + p3;
                result[2] = -2.0*sqrtPP*Math.cos((theta - 2.0*Math.PI) / 3.0) + p3;
            } else {
                double sqrtDisc = Math.sqrt(disc);
                double a = -Math.signum(qq) * Math.cbrt(Math.abs(qq) + sqrtDisc);
                double b = (Math.abs(a) > 1e-30) ? pp / a : 0.0;
                result[0] = a + b + p3;
<<<<<<< ours
<<<<<<< ours
                result[1] = -(a + b) / 2.0 + p3;
                result[2] = -(a + b) / 2.0 + p3;
=======
                result[1] = result[2] = p3;
>>>>>>> theirs
=======
                result[1] = -(a + b) / 2.0 + p3;
                result[2] = -(a + b) / 2.0 + p3;
>>>>>>> theirs
            }
        }
        for (int i = 0; i < 3; i++) if (result[i] < 0) result[i] = 0;
        return result;
    }

    /** Fraction of upward (y > 0) directions that returned a hit. */
    public static float computeCanopyCoverage(Vec3[] directions, boolean[] returned) {
        int total = 0, hit = 0;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i].y > 0) { total++; if (returned[i]) hit++; }
        }
        return total == 0 ? 0.0f : (float) hit / total;
    }

    /** Fraction of upward (y > 0) directions that did NOT return (open sky). */
    public static float computeSkyExposure(Vec3[] directions, boolean[] returned) {
        int total = 0, miss = 0;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i].y > 0) { total++; if (!returned[i]) miss++; }
        }
        return total == 0 ? 0.0f : (float) miss / total;
    }
}
