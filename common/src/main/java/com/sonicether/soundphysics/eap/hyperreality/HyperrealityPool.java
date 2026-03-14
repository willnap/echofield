package com.sonicether.soundphysics.eap.hyperreality;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HyperrealityPool {

    public static final int DEFAULT_POOL_SIZE = 48;
    private static final float MATCH_DISTANCE_SQ = 2.0f * 2.0f;
    private static final float HYSTERESIS_INNER = 1.0f;
    private static final float AIR_ABSORPTION_COEFF = 0.005f;
    private static final float[] REVERB_SCALE = {1.0f, 0.7f, 0.5f, 0.3f};

    private final HyperrealitySource[] sources;
    private final int poolSize;
    private final TerrainBufferFactory bufferFactory;
    private int activeCount;

    public HyperrealityPool(int poolSize, TerrainBufferFactory bufferFactory) {
        this.poolSize = poolSize;
        this.bufferFactory = bufferFactory;
        this.sources = new HyperrealitySource[poolSize];
        for (int i = 0; i < poolSize; i++) {
            sources[i] = new HyperrealitySource(i);
        }
    }

    public HyperrealityPool(TerrainBufferFactory bufferFactory) {
        this(DEFAULT_POOL_SIZE, bufferFactory);
    }

    public HyperrealitySource[] getSources() { return sources; }
    public int getActiveCount() { return activeCount; }
    public int getPoolSize() { return poolSize; }

    public void init() {
        for (int i = 0; i < poolSize; i++) {
            HyperrealitySource src = sources[i];
            src.sourceId = org.lwjgl.openal.AL11.alGenSources();
            org.lwjgl.openal.AL11.alSourcef(src.sourceId, org.lwjgl.openal.AL11.AL_GAIN, 0.0f);
            org.lwjgl.openal.AL11.alSourcei(src.sourceId,
                    org.lwjgl.openal.AL11.AL_SOURCE_RELATIVE, org.lwjgl.openal.AL11.AL_FALSE);
            src.filterId = org.lwjgl.openal.EXTEfx.alGenFilters();
            org.lwjgl.openal.EXTEfx.alFilteri(src.filterId,
                    org.lwjgl.openal.EXTEfx.AL_FILTER_TYPE,
                    org.lwjgl.openal.EXTEfx.AL_FILTER_LOWPASS);
            org.lwjgl.openal.EXTEfx.alFilterf(src.filterId,
                    org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            org.lwjgl.openal.EXTEfx.alFilterf(src.filterId,
                    org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
            for (int j = 0; j < 4; j++) {
                src.sendFilterIds[j] = org.lwjgl.openal.EXTEfx.alGenFilters();
                org.lwjgl.openal.EXTEfx.alFilteri(src.sendFilterIds[j],
                        org.lwjgl.openal.EXTEfx.AL_FILTER_TYPE,
                        org.lwjgl.openal.EXTEfx.AL_FILTER_LOWPASS);
                org.lwjgl.openal.EXTEfx.alFilterf(src.sendFilterIds[j],
                        org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN, 1.0f);
                org.lwjgl.openal.EXTEfx.alFilterf(src.sendFilterIds[j],
                        org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
            }
        }
    }

    public void shutdown() {
        for (int i = 0; i < poolSize; i++) {
            HyperrealitySource src = sources[i];
            if (src.sourceId != 0) {
                org.lwjgl.openal.AL11.alDeleteSources(src.sourceId);
                src.sourceId = 0;
            }
            if (src.filterId != 0) {
                org.lwjgl.openal.EXTEfx.alDeleteFilters(src.filterId);
                src.filterId = 0;
            }
            for (int j = 0; j < 4; j++) {
                if (src.sendFilterIds[j] != 0) {
                    org.lwjgl.openal.EXTEfx.alDeleteFilters(src.sendFilterIds[j]);
                    src.sendFilterIds[j] = 0;
                }
            }
            src.reset();
        }
        activeCount = 0;
    }

    static ReconciliationResult computeReconciliation(
            HyperrealitySource[] sources,
            List<TerrainFeature> features,
            float playerX, float playerY, float playerZ,
            int scanRadius) {

        List<MatchedPair> matched = new ArrayList<>();
        List<Integer> orphaned = new ArrayList<>();
        Set<Integer> hysteresisKept = new HashSet<>();
        boolean[] featureMatched = new boolean[features.size()];

        for (int i = 0; i < sources.length; i++) {
            HyperrealitySource src = sources[i];
            if (!src.active) continue;

            int bestFeatureIdx = -1;
            float bestDistSq = MATCH_DISTANCE_SQ;

            for (int f = 0; f < features.size(); f++) {
                if (featureMatched[f]) continue;
                TerrainFeature feat = features.get(f);
                if (feat.type() != src.type) continue;
                float dx = feat.x() - src.featureX;
                float dy = feat.y() - src.featureY;
                float dz = feat.z() - src.featureZ;
                float distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestFeatureIdx = f;
                }
            }

            if (bestFeatureIdx >= 0) {
                matched.add(new MatchedPair(i, features.get(bestFeatureIdx)));
                featureMatched[bestFeatureIdx] = true;
            } else {
                float dx = src.featureX - playerX;
                float dy = src.featureY - playerY;
                float dz = src.featureZ - playerZ;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                float hysteresisLow = scanRadius - HYSTERESIS_INNER;
                if (dist >= hysteresisLow && dist <= scanRadius) {
                    hysteresisKept.add(i);
                } else {
                    orphaned.add(i);
                }
            }
        }

        List<TerrainFeature> unmatched = new ArrayList<>();
        for (int f = 0; f < features.size(); f++) {
            if (!featureMatched[f]) {
                unmatched.add(features.get(f));
            }
        }
        unmatched.sort(Comparator.comparingDouble(TerrainFeature::saliency).reversed());

        int freeSlots = 0;
        for (HyperrealitySource src : sources) {
            if (!src.active) freeSlots++;
        }

        int directAllocCount = Math.min(unmatched.size(), freeSlots);
        List<TerrainFeature> toAllocate = new ArrayList<>(unmatched.subList(0, directAllocCount));

        List<StealDecision> toSteal = new ArrayList<>();
        if (directAllocCount < unmatched.size()) {
            List<IndexedPriority> activePriorities = new ArrayList<>();

            for (int i = 0; i < sources.length; i++) {
                if (sources[i].active && !hysteresisKept.contains(i)) {
                    activePriorities.add(new IndexedPriority(i, sources[i].computePriority()));
                }
            }
            activePriorities.sort(Comparator.comparingDouble(IndexedPriority::priority));

            int stealIdx = 0;
            for (int f = directAllocCount; f < unmatched.size() && stealIdx < activePriorities.size(); f++) {
                TerrainFeature feat = unmatched.get(f);
                IndexedPriority victim = activePriorities.get(stealIdx);

                float dx = feat.x() - playerX;
                float dy = feat.y() - playerY;
                float dz = feat.z() - playerZ;
                float newDist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                float newPriority = feat.saliency() * (1.0f / (newDist + 1.0f)) * feat.type().familyWeight();

                if (newPriority > victim.priority()) {
                    toSteal.add(new StealDecision(victim.index(), feat));
                    stealIdx++;
                } else {
                    break;
                }
            }
        }

        return new ReconciliationResult(matched, orphaned, toAllocate, toSteal, hysteresisKept);
    }

    public void applyReconciliation(ReconciliationResult result,
                                    float playerX, float playerY, float playerZ) {
        for (MatchedPair mp : result.matched()) {
            HyperrealitySource src = sources[mp.sourceIndex()];
            TerrainFeature feat = mp.feature();
            src.featureX = feat.x();
            src.featureY = feat.y();
            src.featureZ = feat.z();
            src.featureNX = feat.nx();
            src.featureNY = feat.ny();
            src.featureNZ = feat.nz();
            src.material = feat.material();
            src.saliency = feat.saliency();
            src.magnitude = feat.magnitude();
            src.cyclesSinceActivation++;
            src.targetX = feat.x();
            src.targetY = feat.y();
            src.targetZ = feat.z();
            src.targetFilterHF = HyperrealitySource.materialHFGain(feat.material());
            updateDistance(src, playerX, playerY, playerZ);
        }

        for (int idx : result.orphaned()) {
            HyperrealitySource src = sources[idx];
            if (src.fadeTicks >= 0) {
                src.fadeTicks = -HyperrealitySource.FADE_DURATION;
            }
        }

        int allocIdx = 0;
        for (int i = 0; i < poolSize && allocIdx < result.toAllocate().size(); i++) {
            if (!sources[i].active) {
                TerrainFeature feat = result.toAllocate().get(allocIdx);
                activateSource(sources[i], feat, playerX, playerY, playerZ);
                allocIdx++;
            }
        }

        for (StealDecision steal : result.toSteal()) {
            HyperrealitySource victim = sources[steal.victimIndex()];
            victim.reset();
            activateSource(victim, steal.feature(), playerX, playerY, playerZ);
        }

        activeCount = 0;
        for (HyperrealitySource src : sources) {
            if (src.active) activeCount++;
        }
    }

    public void reconcile(List<TerrainFeature> features,
                          float playerX, float playerY, float playerZ,
                          int scanRadius) {
        ReconciliationResult result = computeReconciliation(
                sources, features, playerX, playerY, playerZ, scanRadius);
        applyReconciliation(result, playerX, playerY, playerZ);
    }

    private void activateSource(HyperrealitySource src, TerrainFeature feat,
                                float playerX, float playerY, float playerZ) {
        src.type = feat.type();
        src.featureX = feat.x();
        src.featureY = feat.y();
        src.featureZ = feat.z();
        src.featureNX = feat.nx();
        src.featureNY = feat.ny();
        src.featureNZ = feat.nz();
        src.material = feat.material();
        src.saliency = feat.saliency();
        src.magnitude = feat.magnitude();
        src.active = true;
        src.fadeTicks = HyperrealitySource.FADE_DURATION;
        src.currentGain = 0.0f;
        src.currentX = feat.x();
        src.currentY = feat.y();
        src.currentZ = feat.z();
        src.currentFilterHF = HyperrealitySource.materialHFGain(feat.material());
        src.targetX = feat.x();
        src.targetY = feat.y();
        src.targetZ = feat.z();
        src.targetFilterHF = HyperrealitySource.materialHFGain(feat.material());
        updateDistance(src, playerX, playerY, playerZ);

        if (src.sourceId != 0 && bufferFactory != null) {
            int bufferId = bufferFactory.getBufferId(
                    feat.type().family(),
                    (int) feat.x(), (int) feat.z());
            org.lwjgl.openal.AL11.alSourcei(src.sourceId,
                    org.lwjgl.openal.AL11.AL_BUFFER, bufferId);
            org.lwjgl.openal.AL11.alSourcei(src.sourceId,
                    org.lwjgl.openal.AL11.AL_LOOPING, org.lwjgl.openal.AL11.AL_TRUE);
            org.lwjgl.openal.AL11.alSourcePlay(src.sourceId);
        }
    }

    private static void updateDistance(HyperrealitySource src,
                                       float playerX, float playerY, float playerZ) {
        float dx = src.featureX - playerX;
        float dy = src.featureY - playerY;
        float dz = src.featureZ - playerZ;
        src.distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void silenceAll() {
        for (HyperrealitySource src : sources) {
            src.silence();
            src.reset();
        }
        activeCount = 0;
    }

    // ── Gain model ────────────────────────────────────────────────────

    public static float computeMagnitudeGain(float magnitude) {
        if (magnitude <= 0.0f) return 0.0f;
        double log2 = Math.log(magnitude + 1.0) / Math.log(2.0);
        return (float) Math.min(1.0, log2 / 4.0);
    }

    public static float computeDistanceRolloff(float distance, float maxRange) {
        if (maxRange <= 0.0f) return 0.0f;
        return Math.max(0.0f, 1.0f - distance / maxRange);
    }

    public static float computeFacingAttenuation(float nx, float ny, float nz,
                                                  float dx, float dy, float dz) {
        float dot = nx * dx + ny * dy + nz * dz;
        return 0.5f + 0.5f * Math.max(0.0f, dot);
    }

    public static float familyGain(TerrainFeatureType.Family family) {
        return switch (family) {
<<<<<<< ours
            case VOID -> 4.0f;
            case SURFACE -> 3.0f;
            case GROUND -> 2.0f;
=======
            case VOID -> 0.08f;
            case SURFACE -> 0.06f;
            case GROUND -> 0.03f;
>>>>>>> theirs
        };
    }

    public static float computeBaseGain(float magnitude, float distance, float maxRange,
                                         float nx, float ny, float nz,
                                         float dirToPlayerX, float dirToPlayerY, float dirToPlayerZ,
                                         TerrainFeatureType.Family family) {
        float mg = computeMagnitudeGain(magnitude);
        float dr = computeDistanceRolloff(distance, maxRange);
        float fa = computeFacingAttenuation(nx, ny, nz, dirToPlayerX, dirToPlayerY, dirToPlayerZ);
        float fg = familyGain(family);
        return mg * dr * fa * fg;
    }

    public static float computeAirAbsorptionHF(float distance, float baseHF, float strengthFactor) {
        float absorption = distance * AIR_ABSORPTION_COEFF * strengthFactor;
        float hf = baseHF * (1.0f - absorption);
        return Math.max(0.1f, Math.min(1.0f, hf));
    }

    public static float computeReverbSendGain(float enclosure, float rt60, float reverbScale) {
        return enclosure * Math.min(rt60 / 2.0f, 1.0f) * reverbScale;
    }

    public static float computeReverbHFCutoff(float spectralHFAbsorption) {
        return Math.max(0.1f, 1.0f - spectralHFAbsorption * 0.6f);
    }

    public void tick(float masterGain, float augmentationIntensity,
                     float playerX, float playerY, float playerZ) {
        activeCount = 0;
        for (int i = 0; i < poolSize; i++) {
            HyperrealitySource src = sources[i];
            if (!src.active) continue;

            updateDistance(src, playerX, playerY, playerZ);

            float dx = playerX - src.featureX;
            float dy = playerY - src.featureY;
            float dz = playerZ - src.featureZ;
            float distInv = src.distance > 0.001f ? 1.0f / src.distance : 0.0f;
            float dirX = dx * distInv;
            float dirY = dy * distInv;
            float dirZ = dz * distInv;

            float maxRange = src.type != null ? src.type.maxRange() : 24.0f;
            float baseGain = computeBaseGain(
                    src.magnitude, src.distance, maxRange,
                    src.featureNX, src.featureNY, src.featureNZ,
                    dirX, dirY, dirZ,
                    src.type != null ? src.type.family() : TerrainFeatureType.Family.SURFACE);

            float novelty = src.computeNoveltyBoost();
            src.targetGain = baseGain * novelty * augmentationIntensity * masterGain;

            float materialHF = src.material != null
                    ? HyperrealitySource.materialHFGain(src.material) : 0.5f;
            if (src.type == TerrainFeatureType.SOLID_OBJECT) {
                materialHF *= 0.6f;
            }
            src.targetFilterHF = computeAirAbsorptionHF(src.distance, materialHF, 0.5f);

            boolean fadeOutComplete = src.advanceFade();
            if (fadeOutComplete) {
                src.reset();
                continue;
            }

            src.smoothStep();

            float fadeScale = src.computeFadeScale();
            src.applyToOpenAL(fadeScale);

            activeCount++;
        }
    }

    public void applyReverb(float enclosureFactor, float estimatedRT60,
                            float spectralHFAbsorption, int maxAuxSends) {
        int sends = Math.min(maxAuxSends, 4);
        float hfCutoff = computeReverbHFCutoff(spectralHFAbsorption);

        for (int i = 0; i < poolSize; i++) {
            HyperrealitySource src = sources[i];
            if (!src.active || src.sourceId == 0) continue;

            for (int j = 0; j < sends; j++) {
                float sendGain = computeReverbSendGain(enclosureFactor, estimatedRT60,
                        REVERB_SCALE[j]);

                if (src.sendFilterIds[j] != 0) {
                    org.lwjgl.openal.EXTEfx.alFilterf(src.sendFilterIds[j],
                            org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN, sendGain);
                    org.lwjgl.openal.EXTEfx.alFilterf(src.sendFilterIds[j],
                            org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF, hfCutoff);

                    int auxSlot = com.sonicether.soundphysics.SoundPhysics.getAuxFXSlot(j);
                    org.lwjgl.openal.AL11.alSource3i(src.sourceId,
                            org.lwjgl.openal.EXTEfx.AL_AUXILIARY_SEND_FILTER,
                            auxSlot, j, src.sendFilterIds[j]);
                }
            }
        }
    }

    public record MatchedPair(int sourceIndex, TerrainFeature feature) {}
    public record StealDecision(int victimIndex, TerrainFeature feature) {}
    public record ReconciliationResult(
            List<MatchedPair> matched,
            List<Integer> orphaned,
            List<TerrainFeature> toAllocate,
            List<StealDecision> toSteal,
            Set<Integer> hysteresisKept
    ) {}
    private record IndexedPriority(int index, float priority) {}
}
