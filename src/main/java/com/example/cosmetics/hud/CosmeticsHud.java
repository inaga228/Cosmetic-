package com.example.cosmetics.hud;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.GuiDraw;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay panel in the top-left that lists the currently active effects.
 * Each entry has a small coloured chip matching the feature's colour setting.
 */
public final class CosmeticsHud {
    private static float shownAlpha = 0.0F;

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CosmeticsState s = CosmeticsState.get();
        boolean want = s.isOn(FeatureType.COSMETICS_HUD);
        float target = want ? 1.0F : 0.0F;
        shownAlpha += (target - shownAlpha) * 0.12F;
        if (shownAlpha < 0.01F) return;

        PlayerEntity p = mc.player;
        FontRenderer f = mc.font;

        List<FeatureType> lines = new ArrayList<>();
        for (FeatureType ft : s.active()) {
            if (ft == FeatureType.COSMETICS_HUD) continue;
            if (ft == FeatureType.TARGET_HUD) continue;
            if (ft == FeatureType.WATERMARK) continue;
            lines.add(ft);
        }

        int padding = 8;
        int lineH = 11;
        String playerName = p.getGameProfile().getName();
        int w = Math.max(140, f.width(playerName) + 48);
        for (FeatureType ft : lines) {
            w = Math.max(w, f.width(ft.displayName) + padding * 2 + 18);
        }
        int bodyLines = lines.isEmpty() ? 1 : lines.size();
        int h = padding + 12 + 5 + bodyLines * lineH + padding;

        int x = 8, y = 8;

        long now = System.currentTimeMillis();
        float hueBase = (now % 6000L) / 6000.0F;
        int accent = GuiDraw.hsv(hueBase, 0.55F, 1.0F);

        GuiDraw.roundedPanelAccent(ms, x, y, w, h, shownAlpha, accent);

        int a = Math.max(0, Math.min(255, (int)(shownAlpha * 255)));
        int nameCol = (a << 24) | 0xFFFFFF;
        int emptyCol = (a << 24) | 0x777788;

        // Player name
        f.drawShadow(ms, playerName, x + padding, y + padding, nameCol);

        // Count chip on the right ("3 active")
        if (!lines.isEmpty()) {
            String cnt = lines.size() + " active";
            int cnA = Math.max(0, Math.min(255, (int)(shownAlpha * 180)));
            f.drawShadow(ms, cnt, x + w - padding - f.width(cnt),
                    y + padding, (cnA << 24) | 0xBAA4E8);
        }

        // Divider under header
        GuiDraw.divider(ms, x + padding, y + padding + 11, w - padding * 2,
                shownAlpha, 0x8A5CFF);

        // Body
        int ly = y + padding + 15;
        if (lines.isEmpty()) {
            f.drawShadow(ms, "No effects active", x + padding, ly, emptyCol);
        } else {
            int i = 0;
            for (FeatureType ft : lines) {
                // Chip colour from feature settings
                FeatureSettings fs = s.settings(ft);
                int chipRgb = toRgb(fs.colorR, fs.colorG, fs.colorB);

                // Soft hue-walk for features without explicit colour
                if (chipRgb == 0xFFFFFF) {
                    chipRgb = GuiDraw.hsv(hueBase + i * 0.08F, 0.6F, 1.0F);
                }

                GuiDraw.chip(ms, x + padding, ly + 1, 6, 6, chipRgb, shownAlpha);

                int textRgb = ((a << 24) | 0xD8CFFB);
                f.drawShadow(ms, ft.displayName, x + padding + 12, ly, textRgb);
                ly += lineH;
                i++;
            }
        }
    }

    private static int toRgb(float r, float g, float b) {
        int ir = clamp((int)(r * 255));
        int ig = clamp((int)(g * 255));
        int ib = clamp((int)(b * 255));
        return (ir << 16) | (ig << 8) | ib;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private CosmeticsHud() {}
}
