package com.sonicether.soundphysics.eap.install;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders a full-screen black overlay for blindfold mode.
 * Fade in/out transition over 500ms for temporal continuity.
 */
public final class BlindFoldRenderer {

    private boolean active = false;
    private float opacity = 0f;
    private static final float FADE_SPEED = 0.1f;

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        float target = active ? 1.0f : 0.0f;
        opacity += (target - opacity) * FADE_SPEED;
        if (Math.abs(opacity - target) < 0.01f) {
            opacity = target;
        }
    }

    public void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (opacity <= 0.001f) return;

        int alpha = (int) (opacity * 255);
        int color = (alpha << 24);

        graphics.fill(0, 0, screenWidth, screenHeight, color);

        if (opacity > 0.95f) {
            String text = "Audio Only \u2014 Navigate by sound";
            graphics.drawCenteredString(
                    net.minecraft.client.Minecraft.getInstance().font,
                    text,
                    screenWidth / 2,
                    screenHeight / 2,
                    0x88FFFFFF);
        }
    }

    public float getOpacity() {
        return opacity;
    }
}
