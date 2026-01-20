package com.sonicether.soundphysics.eap.emitter;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Controls timing for triggered (sample-based) emitters.
 * Each emitter gets a countdown timer; when it reaches zero, the emitter fires.
 */
public final class SampleEmitterScheduler {

    private final Map<Emitter, Integer> countdowns = new IdentityHashMap<>();

    /**
     * Check if an emitter should trigger this tick.
     * @return true if the emitter should play a new sound
     */
    public boolean shouldTrigger(Emitter e, EnvironmentConditions cond, Random rng) {
        int remaining = countdowns.getOrDefault(e, -1);
        if (remaining < 0) {
            // First discovery: short initial countdown so emitters start quickly
            // after scanner finds them (avoids multi-second silence after teleport)
            remaining = computeInitialInterval(e.category, rng);
            countdowns.put(e, remaining);
        }
        if (--remaining <= 0) {
            countdowns.put(e, computeInterval(e.category, cond, rng));
            return true;
        }
        countdowns.put(e, remaining);
        return false;
    }

    /**
     * Remove an emitter from tracking (when deallocated).
     */
    public void remove(Emitter e) {
        countdowns.remove(e);
    }

    /**
     * Clear all tracking state.
     */
    public void clear() {
        countdowns.clear();
    }

    /**
     * Short initial countdown for newly-discovered emitters so they start quickly.
     * Staggered randomly to prevent all emitters firing on the same tick.
     */
    private int computeInitialInterval(EmitterCategory category, Random rng) {
        return switch (category) {
            case BIRD -> 5 + rng.nextInt(30);          // 0.25-1.75s
            case INSECT -> 5 + rng.nextInt(20);         // 0.25-1.25s
            case WATER_DRIP -> 3 + rng.nextInt(15);     // 0.15-0.9s
            case CAVE_AMBIENT -> 5 + rng.nextInt(25);   // 0.25-1.5s
            case MECHANICAL -> 5 + rng.nextInt(20);     // 0.25-1.25s
            default -> 5 + rng.nextInt(30);             // 0.25-1.75s
        };
    }

    /**
     * Compute the interval (in ticks at 20 TPS) before the next trigger.
     */
    private int computeInterval(EmitterCategory category, EnvironmentConditions cond, Random rng) {
        return switch (category) {
            case BIRD -> {
                // Base: 60-200 ticks (3-10s)
                // Dawn chorus: much shorter intervals for dense overlapping calls
                // Night: effectively disabled (gain will be 0, but also space out triggers)
                float dawn = cond.dawnChorus();
                int base;
                if (dawn > 0.5f) {
                    base = 10 + rng.nextInt(25);  // Peak dawn chorus: 10-35 ticks (0.5-1.75s)
                } else if (dawn > 0.1f) {
                    base = 20 + rng.nextInt(40);  // Dawn chorus: 20-60 ticks (1-3s)
                } else {
                    base = 60 + rng.nextInt(140); // Normal: 60-200 ticks (3-10s)
                }
                yield base;
            }
            case INSECT -> {
                // Base: 40-120 ticks (2-6s)
                // Night: much shorter intervals for dense overlapping chirps
                float nightFactor = 1.0f - cond.daylight();
                if (nightFactor > 0.5f) {
                    yield 10 + rng.nextInt(35); // Night: 10-45 ticks (0.5-2.25s)
                }
                yield 40 + rng.nextInt(80); // Day: 40-120 ticks (2-6s)
            }
            case FROG -> {
                // Base: 60-180 ticks (3-9s)
                // The eagerness model (Task 8/12) can override this
                yield 60 + rng.nextInt(120);
            }
            case BAT -> {
                // Sparse chirps: 80-200 ticks (4-10s)
                // Only active underground (gain check handles this)
                yield 80 + rng.nextInt(120);
            }
            case WATER_DRIP -> {
                // Constant: 20-100 ticks (1-5s)
                yield 20 + rng.nextInt(80);
            }
            case WATER_RAIN -> {
                // Only triggers when raining (gain check handles this)
                // Short interval: 10-30 ticks (0.5-1.5s)
                yield cond.isRaining() ? 10 + rng.nextInt(20) : 200;
            }
            case CAVE_AMBIENT -> {
                // Sparse: scales with room size. Real caves have long silences
                // punctuated by occasional events. Larger rooms need longer intervals
                // because they have more emitters, and with short intervals multiple
                // emitters overlap to create continuous wash (0 silence regions).
                // roomSize is averageReturnDistance in blocks (~5 for small, ~15+ for large).
                // Small room: 100-300 ticks (5-15s) — intimate, events closer together
                // Large room: 160-460 ticks (8-23s) — spacious, events more separated
                float roomScale = Math.max(1.0f, 1.0f + (cond.roomSize() - 5.0f) * 0.06f);
                roomScale = Math.min(1.6f, roomScale);
                int base = (int) (100 * roomScale);
                int range = (int) (200 * roomScale);
                yield base + rng.nextInt(Math.max(1, range));
            }
            case MECHANICAL -> {
                // Fire crackling: 60-160 ticks (3-8s)
                yield 60 + rng.nextInt(100);
            }
            case WATER_STILL -> {
                yield 100 + rng.nextInt(200);
            }
            default -> 100; // Non-sample categories should not reach here
        };
    }
}
