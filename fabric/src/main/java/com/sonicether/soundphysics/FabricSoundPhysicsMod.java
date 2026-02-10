package com.sonicether.soundphysics;

import com.sonicether.soundphysics.eap.EapSystem;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

import java.nio.file.Path;

public class FabricSoundPhysicsMod extends SoundPhysicsMod implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        init();
    }

    @Override
    public void onInitializeClient() {
        initClient();

        // Register EAP A/B toggle keybind (F7)
        KeyMapping eapToggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.sound_physics_remastered.eap_toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_F7,
                KeyMapping.Category.MISC
        ));

        // Register EAP tick handler — lazy initialization on first tick when configs are ready
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (SoundPhysicsMod.EAP_CONFIG == null || SoundPhysicsMod.REFLECTIVITY_CONFIG == null) {
                return;
            }
            EapSystem eap = EapSystem.getInstance();
            eap.onClientTick(minecraft);

            // Check EAP A/B toggle keybind
            if (eapToggle.consumeClick()) {
                eap.toggleEnabled();
            }
        });

        // Register HUD overlay callback for EAP debug display
        HudRenderCallback.EVENT.register((gui, deltaTracker) -> {
            EapSystem eap = EapSystem.getInstanceOrNull();
            if (eap != null) {
                eap.getDebugRenderer().renderHudOverlay(gui);
            }
        });
    }

    @Override
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

}
