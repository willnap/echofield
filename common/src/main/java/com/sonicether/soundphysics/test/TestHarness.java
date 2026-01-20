package com.sonicether.soundphysics.test;

import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.eap.EapSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.InputWithModifiers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test harness for autonomous audio testing.
 * Activated by ECHOFIELD_TEST_MODE=true environment variable.
 *
 * Instance isolation:
 *   ECHOFIELD_INSTANCE_ID  -- unique identifier (default "0")
 *   ECHOFIELD_TEST_PORT    -- TCP command port (default "4711")
 *
 * Lifecycle:
 * 1. Waits for MC to reach title screen (INIT phase, 3s delay)
 * 2. Programmatically loads "echofield_test" world
 * 3. Waits for world + EAP system to initialize (5s stabilization)
 * 4. Starts TCP command server on configured port
 * 5. Writes ready signal to /tmp/echofield_test_ready_<instance_id>
 */
public class TestHarness {

    private static final String INSTANCE_ID = System.getenv("ECHOFIELD_INSTANCE_ID") != null
            ? System.getenv("ECHOFIELD_INSTANCE_ID") : "0";
    private static final String READY_SIGNAL_PATH = "/tmp/echofield_test_ready_" + INSTANCE_ID;
    private static final String WORLD_NAME = System.getenv("ECHOFIELD_WORLD_NAME") != null
            ? System.getenv("ECHOFIELD_WORLD_NAME") : "echofield_test";
    private static final int COMMAND_PORT;
    private static final int INIT_WAIT_TICKS = 60;          // 3 seconds before loading world
    private static final int STABILIZATION_TICKS = 100;     // 5 seconds after world loaded

    static {
        String portEnv = System.getenv("ECHOFIELD_TEST_PORT");
        int port = 4711;
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                // fall through to default
            }
        }
        COMMAND_PORT = port;
    }

    private enum State { INIT, LOADING_WORLD, WAITING_EAP, READY }

    private static State state = State.INIT;
    private static TestCommandServer commandServer;
    private static int initTicks = 0;
    private static int loadRetries = 0;
    private static int stableTicks = 0;

    public static boolean isTestMode() {
        return "true".equals(System.getenv("ECHOFIELD_TEST_MODE"));
    }

    /**
     * Called every client tick from FabricSoundPhysicsMod.
     * Manages the test harness lifecycle as a state machine.
     */
    public static void onClientTick(Minecraft mc) {
        if (!isTestMode() || state == State.READY) return;

        switch (state) {
            case INIT:
                initTicks++;
                if (initTicks < INIT_WAIT_TICKS) return;

                if (mc.level != null && mc.player != null) {
                    // Already in a world (e.g., --quickPlaySingleplayer worked)
                    Loggers.log("TestHarness: Already in world, skipping world load");
                    state = State.WAITING_EAP;
                    return;
                }

                // Load the test world programmatically
                Loggers.log("TestHarness: Loading world '{}' (game dir: {})", WORLD_NAME, mc.gameDirectory.getAbsolutePath());
                try {
                    mc.createWorldOpenFlows().openWorld(WORLD_NAME, () -> {
                        mc.setScreen(null);
                    });
                    state = State.LOADING_WORLD;
                } catch (Exception e) {
                    loadRetries++;
                    if (loadRetries <= 3) {
                        Loggers.log("TestHarness: Failed to load world (attempt {}): {} ({})",
                                loadRetries, e.getClass().getSimpleName(), e.getMessage());
                    }
                    // Stay in INIT state, will retry next tick
                }
                break;

            case LOADING_WORLD:
                // Auto-confirm any blocking dialogs (experimental settings, version mismatch)
                if (mc.screen != null && mc.level == null) {
                    autoConfirmScreen(mc);
                }
                if (mc.level != null && mc.player != null) {
                    Loggers.log("TestHarness: World loaded, waiting for EAP stabilization");
                    stableTicks = 0;
                    state = State.WAITING_EAP;
                }
                break;

            case WAITING_EAP:
                if (mc.level == null || mc.player == null) {
                    // World was unloaded somehow
                    stableTicks = 0;
                    return;
                }
                if (!EapSystem.isSoundEngineReady()) {
                    stableTicks = 0;
                    return;
                }
                EapSystem eap = EapSystem.getInstanceOrNull();
                if (eap == null) {
                    stableTicks = 0;
                    return;
                }

                stableTicks++;
                if (stableTicks < STABILIZATION_TICKS) return;

                // Start command server
                if (commandServer == null) {
                    try {
                        commandServer = new TestCommandServer(COMMAND_PORT, mc);
                        commandServer.start();
                        Loggers.log("TestHarness [instance=" + INSTANCE_ID + "]: Command server started on port " + COMMAND_PORT);
                    } catch (IOException e) {
                        Loggers.log("TestHarness: Failed to start command server: " + e.getMessage());
                        return;
                    }
                }

                writeReadySignal(mc, eap);
                state = State.READY;
                Loggers.log("TestHarness [instance=" + INSTANCE_ID + "]: READY -- agent can connect to port " + COMMAND_PORT);
                break;

            default:
                break;
        }
    }

    /**
     * Auto-click through blocking dialogs (experimental settings, version mismatch, backup).
     * Finds buttons containing "I know what" or confirmation-like text and clicks them.
     */
    private static void autoConfirmScreen(Minecraft mc) {
        if (mc.screen == null) return;
        for (GuiEventListener child : mc.screen.children()) {
            if (child instanceof Button button) {
                String text = button.getMessage().getString().toLowerCase();
                if (text.contains("i know what") || text.contains("proceed") || text.contains("continue")) {
                    Loggers.log("TestHarness: Auto-confirming dialog: " + button.getMessage().getString());
                    button.onPress(new InputWithModifiers() {
                        @Override public int input() { return 0; }
                        @Override public int modifiers() { return 0; }
                    });
                    return;
                }
            }
        }
    }

    private static void writeReadySignal(Minecraft mc, EapSystem eap) {
        boolean hrtfActive = eap.getHrtfManager().isHrtfActive();
        boolean loopbackCapture = LoopbackAudioCapture.isInitialized();
        String playerPos = String.format("[%.1f, %.1f, %.1f]",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());

        String json = "{\n" +
                "  \"ready\": true,\n" +
                "  \"instance_id\": \"" + INSTANCE_ID + "\",\n" +
                "  \"port\": " + COMMAND_PORT + ",\n" +
                "  \"hrtf_active\": " + hrtfActive + ",\n" +
                "  \"eap_initialized\": true,\n" +
                "  \"loopback_capture\": " + loopbackCapture + ",\n" +
                "  \"player_position\": " + playerPos + ",\n" +
                "  \"world\": \"" + (mc.level != null ? mc.level.dimension().toString() : "unknown") + "\"\n" +
                "}";

        try {
            Files.writeString(Path.of(READY_SIGNAL_PATH), json);
        } catch (IOException e) {
            Loggers.log("TestHarness: Failed to write ready signal: " + e.getMessage());
        }
    }

    /**
     * Called on mod shutdown / disconnect.
     */
    public static void shutdown() {
        if (commandServer != null) {
            commandServer.stopServer();
            commandServer = null;
        }
        LoopbackAudioCapture.shutdown();
        try {
            Files.deleteIfExists(Path.of(READY_SIGNAL_PATH));
        } catch (IOException ignored) {}
        state = State.INIT;
        initTicks = 0;
        stableTicks = 0;
        Loggers.log("TestHarness [instance=" + INSTANCE_ID + "]: Shutdown complete");
    }
}
