package com.sonicether.soundphysics.eap;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentProfileTest {

    @Test void openConstant_hasExpectedValues() {
        EnvironmentProfile o = EnvironmentProfile.OPEN;
        assertEquals(0.0f, o.enclosureFactor());
        assertEquals(0.0f, o.averageReturnDistance());
        assertEquals(0.0f, o.scatteringDensity());
        assertEquals(0.0f, o.estimatedRT60());
        assertEquals(0.0f, o.averageAbsorption());
        assertArrayEquals(new float[]{0, 0, 0}, o.spectralProfile(), 0.001f);
        assertEquals(0.0f, o.directionalBalance());
        assertEquals(1.0f, o.windExposure());
        assertEquals(0.0f, o.canopyCoverage());
        assertTrue(o.taps().isEmpty());
    }

    @Test void lerp_atZero_returnsThis() {
        EnvironmentProfile a = mkProfile(0.8f, 10f, 0.5f, 2f, 0.3f, new float[]{.1f, .2f, .3f}, .7f, .4f, .6f);
        EnvironmentProfile b = mkProfile(0.2f, 3f, 0.1f, .5f, 0.1f, new float[]{.5f, .6f, .7f}, .3f, .8f, .2f);
        EnvironmentProfile r = a.lerp(b, 0.0f);
        assertEquals(a.enclosureFactor(), r.enclosureFactor(), 0.001f);
        assertEquals(a.estimatedRT60(), r.estimatedRT60(), 0.001f);
        assertEquals(a.windExposure(), r.windExposure(), 0.001f);
    }

    @Test void lerp_atOne_returnsOther() {
        EnvironmentProfile a = mkProfile(0.8f, 10f, 0.5f, 2f, 0.3f, new float[]{.1f, .2f, .3f}, .7f, .4f, .6f);
        EnvironmentProfile b = mkProfile(0.2f, 3f, 0.1f, .5f, 0.1f, new float[]{.5f, .6f, .7f}, .3f, .8f, .2f);
        EnvironmentProfile r = a.lerp(b, 1.0f);
        assertEquals(b.enclosureFactor(), r.enclosureFactor(), 0.001f);
        assertEquals(b.estimatedRT60(), r.estimatedRT60(), 0.001f);
        assertEquals(b.windExposure(), r.windExposure(), 0.001f);
    }

    @Test void lerp_atHalf_blends() {
        EnvironmentProfile a = mkProfile(0.8f, 10f, 0.5f, 2f, 0.3f, new float[]{.1f, .2f, .3f}, .7f, .4f, .6f);
        EnvironmentProfile b = mkProfile(0.2f, 3f, 0.1f, .5f, 0.1f, new float[]{.5f, .6f, .7f}, .3f, .8f, .2f);
        EnvironmentProfile r = a.lerp(b, 0.5f);
        assertEquals(0.5f, r.enclosureFactor(), 0.001f);
        assertEquals(6.5f, r.averageReturnDistance(), 0.001f);
        assertEquals(1.25f, r.estimatedRT60(), 0.001f);
        assertEquals(0.6f, r.windExposure(), 0.001f);
        assertEquals(0.4f, r.canopyCoverage(), 0.001f);
        float[] sp = r.spectralProfile();
        assertEquals(0.3f, sp[0], 0.001f);
        assertEquals(0.4f, sp[1], 0.001f);
        assertEquals(0.5f, sp[2], 0.001f);
    }

    @Test void lerp_tapsSwapAtHalf() {
        EnvironmentProfile a = mkProfile(.5f, 5f, .3f, 1f, .2f, new float[]{.1f, .1f, .1f}, .5f, .5f, .5f);
        EnvironmentProfile b = mkProfile(.5f, 5f, .3f, 1f, .2f, new float[]{.1f, .1f, .1f}, .5f, .5f, .5f);
        assertSame(a.taps(), a.lerp(b, 0.49f).taps());
        assertSame(b.taps(), a.lerp(b, 0.5f).taps());
    }

    @Test void windExposure_formula() {
        float expected = 0.8f * (0.5f + 0.5f * (1.0f - 0.6f));
        assertEquals(0.56f, expected, 0.001f);
    }

    @Test void spectralProfile_returnsCopy() {
        EnvironmentProfile p = mkProfile(.5f, 5f, .3f, 1f, .2f, new float[]{.1f, .2f, .3f}, .5f, .5f, .5f);
        float[] s1 = p.spectralProfile();
        s1[0] = 999f;
        assertEquals(0.1f, p.spectralProfile()[0], 0.001f);
    }

    private static EnvironmentProfile mkProfile(float enc, float dist, float scat,
            float rt60, float abs, float[] spec, float bal, float wind, float canopy) {
        return new EnvironmentProfile(Collections.emptyList(), enc, dist, scat, rt60, abs, spec, bal, wind, canopy,
                new Vec3(0, 1, 0));
    }
}
