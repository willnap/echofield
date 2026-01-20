package com.sonicether.soundphysics.eap.install;

import com.sonicether.soundphysics.Loggers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Logs per-tick player metrics during blindfold navigation.
 * Outputs CSV for analysis in the report.
 */
public final class MetricsLogger {

    private final List<String> buffer = new ArrayList<>();
    private boolean recording = false;
    private Vec3 lastPos;
    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private int hesitationTicks = 0;
    private int wallCollisions = 0;
    private String sessionId;

    private static final float HESITATION_THRESHOLD = 0.05f;
    private static final int HESITATION_MIN_TICKS = 20;

    public void startRecording() {
        sessionId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        buffer.clear();
        buffer.add("tick,x,y,z,yaw,pitch,speed,yaw_delta,pitch_delta,hesitating,wall_collision");
        recording = true;
        lastPos = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        } else {
            lastYaw = 0f;
            lastPitch = 0f;
        }
        hesitationTicks = 0;
        wallCollisions = 0;
        Loggers.log("MetricsLogger: recording started (session {})", sessionId);
    }

    public void tick(Minecraft mc) {
        if (!recording || mc.player == null || mc.level == null) return;

        Vec3 pos = mc.player.position();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        long tick = mc.level.getGameTime();

        float speed = 0f;
        if (lastPos != null) {
            speed = (float) pos.distanceTo(lastPos);
        }

        boolean hesitating = false;
        if (speed < HESITATION_THRESHOLD) {
            hesitationTicks++;
            if (hesitationTicks >= HESITATION_MIN_TICKS) {
                hesitating = true;
            }
        } else {
            hesitationTicks = 0;
        }

        boolean wallCollision = false;
        if (lastPos != null && speed < 0.01f) {
            Vec2 moveVector = mc.player.input.getMoveVector();
            float inputForward = moveVector.y;
            float inputStrafe = moveVector.x;
            if (Math.abs(inputForward) > 0.1f || Math.abs(inputStrafe) > 0.1f) {
                wallCollision = true;
                wallCollisions++;
            }
        }

        float yawDelta = yaw - lastYaw;
        float pitchDelta = pitch - lastPitch;
        if (yawDelta > 180f) yawDelta -= 360f;
        if (yawDelta < -180f) yawDelta += 360f;

        buffer.add(String.format("%d,%.3f,%.3f,%.3f,%.1f,%.1f,%.4f,%.2f,%.2f,%b,%b",
                tick, pos.x, pos.y, pos.z, yaw, pitch, speed,
                yawDelta, pitchDelta, hesitating, wallCollision));

        lastPos = pos;
        lastYaw = yaw;
        lastPitch = pitch;
    }

    public void stopRecording() {
        if (!recording) return;
        recording = false;

        File dir = new File("echofield_metrics");
        dir.mkdirs();
        File file = new File(dir, "session_" + sessionId + ".csv");

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String line : buffer) {
                writer.println(line);
            }
            Loggers.log("MetricsLogger: saved {} ticks to {}", buffer.size() - 1, file.getPath());
        } catch (IOException e) {
            Loggers.log("MetricsLogger: failed to save: {}", e.getMessage());
        }
    }

    public boolean isRecording() { return recording; }
    public int getWallCollisions() { return wallCollisions; }
    public int getRecordedTicks() { return Math.max(0, buffer.size() - 1); }
}
