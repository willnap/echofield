package com.sonicether.soundphysics.eap.emitter;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Snapshot of all environment conditions relevant to EAP synthesis,
 * computed once per tick from Minecraft world state and the current
 * {@link EnvironmentProfile}. Drives ALL synthesis parameter modulation.
 */
public record EnvironmentConditions(
    float daylight,         // 0.0 (midnight) -> 1.0 (noon), smooth cosine from dayTime
    float dawnChorus,       // 0->1->0 peak during ticks 23000-1000 (5:00-7:00 AM)
    float duskFactor,       // 0->1->0 peak during ticks 11000-13000 (sunset)
    boolean isRaining,
    boolean isThundering,
    float temperature,      // biome base temperature (0.0=frozen, 0.5=temperate, 1.0+=tropical)
    boolean isForest,       // BiomeTags check
    boolean isSwamp,
    boolean isDesert,
    float windExposure,     // from EnvironmentProfile
    float enclosure,        // from EnvironmentProfile.enclosureFactor()
    float roomSize          // from EnvironmentProfile.averageReturnDistance()
) {

    /**
     * Compute all environment conditions from the current world state and acoustic profile.
     *
     * @param level      the Minecraft world (client or server level)
     * @param playerPos  the player's block position for biome lookup
     * @param profile    the current acoustic environment profile
     * @return a fully populated EnvironmentConditions snapshot
     */
    public static EnvironmentConditions compute(Level level, BlockPos playerPos, EnvironmentProfile profile) {
        long dayTime = level.getDayTime() % 24000L;

        float daylight = computeDaylight(dayTime);
        float dawnChorus = computeDawnChorus(dayTime);
        float duskFactor = computeDuskFactor(dayTime);

        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        // Biome info - defaults for graceful fallback
        float temperature = 0.5f;
        boolean isForest = false;
        boolean isSwamp = false;
        boolean isDesert = false;

        try {
            Holder<Biome> biomeHolder = level.getBiome(playerPos);
            Biome biome = biomeHolder.value();
            temperature = Math.max(0f, Math.min(2f, biome.getBaseTemperature()));
            isForest = biomeHolder.is(BiomeTags.IS_FOREST);
            isSwamp = biomeHolder.is(BiomeTags.HAS_SWAMP_HUT);   // proxy for swamp biome
            isDesert = biomeHolder.is(BiomeTags.HAS_DESERT_PYRAMID); // proxy for desert
        } catch (Exception ex) {
            Loggers.logDebug("EnvironmentConditions: biome lookup failed: {}", ex.getMessage());
        }

        float windExposure = profile.windExposure();
        float enclosure = profile.enclosureFactor();
        float roomSize = profile.averageReturnDistance();

        return new EnvironmentConditions(
            daylight, dawnChorus, duskFactor,
            isRaining, isThundering,
            temperature, isForest, isSwamp, isDesert,
            windExposure, enclosure, roomSize
        );
    }

    // ---- Pure-math helpers, package-private for unit testing ----

    /**
     * Daylight factor: peaks at 1.0 at noon (tick 6000), trough at 0.0 at midnight (tick 18000).
     * Minecraft dayTime 0 = 6:00 AM (sunrise), 6000 = noon, 12000 = sunset, 18000 = midnight.
     */
    static float computeDaylight(long dayTime) {
        float value = 0.5f + 0.5f * (float) Math.cos(2.0 * Math.PI * (dayTime - 6000) / 24000.0);
        return Math.max(0f, Math.min(1f, value));
    }

    /**
     * Dawn chorus factor: raised cosine pulse centered at tick 0 (6:00 AM), half-width 1000 ticks.
     * Peaks at 1.0, falls to 0.0 outside range [23000, 1000] (wrapping around midnight boundary).
     */
    static float computeDawnChorus(long dayTime) {
        float dist = dawnDistance(dayTime);
        return dist < 1000 ? 0.5f + 0.5f * (float) Math.cos(Math.PI * dist / 1000.0) : 0f;
    }

    /**
     * Dusk factor: raised cosine pulse centered at tick 12000 (sunset), half-width 1000 ticks.
     * Peaks at 1.0, falls to 0.0 outside range [11000, 13000].
     */
    static float computeDuskFactor(long dayTime) {
        float dist = Math.abs(dayTime - 12000);
        if (dist > 12000) dist = 24000 - dist;
        return dist < 1000 ? 0.5f + 0.5f * (float) Math.cos(Math.PI * dist / 1000.0) : 0f;
    }

    /**
     * Circular distance from the given dayTime to tick 0 (dawn center), wrapping at 24000.
     */
    private static float dawnDistance(long dayTime) {
        float dist = Math.abs(dayTime);
        if (dist > 12000) dist = 24000 - dist;
        return dist;
    }
}
