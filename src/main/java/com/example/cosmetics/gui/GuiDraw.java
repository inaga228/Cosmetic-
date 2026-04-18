package com.example.cosmetics.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;

/**
 * GUI drawing utilities — rounded panel with glow border and gradient body.
 * Also provides a simple static helper for drawing horizontal gradient bars.
 */
public final class GuiDraw extends AbstractGui {

    private static final GuiDraw I = new GuiDraw();

    /** Dark gradient panel with layered glow border and rounded corners. */
    public static void roundedPanel(MatrixStack ms, int x, int y, int w, int h, float alpha) {
        I.drawRoundedPanel(ms, x, y, w, h, alpha);
    }

    /** Horizontal gradient bar — useful for HP or progress. */
    public static void gradientBar(MatrixStack ms, int x, int y, int w, int h,
                                   int colorLeft, int colorRight, float alpha) {
        int aL = withAlpha(colorLeft,  alpha);
        int aR = withAlpha(colorRight, alpha);
        I.fillGradient(ms, x, y, x + w, y + h, aL, aR);
    }

    private void drawRoundedPanel(MatrixStack ms, int x, int y, int w, int h, float alpha) {
        // Outer glow rings
        for (int i = 5; i >= 1; i--) {
            int glowA = clamp((int)(alpha * (35 - i * 6)));
            fill(ms, x - i, y - i, x + w + i, y + h + i, (glowA << 24) | 0x7A4CFF);
        }

        int a   = clamp((int)(alpha * 255));
        int top = (a << 24) | 0x171325;
        int bot = (a << 24) | 0x0D0B1A;

        // Rounded corner trim (2px cut)
        fillGradient(ms, x + 2, y,         x + w - 2, y + 2,         top, top);
        fillGradient(ms, x + 2, y + h - 2, x + w - 2, y + h,         bot, bot);
        fillGradient(ms, x,     y + 2,     x + 2,     y + h - 2,     top, bot);
        fillGradient(ms, x + w - 2, y + 2, x + w,     y + h - 2,     top, bot);
        // Main body
        fillGradient(ms, x + 2, y + 2,     x + w - 2, y + h - 2,     top, bot);

        // Inner border (subtle glow line)
        int bdr = (clamp((int)(alpha * 110)) << 24) | 0x8A5CFF;
        fill(ms, x + 1, y + 1,         x + w - 1, y + 2,         bdr);
        fill(ms, x + 1, y + h - 2,     x + w - 1, y + h - 1,     bdr);
        fill(ms, x + 1, y + 2,         x + 2,     y + h - 2,     bdr);
        fill(ms, x + w - 2, y + 2,     x + w - 1, y + h - 2,     bdr);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = clamp((int)(alpha * 255));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private GuiDraw() {}
}
