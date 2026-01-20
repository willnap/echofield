package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.config.blocksound.BlockSoundConfigBase;
import com.sonicether.soundphysics.eap.math.DirectionalAnalysis;
import com.sonicether.soundphysics.eap.math.RT60Estimator;
import com.sonicether.soundphysics.eap.math.SpectralFilter;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.List;

/** Immutable acoustic profile of the player's surroundings. All fields derived from geometry. */
public final class EnvironmentProfile {

    public static final EnvironmentProfile OPEN = new EnvironmentProfile(
            Collections.emptyList(), 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            new float[]{0.0f, 0.0f, 0.0f}, 0.0f, 1.0f, 0.0f, new Vec3(0, 1, 0));

    private final List<ReflectionTap> taps;
    private final float enclosureFactor, averageReturnDistance, scatteringDensity;
    private final float estimatedRT60, averageAbsorption;
    private final float[] spectralProfile;
    private final float directionalBalance, windExposure, canopyCoverage;
    private final Vec3 mostOpenSkyDirection;

    public EnvironmentProfile(List<ReflectionTap> taps, float enclosureFactor,
            float averageReturnDistance, float scatteringDensity, float estimatedRT60,
            float averageAbsorption, float[] spectralProfile, float directionalBalance,
            float windExposure, float canopyCoverage, Vec3 mostOpenSkyDirection) {
        this.taps = List.copyOf(taps);
        this.enclosureFactor = enclosureFactor;
        this.averageReturnDistance = averageReturnDistance;
        this.scatteringDensity = scatteringDensity;
        this.estimatedRT60 = estimatedRT60;
        this.averageAbsorption = averageAbsorption;
        this.spectralProfile = new float[]{spectralProfile[0], spectralProfile[1], spectralProfile[2]};
        this.directionalBalance = directionalBalance;
        this.windExposure = windExposure;
        this.canopyCoverage = canopyCoverage;
        this.mostOpenSkyDirection = mostOpenSkyDirection;
    }

    /**
     * Compute all aggregate stats from raw tap data and ray metadata.
     * The config is used to look up per-block reflectivity values for spectral analysis.
     */
    public static EnvironmentProfile fromTaps(List<ReflectionTap> taps, int totalRays,
            Vec3[] allDirections, boolean[] returned, BlockSoundConfigBase config) {
        float skyExposure = DirectionalAnalysis.computeSkyExposure(allDirections, returned);
        float canopy = DirectionalAnalysis.computeCanopyCoverage(allDirections, returned);

        Vec3 openSkyDir = computeMostOpenSkyDirection(allDirections, returned, taps);

        if (taps.isEmpty()) {
            float wind = skyExposure;
            return new EnvironmentProfile(taps, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    new float[]{0.0f, 0.0f, 0.0f}, 0.0f, wind, canopy, openSkyDir);
        }

        // Enclosure
        int retCount = 0;
        for (boolean r : returned) if (r) retCount++;
        float enclosure = (float) retCount / totalRays;

        // Average distance
        double distSum = 0;
        for (ReflectionTap t : taps) distSum += t.distance();
        float avgDist = (float) (distSum / taps.size());

        // Scattering density (coefficient of variation)
        float scatter;
        if (retCount < 3) {
            scatter = 0.0f;
        } else {
            double varSum = 0;
            for (ReflectionTap t : taps) { double d = t.distance() - avgDist; varSum += d * d; }
            double stdDev = Math.sqrt(varSum / taps.size());
            scatter = avgDist > 1e-6 ? (float) (stdDev / avgDist) : 0.0f;
        }

        // RT60
        double[] times = new double[taps.size()];
        double[] energies = new double[taps.size()];
        for (int i = 0; i < taps.size(); i++) {
            times[i] = taps.get(i).delay();
            energies[i] = taps.get(i).energy();
        }
        float rt60 = (float) RT60Estimator.estimate(times, energies);

        // Absorption and spectral profile
        float absSum = 0;
        float[] specSum = new float[3];
        for (ReflectionTap tap : taps) {
            float[] abs = SpectralFilter.computeAbsorption(tap.material(), config);
            absSum += (abs[0] + abs[1] + abs[2]) / 3.0f; // mean of all 3 bands per spec
            specSum[0] += abs[0]; specSum[1] += abs[1]; specSum[2] += abs[2];
        }
        float avgAbs = absSum / taps.size();
        float[] spectral = {specSum[0] / taps.size(), specSum[1] / taps.size(), specSum[2] / taps.size()};

        // Directional balance (from returned ray directions only)
        Vec3[] retDirs = new Vec3[retCount];
        int idx = 0;
        for (int i = 0; i < allDirections.length; i++)
            if (returned[i]) retDirs[idx++] = allDirections[i];
        float dirBal = DirectionalAnalysis.computeDirectionalBalance(retDirs);

        // Wind exposure = skyExposure * (0.5 + 0.5 * asymmetry)
        float wind = skyExposure * (0.5f + 0.5f * (1.0f - dirBal));

        return new EnvironmentProfile(taps, enclosure, avgDist, scatter, rt60,
                avgAbs, spectral, dirBal, wind, canopy, openSkyDir);
    }

    private static Vec3 computeMostOpenSkyDirection(Vec3[] allDirections,
            boolean[] returned, List<ReflectionTap> taps) {
        double ax = 0, ay = 0, az = 0;
        int escapedCount = 0;
        for (int i = 0; i < allDirections.length; i++) {
            if (!returned[i] && allDirections[i].y > 0) {
                ax += allDirections[i].x; ay += allDirections[i].y; az += allDirections[i].z;
                escapedCount++;
            }
        }
        if (escapedCount > 0) {
            Vec3 avg = new Vec3(ax / escapedCount, ay / escapedCount, az / escapedCount);
            double len = avg.length();
            return len > 1e-6 ? avg.scale(1.0 / len) : new Vec3(0, 1, 0);
        }
        Vec3 bestDir = new Vec3(0, 1, 0);
        double longestDist = -1;
        for (ReflectionTap tap : taps) {
            if (tap.direction().y() > 0 && tap.distance() > longestDist) {
                longestDist = tap.distance();
                bestDir = tap.direction().normalize();
            }
        }
        return bestDir;
    }

    /** Linearly interpolate all stats. Taps and direction come from other when t >= 0.5. */
    public EnvironmentProfile lerp(EnvironmentProfile other, float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t)), inv = 1.0f - c;
        return new EnvironmentProfile(
                c >= 0.5f ? other.taps : this.taps,
                inv * enclosureFactor + c * other.enclosureFactor,
                inv * averageReturnDistance + c * other.averageReturnDistance,
                inv * scatteringDensity + c * other.scatteringDensity,
                inv * estimatedRT60 + c * other.estimatedRT60,
                inv * averageAbsorption + c * other.averageAbsorption,
                new float[]{
                    inv * spectralProfile[0] + c * other.spectralProfile[0],
                    inv * spectralProfile[1] + c * other.spectralProfile[1],
                    inv * spectralProfile[2] + c * other.spectralProfile[2]
                },
                inv * directionalBalance + c * other.directionalBalance,
                inv * windExposure + c * other.windExposure,
                inv * canopyCoverage + c * other.canopyCoverage,
                c >= 0.5f ? other.mostOpenSkyDirection : this.mostOpenSkyDirection);
    }

    public List<ReflectionTap> taps() { return taps; }
    public float enclosureFactor() { return enclosureFactor; }
    public float averageReturnDistance() { return averageReturnDistance; }
    public float scatteringDensity() { return scatteringDensity; }
    public float estimatedRT60() { return estimatedRT60; }
    public float averageAbsorption() { return averageAbsorption; }
    public float[] spectralProfile() { return new float[]{spectralProfile[0], spectralProfile[1], spectralProfile[2]}; }
    public float directionalBalance() { return directionalBalance; }
    public float windExposure() { return windExposure; }
    public float canopyCoverage() { return canopyCoverage; }
    public Vec3 mostOpenSkyDirection() { return mostOpenSkyDirection; }

    /**
     * Estimates room volume from enclosure factor and average return distance.
     * @return estimated volume in m³ (blocks³)
     */
    public float estimatedVolume() {
        return RoomGeometry.estimateVolume(enclosureFactor, averageReturnDistance);
    }

    /**
     * Computes the Sabine critical distance for this space.
     * @return critical distance in blocks
     */
    public float criticalDistance() {
        return RoomGeometry.criticalDistance(estimatedVolume(), estimatedRT60);
    }
}
