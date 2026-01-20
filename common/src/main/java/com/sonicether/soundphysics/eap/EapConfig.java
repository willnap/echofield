package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.eap.emitter.EmitterCategory;
import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

/**
 * Configuration for EchoField. Accessible via Cloth Config GUI.
 * Settings correspond to spec section 8.5.
 */
public class EapConfig {

    // --- Master ---
    public final ConfigEntry<Boolean> eapEnabled;
    public final ConfigEntry<Float> eapMasterVolume;

    // --- Layer 1: World Enrichment ---
    public final ConfigEntry<Boolean> layer1Enabled;
    public final ConfigEntry<Float> emitterDensity;
    public final ConfigEntry<Integer> emitterPoolSize;
    public final ConfigEntry<Boolean> faunaEnabled;
    public final ConfigEntry<Float> faunaVolume;

    // --- Per-emitter category toggles ---
    public final ConfigEntry<Boolean> emitterBird;
    public final ConfigEntry<Boolean> emitterInsect;
    public final ConfigEntry<Boolean> emitterFrog;
    public final ConfigEntry<Boolean> emitterBat;
    public final ConfigEntry<Boolean> emitterWindLeaf;
    public final ConfigEntry<Boolean> emitterWindGrass;
    public final ConfigEntry<Boolean> emitterWindWhistle;
    public final ConfigEntry<Boolean> emitterWaterFlow;
    public final ConfigEntry<Boolean> emitterWaterDrip;
    public final ConfigEntry<Boolean> emitterWaterRain;
    public final ConfigEntry<Boolean> emitterLava;
    public final ConfigEntry<Boolean> emitterCaveAmbient;
    public final ConfigEntry<Boolean> emitterCaveDrone;
    public final ConfigEntry<Boolean> emitterMechanical;

    // --- Layer 2: Spatial Audio Physics ---
    public final ConfigEntry<Boolean> hrtfEnabled;
    public final ConfigEntry<Boolean> perSourceDR;
    public final ConfigEntry<Boolean> diffractionEnabled;
    public final ConfigEntry<Boolean> materialTransmission;
    public final ConfigEntry<Boolean> airAbsorptionEnabled;

    // --- Layer 3: Hyperreality ---
    public final ConfigEntry<Boolean> hyperrealityEnabled;
    public final ConfigEntry<Float> augmentationIntensity;
    public final ConfigEntry<Integer> hyperrealityRange;

    // --- Installation ---
    public final ConfigEntry<Boolean> installationMode;

    // --- Early Reflections (kept from original) ---
    public final ConfigEntry<Float> earlyReflectionIntensity;

    // --- Debug ---
    public final ConfigEntry<Boolean> debugOverlay;
    public final ConfigEntry<Boolean> debugRays;
    public final ConfigEntry<Boolean> diagnosticLogging;
    public final ConfigEntry<Integer> rayCount;
    public final ConfigEntry<Integer> raysPerTick;

    // --- Legacy aliases (so existing config files don't break) ---
    public final ConfigEntry<Float> excitationVolume;

    // --- Per-excitation type toggles ---
    public final ConfigEntry<Boolean> excitationWind;
    public final ConfigEntry<Boolean> excitationFoliage;
    public final ConfigEntry<Boolean> excitationGrass;
    public final ConfigEntry<Boolean> excitationWater;
    public final ConfigEntry<Boolean> excitationLava;

    public EapConfig(ConfigBuilder builder) {
        // Master
        eapEnabled = builder.booleanEntry("eap_enabled", true)
                .comment("Master enable/disable for EchoField");
        eapMasterVolume = builder.floatEntry("eap_master_volume", 0.5f, 0f, 1f)
                .comment("Master volume for all EchoField audio");

        // Layer 1
        layer1Enabled = builder.booleanEntry("layer1_world_enrichment", true)
                .comment("Enable Layer 1: positioned environmental emitters");
        emitterDensity = builder.floatEntry("emitter_density", 0.75f, 0.25f, 1f)
                .comment("LOD aggressiveness for emitter placement (0.25=sparse, 1.0=dense)");
        emitterPoolSize = builder.integerEntry("emitter_pool_size", 32, 8, 64)
                .comment("Maximum number of concurrent emitter sources (8-64)");
        faunaEnabled = builder.booleanEntry("fauna_enabled", true)
                .comment("Enable fauna sounds (birds, insects, frogs)");
        faunaVolume = builder.floatEntry("fauna_volume", 0.8f, 0f, 1f)
                .comment("Volume multiplier for fauna sounds");

        // Per-emitter category
        emitterBird = builder.booleanEntry("emitter_bird", true)
                .comment("Enable bird emitters");
        emitterInsect = builder.booleanEntry("emitter_insect", true)
                .comment("Enable insect emitters");
        emitterFrog = builder.booleanEntry("emitter_frog", true)
                .comment("Enable frog emitters");
        emitterBat = builder.booleanEntry("emitter_bat", true)
                .comment("Enable bat emitters");
        emitterWindLeaf = builder.booleanEntry("emitter_wind_leaf", true)
                .comment("Enable wind-through-leaves emitters");
        emitterWindGrass = builder.booleanEntry("emitter_wind_grass", true)
                .comment("Enable wind-through-grass emitters");
        emitterWindWhistle = builder.booleanEntry("emitter_wind_whistle", true)
                .comment("Enable wind whistle emitters");
        emitterWaterFlow = builder.booleanEntry("emitter_water_flow", true)
                .comment("Enable flowing water emitters");
        emitterWaterDrip = builder.booleanEntry("emitter_water_drip", true)
                .comment("Enable dripping water emitters");
        emitterWaterRain = builder.booleanEntry("emitter_water_rain", true)
                .comment("Enable rain emitters");
        emitterLava = builder.booleanEntry("emitter_lava", true)
                .comment("Enable lava emitters");
        emitterCaveAmbient = builder.booleanEntry("emitter_cave_ambient", true)
                .comment("Enable cave ambient emitters");
        emitterCaveDrone = builder.booleanEntry("emitter_cave_drone", true)
                .comment("Enable cave drone emitters");
        emitterMechanical = builder.booleanEntry("emitter_mechanical", true)
                .comment("Enable mechanical emitters");

        // Layer 2
        hrtfEnabled = builder.booleanEntry("hrtf_enabled", true)
                .comment("Enable HRTF binaural rendering (requires OpenAL Soft)");
        perSourceDR = builder.booleanEntry("per_source_dr", false)
                .comment("Per-source Direct-to-Reverberant ratio based on distance (DISABLED: overwrites SPR reverb sends)");
        diffractionEnabled = builder.booleanEntry("diffraction", true)
                .comment("Wave diffraction around corners and through openings");
        materialTransmission = builder.booleanEntry("material_transmission", true)
                .comment("Frequency-dependent sound transmission through materials");
        airAbsorptionEnabled = builder.booleanEntry("air_absorption", true)
                .comment("ISO 9613-1 multi-band air absorption (replaces SPR single-coeff)");

        // Layer 3
        hyperrealityEnabled = builder.booleanEntry("hyperreality_enabled", true)
                .comment("Layer 3: hyperreality terrain sonification (augmented perception)");
        augmentationIntensity = builder.floatEntry("augmentation_intensity", 0.5f, 0f, 1f)
                .comment("Hyperreality intensity (0=off, 1=maximum)");
        hyperrealityRange = builder.integerEntry("hyperreality_range", 24, 8, 32)
                .comment("Scan and cull radius in blocks (8-32)");

        // Installation
        installationMode = builder.booleanEntry("installation_mode", false)
                .comment("Enable installation stage progression and blindfold mode");

        // Early Reflections
        earlyReflectionIntensity = builder.floatEntry("early_reflection_intensity", 0.3f, 0f, 1f)
                .comment("Intensity of early reflections applied to game sounds");

        // Debug
        debugOverlay = builder.booleanEntry("debug_overlay", false)
                .comment("Show EchoField debug overlay");
        debugRays = builder.booleanEntry("debug_rays", false)
                .comment("Render profiling rays in the world");
        diagnosticLogging = builder.booleanEntry("diagnostic_logging", false)
                .comment("Verbose console diagnostic output");
        rayCount = builder.integerEntry("ray_count", 128, 64, 128)
                .comment("Rays per profiling cycle (snapped to 64/96/128)");
        raysPerTick = builder.integerEntry("rays_per_tick", 8, 4, 32)
                .comment("Rays cast per tick during profiling");

        // Legacy
        excitationVolume = builder.floatEntry("excitation_volume", 0.5f, 0f, 1f)
                .comment("(Legacy) Volume for excitation sources");

        // Per-excitation type
        excitationWind = builder.booleanEntry("excitation_wind", true)
                .comment("Enable wind excitation source");
        excitationFoliage = builder.booleanEntry("excitation_foliage", true)
                .comment("Enable foliage excitation source");
        excitationGrass = builder.booleanEntry("excitation_grass", true)
                .comment("Enable grass excitation source");
        excitationWater = builder.booleanEntry("excitation_water", true)
                .comment("Enable water excitation source");
        excitationLava = builder.booleanEntry("excitation_lava", true)
                .comment("Enable lava excitation source");
    }

    public int getSnappedRayCount() {
        int raw = rayCount.get();
        if (raw <= 80) return 64;
        else if (raw <= 112) return 96;
        else return 128;
    }

    public boolean isEmitterCategoryEnabled(EmitterCategory category) {
        return switch (category) {
            case BIRD -> emitterBird.get();
            case INSECT -> emitterInsect.get();
            case FROG -> emitterFrog.get();
            case BAT -> emitterBat.get();
            case WIND_LEAF -> emitterWindLeaf.get();
            case WIND_GRASS -> emitterWindGrass.get();
            case WIND_WHISTLE -> emitterWindWhistle.get();
            case WATER_FLOW -> emitterWaterFlow.get();
            case WATER_DRIP -> emitterWaterDrip.get();
            case WATER_RAIN -> emitterWaterRain.get();
            case WATER_STILL -> emitterWaterFlow.get();
            case LAVA -> emitterLava.get();
            case CAVE_AMBIENT -> emitterCaveAmbient.get();
            case CAVE_DRONE -> emitterCaveDrone.get();
            case MECHANICAL -> emitterMechanical.get();
        };
    }

    public boolean isExcitationEnabled(ExcitationType type) {
        return switch (type) {
            case WIND -> excitationWind.get();
            case FOLIAGE -> excitationFoliage.get();
            case GRASS -> excitationGrass.get();
            case WATER -> excitationWater.get();
            case LAVA -> excitationLava.get();
        };
    }
}
