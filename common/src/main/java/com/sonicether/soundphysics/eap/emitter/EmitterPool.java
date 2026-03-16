package com.sonicether.soundphysics.eap.emitter;

import com.sonicether.soundphysics.Loggers;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a fixed-size pool of OpenAL sources for Layer 1 emitters.
 * Handles allocation, priority-based recycling, and fade-out before deallocation.
 *
 * Pool size is configured at construction (default 64, max 80 per spec budget).
 * When pool is full, lowest-priority emitters are faded out and recycled.
 */
public final class EmitterPool {

    private static final float FADE_OUT_TICKS = 4; // 200ms at 20 tps (≥50ms per spec 2.4.1)
    private static final float SMOOTH_FACTOR = 0.15f;

<<<<<<< ours
<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
    private SamplePoolLoader samplePoolLoader;

    void setSamplePoolLoader(SamplePoolLoader loader) {
        this.samplePoolLoader = loader;
    }

    private int maxSources;
=======
    private final int maxSources;
>>>>>>> theirs
=======
    private int maxSources;
>>>>>>> theirs
    private final List<Emitter> activeEmitters = new ArrayList<>();

    // Pre-allocated OpenAL source IDs
    private final int[] sourceIds;
    private final int[] filterIds;
    private final boolean[] sourceInUse;

    public EmitterPool(int maxSources) {
<<<<<<< ours
<<<<<<< ours
=======
        this.maxSources = maxSources;
>>>>>>> theirs
=======
>>>>>>> theirs
        this.sourceIds = new int[maxSources];
        this.filterIds = new int[maxSources];
        this.sourceInUse = new boolean[maxSources];

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
        int allocated = 0;
        for (int i = 0; i < maxSources; i++) {
            sourceIds[i] = AL10.alGenSources();
            if (sourceIds[i] == 0) {
                Loggers.log("EmitterPool: alGenSources failed at slot {}, stopping", i);
                break;
            }
            filterIds[i] = EXTEfx.alGenFilters();
            if (filterIds[i] == 0) {
                AL10.alDeleteSources(sourceIds[i]);
                sourceIds[i] = 0;
                Loggers.log("EmitterPool: alGenFilters failed at slot {}, stopping", i);
                break;
            }
<<<<<<< ours
=======
        for (int i = 0; i < maxSources; i++) {
            sourceIds[i] = AL10.alGenSources();
            filterIds[i] = EXTEfx.alGenFilters();
>>>>>>> theirs
=======
>>>>>>> theirs
            EXTEfx.alFilteri(filterIds[i], EXTEfx.AL_FILTER_TYPE,
                    EXTEfx.AL_FILTER_LOWPASS);

            // Configure source defaults
            AL10.alSourcef(sourceIds[i], AL10.AL_GAIN, 0.0f);
<<<<<<< ours
            AL10.alSourcef(sourceIds[i], AL10.AL_ROLLOFF_FACTOR, 0.0f);
=======
            AL10.alSourcef(sourceIds[i], AL10.AL_ROLLOFF_FACTOR, 1.0f);
>>>>>>> theirs
            AL10.alSourcef(sourceIds[i], AL10.AL_REFERENCE_DISTANCE, 2.0f);
            AL10.alSourcef(sourceIds[i], AL10.AL_MAX_DISTANCE, 48.0f);
            AL10.alSourcei(sourceIds[i], AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);

            sourceInUse[i] = false;
<<<<<<< ours
<<<<<<< ours
            allocated++;
        }
        this.maxSources = allocated;

        Loggers.log("EmitterPool: allocated {} sources (requested {})", allocated, maxSources);
=======
=======
            allocated++;
>>>>>>> theirs
        }
        this.maxSources = allocated;

<<<<<<< ours
        Loggers.log("EmitterPool: allocated {} sources", maxSources);
>>>>>>> theirs
=======
        Loggers.log("EmitterPool: allocated {} sources (requested {})", allocated, maxSources);
>>>>>>> theirs
    }

    /**
     * Tries to assign an OpenAL source to the given emitter.
     * If pool is full, steals from the lowest-priority active emitter
     * (only if the new emitter has higher priority).
     *
     * @return true if source was assigned
     */
    public boolean allocate(Emitter emitter) {
        // Find a free slot
        for (int i = 0; i < maxSources; i++) {
            if (!sourceInUse[i]) {
                assignSlot(i, emitter);
                return true;
            }
        }

        // Pool full — try to steal from lowest priority
        Emitter weakest = null;
        for (Emitter e : activeEmitters) {
            if (weakest == null || e.priorityScore() < weakest.priorityScore()) {
                weakest = e;
            }
        }

        if (weakest != null && emitter.priorityScore() > weakest.priorityScore()) {
<<<<<<< ours
            // Synchronous release: immediately stop and free the slot
            forceRelease(weakest);
=======
            deallocate(weakest);
>>>>>>> theirs
            // Find the now-free slot
            for (int i = 0; i < maxSources; i++) {
                if (!sourceInUse[i]) {
                    assignSlot(i, emitter);
                    return true;
                }
            }
        }

        return false; // Could not allocate
    }

    private void assignSlot(int slotIdx, Emitter emitter) {
        sourceInUse[slotIdx] = true;
        emitter.sourceId = sourceIds[slotIdx];
        emitter.filterId = filterIds[slotIdx];
        emitter.active = true;
        emitter.currentGain = 0f; // Fade in from silence
<<<<<<< ours
        emitter.currentFilterGainHF = 1.0f; // Reset filter state
=======
>>>>>>> theirs
        emitter.fadeProgress = 0f;
        activeEmitters.add(emitter);
    }

    /**
     * Initiates fade-out on an emitter before releasing its source.
     */
    public void deallocate(Emitter emitter) {
        if (!emitter.active) return;
        emitter.targetGain = 0f;
        emitter.fadeProgress = -1f; // Signal: "fading out, release when done"
    }

    /**
<<<<<<< ours
     * Immediately stops and releases an emitter's source (synchronous).
     * Used by priority stealing where we need the slot back NOW.
     */
    private void forceRelease(Emitter emitter) {
        if (!emitter.active) return;
        AL10.alSourceStop(emitter.sourceId);
        AL10.alSourcef(emitter.sourceId, AL10.AL_GAIN, 0f);
        if (emitter.category.sampleBased) {
            int buf = AL10.alGetSourcei(emitter.sourceId, AL10.AL_BUFFER);
            AL10.alSourcei(emitter.sourceId, AL10.AL_BUFFER, 0);
            if (buf != 0 && (samplePoolLoader == null || !samplePoolLoader.isPooledBuffer(buf))) {
                AL10.alDeleteBuffers(buf);
            }
        } else {
            AL10.alSourcei(emitter.sourceId, AL10.AL_BUFFER, 0);
        }
        for (int i = 0; i < maxSources; i++) {
            if (sourceIds[i] == emitter.sourceId) { sourceInUse[i] = false; break; }
        }
        emitter.sourceId = 0; emitter.filterId = 0; emitter.active = false;
        activeEmitters.remove(emitter);
    }

    /**
=======
>>>>>>> theirs
     * Updates all active emitters: smooth parameters, apply to OpenAL,
     * handle fade-in/fade-out, recycle finished sample-based emitters.
     */
    public void tick(float masterGain, long currentTick) {
        var iter = activeEmitters.iterator();
        while (iter.hasNext()) {
            Emitter e = iter.next();

            // Handle fade-out → release cycle
            if (e.fadeProgress < 0f) {
                e.currentGain *= (1.0f - 1.0f / FADE_OUT_TICKS);
                if (e.currentGain < 0.001f) {
                    AL10.alSourceStop(e.sourceId);
                    AL10.alSourcef(e.sourceId, AL10.AL_GAIN, 0f);
<<<<<<< ours
                    // Delete dynamically-generated buffers for sample-based emitters
                    if (e.category.sampleBased) {
                        int buf = AL10.alGetSourcei(e.sourceId, AL10.AL_BUFFER);
                        AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
                        if (buf != 0 && (samplePoolLoader == null || !samplePoolLoader.isPooledBuffer(buf))) {
                            AL10.alDeleteBuffers(buf);
                        }
                    } else {
                        AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
                    }
=======
                    AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
>>>>>>> theirs
                    for (int i = 0; i < maxSources; i++) {
                        if (sourceIds[i] == e.sourceId) { sourceInUse[i] = false; break; }
                    }
                    e.sourceId = 0; e.filterId = 0; e.active = false;
                    iter.remove();
                    continue;
                }
                e.applyToOpenAL(masterGain);
                continue;
            }

            // Fade in
            if (e.fadeProgress < 1.0f) {
                e.fadeProgress = Math.min(1.0f, e.fadeProgress + 1.0f / FADE_OUT_TICKS);
            }

            // Smooth and apply
            e.smoothStep(SMOOTH_FACTOR);
            e.applyToOpenAL(masterGain);
            e.lastActiveTick = currentTick;

<<<<<<< ours
<<<<<<< ours
=======
>>>>>>> theirs
            // Recycle finished one-shot sources, deleting dynamic buffers.
            // AL_INITIAL means "never played" (awaiting trigger) — do NOT recycle.
            // Only recycle AL_STOPPED (playback completed).
            if (e.category.sampleBased && e.active) {
                int state = AL10.alGetSourcei(e.sourceId, AL10.AL_SOURCE_STATE);
                if (state == AL10.AL_STOPPED) {
                    AL10.alSourcef(e.sourceId, AL10.AL_GAIN, 0f);
                    int buf = AL10.alGetSourcei(e.sourceId, AL10.AL_BUFFER);
                    AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
                    if (buf != 0 && (samplePoolLoader == null || !samplePoolLoader.isPooledBuffer(buf))) {
                        AL10.alDeleteBuffers(buf);
                    }
<<<<<<< ours
=======
            // Recycle finished one-shot sources
            if (e.category.sampleBased && e.active) {
                int state = AL10.alGetSourcei(e.sourceId, AL10.AL_SOURCE_STATE);
                if (state == AL10.AL_STOPPED || state == AL10.AL_INITIAL) {
                    AL10.alSourcef(e.sourceId, AL10.AL_GAIN, 0f);
                    AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
>>>>>>> theirs
=======
>>>>>>> theirs
                    for (int i = 0; i < maxSources; i++) {
                        if (sourceIds[i] == e.sourceId) { sourceInUse[i] = false; break; }
                    }
                    e.sourceId = 0; e.filterId = 0; e.active = false;
                    iter.remove();
                }
            }
        }
    }

    public int getActiveCount() {
        return activeEmitters.size();
    }

    public int getMaxSources() {
        return maxSources;
    }

    public List<Emitter> getActiveEmitters() {
<<<<<<< ours
<<<<<<< ours
        return new ArrayList<>(activeEmitters);
=======
        return activeEmitters;
>>>>>>> theirs
=======
        return new ArrayList<>(activeEmitters);
>>>>>>> theirs
    }

    public void silenceAll() {
        for (Emitter e : activeEmitters) {
            e.targetGain = 0f;
<<<<<<< ours
            e.currentGain = 0f;
            if (e.sourceId != 0) {
                AL10.alSourcef(e.sourceId, AL10.AL_GAIN, 0f);
                AL10.alSourceStop(e.sourceId);
            }
=======
>>>>>>> theirs
        }
    }

    public void shutdown() {
<<<<<<< ours
        // Stop all sources and delete any dynamic buffers before releasing resources
        for (Emitter e : activeEmitters) {
            if (e.sourceId != 0) {
                AL10.alSourceStop(e.sourceId);
                if (e.category.sampleBased) {
                    int buf = AL10.alGetSourcei(e.sourceId, AL10.AL_BUFFER);
                    AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
                    if (buf != 0 && (samplePoolLoader == null || !samplePoolLoader.isPooledBuffer(buf))) {
                        AL10.alDeleteBuffers(buf);
                    }
                } else {
                    AL10.alSourcei(e.sourceId, AL10.AL_BUFFER, 0);
                }
            }
            e.sourceId = 0; e.filterId = 0; e.active = false;
        }
        activeEmitters.clear();
        for (int i = 0; i < maxSources; i++) {
            sourceInUse[i] = false;
=======
        for (Emitter e : new ArrayList<>(activeEmitters)) {
            deallocate(e);
        }
        for (int i = 0; i < maxSources; i++) {
>>>>>>> theirs
            if (sourceIds[i] != 0) AL10.alDeleteSources(sourceIds[i]);
            if (filterIds[i] != 0) EXTEfx.alDeleteFilters(filterIds[i]);
        }
        Loggers.log("EmitterPool: shut down");
    }
}
