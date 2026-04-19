package com.example.cosmetics.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A toggle (checkbox) widget.
 * Shows a styled ON/OFF button with a label.
 */
public class ToggleButton {
    public final int x, y, w, h;
    public final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ToggleButton(int x, int y, int w, int h, String label,
                        BooleanSupplier getter, Consumer<Boolean> setter) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mousePressed(double mx, double my, int button) {
        if (button == 0 && contains(mx, my)) {
            setter.accept(!getter.getAsBoolean());
            return true;
        }
        return false;
    }

    public void draw(MatrixStack ms, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        int a = clamp((int)(alpha * 255));
        boolean on = getter.getAsBoolean();

        // Button box (right side, 40px wide)
        int bx = x + w - 42;
        int by = y;
        int bw = 42;

        // Background
        AbstractGui.fill(ms, x, y, x + w, y + h, (a << 24) | 0x16132A);

        // Toggle background
        int bgColor = on ? 0x4A2EA0 : 0x2A2240;
        AbstractGui.fill(ms, bx, by, bx + bw, by + h, (a << 24) | bgColor);

        // Indicator dot / pill
        if (on) {
            AbstractGui.fill(ms, bx + bw - 14, by + 2, bx + bw - 2, by + h - 2,
                    (a << 24) | 0x9B6DFF);
        } else {
            AbstractGui.fill(ms, bx + 2, by + 2, bx + 14, by + h - 2,
                    (a << 24) | 0x4A4060);
        }

        // Border highlight
        int borderColor = on ? 0x8A5CFF : 0x443366;
        AbstractGui.fill(ms, bx, by, bx + bw, by + 1, (a << 24) | borderColor);
        AbstractGui.fill(ms, bx, by + h - 1, bx + bw, by + h, (a << 24) | borderColor);
        AbstractGui.fill(ms, bx, by, bx + 1, by + h, (a << 24) | borderColor);
        AbstractGui.fill(ms, bx + bw - 1, by, bx + bw, by + h, (a << 24) | borderColor);

        // ON/OFF text inside box
        String stateText = on ? "ON" : "OFF";
        int textX = bx + bw / 2 - mc.font.width(stateText) / 2;
        int textColor = on ? ((a << 24) | 0xD8C8FF) : ((a << 24) | 0x665588);
        mc.font.drawShadow(ms, stateText, textX, by + (h - 8) / 2, textColor);

        // Label on left
        mc.font.drawShadow(ms, label, x + 5, y + (h - 8) / 2, (a << 24) | 0xE0D8FF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
