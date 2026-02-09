package com.sonicether.soundphysics;

import com.sonicether.soundphysics.eap.EapSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricSoundPhysicsMod extends SoundPhysicsMod implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        init();
    }

    @Override
    public void onInitializeClient() {
        initClient();

        // Register EAP tick handler — lazy initialization on first tick when configs are ready
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (SoundPhysicsMod.EAP_CONFIG == null || SoundPhysicsMod.REFLECTIVITY_CONFIG == null) {
                return;
            }
            EapSystem eap = EapSystem.getInstance();
            eap.onClientTick(minecraft);
        });
    }

    @Override
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

}
