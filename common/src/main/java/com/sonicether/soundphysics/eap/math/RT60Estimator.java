package com.sonicether.soundphysics.eap.math;

/**
 * Estimates RT60 from multi-bounce energy decay data via least-squares
 * exponential fit. Bins energy-vs-time into 10ms bins, fits
 * ln(E) = ln(E0) - alpha*t, returns RT60 = ln(1000) / alpha.
 */
public final class RT60Estimator {
    private static final double BIN_WIDTH = 0.01;
    private static final double LN_1000 = Math.log(1000.0);
    private static final int MAX_BINS = 500;
    private static final int MIN_BINS = 3;

    private RT60Estimator() {}

    public static double estimate(double[] times, double[] energies) {
        if (times.length != energies.length)
            throw new IllegalArgumentException("times and energies must have same length");
        if (times.length == 0) return 0.0;

        // Bin the data
        double[] binEnergySum = new double[MAX_BINS];
        int[] binCount = new int[MAX_BINS];
        for (int i = 0; i < times.length; i++) {
            if (energies[i] <= 0.0) continue;
            int bin = (int) (times[i] / BIN_WIDTH);
            if (bin < 0 || bin >= MAX_BINS) continue;
            binEnergySum[bin] += energies[i];
            binCount[bin]++;
        }

        // Compute mean energy per bin, collect non-empty bins
        double[] t = new double[MAX_BINS];
        double[] lnE = new double[MAX_BINS];
        int n = 0;
        for (int bin = 0; bin < MAX_BINS; bin++) {
            if (binCount[bin] > 0) {
                double mean = binEnergySum[bin] / binCount[bin];
                if (mean > 0.0) {
                    t[n] = (bin + 0.5) * BIN_WIDTH;
                    lnE[n] = Math.log(mean);
                    n++;
                }
            }
        }

        if (n < MIN_BINS) return 0.0;

        // Linear regression: lnE = b - alpha * t
        double sumT = 0, sumLnE = 0, sumTLnE = 0, sumT2 = 0;
        for (int i = 0; i < n; i++) {
            sumT += t[i]; sumLnE += lnE[i];
            sumTLnE += t[i] * lnE[i]; sumT2 += t[i] * t[i];
        }
        double denom = n * sumT2 - sumT * sumT;
        if (Math.abs(denom) < 1e-15) return 0.0;

        double slope = (n * sumTLnE - sumT * sumLnE) / denom;
        double alpha = -slope;
        if (alpha <= 0.0) return 0.0;

        return LN_1000 / alpha;
    }
}
