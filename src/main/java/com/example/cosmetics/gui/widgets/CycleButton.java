package com.example.cosmetics.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * A cycle-through button: click to go to next option.
 * Like a dropdown but simpler — left click = next, right click = prev.
 */
public class CycleButton {
    public final int x, y, w, h;
    public final String label;
    private final String[] options;
    private final IntSupplier getter;
    private final Consumer<Integer> setter;

    public CycleButton(int x, int y, int w, int h, String label,
                       String[] options, IntSupplier getter, Consumer<Integer> setter) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label;
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mousePressed(double mx, double my, int button) {
        if (!contains(mx, my)) return false;
        int cur = Math.floorMod(getter.getAsInt(), options.length);
        if (button == 0) {
            // Left click = next
            setter.accept((cur + 1) % options.length);
        } else if (button == 1) {
            // Right click = prev
            setter.accept((cur - 1 + options.length) % options.length);
        }
        return true;
    }

    public void draw(MatrixStack ms, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        int a = clamp((int)(alpha * 255));
        int cur = Math.floorMod(getter.getAsInt(), options.length);
        String curLabel = options[cur];

        // Background
        AbstractGui.fill(ms, x, y, x + w, y + h, (a << 24) | 0x16132A);

        // Value box (right portion)
        int vw = 90;
        int vx = x + w - vw;

        AbstractGui.fill(ms, vx, y, vx + vw, y + h, (a << 24) | 0x3A1E8A);

        // Left arrow area
        AbstractGui.fill(ms, vx, y, vx + 14, y + h, (a << 24) | 0x4A2EA0);
        mc.font.drawShadow(ms, "<", vx + 4, y + (h - 8) / 2, (a << 24) | 0xB090FF);

        // Right arrow area
        AbstractGui.fill(ms, vx + vw - 14, y, vx + vw, y + h, (a << 24) | 0x4A2EA0);
        mc.font.drawShadow(ms, ">", vx + vw - 10, y + (h - 8) / 2, (a << 24) | 0xB090FF);

        // Current value text centered
        int textX = vx + vw / 2 - mc.font.width(curLabel) / 2;
        mc.font.drawShadow(ms, curLabel, textX, y + (h - 8) / 2, (a << 24) | 0xE0D8FF);

        // Purple border
        int bc = 0x6A4CBF;
        AbstractGui.fill(ms, vx, y, vx + vw, y + 1, (a << 24) | bc);
        AbstractGui.fill(ms, vx, y + h - 1, vx + vw, y + h, (a << 24) | bc);
        AbstractGui.fill(ms, vx, y, vx + 1, y + h, (a << 24) | bc);
        AbstractGui.fill(ms, vx + vw - 1, y, vx + vw, y + h, (a << 24) | bc);

        // Label
        mc.font.drawShadow(ms, label, x + 5, y + (h - 8) / 2, (a << 24) | 0xE0D8FF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
