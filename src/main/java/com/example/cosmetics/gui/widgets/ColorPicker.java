package com.example.cosmetics.gui.widgets;

import com.example.cosmetics.feature.FeatureSettings;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

public class ColorPicker {
    private final int x, y, w, h;
    private final FeatureSettings fs;

    private static final int[] PRESETS = {
        0xFFFF4444, 0xFFFF8800, 0xFFFFFF00, 0xFF44FF44,
        0xFF00FFFF, 0xFF4488FF, 0xFF9955FF, 0xFFFF55CC,
        0xFFFFFFFF, 0xFF888888, 0xFF333333, 0xFF000000
    };

    public ColorPicker(int x, int y, int w, int h, FeatureSettings fs) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.fs = fs;
    }

    // Старые методы (для совместимости если где-то вызываются)
    public void draw(MatrixStack ms, int mouseX, int mouseY, float alpha) {
        drawAt(ms, mouseX, mouseY, alpha, 0, 0);
    }
    public boolean mousePressed(double mx, double my, int button) {
        return mousePressedAt(mx, my, button, 0, 0);
    }
    public void mouseDragged(double mx, double my) {}
    public void mouseReleased() {}

    // Offset-aware версии для скролла
    public void drawAt(MatrixStack ms, int mouseX, int mouseY, float alpha, int ox, int oy) {
        int ax = ox + x, ay = oy + y;
        int a = Math.max(0, Math.min(255, (int)(alpha * 255)));

        Minecraft.getInstance().font.drawShadow(ms, "Color:", ax, ay - 2, (a << 24) | 0xCCCCCC);

        int prevSize = h - 2;
        int r = Math.max(0, Math.min(255, (int)(fs.colorR * 255)));
        int g = Math.max(0, Math.min(255, (int)(fs.colorG * 255)));
        int b = Math.max(0, Math.min(255, (int)(fs.colorB * 255)));
        AbstractGui.fill(ms, ax, ay + 10, ax + prevSize, ay + 10 + prevSize, (a << 24) | 0x111111);
        AbstractGui.fill(ms, ax + 1, ay + 11, ax + prevSize - 1, ay + 10 + prevSize - 1,
                (a << 24) | (r << 16) | (g << 8) | b);

        int swatchSize = 12, swatchPad = 2;
        int startX = ax + prevSize + 8;
        int perRow = (w - prevSize - 8) / (swatchSize + swatchPad);
        for (int i = 0; i < PRESETS.length; i++) {
            int col = PRESETS[i];
            int sx = startX + (i % perRow) * (swatchSize + swatchPad);
            int sy = ay + 10 + (i / perRow) * (swatchSize + swatchPad);
            AbstractGui.fill(ms, sx - 1, sy - 1, sx + swatchSize + 1, sy + swatchSize + 1,
                    (a << 24) | 0x555555);
            AbstractGui.fill(ms, sx, sy, sx + swatchSize, sy + swatchSize,
                    (a << 24) | (col & 0x00FFFFFF));
            if (mouseX >= sx && mouseX < sx + swatchSize && mouseY >= sy && mouseY < sy + swatchSize) {
                AbstractGui.fill(ms, sx - 1, sy - 1, sx + swatchSize + 1, sy + swatchSize + 1,
                        (Math.min(255, a + 80) << 24) | 0xFFFFFF);
            }
        }
    }

    public boolean mousePressedAt(double mx, double my, int button, int ox, int oy) {
        if (button != 0) return false;
        int ax = ox + x, ay = oy + y;
        int prevSize = h - 2;
        int swatchSize = 12, swatchPad = 2;
        int startX = ax + prevSize + 8;
        int perRow = (w - prevSize - 8) / (swatchSize + swatchPad);
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = startX + (i % perRow) * (swatchSize + swatchPad);
            int sy = ay + 10 + (i / perRow) * (swatchSize + swatchPad);
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

    public void mouseDraggedAt(double mx, double my, int ox, int oy) {}
}
