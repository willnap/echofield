package com.sonicether.soundphysics.eap.install;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.eap.EapSystem;
import net.minecraft.client.Minecraft;

/**
 * Manages the installation stage progression for the EchoField research study.
 * Participants move through four stages, each lasting at least 5 minutes:
 *
 * <ol>
 *   <li><strong>PLAY</strong> — Vanilla Minecraft, no EchoField processing</li>
 *   <li><strong>AUGMENTED</strong> — Full EchoField spatial audio active</li>
 *   <li><strong>BLINDFOLD</strong> — Screen blacked out, navigate by audio only</li>
 *   <li><strong>COMPARISON</strong> — A/B toggle unlocked for self-comparison</li>
 * </ol>
 *
 * <p>Stage transitions are manual (triggered by the researcher via advanceStage).
 * The minimum stage duration is enforced to ensure adequate data collection.
 *
 * <p>During BLINDFOLD, the {@link MetricsLogger} records per-tick navigation data
 * and the {@link BlindFoldRenderer} blacks out the screen.
 */
public final class InstallationManager {

    /**
     * The four stages of the installation experience.
     */
    public enum Stage {
        PLAY("Stage 1: Play (Vanilla)"),
        AUGMENTED("Stage 2: Augmented"),
        BLINDFOLD("Stage 3: Blindfold"),
        COMPARISON("Stage 4: Comparison");

        public final String label;

        Stage(String label) {
            this.label = label;
        }

        /**
         * Returns the next stage, or null if this is the final stage.
         */
        public Stage next() {
            Stage[] values = values();
            int nextOrd = ordinal() + 1;
            return nextOrd < values.length ? values[nextOrd] : null;
        }
    }

    /** Minimum ticks per stage: 6000 ticks = 5 minutes at 20 tps. */
    public static final int MIN_STAGE_TICKS = 6000;

    private final MetricsLogger metrics;
    private final BlindFoldRenderer blindfold;

    private Stage currentStage = Stage.PLAY;
    private boolean active = false;
    private long stageStartTick = 0;
    private long currentTick = 0;

    public InstallationManager() {
        this.metrics = new MetricsLogger();
        this.blindfold = new BlindFoldRenderer();
    }

    /**
     * Enables or disables installation mode. When disabled, blindfold and
     * metrics recording are stopped.
     */
    public void setActive(boolean active) {
        if (!active && this.active) {
            // Deactivating — clean up
            blindfold.setActive(false);
            if (metrics.isRecording()) {
                metrics.stopRecording();
            }
        }
        this.active = active;
    }

    /**
     * Per-tick update. Ticks the blindfold renderer and metrics logger.
     */
    public void tick(Minecraft mc) {
        if (!active) return;

        if (mc.level != null) {
            currentTick = mc.level.getGameTime();
        }

        blindfold.tick();

        if (metrics.isRecording()) {
            metrics.tick(mc);
        }
    }

    /**
     * Advances to the next installation stage. Enforces minimum stage duration.
     * Handles stage-specific setup and teardown (blindfold, metrics recording).
     *
     * @param eap the EapSystem to configure for the new stage
     * @return true if the stage was advanced, false if not yet allowed
     */
    public boolean advanceStage(EapSystem eap) {
        if (!active) return false;

        // Enforce minimum stage duration
        long elapsed = currentTick - stageStartTick;
        if (elapsed < MIN_STAGE_TICKS && stageStartTick > 0) {
            Loggers.log("InstallationManager: cannot advance, {}s remaining",
                    (MIN_STAGE_TICKS - elapsed) / 20);
            return false;
        }

        Stage next = currentStage.next();
        if (next == null) {
            Loggers.log("InstallationManager: already at final stage");
            return false;
        }

        // Teardown for current stage
        switch (currentStage) {
            case BLINDFOLD -> {
                // Leaving blindfold: stop recording, remove blindfold
                if (metrics.isRecording()) {
                    metrics.stopRecording();
                }
                blindfold.setActive(false);
            }
            default -> {}
        }

        currentStage = next;
        stageStartTick = currentTick;
        Loggers.log("InstallationManager: advanced to {}", currentStage.label);

        // Setup for new stage
        switch (currentStage) {
            case BLINDFOLD -> {
                // Entering blindfold: start recording, enable blindfold
                metrics.startRecording();
                blindfold.setActive(true);
                Loggers.log("InstallationManager: blindfold active, metrics recording started");
            }
            case COMPARISON -> {
                // Entering comparison: ensure blindfold is off, stop any recording
                blindfold.setActive(false);
                if (metrics.isRecording()) {
                    metrics.stopRecording();
                }
                Loggers.log("InstallationManager: comparison mode unlocked");
            }
            default -> {}
        }

        return true;
    }

    /**
     * Returns whether hyperreality should be active for the current stage.
     * Hyperreality is only allowed in AUGMENTED, BLINDFOLD, and COMPARISON stages.
     */
    public boolean isHyperrealityAllowed() {
        if (!active) return true; // If installation mode is off, no restriction
        return currentStage != Stage.PLAY;
    }

    /**
     * Returns whether the A/B/C comparison toggle is allowed.
     * Only unlocked in the COMPARISON stage.
     */
    public boolean isComparisonAllowed() {
        if (!active) return true; // If installation mode is off, no restriction
        return currentStage == Stage.COMPARISON;
    }

    /**
     * Returns seconds remaining in the current stage's minimum duration.
     */
    public int getSecondsRemaining() {
        long elapsed = currentTick - stageStartTick;
        long remaining = MIN_STAGE_TICKS - elapsed;
        if (remaining <= 0) return 0;
        return (int) (remaining / 20);
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public MetricsLogger getMetrics() {
        return metrics;
    }

    public BlindFoldRenderer getBlindfold() {
        return blindfold;
    }

    public boolean isActive() {
        return active;
    }
}
