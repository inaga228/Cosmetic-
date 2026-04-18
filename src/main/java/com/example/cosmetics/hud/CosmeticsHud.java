package com.example.cosmetics.hud;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.GuiDraw;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-left HUD panel showing the active cosmetics.
 *
 * Styles (via Caps.STYLE):
 *   0 = NEON     — purple gradient (classic)
 *   1 = MINIMAL  — flat dark bar with a single accent
 *   2 = OCEAN    — cyan/blue glow theme
 *   3 = FIRE     — orange/red wave theme with moving underline
 *   4 = MINT     — green neon theme
 */
public final class CosmeticsHud {

    public static final String[] STYLE_NAMES = { "Neon", "Minimal", "Ocean", "Fire", "Mint" };

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
        FeatureSettings fs = s.settings(FeatureType.COSMETICS_HUD);

        List<String> lines = new ArrayList<>();
        for (FeatureType ft : s.active()) {
            if (ft == FeatureType.COSMETICS_HUD) continue;
            if (ft == FeatureType.TARGET_HUD) continue;
            lines.add(ft.displayName);
        }

        int padding = 7;
        int lineH = 11;
        // Use getName() to avoid depending on the concrete subclass that exposes getGameProfile().
        String playerName = p.getName().getString();
        int w = Math.max(130, f.width(playerName) + 44);
        for (String l : lines) w = Math.max(w, f.width("• " + l) + padding * 2 + 6);
        int h = padding + 12 + 5 + Math.max(1, lines.size()) * lineH + padding;

        int x = 8, y = 8;
        int style = Math.floorMod(fs.style, STYLE_NAMES.length);

        // Palette per style
        int accent = accentForStyle(style, fs);
        int dotCol, textCol, nameCol;
        int a = Math.max(0, Math.min(255, (int)(shownAlpha * 255)));

        switch (style) {
            case 1: // MINIMAL
                // No glow panel — single flat bar + accent strip on the left
                AbstractGui.fill(ms, x, y, x + w, y + h, (a << 24) | 0x0E0E12);
                AbstractGui.fill(ms, x, y, x + 2, y + h, (a << 24) | (accent & 0xFFFFFF));
                nameCol = (a << 24) | 0xFFFFFF;
                dotCol  = (a << 24) | (accent & 0xFFFFFF);
                textCol = (a << 24) | 0xBBBBBB;
                break;
            case 2: // OCEAN
                GuiDraw.themedPanel(ms, x, y, w, h, shownAlpha, 0x0A1A2E, 0x061426, 0x2BA7FF);
                nameCol = (a << 24) | 0xBEEEFF;
                dotCol  = (a << 24) | 0x2BA7FF;
                textCol = (a << 24) | 0xB0D8FF;
                break;
            case 3: // FIRE
                GuiDraw.themedPanel(ms, x, y, w, h, shownAlpha, 0x2A0F08, 0x180603, 0xFF7A2A);
                nameCol = (a << 24) | 0xFFE2B4;
                dotCol  = (a << 24) | 0xFFA02A;
                textCol = (a << 24) | 0xFFD0A0;
                break;
            case 4: // MINT
                GuiDraw.themedPanel(ms, x, y, w, h, shownAlpha, 0x08291E, 0x04170F, 0x3CFFA8);
                nameCol = (a << 24) | 0xD8FFEA;
                dotCol  = (a << 24) | 0x3CFFA8;
                textCol = (a << 24) | 0xB0F0D0;
                break;
            case 0:
            default:    // NEON (classic)
                GuiDraw.roundedPanel(ms, x, y, w, h, shownAlpha);
                nameCol = (a << 24) | 0xFFFFFF;
                dotCol  = (a << 24) | 0x9B6DFF;
                textCol = (a << 24) | 0xCCBBFF;
                break;
        }

        int divA = Math.max(0, Math.min(255, (int)(shownAlpha * 130)));
        int divCol = (divA << 24) | (accent & 0xFFFFFF);

        f.drawShadow(ms, playerName, x + padding, y + padding, nameCol);
        // Animated underline for FIRE style, simple line otherwise.
        if (style == 3) {
            long t = System.currentTimeMillis();
            int len = w - padding * 2;
            int off = (int)((t / 20) % len);
            for (int i = 0; i < len; i++) {
                float k = ((i + off) % len) / (float) len;
                int ai = (int)((0.3F + 0.7F * (float) Math.sin(k * Math.PI)) * 180 * shownAlpha);
                AbstractGui.fill(ms, x + padding + i, y + padding + 11, x + padding + i + 1, y + padding + 12,
                        (Math.max(0, Math.min(255, ai)) << 24) | (accent & 0xFFFFFF));
            }
        } else {
            AbstractGui.fill(ms, x + padding, y + padding + 11, x + w - padding, y + padding + 12, divCol);
        }

        int ly = y + padding + 15;
        if (lines.isEmpty()) {
            f.drawShadow(ms, "No effects active", x + padding, ly, (a << 24) | 0x777777);
        } else {
            for (String l : lines) {
                f.drawShadow(ms, "•", x + padding, ly, dotCol);
                f.drawShadow(ms, l, x + padding + 8, ly, textCol);
                ly += lineH;
            }
        }
    }

    private static int accentForStyle(int style, FeatureSettings fs) {
        switch (style) {
            case 1: return 0xFF6B6B6B;
            case 2: return 0xFF2BA7FF;
            case 3: return 0xFFFF7A2A;
            case 4: return 0xFF3CFFA8;
            default:
                int r = Math.max(0, Math.min(255, (int)(fs.colorR * 255)));
                int g = Math.max(0, Math.min(255, (int)(fs.colorG * 255)));
                int b = Math.max(0, Math.min(255, (int)(fs.colorB * 255)));
                return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private CosmeticsHud() {}
}
