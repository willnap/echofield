package com.sonicether.soundphysics.mixin;

import com.mojang.blaze3d.audio.Channel;
import com.sonicether.soundphysics.Loggers;
import com.sonicether.soundphysics.SoundPhysics;
import com.sonicether.soundphysics.SoundPhysicsMod;
import com.sonicether.soundphysics.eap.EapSystem;
import com.sonicether.soundphysics.eap.EnvironmentProfile;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public class SourceMixin {

    @Shadow
    @Final
    private int source;

    @Unique
    private Vec3 pos;

    @Inject(method = "setSelfPosition", at = @At("HEAD"))
    private void setSelfPosition(Vec3 poss, CallbackInfo ci) {
        this.pos = poss;
    }

    @Inject(method = "play", at = @At("HEAD"))
    private void play(CallbackInfo ci) {
        if (pos == null) {
            return;
        }
        SoundPhysics.onPlaySound(pos.x, pos.y, pos.z, source);
        Loggers.logALError("Sound play injector");

        // EAP early reflection hook
        EapSystem eap = EapSystem.getInstanceOrNull();
        if (eap != null) {
            eap.onSoundPlay(source, pos);

            // Per-source D/R ratio override
            if (eap.getDRProcessor().isEnabled()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null && mc.level != null) {
                    EnvironmentProfile profile = eap.getProfiler().getCurrentProfile();
                    if (profile != null) {
                        float critDist = profile.criticalDistance();
                        long tick = mc.level.getGameTime();
                        eap.getDRProcessor().applyDR(source,
                                (float) pos.x, (float) pos.y, (float) pos.z,
                                (float) mc.player.getX(), (float) mc.player.getY(),
                                (float) mc.player.getZ(),
                                critDist, tick);
                    }
                }
            }
        }
    }

    @ModifyVariable(method = "linearAttenuation", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float injected(float attenuation) {
        if (!SoundPhysicsMod.CONFIG.enabled.get()) {
            return attenuation;
        }
        return attenuation / SoundPhysicsMod.CONFIG.attenuationFactor.get();
    }

    @Inject(method = "linearAttenuation", at = @At("RETURN"))
    private void linearAttenuation2(float attenuation, CallbackInfo ci) {
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, attenuation / 2F);
    }

}
