package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.player.ClientPlayerEntity;

/**
 * Tiny HUD overlays for the Utility tab: FPS counter and Coords HUD.
 * Both are lightweight — a single {@code fill} rectangle + one line of
 * text — and only render when their feature is enabled.
 */
public final class UtilityHud extends AbstractGui {

    private static final UtilityHud I = new UtilityHud();

    // Smooth FPS (updated once per second from Minecraft's own counter).
    private static int displayedFps = 60;
    private static long lastFpsUpdate = 0L;

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug) return;   // vanilla F3 screen is already showing

        CosmeticsState s = CosmeticsState.get();
        boolean fps = s.isOn(FeatureType.FPS_COUNTER);
        boolean coords = s.isOn(FeatureType.COORDS_HUD);
        if (!fps && !coords) return;

        FontRenderer font = mc.font;
        long now = System.currentTimeMillis();

        if (fps) {
            if (now - lastFpsUpdate > 250L) {
                displayedFps = readFps();
                lastFpsUpdate = now;
            }
            FeatureSettings f = s.settings(FeatureType.FPS_COUNTER);
            int col = 0xFF000000 | (f.argb() & 0x00FFFFFF);
            int x = Math.round(f.offsetX);
            int y = Math.round(f.offsetY);
            drawChip(ms, font, x, y, displayedFps + " fps", col);
        }

        if (coords) {
            ClientPlayerEntity p = mc.player;
            if (p != null) {
                FeatureSettings f = s.settings(FeatureType.COORDS_HUD);
                int col = 0xFF000000 | (f.argb() & 0x00FFFFFF);
                int x = Math.round(f.offsetX);
                int y = Math.round(f.offsetY);
                String text = String.format("XYZ: %.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ());
                drawChip(ms, font, x, y, text, col);
            }
        }
    }

    /**
     * Reads the current FPS in a mapping-agnostic way. 1.16.5 has a public
     * static field {@code Minecraft.fps}; later/other mappings expose a
     * {@code getFrameRate()} or {@code getFps()} accessor. Try all of them.
     */
    private static int readFps() {
        // 1. Public static field Minecraft.fps (MCP 1.16.5)
        try {
            java.lang.reflect.Field f = Minecraft.class.getField("fps");
            Object v = f.get(null);
            if (v instanceof Integer) return Math.max(0, (Integer) v);
        } catch (Throwable ignored) {}

        // 2. Method getFrameRate() / getFps()
        for (String name : new String[]{"getFrameRate", "getFps"}) {
            try {
                java.lang.reflect.Method m = Minecraft.class.getMethod(name);
                Object v = m.invoke(Minecraft.getInstance());
                if (v instanceof Integer) return Math.max(0, (Integer) v);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private static void drawChip(MatrixStack ms, FontRenderer font,
                                 int x, int y, String text, int textColor) {
        int w = font.width(text) + 6;
        int h = 11;
        I.fill(ms, x, y, x + w, y + h, 0x90000000);
        I.fill(ms, x, y, x + 2, y + h, 0xFF8A5CFF);
        font.drawShadow(ms, text, x + 4, y + 2, textColor);
    }

    private UtilityHud() {}
}
