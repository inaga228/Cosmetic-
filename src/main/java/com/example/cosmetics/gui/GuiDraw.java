package com.example.cosmetics.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;

/**
 * Small helper utilities for the custom GUI. Approximates rounded corners by
 * inset rectangles (cheap and works without shaders), and draws a glowing
 * border by layering increasingly translucent outlines.
 */
public final class GuiDraw extends AbstractGui {

    /** Draws a dark gradient panel with glowing rounded-looking border. */
    public static void roundedPanel(MatrixStack ms, int x, int y, int w, int h, float alpha) {
        int a = clamp((int) (alpha * 255));

        // Outer glow (several translucent outlines, fading out).
        for (int i = 4; i >= 1; i--) {
            int glowA = clamp((int) (alpha * (40 - i * 8)));
            int glow  = (glowA << 24) | 0x8A5CFF;
            fill(ms, x - i, y - i, x + w + i, y + h + i, glow);
        }

        // Rounded-corner approximation: draw main rect + trim corners with 2px inset.
        int topCol    = (a << 24) | 0x171322;
        int bottomCol = (a << 24) | 0x0C0A16;

        fillGradient(ms, x + 2, y,       x + w - 2, y + 2,     topCol, topCol);
        fillGradient(ms, x + 2, y + h - 2, x + w - 2, y + h,     bottomCol, bottomCol);
        fillGradient(ms, x,     y + 2,   x + 2,     y + h - 2, topCol, bottomCol);
        fillGradient(ms, x + w - 2, y + 2, x + w,   y + h - 2, topCol, bottomCol);
        // Center gradient body
        fillGradient(ms, x + 2, y + 2, x + w - 2, y + h - 2, topCol, bottomCol);

        // Inner subtle 1px border
        int border = (clamp((int)(alpha * 120)) << 24) | 0x8A5CFF;
        fill(ms, x + 1, y + 1, x + w - 1, y + 2, border);
        fill(ms, x + 1, y + h - 2, x + w - 1, y + h - 1, border);
        fill(ms, x + 1, y + 2, x + 2, y + h - 2, border);
        fill(ms, x + w - 2, y + 2, x + w - 1, y + h - 2, border);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private GuiDraw() {}
}
