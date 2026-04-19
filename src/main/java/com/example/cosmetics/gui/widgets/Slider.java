package com.example.cosmetics.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/** Styled horizontal slider with label and value display. */
public class Slider {
    public final int x, y, w, h;
    public final String label;
    public final float min, max;
    private final DoubleSupplier getter;
    private final Consumer<Float> setter;
    private boolean dragging = false;
    private float hoverAnim = 0F;

    public Slider(int x, int y, int w, int h, String label, float min, float max,
                  DoubleSupplier getter, Consumer<Float> setter) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label; this.min = min; this.max = max;
        this.getter = getter; this.setter = setter;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mousePressed(double mx, double my, int button) {
        if (button == 0 && contains(mx, my)) {
            dragging = true; updateFromMouse(mx, x); return true;
        }
        return false;
    }

    /** Версия с offset для скролла: ox/oy — смещение начала координат виджетов */
    public boolean mousePressedAt(double mx, double my, int button, int ox, int oy) {
        int ax = ox + x, ay = oy + y;
        if (button == 0 && mx >= ax && mx <= ax + w && my >= ay && my <= ay + h) {
            dragging = true; updateFromMouse(mx, ax); return true;
        }
        return false;
    }

    public void mouseReleased() { dragging = false; }

    public void mouseDragged(double mx) { if (dragging) updateFromMouse(mx, x); }

    public void mouseDraggedAt(double mx, int ox, int oy) {
        if (dragging) updateFromMouse(mx, ox + x);
    }

    private void updateFromMouse(double mx, int startX) {
        double t = Math.max(0, Math.min(1, (mx - startX) / (double) w));
        setter.accept((float)(min + t * (max - min)));
    }

    /** Override to return a custom string for the value (e.g. style names). */
    public String formatValue() {
        double v = getter.getAsDouble();
        if (max - min <= 30 && (max - min) == Math.floor(max - min)) {
            return String.valueOf((int) v);
        }
        return String.format("%.2f", v);
    }

    public void draw(MatrixStack ms, float alpha) {
        drawAt(ms, alpha, 0, 0);
    }

    /** Рисует виджет со смещением ox/oy (для скролла). */
    public void drawAt(MatrixStack ms, float alpha, int ox, int oy) {
        Minecraft mc = Minecraft.getInstance();
        int ax = ox + x, ay = oy + y;
        int a = clamp((int)(alpha * 255));

        AbstractGui.fill(ms, ax, ay, ax + w, ay + h, (a << 24) | 0x16132A);

        float t = (float)((getter.getAsDouble() - min) / (max - min));
        t = Math.max(0, Math.min(1, t));
        int fillW = (int)(w * t);

        if (fillW > 0) {
            AbstractGui.fill(ms, ax, ay, ax + fillW, ay + h, (a << 24) | 0x5A3AB0);
            AbstractGui.fill(ms, ax, ay, ax + Math.min(fillW, 3), ay + h, (a << 24) | 0x9B6DFF);
        }

        int knobX = ax + fillW - 2;
        AbstractGui.fill(ms, knobX, ay - 1, knobX + 5, ay + h + 1, (a << 24) | 0xFFFFFF);
        AbstractGui.fill(ms, knobX + 1, ay, knobX + 4, ay + h, (a << 24) | 0xDDCCFF);

        String text = label + ": " + formatValue();
        mc.font.drawShadow(ms, text, ax + 5, ay + (h - 8) / 2, (a << 24) | 0xE0D8FF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
