package com.sonicether.soundphysics.eap;

import com.sonicether.soundphysics.Loggers;
<<<<<<< ours
import com.sonicether.soundphysics.SoundPhysics;
=======
>>>>>>> theirs
import com.sonicether.soundphysics.SoundPhysicsMod;
import com.sonicether.soundphysics.eap.debug.AudioDiagnostics;
import com.sonicether.soundphysics.eap.debug.AudioEnergyMeter;
import com.sonicether.soundphysics.eap.debug.EapDebugRenderer;
<<<<<<< ours
import com.sonicether.soundphysics.eap.emitter.EmitterManager;
import com.sonicether.soundphysics.eap.emitter.EnvironmentConditions;
import com.sonicether.soundphysics.eap.install.InstallationManager;
import com.sonicether.soundphysics.eap.hyperreality.HyperrealitySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.List;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
>>>>>>> theirs

/**
 * Top-level coordinator for the Environmental Audio Processing (EAP) system.
 * Manages the lifecycle of the EnvironmentProfiler, ExcitationSourceManager,
 * and EarlyReflectionProcessor. Called from Fabric mod initialization and
 * mixin hooks.
<<<<<<< ours
 *
 * <p><strong>Thread model:</strong> In Minecraft Fabric 1.21.x, both the
 * {@code END_CLIENT_TICK} event and {@code Channel.play()} (where SourceMixin
 * injects) execute on the main render thread, which is also the thread that
 * initializes the OpenAL context. All OpenAL calls in this system therefore
 * run on the correct thread.
 */
public final class EapSystem {

    public enum CompareMode {
        VANILLA("A — Vanilla"),
        PHYSICS("B — Physics"),
        FULL("C — Full EchoField");
        public final String label;
        CompareMode(String label) { this.label = label; }
        public CompareMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static volatile EapSystem instance;
    private static volatile boolean soundEngineReady = false;
=======
 */
public final class EapSystem {

<<<<<<< ours
    private static EapSystem instance;
>>>>>>> theirs
=======
    private static volatile EapSystem instance;
>>>>>>> theirs

    private final EnvironmentProfiler profiler;
    private final ExcitationSourceManager excitation;
    private final EarlyReflectionProcessor reflections;
    private final EapDebugRenderer debugRenderer;
<<<<<<< ours
    private final HrtfManager hrtfManager;
    private final PerSourceDRProcessor drProcessor;
    private final EmitterManager emitterManager;
    private final AirAbsorptionProcessor airAbsorption;
    private final HyperrealitySystem hyperreality;
    private final InstallationManager installation;

    private final AudioEnergyMeter energyMeter;

    private final AudioEnergyMeter energyMeter;

    private boolean initialized = false;
<<<<<<< ours
    private CompareMode compareMode = CompareMode.FULL;
    private int diagnosticTickCounter = 0;
    private int debugReverbCounter = 0;
    private Vec3 lastPlayerPos;
    private Vec3 prevPlayerPos;

    // Last-computed reverb parameters for diagnostic access (test mode)
    private volatile String lastReverbParamsJson = "{}";
=======

    private boolean initialized = false;
    private int diagnosticTickCounter = 0;
>>>>>>> theirs
=======
    private boolean eapToggleActive = true;
    private int diagnosticTickCounter = 0;
    private Vec3 lastPlayerPos;
>>>>>>> theirs

    private EapSystem() {
        EapConfig config = SoundPhysicsMod.EAP_CONFIG;

        int snappedRays = config.getSnappedRayCount();
        int rpt = config.raysPerTick.get();

        this.profiler = new EnvironmentProfiler(
                SoundPhysicsMod.REFLECTIVITY_CONFIG,
                snappedRays, rpt);

        this.excitation = new ExcitationSourceManager();
        this.reflections = new EarlyReflectionProcessor();
        this.debugRenderer = new EapDebugRenderer();
<<<<<<< ours
<<<<<<< ours
        this.energyMeter = new AudioEnergyMeter();
        this.hrtfManager = new HrtfManager();
        this.drProcessor = new PerSourceDRProcessor();
        this.drProcessor.init();
        this.emitterManager = new EmitterManager();
        this.airAbsorption = new AirAbsorptionProcessor();
        this.hyperreality = new HyperrealitySystem();
        this.installation = new InstallationManager();
=======
>>>>>>> theirs
=======
        this.energyMeter = new AudioEnergyMeter();
>>>>>>> theirs

        // Apply initial config values
        excitation.setMasterGain(config.eapMasterVolume.get());
        excitation.setExcitationVolume(config.excitationVolume.get());
        reflections.setMasterGain(config.eapMasterVolume.get());
        reflections.setReflectionIntensity(config.earlyReflectionIntensity.get());

        initialized = true;
        Loggers.log("EapSystem: initialized (rays={}, raysPerTick={})", snappedRays, rpt);
<<<<<<< ours

        // Verify HRTF status — FATAL if not active
        long device = org.lwjgl.openal.ALC10.alcGetContextsDevice(
                org.lwjgl.openal.ALC10.alcGetCurrentContext());
        hrtfManager.verifyHrtf(device);
        if (!hrtfManager.isHrtfActive()) {
            Loggers.log("EapSystem: HRTF not active — all audio processing will be skipped");
        }

        // Configure Doppler effect — pitch shift on moving sources
        org.lwjgl.openal.AL10.alDopplerFactor(1.0f);
        org.lwjgl.openal.AL11.alSpeedOfSound(343.0f);
        Loggers.log("EapSystem: Doppler configured (factor=1.0, speed=343 blocks/s)");
=======
>>>>>>> theirs
    }

    // ── Singleton access ────────────────────────────────────────────

    /**
     * Returns the singleton instance, creating it if necessary.
     * Must be called after SoundPhysicsMod.initClient() has run.
     */
<<<<<<< ours
    public static synchronized EapSystem getInstance() {
=======
    public static EapSystem getInstance() {
>>>>>>> theirs
        if (instance == null) {
            instance = new EapSystem();
        }
        return instance;
    }

    /**
     * Returns the singleton instance without creating it.
     * Returns null if the system has not been initialized.
     */
    public static EapSystem getInstanceOrNull() {
        return instance;
    }

    // ── Client tick ─────────────────────────────────────────────────

    /**
     * Called once per client tick from the Fabric tick event.
     * Drives the profiler, updates excitation sources, and ticks reflections.
     */
    public void onClientTick(Minecraft minecraft) {
        if (!initialized) {
            return;
        }

        EapConfig config = SoundPhysicsMod.EAP_CONFIG;
<<<<<<< ours
<<<<<<< ours
<<<<<<< ours
        if (config == null || !config.eapEnabled.get() || !SoundPhysicsMod.CONFIG.enabled.get()
                || compareMode == CompareMode.VANILLA || minecraft.isPaused()) {
=======
        if (config == null || !config.eapEnabled.get() || !eapToggleActive || minecraft.isPaused()) {
>>>>>>> theirs
            excitation.setEnabled(false);
            excitation.silenceAll();
            reflections.muteAll();
            emitterManager.silenceAll();
            hyperreality.silenceAll();
            return;
        }

        // HRTF gate — refuse to process without binaural cues
        hrtfManager.tick();
        if (!hrtfManager.isHrtfActive()) {
            excitation.setEnabled(false);
            excitation.silenceAll();
=======
        if (config == null || !config.eapEnabled.get()) {
=======
        if (config == null || !config.eapEnabled.get() || !eapToggleActive) {
>>>>>>> theirs
            excitation.setEnabled(false);
>>>>>>> theirs
            reflections.muteAll();
            return;
        }

<<<<<<< ours
<<<<<<< ours
=======
        excitation.setEnabled(true);
>>>>>>> theirs
=======
>>>>>>> theirs
        reflections.unmuteAll();

        // Sync config values each tick (cheap reads)
        excitation.setMasterGain(config.eapMasterVolume.get());
        excitation.setExcitationVolume(config.excitationVolume.get());
<<<<<<< ours
        for (ExcitationType t : ExcitationType.values()) {
            excitation.setTypeEnabled(t, config.isExcitationEnabled(t));
        }
        reflections.setMasterGain(config.eapMasterVolume.get());
        reflections.setReflectionIntensity(config.earlyReflectionIntensity.get());
        drProcessor.setEnabled(config.perSourceDR.get());
        airAbsorption.setEnabled(config.airAbsorptionEnabled.get());

        // Tick installation manager
        installation.setActive(config.installationMode.get());
        installation.tick(minecraft);
=======
        reflections.setMasterGain(config.eapMasterVolume.get());
        reflections.setReflectionIntensity(config.earlyReflectionIntensity.get());
>>>>>>> theirs

        // Update ray config if changed
        int snappedRays = config.getSnappedRayCount();
        int rpt = config.raysPerTick.get();
        if (snappedRays != profiler.getTotalRays() || rpt != profiler.getRaysPerTick()) {
            profiler.updateConfig(snappedRays, rpt);
        }

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 currentPos = minecraft.player.position();
        long currentTick = minecraft.level.getGameTime();

<<<<<<< ours
<<<<<<< ours
        // B2: Teleportation detection — invalidate profiler and emitters on large position jumps
        if (lastPlayerPos != null && lastPlayerPos.distanceTo(currentPos) > 32.0) {
            Loggers.log("EapSystem: teleportation detected ({}m), invalidating profiler + emitters + hyperreality",
                    (int) lastPlayerPos.distanceTo(currentPos));
            profiler.invalidate(currentTick);
            excitation.silenceAll();
            emitterManager.silenceAll();
            emitterManager.forceRescan();
            hyperreality.silenceAll();
            hyperreality.forceRescan();
        }
        lastPlayerPos = currentPos;

        // Update listener velocity for Doppler effect
        if (prevPlayerPos != null) {
            float vx = (float) (currentPos.x - prevPlayerPos.x) * 20.0f;
            float vy = (float) (currentPos.y - prevPlayerPos.y) * 20.0f;
            float vz = (float) (currentPos.z - prevPlayerPos.z) * 20.0f;
            org.lwjgl.openal.AL10.alListener3f(
                    org.lwjgl.openal.AL10.AL_VELOCITY, vx, vy, vz);
        }
        prevPlayerPos = currentPos;

=======
        // B2: Teleportation detection — invalidate profiler on large position jumps
        if (lastPlayerPos != null && lastPlayerPos.distanceTo(currentPos) > 32.0) {
            Loggers.log("EapSystem: teleportation detected ({}m), invalidating profiler",
                    (int) lastPlayerPos.distanceTo(currentPos));
            profiler.invalidate(currentTick);
            excitation.silenceAll();
        }
        lastPlayerPos = currentPos;

>>>>>>> theirs
        // B3: Underwater excitation muting
        boolean underwater = minecraft.player.isUnderWater();
        excitation.setEnabled(!underwater);

<<<<<<< ours
=======
>>>>>>> theirs
=======
>>>>>>> theirs
        // Update player position for excitation source positioning
        excitation.setPlayerPosition(currentPos.x, currentPos.y, currentPos.z);

        // Tick the profiler (casts raysPerTick rays)
        profiler.tick(minecraft);

        // Get the current profile (with crossfade)
        EnvironmentProfile profile = profiler.getCurrentProfile(currentTick);

<<<<<<< ours
        // Update excitation sources from profile.
        // When Layer 1 (emitter system) is active, silence the 4 categories that
        // overlap with EmitterManager to prevent double audio and wasted sources.
        excitation.update(profile);
        if (config.layer1Enabled.get()) {
            excitation.silenceOverlappingWithEmitters();
        }

        // Compute environment conditions for synthesis parameter modulation and debug display
        EnvironmentConditions conditions = EnvironmentConditions.compute(
                minecraft.level, minecraft.player.blockPosition(), profile);

        // Layer 1: positioned environmental emitters
        if (config.layer1Enabled.get()) {
            emitterManager.setMasterGain(config.eapMasterVolume.get());
            emitterManager.setDensity(config.emitterDensity.get());
            emitterManager.setFaunaEnabled(config.faunaEnabled.get());
            emitterManager.setFaunaVolume(config.faunaVolume.get());
            for (com.sonicether.soundphysics.eap.emitter.EmitterCategory cat : com.sonicether.soundphysics.eap.emitter.EmitterCategory.values()) {
                emitterManager.setCategoryEnabled(cat, config.isEmitterCategoryEnabled(cat));
            }
            emitterManager.tick(minecraft, profile, conditions);
        }

        // Layer 3: hyperreality — terrain-aware spatial sonification
        // TEMPORARILY DISABLED — sound design needs rework
        boolean hyperrealityAllowed = false; // installation.isHyperrealityAllowed();
        if (false && config.hyperrealityEnabled.get() && compareMode == CompareMode.FULL
                && hyperrealityAllowed && !underwater) {
            hyperreality.setIntensity(config.augmentationIntensity.get());
            hyperreality.setRange(config.hyperrealityRange.get());
            float sceneEnergy = emitterManager.getSceneEnergy() + energyMeter.getLatestEnergy();
            hyperreality.tick(profile, config.eapMasterVolume.get(), sceneEnergy, currentPos);
        } else {
            hyperreality.silenceAll();
        }

        // Override SPR's static reverb params with geometry-driven values
        overrideReverbParams(profile);
        if (debugReverbCounter++ % 200 == 0) {
            Loggers.log("REVERB-DEBUG enc={} rt60={} vol={} scatter={} avgAbs={} taps={}",
                    String.format("%.3f", profile.enclosureFactor()),
                    String.format("%.3f", profile.estimatedRT60()),
                    String.format("%.1f", profile.estimatedVolume()),
                    String.format("%.3f", profile.scatteringDensity()),
                    String.format("%.4f", profile.averageAbsorption()),
                    profile.taps().size());
        }

        // Route excitation sources through SPR's reverb system
        applyReverbToExcitationSources(profile);
=======
        // Update excitation sources from profile
        excitation.update(profile);
>>>>>>> theirs

        // Tick early reflections (fires pending delayed plays, recycles slots)
        reflections.tick(currentTick);

<<<<<<< ours
<<<<<<< ours
        // Update energy meter
        energyMeter.computeTotalEnergy(excitation, reflections);

        // Update debug renderer references
        debugRenderer.setExcitationManager(excitation);
        debugRenderer.setCurrentProfile(profile);
        debugRenderer.setEmitterManager(emitterManager);
        debugRenderer.setHyperrealitySystem(hyperreality);
        debugRenderer.setConditions(conditions);

        // D/R processor cleanup — MUST run unconditionally to prevent filter leak
        diagnosticTickCounter++;
        if (diagnosticTickCounter >= 100) {
            diagnosticTickCounter = 0;
            drProcessor.cleanupStale(currentTick);
            // Diagnostic logging (optional)
            if (config.diagnosticLogging.get()) {
                AudioDiagnostics.logAllEapSources(excitation, reflections);
                AudioDiagnostics.logEnvironmentProfile(profile);
                Loggers.log("EapSystem reverb: enc={} rt60={} vol={} abs={} | {}",
                        String.format("%.2f", profile.enclosureFactor()),
                        String.format("%.3f", profile.estimatedRT60()),
                        String.format("%.0f", profile.estimatedVolume()),
                        String.format("%.4f", profile.averageAbsorption()),
                        lastReverbParamsJson);
            }
        }
    }

    // ── Reverb routing ─────────────────────────────────────────────

    /**
     * Routes each excitation source through SPR's reverb system using
     * aggregate profile stats. This gives excitation sources proper
     * environmental reverb — wind in a cave reverberates.
     */
    private void applyReverbToExcitationSources(EnvironmentProfile profile) {
        int maxSends = SoundPhysics.getMaxAuxSends();
        if (maxSends < 1) return;

        float enclosure = profile.enclosureFactor();
        float rt60 = profile.estimatedRT60();
        float[] spectral = profile.spectralProfile();

        // Compute reverb send gains from profile — more enclosed = more reverb
        float reverbAmount = enclosure * Math.min(rt60 / 2.0f, 1.0f);

        float[] sendGains = {
                reverbAmount * 0.5f,
                reverbAmount * 0.6f,
                reverbAmount * 0.8f,
                reverbAmount * 0.7f
        };

        // HF cutoff based on spectral absorption — more absorption = darker reverb
        float hfAbsorption = spectral[2];
        float cutoff = 1.0f - hfAbsorption * 0.6f;
        cutoff = Math.max(0.1f, Math.min(1.0f, cutoff));

        // Route through aux sends — send j → slot j (ascending order).
        // Matches SoundPhysics.setEnvironment() convention.
        ExcitationSource[] sources = excitation.getSources();
        for (ExcitationSource source : sources) {
            for (int j = 0; j < 4 && j < maxSends; j++) {
                int sendFilter = source.sendFilterIds[j];
                org.lwjgl.openal.EXTEfx.alFilterf(sendFilter,
                        org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN, sendGains[j]);
                org.lwjgl.openal.EXTEfx.alFilterf(sendFilter,
                        org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF, cutoff);
                org.lwjgl.openal.AL11.alSource3i(source.sourceId,
                        org.lwjgl.openal.EXTEfx.AL_AUXILIARY_SEND_FILTER,
                        SoundPhysics.getAuxFXSlot(j), j, sendFilter);
            }
        }
    }

    // ── Geometry-driven reverb override ──────────────────────────────

    /**
     * Overrides SPR's static reverb parameters with values derived from the
     * EnvironmentProfile. This makes reverb respond to room geometry:
     * - Decay time scales with estimated RT60 (Sabine/Eyring)
     * - HF ratio reflects surface material (stone = bright, wood = dark)
     * - Density and diffusion scale with scattering
     */
    private void overrideReverbParams(EnvironmentProfile profile) {
        float[] spectral = profile.spectralProfile();
        float enclosure = profile.enclosureFactor();
        float scatter = profile.scatteringDensity();
        float avgReturnDist = profile.averageReturnDistance();
        float avgAbsorption = profile.averageAbsorption();
        List<ReflectionTap> taps = profile.taps();

        // --- HF and LF decay ratios from Eyring absorption ---
        // Uses ratio of Eyring absorption terms: hfRatio = ln(1-α_mid) / ln(1-α_high)
        // This correctly models multi-bounce frequency-dependent decay.
        // Stone (mid=0.03, high=0.05): hfRatio ≈ 0.59 (HF decays ~1.7x faster)
        // Previous linear formula (0.95 - α*0.45) gave 0.928 — far too close to 1.0.
        float lowAlpha = Math.max(0.01f, spectral[0]);
        float midAlpha = Math.max(0.01f, spectral[1]);
        float highAlpha = Math.max(0.01f, spectral[2]);
        float hfRatio = (float) (Math.log(1.0 - midAlpha) / Math.log(1.0 - highAlpha));
        hfRatio = Math.max(0.1f, Math.min(1.0f, hfRatio));

        // --- RT60 from Eyring formula (replaces unreliable tap-based estimate) ---
        // Eyring: RT60 = 0.161 * V / (-S * ln(1 - α))
        // Better than Sabine for high-absorption rooms (wool, carpet)
        float volume = profile.estimatedVolume();
        // Estimate surface area from volume (cube approximation: S ≈ 6 * V^(2/3))
        float surfaceArea = 6.0f * (float) Math.pow(volume, 2.0 / 3.0);
        // Clamp absorption to avoid ln(0) — minimum 0.01 (polished concrete)
        float clampedAbsorption = Math.max(0.01f, Math.min(0.99f, avgAbsorption));
        float eyringRT60 = 0.161f * volume / (-surfaceArea * (float) Math.log(1.0 - clampedAbsorption));
        // Clamp to physically plausible range for game environments.
        // 4s max (large cathedral). Stone rooms in Minecraft with α≈0.02
        // produce Eyring RT60 of 10s+ which sounds unrealistically reverberant.
        // Logarithmic compression: maps wide Eyring range (0.1-200s) to perceptual
        // range (0.3-4s) while preserving ordering. Without this, cave_small (59s)
        // and cave_large (159s) both hit the 4s hard clamp and sound identical.
        // Coefficient 2.0: maps Eyring RT60 (0.1-200s) to perceptual range (0.3-4s).
        // 1.0 was too compressed — stone rooms got sub-1.5s RT60. 1.5 gave insufficient
        // differentiation between small (2.2s) and large (2.83s) caves despite 20x volume
        // difference. 2.0 spreads the range: open_field ~0.95s, small_cave ~2.84s,
        // large_cave ~3.67s — perceptually distinct.
        float compressedRT60 = 0.3f + 2.0f * (float) Math.log10(1.0f + eyringRT60);
        float clampedRT60 = Math.max(0.1f, Math.min(4.0f, compressedRT60));
        // Decay ratios per slot — controls how each of the 4 reverb slots
        // subdivides the total RT60. Higher ratios = longer early slots = more
        // audible short-range reverb. Stock SPR effective: {0.04, 0.13, 0.41, 1.0}.
        float[] decayRatios = {0.15f, 0.35f, 0.65f, 1.0f};

        // --- NEW: Compute dominant reflection direction from strongest first-order taps ---
        float panX = 0f, panY = 0f, panZ = 0f;
        float totalPanEnergy = 0f;
        for (ReflectionTap tap : taps) {
            if (tap.order() == 1) {
                float e = (float) tap.energy();
                panX += (float) tap.direction().x * e;
                panY += (float) tap.direction().y * e;
                panZ += (float) tap.direction().z * e;
                totalPanEnergy += e;
            }
        }
        // Normalize and scale magnitude by total energy
        float panMag = (float) Math.sqrt(panX * panX + panY * panY + panZ * panZ);
        if (panMag > 0.001f) {
            panX /= panMag;
            panY /= panMag;
            panZ /= panMag;
            // Scale by energy strength (0 = diffuse, 1 = strongly directional)
            float panStrength = Math.min(1.0f, totalPanEnergy * 2.0f);
            panX *= panStrength;
            panY *= panStrength;
            panZ *= panStrength;
        }
        // Transform to listener-relative coordinates
        // OpenAL listener orientation: at vector and up vector
        // For now use world-space direction — OpenAL EAX reverb pan is in
        // listener-relative space, but the listener orientation is set by
        // Minecraft's camera, so we must transform.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            float yaw = (float) Math.toRadians(-mc.player.getYRot());
            float cosYaw = (float) Math.cos(yaw);
            float sinYaw = (float) Math.sin(yaw);
            float relX = panX * cosYaw - panZ * sinYaw;
            float relZ = panX * sinYaw + panZ * cosYaw;
            panX = relX;
            panZ = relZ;
            // panY stays in world space (up is up)
        }

        // --- LF decay ratio from Eyring absorption ---
        // lfRatio = ln(1-α_mid) / ln(1-α_low): how LF decays relative to mid.
        // Stone (low=0.02, mid=0.03): lfRatio ≈ 1.5 (LF decays slower — bass builds up).
        // This replaces the per-tap energy-weighted approach for consistency with hfRatio.
        float lfRatio = (float) (Math.log(1.0 - midAlpha) / Math.log(1.0 - lowAlpha));
        lfRatio = Math.max(0.1f, Math.min(2.0f, lfRatio));
        // LF gain: slight boost for hard reflective surfaces, reduction for absorptive
        float lfGainAvg = 1.0f - lowAlpha * 2.0f;
        lfGainAvg = Math.max(0.5f, Math.min(1.0f, lfGainAvg));

        // --- NEW: Distance-driven air absorption ---
        float airAbsorption = 1.0f - avgReturnDist * 0.003f;
        airAbsorption = Math.max(0.892f, Math.min(1.0f, airAbsorption));

        // --- NEW: Geometry-driven reflection timing ---
        // Use AVERAGE first-order tap distance for base delay, not just nearest.
        // This scales properly with room size: small room avg ~3m → 9ms,
        // large room avg ~15m → 44ms. Using nearest-only gave 0.004-0.006s
        // regardless of size because the nearest wall was always close.
        float baseReflDelay = 0f;
        float totalFirstOrderEnergy = 0f;
        float avgFirstOrderDist = 0f;
        int firstOrderCount = 0;
        for (ReflectionTap tap : taps) {
            if (tap.order() == 1) {
                totalFirstOrderEnergy += (float) tap.energy();
                avgFirstOrderDist += (float) tap.distance();
                firstOrderCount++;
            }
        }
        if (firstOrderCount > 0) {
            avgFirstOrderDist /= firstOrderCount;
            baseReflDelay = avgFirstOrderDist / 343.0f;
        }
        baseReflDelay = Math.max(0f, Math.min(0.3f, baseReflDelay));
        // Cap with enclosure: open fields (enc~0.4) get weaker early reflections,
        // enclosed rooms (enc~1.0) get strong early reflections.
        // Scaling the cap itself ensures open fields can never reach cave-level reflGain.
        float reflGainCap = 3.16f * enclosure;
        float reflGain = Math.max(0f, Math.min(reflGainCap, totalFirstOrderEnergy * 2.5f));

        // --- NEW: Flutter echo detection ---
        // Detect parallel wall pairs: opposing first-order taps with hard materials.
        // Default echoTime scales with room size (mean free path).
        // Flutter echo detection can increase echoTime (for distinct flutter patterns)
        // but never DECREASE it below the room-size default — otherwise nearest wall
        // pairs (with highest energy/depth) override the room size signal.
        float roomEchoTime = Math.max(0.04f, Math.min(0.25f,
                2.0f * avgFirstOrderDist / 343.0f));
        float echoTime = roomEchoTime;
        float echoDepth = 0f;
        for (ReflectionTap tap1 : taps) {
            if (tap1.order() != 1) continue;
            if (tap1.distance() < 1 || tap1.distance() > 40) continue;
            SpectralCategory cat1 = SpectralCategory.fromBlockState(tap1.material());
            if (cat1 != SpectralCategory.HARD) continue;
            for (ReflectionTap tap2 : taps) {
                if (tap2.order() != 1 || tap2 == tap1) continue;
                if (tap2.distance() < 1 || tap2.distance() > 40) continue;
                SpectralCategory cat2 = SpectralCategory.fromBlockState(tap2.material());
                if (cat2 != SpectralCategory.HARD) continue;
                // Check for opposite directions
                double dot = tap1.direction().dot(tap2.direction());
                if (dot < -0.8) {
                    float roundTrip = (float) (tap1.distance() + tap2.distance());
                    float candidateTime = roundTrip / 343.0f;
                    candidateTime = Math.max(0.04f, Math.min(0.25f, candidateTime));
                    float avgAbs = (float) ((1.0 - tap1.energy()) + (1.0 - tap2.energy())) / 2f;
                    float candidateDepth = (1.0f - avgAbs) * 0.9f;
                    candidateDepth = Math.max(0f, Math.min(1.0f, candidateDepth));
                    if (candidateDepth > echoDepth) {
                        echoDepth = candidateDepth;
                        // Only update echoTime if this flutter path is longer than
                        // the room-size default — don't let short wall pairs override
                        echoTime = Math.max(roomEchoTime, candidateTime);
                    }
                }
            }
        }

        // --- NEW: Reverb tail modulation from cavity irregularity ---
        float modDepth = scatter * 0.15f;
        modDepth = Math.max(0f, Math.min(1.0f, modDepth));
        float modTime = 0.25f + scatter * 1.5f;
        modTime = Math.max(0.04f, Math.min(4.0f, modTime));

        // --- Per-slot application ---
        float[] reflDelayScale = {1.0f, 1.5f, 2.5f, 4.0f};
        float[] lateDelayScale = {3.0f, 4.5f, 7.5f, 12.0f};

        for (int i = 0; i < 4; i++) {
            int reverbEffect = SoundPhysics.getReverbEffect(i);
            int auxSlot = SoundPhysics.getAuxFXSlot(i);
            if (reverbEffect == 0 || auxSlot == 0) continue;

            // Existing: decay time, HF ratio, density, diffusion
            float decayTime = clampedRT60 * decayRatios[i];
            decayTime = Math.max(0.1f, Math.min(20.0f, decayTime));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DECAY_TIME, decayTime);
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, hfRatio);
            // Volume-dependent density: larger rooms produce more spaced-out reflections
            // (lower density). Without this, a 10k-block cave and a 216k-block cavern
            // have near-identical density (0.854 vs 0.86). Volume factor scales from
            // 1.0 (small rooms) to ~0.65 (massive caverns).
            float volumeFactor = 1.0f - 0.15f * (float) Math.log10(1.0f + volume / 1000.0f);
            volumeFactor = Math.max(0.5f, Math.min(1.0f, volumeFactor));
            // Floor of 0.2 prevents reverb from sounding too sparse in rooms
            // with few scattering surfaces (0 density = discrete echoes, not reverb)
            float density = Math.max(0.2f, Math.min(1.0f, (scatter * 1.5f + 0.2f) * volumeFactor));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DENSITY, density);
            // Diffusion: high scatter = smooth reverb, low scatter = grainy
            float diffusion = Math.max(0.0f, Math.min(1.0f, scatter * 1.2f + 0.3f));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DIFFUSION, diffusion);

            // Late reverb gain: scale with enclosure so reverb tails are
            // audible in enclosed rooms. Without this, the late reverb defaults
            // to whatever SPR sets, which may be too quiet for the tail to
            // register above ambient.
            float lateGain = 0.5f + enclosure * 1.5f;
            lateGain = Math.max(0.0f, Math.min(5.0f, lateGain));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, lateGain);

            // NEW: HF gain from material absorption — controls initial HF content
            // Stone (highAlpha=0.05): gainHF≈0.95 (bright). Wood (0.12): 0.88.
            // Carpet (0.55): 0.45 (dark). Complements hfRatio which controls decay rate.
            float gainHF = Math.max(0.1f, Math.min(0.99f, 1.0f - highAlpha));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_GAINHF, gainHF);

            // HF reference: lowered from default 5000Hz to 2500Hz.
            // At 5000Hz, hfRatio only affects content above 5kHz where there's
            // minimal reverb energy — making frequency-dependent decay inaudible.
            // At 2500Hz, the decay difference affects 2.5-5kHz where reverb energy
            // is perceptually prominent, making stone vs wood vs carpet audible.
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_HFREFERENCE, 2500.0f);

            // NEW: LF decay ratio
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DECAY_LFRATIO, lfRatio);

            // NEW: LF gain
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_GAINLF, lfGainAvg);

            // NEW: LF reference frequency
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_LFREFERENCE, 250.0f);

            // NEW: Air absorption
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, airAbsorption);

            // NEW: Reflection timing
            float slotReflDelay = baseReflDelay * reflDelayScale[i];
            slotReflDelay = Math.max(0f, Math.min(0.3f, slotReflDelay));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, slotReflDelay);

            // NEW: Reflection gain from first-order tap energy
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, reflGain);

            // NEW: Late reverb delay
            float slotLateDelay = baseReflDelay * lateDelayScale[i];
            slotLateDelay = Math.max(0f, Math.min(0.1f, slotLateDelay));
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, slotLateDelay);

            // NEW: Spatial panning — early slots directional, late slots diffuse
            if (i <= 1) {
                // Early reflection slots: pan toward dominant wall direction
                org.lwjgl.openal.EXTEfx.alEffectfv(reverbEffect,
                        org.lwjgl.openal.EXTEfx.AL_EAXREVERB_REFLECTIONS_PAN,
                        new float[]{panX, panY, panZ});
            } else {
                // Late reverb slots: omnidirectional diffuse field
                org.lwjgl.openal.EXTEfx.alEffectfv(reverbEffect,
                        org.lwjgl.openal.EXTEfx.AL_EAXREVERB_REFLECTIONS_PAN,
                        new float[]{0f, 0f, 0f});
            }
            // Late reverb pan always omnidirectional
            org.lwjgl.openal.EXTEfx.alEffectfv(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_LATE_REVERB_PAN,
                    new float[]{0f, 0f, 0f});

            // NEW: Flutter echo
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_ECHO_TIME, echoTime);
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_ECHO_DEPTH, echoDepth);

            // NEW: Reverb tail modulation
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_MODULATION_TIME, modTime);
            org.lwjgl.openal.EXTEfx.alEffectf(reverbEffect,
                    org.lwjgl.openal.EXTEfx.AL_EAXREVERB_MODULATION_DEPTH, modDepth);

            // Re-attach updated effect to slot
            org.lwjgl.openal.EXTEfx.alAuxiliaryEffectSloti(auxSlot,
                    org.lwjgl.openal.EXTEfx.AL_EFFECTSLOT_EFFECT, reverbEffect);
        }

        // Store diagnostic snapshot for test access
        lastReverbParamsJson = String.format(
                "{\"eyringRT60\":%.3f,\"clampedRT60\":%.3f,\"volume\":%.1f,\"surfaceArea\":%.1f,"
                + "\"avgAbsorption\":%.4f,\"enclosure\":%.3f,\"scatter\":%.3f,"
                + "\"hfRatio\":%.3f,\"gainHF\":%.3f,\"hfRef\":%.0f,\"lfRatio\":%.3f,\"lfGain\":%.3f,"
                + "\"airAbsorption\":%.4f,\"echoTime\":%.3f,\"echoDepth\":%.3f,"
                + "\"modTime\":%.3f,\"modDepth\":%.3f,"
                + "\"reflGain\":%.3f,\"baseReflDelay\":%.4f,"
                + "\"panX\":%.3f,\"panY\":%.3f,\"panZ\":%.3f,"
                + "\"slot3_decayTime\":%.3f,\"slot3_lateGain\":%.3f,\"slot3_density\":%.3f,\"slot3_diffusion\":%.3f}",
                eyringRT60, clampedRT60, volume, surfaceArea,
                avgAbsorption, enclosure, scatter,
                hfRatio, Math.max(0.1f, Math.min(0.99f, 1.0f - highAlpha)), 2500.0f, lfRatio, lfGainAvg,
                airAbsorption, echoTime, echoDepth,
                modTime, modDepth,
                reflGain, baseReflDelay,
                panX, panY, panZ,
                clampedRT60 * decayRatios[3], 0.5f + enclosure * 1.5f,
                Math.max(0.0f, Math.min(1.0f, scatter * 1.5f)),
                Math.max(0.0f, Math.min(1.0f, scatter * 1.2f + 0.3f)));
    }

=======
=======
        // Update energy meter
        energyMeter.computeTotalEnergy(excitation, reflections);

>>>>>>> theirs
        // Update debug renderer references
        debugRenderer.setExcitationManager(excitation);
        debugRenderer.setCurrentProfile(profile);

        // Diagnostic logging (every 100 ticks = 5 seconds)
        if (config.diagnosticLogging.get()) {
            diagnosticTickCounter++;
            if (diagnosticTickCounter >= 100) {
                diagnosticTickCounter = 0;
                AudioDiagnostics.logAllEapSources(excitation, reflections);
                AudioDiagnostics.logEnvironmentProfile(profile);
            }
        } else {
            diagnosticTickCounter = 0;
        }
    }

>>>>>>> theirs
    // ── Sound play hook ─────────────────────────────────────────────

    /**
     * Called from the SourceMixin when a game sound starts playing.
     * Feeds the sound to the early reflection processor.
     *
     * @param sourceId the OpenAL source ID of the played sound
     * @param pos      the world-space position of the sound
     */
    public void onSoundPlay(int sourceId, Vec3 pos) {
        if (!initialized) {
            return;
        }

        EapConfig config = SoundPhysicsMod.EAP_CONFIG;
        if (config == null || !config.eapEnabled.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long currentTick = minecraft.level.getGameTime();

        // Use the raw latest profile (without crossfade) for tap positions
        EnvironmentProfile profile = profiler.getLatestProfile();

        reflections.onSoundPlay(sourceId, pos, profile, currentTick);
    }

    // ── Level change hook ───────────────────────────────────────────

    /**
     * Called from MinecraftMixin when the client level changes (dimension change, disconnect).
     * Invalidates the profiler and silences all sources.
     */
    public void onLevelChange() {
        if (!initialized) {
            return;
        }

        Loggers.log("EapSystem: level change detected, invalidating profiler");
        profiler.invalidate();
<<<<<<< ours
<<<<<<< ours
        excitation.silenceAll();
        reflections.muteAll();
        emitterManager.silenceAll();
        hyperreality.silenceAll();
        hyperreality.forceRescan();
=======
=======
        excitation.silenceAll();
>>>>>>> theirs
        reflections.muteAll();
>>>>>>> theirs
    }

    // ── Shutdown ────────────────────────────────────────────────────

    /**
     * Cleans up all OpenAL resources. Should be called on mod unload.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        Loggers.log("EapSystem: shutting down");
        excitation.shutdown();
        reflections.shutdown();
<<<<<<< ours
        drProcessor.shutdown();
        emitterManager.shutdown();
        hyperreality.shutdown();
=======
>>>>>>> theirs
        initialized = false;
        instance = null;
    }

<<<<<<< ours
<<<<<<< ours
    // ── A/B/C Compare Toggle ──────────────────────────────────────

    /**
     * Cycles through comparison modes: Vanilla -> Physics -> Full EchoField.
     * VANILLA silences all EAP processing; PHYSICS and FULL re-enable.
     */
    public void toggleEnabled() {
        if (installation.isActive() && !installation.isComparisonAllowed()) {
            Loggers.log("EapSystem: A/B/C toggle locked — installation stage: {}", installation.getCurrentStage().label);
            return;
        }
        compareMode = compareMode.next();
        Loggers.log("EapSystem: compare mode -> {}", compareMode.label);
        switch (compareMode) {
            case VANILLA -> {
                excitation.silenceAll();
                reflections.muteAll();
                drProcessor.setEnabled(false);
                hyperreality.silenceAll();
            }
            case PHYSICS, FULL -> {
                excitation.setEnabled(true);
                reflections.unmuteAll();
                drProcessor.setEnabled(true);
            }
        }
    }

    public CompareMode getCompareMode() { return compareMode; }

    /**
     * Returns whether the compare toggle is currently active (not VANILLA).
     */
    public boolean isToggleActive() {
        return compareMode != CompareMode.VANILLA;
    }

    // ── Sound engine lifecycle ────────────────────────────────────

    /**
     * Called from SoundSystemMixin.loadLibrary() after the OpenAL context is created.
     * On first call, enables lazy initialization via getInstance().
     * On subsequent calls (sound engine reload), destroys the old instance so that
     * fresh OpenAL resources are created with the new context.
     */
    public static synchronized void onSoundEngineReady() {
        if (instance != null) {
            Loggers.log("EapSystem: sound engine reloaded, recreating OpenAL resources");
            instance.initialized = false;
            // Don't call shutdown() — old OpenAL IDs are already invalid
            instance = null;
        }
        soundEngineReady = true;
        Loggers.log("EapSystem: sound engine ready, OpenAL context available");
    }

    /**
     * Returns true if the sound engine has initialized and OpenAL calls are safe.
     */
    public static boolean isSoundEngineReady() {
        return soundEngineReady;
    }

=======
>>>>>>> theirs
=======
    // ── A/B Toggle ──────────────────────────────────────────────────

    /**
     * Toggles the EAP system on/off for A/B comparison.
     * When toggled off, silences all excitation and mutes reflections.
     * When toggled on, re-enables excitation and unmutes reflections.
     */
    public void toggleEnabled() {
        eapToggleActive = !eapToggleActive;
        Loggers.log("EapSystem: A/B toggle -> {}", eapToggleActive ? "ON" : "OFF");
        if (!eapToggleActive) {
            excitation.silenceAll();
            reflections.muteAll();
        } else {
            excitation.setEnabled(true);
            reflections.unmuteAll();
        }
    }

    /**
     * Returns whether the A/B toggle is currently active (EAP enabled).
     */
    public boolean isToggleActive() {
        return eapToggleActive;
    }

>>>>>>> theirs
    // ── Accessors ───────────────────────────────────────────────────

    public EnvironmentProfiler getProfiler() {
        return profiler;
    }

    public ExcitationSourceManager getExcitation() {
        return excitation;
    }

    public EarlyReflectionProcessor getReflections() {
        return reflections;
    }

    public EapDebugRenderer getDebugRenderer() {
        return debugRenderer;
    }

<<<<<<< ours
    public HrtfManager getHrtfManager() {
        return hrtfManager;
    }

    public PerSourceDRProcessor getDRProcessor() {
        return drProcessor;
    }

    public EmitterManager getEmitterManager() {
        return emitterManager;
    }

    public AirAbsorptionProcessor getAirAbsorption() {
        return airAbsorption;
    }

    public HyperrealitySystem getHyperreality() {
        return hyperreality;
    }

    public InstallationManager getInstallation() {
        return installation;
    }

    /**
     * Returns the last-computed reverb parameters as a JSON string.
     * Used by TestCommandServer for diagnostic output.
     */
    public String getLastReverbParamsJson() {
        return lastReverbParamsJson;
    }

=======
>>>>>>> theirs
    /**
     * Returns a debug HUD string with key EAP metrics.
     */
    public String getDebugHudText() {
        if (!initialized || SoundPhysicsMod.EAP_CONFIG == null) {
            return "EAP: not initialized";
        }

        if (!SoundPhysicsMod.EAP_CONFIG.eapEnabled.get()) {
            return "EAP: disabled";
        }

<<<<<<< ours
<<<<<<< ours
        if (compareMode == CompareMode.VANILLA) {
            return "EAP: " + CompareMode.VANILLA.label;
        }

        EnvironmentProfile profile = profiler.getCurrentProfile();
        float totalEnergy = energyMeter.getLatestEnergy();
        int activeSlots = reflections.getActiveSlotCount();
        float cycleProgress = profiler.getCycleProgress();

        int emitterActive = emitterManager.getActiveCount();
        int emitterTracked = emitterManager.getTotalTracked();
        int hyperActive = hyperreality.getActiveCount();
        int hyperTotal = hyperreality.getSourceCount();

        String hrtfStatus = hrtfManager.getStatusText();
        String sourceBudget = debugRenderer.getSourceBudgetText(emitterManager, hyperreality, reflections);
        String conditionsText = debugRenderer.getConditionsText();
        String installInfo = "";
        if (installation.isActive()) {
            installInfo = "\nInstall: " + installation.getCurrentStage().label
                    + " (" + installation.getSecondsRemaining() + "s remaining)";
            if (installation.getMetrics().isRecording()) {
                installInfo += String.format(" | Recording: %d ticks, %d collisions",
                        installation.getMetrics().getRecordedTicks(),
                        installation.getMetrics().getWallCollisions());
            }
        }
        String base = String.format("%s | EAP[%s]: enc=%.2f rt60=%.2f wind=%.2f energy=%.3f refl=%d/%d emit=%d/%d hyper=%d/%d cycle=%.0f%%\n%s\n%s",
                hrtfStatus, compareMode.label,
                profile.enclosureFactor(), profile.estimatedRT60(), profile.windExposure(),
                totalEnergy, activeSlots, EarlyReflectionProcessor.POOL_SIZE,
                emitterActive, emitterTracked,
                hyperActive, hyperTotal,
                cycleProgress * 100f, energyMeter.getFormattedEnergy(),
                sourceBudget);
        if (conditionsText != null) {
            base += "\n" + conditionsText;
        }
        return base + installInfo;
    }

    /**
     * Returns the energy meter instance for external access.
     */
    public AudioEnergyMeter getEnergyMeter() {
        return energyMeter;
=======
=======
        if (!eapToggleActive) {
            return "EAP: toggled OFF (A/B)";
        }

>>>>>>> theirs
        EnvironmentProfile profile = profiler.getCurrentProfile();
        float totalEnergy = energyMeter.getLatestEnergy();
        int activeSlots = reflections.getActiveSlotCount();
        float cycleProgress = profiler.getCycleProgress();

        return String.format("EAP: enc=%.2f rt60=%.2f wind=%.2f energy=%.3f refl=%d/%d cycle=%.0f%%\n%s",
                profile.enclosureFactor(), profile.estimatedRT60(), profile.windExposure(),
                totalEnergy, activeSlots, EarlyReflectionProcessor.POOL_SIZE,
<<<<<<< ours
                cycleProgress * 100f);
>>>>>>> theirs
=======
                cycleProgress * 100f, energyMeter.getFormattedEnergy());
    }

    /**
     * Returns the energy meter instance for external access.
     */
    public AudioEnergyMeter getEnergyMeter() {
        return energyMeter;
>>>>>>> theirs
    }

}
