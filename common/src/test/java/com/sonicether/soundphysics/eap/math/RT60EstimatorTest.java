package com.sonicether.soundphysics.eap.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RT60EstimatorTest {
    private static final double LN_1000 = Math.log(1000.0);

    @Test void perfectExponentialDecay_returnsExactRT60() {
        double targetRT60 = 1.5;
        double alpha = LN_1000 / targetRT60;
        int count = 50;
        double[] times = new double[count], energies = new double[count];
        for (int i = 0; i < count; i++) {
            times[i] = 0.01 * (i + 1);
            energies[i] = Math.exp(-alpha * times[i]);
        }
        assertEquals(targetRT60, RT60Estimator.estimate(times, energies), 0.05);
    }

    @Test void noisyExponentialDecay_returnsApproximateRT60() {
        double targetRT60 = 2.0, alpha = LN_1000 / targetRT60;
        int count = 100;
        double[] times = new double[count], energies = new double[count];
        for (int i = 0; i < count; i++) {
            times[i] = 0.005 * (i + 1);
            energies[i] = Math.exp(-alpha * times[i]) * (1.0 + 0.1 * Math.sin(i * 7.3));
        }
        assertEquals(targetRT60, RT60Estimator.estimate(times, energies), 0.3);
    }

    @Test void fewerThan3Bins_returnsZero() {
        assertEquals(0.0, RT60Estimator.estimate(new double[]{0.005, 0.015}, new double[]{1.0, 0.8}));
    }

    @Test void emptyInput_returnsZero() {
        assertEquals(0.0, RT60Estimator.estimate(new double[0], new double[0]));
    }

    @Test void singleBounceData_returnsZero() {
        assertEquals(0.0, RT60Estimator.estimate(
                new double[]{0.001, 0.003, 0.007}, new double[]{0.9, 0.85, 0.8}));
    }

    @Test void mismatchedArrayLengths_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> RT60Estimator.estimate(new double[]{1.0}, new double[]{1.0, 2.0}));
    }

    @Test void flatEnergy_returnsZero() {
        double[] times = new double[20], energies = new double[20];
        for (int i = 0; i < 20; i++) { times[i] = 0.015 * (i + 1); energies[i] = 1.0; }
        assertEquals(0.0, RT60Estimator.estimate(times, energies));
    }
}
