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
        if (button == 0) setter.accept((cur + 1) % options.length);
        else if (button == 1) setter.accept((cur - 1 + options.length) % options.length);
        return true;
    }

    public boolean mousePressedAt(double mx, double my, int button, int ox, int oy) {
        int ax = ox + x, ay = oy + y;
        if (mx < ax || mx > ax + w || my < ay || my > ay + h) return false;
        int cur = Math.floorMod(getter.getAsInt(), options.length);
        if (button == 0) setter.accept((cur + 1) % options.length);
        else if (button == 1) setter.accept((cur - 1 + options.length) % options.length);
        return true;
    }

    public void draw(MatrixStack ms, float alpha) { drawAt(ms, alpha, 0, 0); }

    public void drawAt(MatrixStack ms, float alpha, int ox, int oy) {
        Minecraft mc = Minecraft.getInstance();
        int ax = ox + x, ay = oy + y;
        int a = clamp((int)(alpha * 255));
        int cur = Math.floorMod(getter.getAsInt(), options.length);
        String curLabel = options[cur];

        AbstractGui.fill(ms, ax, ay, ax + w, ay + h, (a << 24) | 0x16132A);

        int vw = 90, vx = ax + w - vw;
        AbstractGui.fill(ms, vx, ay, vx + vw, ay + h, (a << 24) | 0x3A1E8A);
        AbstractGui.fill(ms, vx, ay, vx + 14, ay + h, (a << 24) | 0x4A2EA0);
        mc.font.drawShadow(ms, "<", vx + 4, ay + (h - 8) / 2, (a << 24) | 0xB090FF);
        AbstractGui.fill(ms, vx + vw - 14, ay, vx + vw, ay + h, (a << 24) | 0x4A2EA0);
        mc.font.drawShadow(ms, ">", vx + vw - 10, ay + (h - 8) / 2, (a << 24) | 0xB090FF);

        int textX = vx + vw / 2 - mc.font.width(curLabel) / 2;
        mc.font.drawShadow(ms, curLabel, textX, ay + (h - 8) / 2, (a << 24) | 0xE0D8FF);

        int bc = 0x6A4CBF;
        AbstractGui.fill(ms, vx, ay, vx + vw, ay + 1, (a << 24) | bc);
        AbstractGui.fill(ms, vx, ay + h - 1, vx + vw, ay + h, (a << 24) | bc);
        AbstractGui.fill(ms, vx, ay, vx + 1, ay + h, (a << 24) | bc);
        AbstractGui.fill(ms, vx + vw - 1, ay, vx + vw, ay + h, (a << 24) | bc);

        mc.font.drawShadow(ms, label, ax + 5, ay + (h - 8) / 2, (a << 24) | 0xE0D8FF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
