package com.example.cosmetics.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;

/**
 * GUI drawing utilities — polished panels, gradient bars, hollow shapes,
 * HSV helpers. All drawing uses 2D {@link AbstractGui#fill}/{@code fillGradient},
 * so it is safe to call from any overlay/screen render tick.
 */
public final class GuiDraw extends AbstractGui {

    private static final GuiDraw I = new GuiDraw();

    // ---- Public API ------------------------------------------------------

    /** Dark gradient panel with layered glow border and rounded corners. */
    public static void roundedPanel(MatrixStack ms, int x, int y, int w, int h, float alpha) {
        I.drawRoundedPanel(ms, x, y, w, h, alpha, 0x8A5CFF);
    }

    /** Rounded panel with a custom glow/accent colour (RGB). */
    public static void roundedPanelAccent(MatrixStack ms, int x, int y, int w, int h,
                                          float alpha, int accentRgb) {
        I.drawRoundedPanel(ms, x, y, w, h, alpha, accentRgb);
    }

    /** Slimmer variant for small widgets (less glow). */
    public static void slimPanel(MatrixStack ms, int x, int y, int w, int h, float alpha) {
        I.drawSlimPanel(ms, x, y, w, h, alpha);
    }

    /** Horizontal gradient bar — useful for HP or progress. */
    public static void gradientBar(MatrixStack ms, int x, int y, int w, int h,
                                   int colorLeft, int colorRight, float alpha) {
        int aL = withAlpha(colorLeft,  alpha);
        int aR = withAlpha(colorRight, alpha);
        I.fillGradient(ms, x, y, x + w, y + h, aL, aR);
    }

    /** Two-colour horizontal line used as a divider. */
    public static void divider(MatrixStack ms, int x, int y, int w, float alpha, int rgb) {
        int a = clamp((int)(alpha * 140));
        int edge = (clamp((int)(alpha * 0)) << 24) | (rgb & 0x00FFFFFF);
        int mid  = (a << 24) | (rgb & 0x00FFFFFF);
        I.fillGradient(ms, x,         y, x + w / 2, y + 1, edge, mid);
        I.fillGradient(ms, x + w / 2, y, x + w,     y + 1, mid,  edge);
    }

    /** Small filled coloured pill (used for feature chips in HUD). */
    public static void chip(MatrixStack ms, int x, int y, int w, int h, int rgb, float alpha) {
        int a = clamp((int)(alpha * 255));
        int col = (a << 24) | (rgb & 0x00FFFFFF);
        AbstractGui.fill(ms, x + 1, y, x + w - 1, y + h, col);
        AbstractGui.fill(ms, x, y + 1, x + w, y + h - 1, col);
    }

    // ---- Colour helpers --------------------------------------------------

    /** HSV → packed RGB (0xRRGGBB), h ∈ [0,1], s,v ∈ [0,1]. */
    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: default: r = v; g = p; b = q; break;
        }
        int ir = clamp((int)(r * 255));
        int ig = clamp((int)(g * 255));
        int ib = clamp((int)(b * 255));
        return (ir << 16) | (ig << 8) | ib;
    }

    /** Pack argb from alpha (0..1) and rgb. */
    public static int argb(float alpha, int rgb) {
        int a = clamp((int)(alpha * 255));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    /** Lerp two argb colours (t in 0..1). */
    public static int blend(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a1 = (c1 >>> 24) & 0xFF, a2 = (c2 >>> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF, r2 = (c2 >> 16) & 0xFF;
        int g1 = (c1 >>  8) & 0xFF, g2 = (c2 >>  8) & 0xFF;
        int b1 =  c1        & 0xFF, b2 =  c2        & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ---- Internals -------------------------------------------------------

    private void drawRoundedPanel(MatrixStack ms, int x, int y, int w, int h,
                                  float alpha, int accentRgb) {
        // Outer glow rings (6 layers, soft falloff)
        for (int i = 6; i >= 1; i--) {
            int glowA = clamp((int)(alpha * (40 - i * 6)));
            fill(ms, x - i, y - i, x + w + i, y + h + i, (glowA << 24) | (accentRgb & 0x00FFFFFF));
        }

        int a   = clamp((int)(alpha * 255));
        int top = (a << 24) | 0x17132A;
        int mid = (a << 24) | 0x120E22;
        int bot = (a << 24) | 0x0A0815;

        // Rounded corner trim (2px cut)
        fillGradient(ms, x + 2, y,         x + w - 2, y + 2,         top, top);
        fillGradient(ms, x + 2, y + h - 2, x + w - 2, y + h,         bot, bot);
        fillGradient(ms, x,     y + 2,     x + 2,     y + h - 2,     top, bot);
        fillGradient(ms, x + w - 2, y + 2, x + w,     y + h - 2,     top, bot);
        // Main body with vertical gradient
        fillGradient(ms, x + 2, y + 2,     x + w - 2, y / 2 + h / 2, top, mid);
        fillGradient(ms, x + 2, y / 2 + h / 2, x + w - 2, y + h - 2, mid, bot);

        // Subtle top highlight (1px)
        int hi = (clamp((int)(alpha * 60)) << 24) | 0xFFFFFF;
        fill(ms, x + 4, y + 2, x + w - 4, y + 3, hi);

        // Inner border (subtle glow line)
        int bdr = (clamp((int)(alpha * 110)) << 24) | (accentRgb & 0x00FFFFFF);
        fill(ms, x + 1, y + 1,         x + w - 1, y + 2,         bdr);
        fill(ms, x + 1, y + h - 2,     x + w - 1, y + h - 1,     bdr);
        fill(ms, x + 1, y + 2,         x + 2,     y + h - 2,     bdr);
        fill(ms, x + w - 2, y + 2,     x + w - 1, y + h - 2,     bdr);
    }

    private void drawSlimPanel(MatrixStack ms, int x, int y, int w, int h, float alpha) {
        for (int i = 3; i >= 1; i--) {
            int glowA = clamp((int)(alpha * (22 - i * 5)));
            fill(ms, x - i, y - i, x + w + i, y + h + i, (glowA << 24) | 0x8A5CFF);
        }
        int a   = clamp((int)(alpha * 230));
        int top = (a << 24) | 0x16122A;
        int bot = (a << 24) | 0x0C0A1C;
        fillGradient(ms, x, y, x + w, y + h, top, bot);
        int bdr = (clamp((int)(alpha * 90)) << 24) | 0x8A5CFF;
        fill(ms, x, y, x + w, y + 1, bdr);
        fill(ms, x, y + h - 1, x + w, y + h, bdr);
        fill(ms, x, y, x + 1, y + h, bdr);
        fill(ms, x + w - 1, y, x + w, y + h, bdr);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = clamp((int)(alpha * 255));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private GuiDraw() {}
}
