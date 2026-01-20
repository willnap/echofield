package com.sonicether.soundphysics.test;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.eap.EapSystem;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple TCP command server for receiving Minecraft commands from the test agent.
 *
 * Protocol:
 *   - Connect via TCP to port 4711
 *   - Send commands as newline-delimited strings
 *   - Receive "OK\n" or status JSON for each command
 *
 * Special commands:
 *   STATUS  -- returns JSON with EAP status, HRTF, player position
 *   QUIT    -- saves world and exits Minecraft
 *
 * Regular commands (e.g., "tp @p 1000 80 1000"):
 *   - Dispatched to Minecraft's command system on the render thread
 *   - Leading "/" is stripped if present
 */
public class TestCommandServer extends Thread {

    private final int port;
    private final Minecraft mc;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public TestCommandServer(int port, Minecraft mc) throws IOException {
        super("EchoField-TestCommandServer");
        this.port = port;
        this.mc = mc;
        this.serverSocket = new ServerSocket(port);
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (running) {
                Socket client = serverSocket.accept();
                // Handle each client in a separate thread
                Thread handler = new Thread(() -> handleClient(client), "EchoField-TestClient");
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                Loggers.log("TestCommandServer: Accept error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;

                String response = executeCommand(cmd);
                out.println(response);

                if ("QUIT".equalsIgnoreCase(cmd)) break;
            }
        } catch (IOException e) {
            // Client disconnected -- normal
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private String executeCommand(String cmd) {
        if ("STATUS".equalsIgnoreCase(cmd)) {
            return getStatusJson();
        }

        if ("EAP_PARAMS".equalsIgnoreCase(cmd)) {
            EapSystem eap = EapSystem.getInstanceOrNull();
            if (eap == null) return "{\"error\":\"EAP not initialized\"}";
            return eap.getLastReverbParamsJson();
        }

        if ("QUIT".equalsIgnoreCase(cmd)) {
            // Shut down loopback capture BEFORE stopping MC to prevent SIGSEGV
            // (render thread must stop before OpenAL device is destroyed)
            LoopbackAudioCapture.shutdown();
            mc.execute(() -> {
                Loggers.log("TestCommandServer: QUIT received, stopping Minecraft");
                mc.stop();
            });
            return "OK:quit";
        }

        // --- Audio capture commands (handled directly, not on render thread) ---

        if ("CAPTURE_START".equalsIgnoreCase(cmd)) {
            if (!LoopbackAudioCapture.isInitialized()) {
                return "ERROR:loopback not initialized";
            }
            LoopbackAudioCapture.startCapture();
            return "OK:capturing";
        }

        if (cmd.toUpperCase().startsWith("CAPTURE_STOP ")) {
            String outputPath = cmd.substring("CAPTURE_STOP ".length()).trim();
            if (!LoopbackAudioCapture.isCapturing()) {
                return "ERROR:not capturing";
            }
            try {
                String path = LoopbackAudioCapture.stopCapture(outputPath);
                return "OK:" + path;
            } catch (IOException e) {
                Loggers.log("TestCommandServer: CAPTURE_STOP error: " + e.getMessage());
                return "ERROR:" + e.getMessage();
            }
        }

        if (cmd.toUpperCase().startsWith("CAPTURE ")) {
            String[] parts = cmd.split("\\s+", 3);
            if (parts.length < 3) {
                return "ERROR:usage: CAPTURE <duration_ms> <output_path>";
            }
            int durationMs;
            try {
                durationMs = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return "ERROR:invalid duration: " + parts[1];
            }
            String outputPath = parts[2];

            if (!LoopbackAudioCapture.isInitialized()) {
                return "ERROR:loopback not initialized";
            }

            LoopbackAudioCapture.startCapture();
            try {
                Thread.sleep(durationMs);
            } catch (InterruptedException e) {
                // Continue to stop capture even if interrupted
            }
            try {
                String path = LoopbackAudioCapture.stopCapture(outputPath);
                return "OK:" + path;
            } catch (IOException e) {
                Loggers.log("TestCommandServer: CAPTURE error: " + e.getMessage());
                return "ERROR:" + e.getMessage();
            }
        }

        // Regular Minecraft command
        String finalCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;

        // Execute on render thread (required for all MC API calls)
        CompletableFuture<Void> future = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.sendCommand(finalCmd);
                }
            } catch (Exception e) {
                Loggers.log("TestCommandServer: Command error: " + e.getMessage());
            }
            future.complete(null);
        });

        // Wait briefly for command to be dispatched
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "ERR:timeout";
        }

        return "OK";
    }

    private String getStatusJson() {
        EapSystem eap = EapSystem.getInstanceOrNull();
        boolean hrtf = eap != null && eap.getHrtfManager().isHrtfActive();
        boolean eapReady = eap != null;
        double px = mc.player != null ? mc.player.getX() : 0;
        double py = mc.player != null ? mc.player.getY() : 0;
        double pz = mc.player != null ? mc.player.getZ() : 0;

        return String.format(
                "{\"ready\":true,\"hrtf\":%s,\"eap\":%s,\"pos\":[%.1f,%.1f,%.1f]}",
                hrtf, eapReady, px, py, pz
        );
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
    }
}
