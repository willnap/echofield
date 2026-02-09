package com.sonicether.soundphysics.mixin;

import com.sonicether.soundphysics.SoundPhysicsMod;
import com.sonicether.soundphysics.debug.RaycastRenderer;
import com.sonicether.soundphysics.eap.EapSystem;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

    @Shadow
    @Final
    private List<DebugRenderer.SimpleDebugRenderer> renderers;

    @Inject(method = "refreshRendererList", at = @At("RETURN"))
    private void refreshRendererList(CallbackInfo ci) {
        if (SoundPhysicsMod.CONFIG == null) {
            return;
        }
        //TODO Check if this gets called when the config value changed
        if (SoundPhysicsMod.CONFIG.renderSoundBounces.get() || SoundPhysicsMod.CONFIG.renderOcclusion.get()) {
            renderers.add(new RaycastRenderer());
        }
        // Add EAP debug renderer if enabled
        if (SoundPhysicsMod.EAP_CONFIG != null && SoundPhysicsMod.EAP_CONFIG.debugRays.get()) {
            EapSystem eap = EapSystem.getInstanceOrNull();
            if (eap != null) {
                renderers.add(eap.getDebugRenderer());
            }
        }
    }

}
