package com.example.cosmetics.gui;

import com.example.cosmetics.client.CosmeticsState;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Main cosmetics menu. Dark gradient background, soft edge-glow, rounded-ish
 * corners (approximated with two inset rectangles), smooth open/close animation
 * driven by partialTicks.
 */
public class MainMenuScreen extends Screen {

    private enum Category { TRAILS, PARTICLES, HAT, HUD }

    private Category current = Category.TRAILS;
    private long openedAtMs = 0L;
    private boolean closing = false;
    private long closingAtMs = 0L;
    private static final long ANIM_MS = 220L;

    private final List<CatButton>    categoryButtons = new ArrayList<>();
    private final List<ToggleButton> toggleButtons   = new ArrayList<>();

    public MainMenuScreen() {
        super(new StringTextComponent("Cosmetics"));
    }

    @Override
    protected void init() {
        this.openedAtMs = System.currentTimeMillis();
        this.closing = false;

        categoryButtons.clear();
        toggleButtons.clear();

        int panelW = 320;
        int panelH = 200;
        int px = (this.width  - panelW) / 2;
        int py = (this.height - panelH) / 2;

        // Category tabs along the left side
        int i = 0;
        for (Category c : Category.values()) {
            categoryButtons.add(new CatButton(px + 10, py + 40 + i * 28, 90, 24, c));
            i++;
        }

        rebuildToggles(px, py);
    }

    private void rebuildToggles(int px, int py) {
        toggleButtons.clear();
        int rx = px + 115;
        int ry = py + 40;
        int rowH = 28;

        CosmeticsState s = CosmeticsState.get();
        switch (current) {
            case TRAILS:
                toggleButtons.add(new ToggleButton(rx, ry + 0 * rowH, 190, 24,
                        "Rainbow Trail", () -> s.isTrailOn(CosmeticsState.Trail.RAINBOW),
                        () -> s.toggleTrail(CosmeticsState.Trail.RAINBOW)));
                toggleButtons.add(new ToggleButton(rx, ry + 1 * rowH, 190, 24,
                        "Flame Trail", () -> s.isTrailOn(CosmeticsState.Trail.FLAME),
                        () -> s.toggleTrail(CosmeticsState.Trail.FLAME)));
                toggleButtons.add(new ToggleButton(rx, ry + 2 * rowH, 190, 24,
                        "Galaxy Trail", () -> s.isTrailOn(CosmeticsState.Trail.GALAXY),
                        () -> s.toggleTrail(CosmeticsState.Trail.GALAXY)));
                break;
            case PARTICLES:
                toggleButtons.add(new ToggleButton(rx, ry + 0 * rowH, 190, 24,
                        "Aura", () -> s.isAuraOn(CosmeticsState.Aura.AURA),
                        () -> s.toggleAura(CosmeticsState.Aura.AURA)));
                toggleButtons.add(new ToggleButton(rx, ry + 1 * rowH, 190, 24,
                        "Snow Aura", () -> s.isAuraOn(CosmeticsState.Aura.SNOW),
                        () -> s.toggleAura(CosmeticsState.Aura.SNOW)));
                toggleButtons.add(new ToggleButton(rx, ry + 2 * rowH, 190, 24,
                        "Hearts", () -> s.isAuraOn(CosmeticsState.Aura.HEARTS),
                        () -> s.toggleAura(CosmeticsState.Aura.HEARTS)));
                break;
            case HAT:
                toggleButtons.add(new ToggleButton(rx, ry + 0 * rowH, 190, 24,
                        "China Hat", () -> s.isHatOn(CosmeticsState.Hat.CHINA),
                        () -> s.toggleHat(CosmeticsState.Hat.CHINA)));
                break;
            case HUD:
                toggleButtons.add(new ToggleButton(rx, ry + 0 * rowH, 190, 24,
                        "HUD Panel", s::isHudEnabled, s::toggleHud));
                break;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        float anim = animProgress();
        float alpha = anim; // 0..1

        // Dim background slightly.
        fill(ms, 0, 0, this.width, this.height, (int)(alpha * 140) << 24);

        int panelW = 320;
        int panelH = 200;
        int px = (this.width  - panelW) / 2;
        int py = (this.height - panelH) / 2;

        // Scale animation around center
        ms.pushPose();
        float scale = 0.9F + 0.1F * anim;
        ms.translate(this.width / 2f, this.height / 2f, 0);
        ms.scale(scale, scale, 1.0F);
        ms.translate(-this.width / 2f, -this.height / 2f, 0);

        GuiDraw.roundedPanel(ms, px, py, panelW, panelH, alpha);

        // Title
        int titleColor = withAlpha(0xFFFFFFFF, alpha);
        drawCenteredString(ms, this.font, "Cosmetics", px + panelW / 2, py + 14, titleColor);
        // Accent underline
        fill(ms, px + panelW / 2 - 40, py + 28, px + panelW / 2 + 40, py + 30,
                withAlpha(0xFF8A5CFF, alpha));

        for (CatButton c : categoryButtons) c.draw(ms, mouseX, mouseY, alpha, current);
        for (ToggleButton t : toggleButtons) t.draw(ms, mouseX, mouseY, alpha);

        // Footer hint
        drawString(ms, this.font, "Right Shift / Esc to close",
                px + 10, py + panelH - 16, withAlpha(0xFFAAAAAA, alpha));

        ms.popPose();

        if (closing && anim <= 0.001F) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        for (CatButton c : categoryButtons) {
            if (c.contains(mouseX, mouseY)) {
                current = c.category;
                int panelW = 320, panelH = 200;
                int px = (this.width  - panelW) / 2;
                int py = (this.height - panelH) / 2;
                rebuildToggles(px, py);
                return true;
            }
        }
        for (ToggleButton t : toggleButtons) {
            if (t.contains(mouseX, mouseY)) {
                t.onClick.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // 256 = GLFW_KEY_ESCAPE, 344 = GLFW_KEY_RIGHT_SHIFT
        if (key == 256 || key == 344) {
            beginClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (!closing) {
            beginClose();
        } else {
            super.onClose();
        }
    }

    private void beginClose() {
        closing = true;
        closingAtMs = System.currentTimeMillis();
    }

    private float animProgress() {
        long now = System.currentTimeMillis();
        if (!closing) {
            float t = Math.min(1.0F, (now - openedAtMs) / (float) ANIM_MS);
            return easeOut(t);
        } else {
            float t = Math.min(1.0F, (now - closingAtMs) / (float) ANIM_MS);
            return 1.0F - easeOut(t);
        }
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) ((argb >>> 24 & 0xFF) * alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    // ---- Inner button types -------------------------------------------------

    private class CatButton {
        final int x, y, w, h;
        final Category category;
        CatButton(int x, int y, int w, int h, Category c) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.category = c;
        }
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
        void draw(MatrixStack ms, int mx, int my, float alpha, Category selected) {
            boolean hover = contains(mx, my);
            boolean sel = selected == category;
            int base = sel ? 0xFF3A2D5E : (hover ? 0xFF2B2540 : 0xFF1E1B2E);
            fill(ms, x, y, x + w, y + h, withAlpha(base, alpha));
            if (sel) fill(ms, x, y, x + 3, y + h, withAlpha(0xFF8A5CFF, alpha));
            String label = category.name().charAt(0) + category.name().substring(1).toLowerCase();
            drawString(ms, font, label, x + 10, y + (h - 8) / 2,
                    withAlpha(0xFFFFFFFF, alpha));
        }
    }

    private class ToggleButton {
        final int x, y, w, h;
        final String label;
        final java.util.function.BooleanSupplier isOn;
        final Runnable onClick;
        ToggleButton(int x, int y, int w, int h, String label,
                     java.util.function.BooleanSupplier isOn, Runnable onClick) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.isOn = isOn; this.onClick = onClick;
        }
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
        void draw(MatrixStack ms, int mx, int my, float alpha) {
            boolean hover = contains(mx, my);
            int base = hover ? 0xFF2B2540 : 0xFF1E1B2E;
            fill(ms, x, y, x + w, y + h, withAlpha(base, alpha));

            // Toggle pill on the right
            int pillW = 30, pillH = 14;
            int px = x + w - pillW - 8;
            int py = y + (h - pillH) / 2;
            int pillBg = isOn.getAsBoolean() ? 0xFF7A4CFF : 0xFF444050;
            fill(ms, px, py, px + pillW, py + pillH, withAlpha(pillBg, alpha));
            int knobX = isOn.getAsBoolean() ? px + pillW - pillH : px;
            fill(ms, knobX, py, knobX + pillH, py + pillH, withAlpha(0xFFFFFFFF, alpha));

            drawString(ms, font, label, x + 10, y + (h - 8) / 2,
                    withAlpha(0xFFFFFFFF, alpha));
        }
    }
}
