package com.example.cosmetics.gui.widgets;

import com.example.cosmetics.feature.FeatureSettings;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

/**
 * Compact color picker: a row of preset swatches + live preview box.
 * RGB fine-tuning is done via sliders elsewhere.
 */
public class ColorPicker {
    private final int x, y, w, h;
    private final FeatureSettings fs;

    private static final int[] PRESETS = {
        0xFFFF4444, 0xFFFF8800, 0xFFFFFF00, 0xFF44FF44,
        0xFF00FFFF, 0xFF4488FF, 0xFF9955FF, 0xFFFF55CC,
        0xFFFFFFFF, 0xFF888888, 0xFF333333, 0xFF000000
    };

    private boolean draggingHue = false;
    private boolean draggingBright = false;

    public ColorPicker(int x, int y, int w, int h, FeatureSettings fs) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.fs = fs;
    }

    public void draw(MatrixStack ms, int mouseX, int mouseY, float alpha) {
        int a = Math.max(0, Math.min(255, (int)(alpha * 255)));

        // Label
        Minecraft.getInstance().font.drawShadow(ms, "Color:", x, y - 2, (a << 24) | 0xCCCCCC);

        // Preview box
        int prevSize = h - 2;
        int r = (int)(fs.colorR * 255);
        int g = (int)(fs.colorG * 255);
        int b = (int)(fs.colorB * 255);
        int previewCol = (a << 24) | (r << 16) | (g << 8) | b;
        AbstractGui.fill(ms, x, y + 10, x + prevSize, y + 10 + prevSize, (a << 24) | 0x111111);
        AbstractGui.fill(ms, x + 1, y + 11, x + prevSize - 1, y + 10 + prevSize - 1, previewCol);

        // Preset swatches
        int swatchSize = 12;
        int swatchPad = 2;
        int startX = x + prevSize + 8;
        int perRow = (w - prevSize - 8) / (swatchSize + swatchPad);
        for (int i = 0; i < PRESETS.length; i++) {
            int col = PRESETS[i];
            int sx = startX + (i % perRow) * (swatchSize + swatchPad);
            int sy = y + 10 + (i / perRow) * (swatchSize + swatchPad);
            // Border
            AbstractGui.fill(ms, sx - 1, sy - 1, sx + swatchSize + 1, sy + swatchSize + 1,
                    (a << 24) | 0x555555);
            // Swatch
            int swatchA = (a << 24) | (col & 0x00FFFFFF);
            AbstractGui.fill(ms, sx, sy, sx + swatchSize, sy + swatchSize, swatchA);
            // Hover highlight
            if (mouseX >= sx && mouseX < sx + swatchSize && mouseY >= sy && mouseY < sy + swatchSize) {
                AbstractGui.fill(ms, sx - 1, sy - 1, sx + swatchSize + 1, sy + swatchSize + 1,
                        (Math.min(255, a + 60) << 24) | 0xFFFFFF & 0x40FFFFFF);
            }
        }
    }

    public boolean mousePressed(double mx, double my, int button) {
        if (button != 0) return false;
        int prevSize = h - 2;
        int swatchSize = 12;
        int swatchPad = 2;
        int startX = x + prevSize + 8;
        int perRow = (w - prevSize - 8) / (swatchSize + swatchPad);
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = startX + (i % perRow) * (swatchSize + swatchPad);
            int sy = y + 10 + (i / perRow) * (swatchSize + swatchPad);
            if (mx >= sx && mx < sx + swatchSize && my >= sy && my < sy + swatchSize) {
                int col = PRESETS[i];
                fs.colorR = ((col >> 16) & 0xFF) / 255F;
                fs.colorG = ((col >> 8) & 0xFF) / 255F;
                fs.colorB = (col & 0xFF) / 255F;
                return true;
            }
        }
        return false;
    }

    public void mouseReleased() {
        draggingHue = false;
        draggingBright = false;
    }

    public void mouseDragged(double mx, double my) {}
}
