package com.example.cosmetics.gui;

import com.example.cosmetics.client.BindManager;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

/**
 * "Press any key" overlay to assign a keybind to a feature.
 * Opened from SettingsScreen.
 * ESC = cancel, DELETE = clear bind.
 */
public class BindKeyScreen extends Screen {

    private final FeatureType feature;
    private final Screen returnTo;
    private long openedAtMs;

    // Pulse animation for the prompt text
    private int pulse = 0;

    public BindKeyScreen(FeatureType feature, Screen returnTo) {
        super(new StringTextComponent("Bind Key"));
        this.feature  = feature;
        this.returnTo = returnTo;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        pulse++;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        // Dim background
        float anim = animProgress();
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 180) << 24);

        int panelW = 280, panelH = 130;
        int px = (this.width  - panelW) / 2;
        int py = (this.height - panelH) / 2;

        GuiDraw.roundedPanel(ms, px, py, panelW, panelH, anim);

        int a = clamp((int)(anim * 255));

        // Title
        fillGradient(ms, px + 2, py + 2, px + panelW - 2, py + 36,
                (a << 24) | 0x1A1430, (a << 24) | 0x120E22);
        drawCenteredString(ms, this.font,
                "Bind: " + feature.displayName,
                px + panelW / 2, py + 14, (a << 24) | 0xFFFFFF);

        // Divider
        int div = (clamp((int)(anim * 180)) << 24) | 0x8A5CFF;
        fill(ms, px + panelW / 2 - 60, py + 28, px + panelW / 2 + 60, py + 30, div);

        // Current bind label
        int curKey = BindManager.get().getBind(feature);
        String curText = "Current: " + BindManager.keyName(curKey);
        drawCenteredString(ms, this.font, curText,
                px + panelW / 2, py + 44, (a << 24) | 0xAA99DD);

        // Pulsing "press key" text
        float bright = 0.6F + 0.4F * (float) Math.sin(pulse * 0.08F);
        int promptA = clamp((int)(anim * bright * 255));
        drawCenteredString(ms, this.font, "Press any key to bind...",
                px + panelW / 2, py + 64, (promptA << 24) | 0xE0D8FF);

        // ESC / DEL hints
        int hintA = clamp((int)(anim * 140));
        drawCenteredString(ms, this.font, "ESC = cancel    DEL = clear bind",
                px + panelW / 2, py + panelH - 18, (hintA << 24) | 0x887799);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            // ESC = cancel, go back
            Minecraft.getInstance().setScreen(returnTo);
            return true;
        }
        if (key == 261) {
            // DELETE = clear bind
            BindManager.get().clearBind(feature);
            Minecraft.getInstance().setScreen(returnTo);
            return true;
        }
        // Any other key = set bind
        BindManager.get().setBind(feature, key);
        Minecraft.getInstance().setScreen(returnTo);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return true; // absorb all char input
    }

    private float animProgress() {
        float t = Math.min(1F, (System.currentTimeMillis() - openedAtMs) / 180F);
        return 1F - (1F - t) * (1F - t);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
