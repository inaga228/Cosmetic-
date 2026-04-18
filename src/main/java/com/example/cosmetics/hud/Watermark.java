package com.example.cosmetics.hud;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.GuiDraw;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Animated watermark rendered in the top-right corner of the screen.
 *
 * <p>Three styles (cycled via the STYLE setting):
 * <ul>
 *   <li>0 — RAINBOW: hue-cycling gradient over every letter</li>
 *   <li>1 — PULSE: single accent colour (from settings) with a soft
 *       sinusoidal alpha pulse</li>
 *   <li>2 — MINIMAL: flat accent colour, no panel, text only with shadow</li>
 * </ul>
 *
 * <p>The watermark also draws a small live FPS readout and a stylised
 * star/diamond glyph. Designed to look clean but decorative.
 */
public final class Watermark {

    private static final String LOGO = "\u2726";        // black four-pointed star
    private static final String TITLE = "Cosmetics";
    private static final long TICK_MS = 16L;

    private static float shownAlpha = 0.0F;

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        CosmeticsState state = CosmeticsState.get();
        boolean want = state.isOn(FeatureType.WATERMARK);
        float target = want ? 1.0F : 0.0F;
        shownAlpha += (target - shownAlpha) * 0.12F;
        if (shownAlpha < 0.01F) return;

        FontRenderer f = mc.font;
        FeatureSettings fs = state.settings(FeatureType.WATERMARK);
        int style = Math.floorMod(fs.style, 3);

        long now = System.currentTimeMillis();
        float hueBase = (now % 6000L) / 6000.0F;

        // Compose the displayed string: logo + title + small fps chip
        String fpsText = Minecraft.getInstance().fpsString == null ? "" :
                extractFps(Minecraft.getInstance().fpsString);

        int titleW = f.width(LOGO + " " + TITLE);
        int fpsW   = fpsText.isEmpty() ? 0 : (f.width(fpsText) + 10);
        int padX = 10, padY = 6;
        int w = padX * 2 + titleW + fpsW;
        int h = padY * 2 + 8;

        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw - w - 8;
        int y = 8;

        int accentRgb = toRgb(fs.colorR, fs.colorG, fs.colorB);

        if (style != 2) {
            int panelAccent = style == 0
                    ? GuiDraw.hsv(hueBase, 0.7F, 1.0F)
                    : accentRgb;
            GuiDraw.roundedPanelAccent(ms, x, y, w, h, shownAlpha, panelAccent);
        }

        // Text rendering
        int tx = x + padX;
        int ty = y + padY;

        // Draw the star glyph
        int starCol = style == 0
                ? GuiDraw.hsv(hueBase, 0.9F, 1.0F)
                : accentRgb;
        int starArgb = GuiDraw.argb(shownAlpha, starCol);
        f.drawShadow(ms, LOGO, tx, ty, starArgb);

        // Draw the title with per-letter rainbow on style 0, solid on 1/2
        int titleX = tx + f.width(LOGO + " ");
        switch (style) {
            case 0:
                drawRainbowText(ms, f, TITLE, titleX, ty, shownAlpha, hueBase);
                break;
            case 1: {
                float pulse = 0.75F + 0.25F * (float)Math.sin(now / 380.0);
                int col = GuiDraw.argb(shownAlpha * pulse, accentRgb);
                f.drawShadow(ms, TITLE, titleX, ty, col);
                break;
            }
            case 2: default: {
                int col = GuiDraw.argb(shownAlpha, accentRgb);
                f.drawShadow(ms, TITLE, titleX, ty, col);
                break;
            }
        }

        // FPS chip on the right
        if (!fpsText.isEmpty()) {
            int chipX = x + w - padX - f.width(fpsText);
            int chipCol = GuiDraw.argb(shownAlpha * 0.8F, 0xCCBBFF);
            f.drawShadow(ms, fpsText, chipX, ty, chipCol);
        }
    }

    private static void drawRainbowText(MatrixStack ms, FontRenderer f, String text,
                                         int x, int y, float alpha, float hueBase) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            float h = hueBase + i * 0.06F;
            int rgb = GuiDraw.hsv(h, 0.85F, 1.0F);
            int col = GuiDraw.argb(alpha, rgb);
            String ch = String.valueOf(text.charAt(i));
            f.drawShadow(ms, ch, cx, y, col);
            cx += f.width(ch);
        }
    }

    private static String extractFps(String debugLine) {
        // "fpsString" is e.g. "120 fps T: 60 (..)" on 1.16.5 — keep just the first two tokens.
        int space = debugLine.indexOf(' ');
        if (space <= 0) return debugLine;
        int second = debugLine.indexOf(' ', space + 1);
        return second > 0 ? debugLine.substring(0, second) : debugLine.substring(0, space + 1) + "fps";
    }

    private static int toRgb(float r, float g, float b) {
        int ir = clamp((int)(r * 255));
        int ig = clamp((int)(g * 255));
        int ib = clamp((int)(b * 255));
        return (ir << 16) | (ig << 8) | ib;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private Watermark() {}
}
