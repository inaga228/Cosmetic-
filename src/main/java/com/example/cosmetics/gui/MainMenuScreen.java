package com.example.cosmetics.gui;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Main cosmetics menu.
 * Left side: category tabs. Right side: feature cards.
 * LMB = toggle, RMB = open settings.
 * Beautiful gradient style with glow, rounded corners, hover effects.
 */
public class MainMenuScreen extends Screen {

    private FeatureType.Category current = FeatureType.Category.TRAILS;
    private long openedAtMs;
    private boolean closing = false;
    private long closingAtMs;
    private static final long ANIM_MS = 240L;

    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private final List<FeatureCard> cards = new ArrayList<>();

    // For smooth card hover animation
    private final float[] cardHover = new float[FeatureType.values().length];

    public MainMenuScreen() { super(new StringTextComponent("Cosmetics")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
        closing = false;
        categoryTabs.clear();

        int panelW = 380;
        int panelH = 240;
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        int i = 0;
        for (FeatureType.Category c : FeatureType.Category.values()) {
            categoryTabs.add(new CategoryTab(px + 8, py + 44 + i * 26, 96, 22, c));
            i++;
        }
        rebuildCards(px, py);
    }

    private void rebuildCards(int px, int py) {
        cards.clear();
        int cx = px + 114;
        int cy = py + 44;
        int cw = 250;
        int ch = 24;
        int i = 0;
        for (FeatureType f : FeatureType.values()) {
            if (f.category != current) continue;
            cards.add(new FeatureCard(cx, cy + i * (ch + 5), cw, ch, f));
            i++;
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        float anim = animProgress();

        // Dim background
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 155) << 24);

        int panelW = 380;
        int panelH = 240;
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        ms.pushPose();
        float scale = 0.88F + 0.12F * anim;
        ms.translate(this.width / 2f, this.height / 2f, 0);
        ms.scale(scale, scale, 1.0F);
        ms.translate(-this.width / 2f, -this.height / 2f, 0);

        GuiDraw.roundedPanel(ms, px, py, panelW, panelH, anim);

        // Gradient title bar
        int titleA = clamp((int)(anim * 255));
        int barTop = (titleA << 24) | 0x1A1430;
        int barBot = (titleA << 24) | 0x120E22;
        fillGradient(ms, px + 2, py + 2, px + panelW - 2, py + 36, barTop, barBot);

        // Title text
        int titleCol = (clamp((int)(anim * 255)) << 24) | 0xFFFFFF;
        drawCenteredString(ms, this.font, "✦ Cosmetics ✦", px + panelW / 2, py + 13, titleCol);

        // Accent underline (glow effect)
        int glowC = (clamp((int)(anim * 200)) << 24) | 0x9B6DFF;
        fill(ms, px + panelW / 2 - 55, py + 28, px + panelW / 2 + 55, py + 30, glowC);
        fill(ms, px + panelW / 2 - 40, py + 30, px + panelW / 2 + 40, py + 31,
                withAlpha(0xFF6040CC, anim * 0.6F));

        for (CategoryTab c : categoryTabs) c.draw(ms, mouseX, mouseY, anim, current);
        for (FeatureCard c : cards) c.draw(ms, mouseX, mouseY, anim);

        int hintA = clamp((int)(anim * 160));
        drawCenteredString(ms, this.font, "LMB toggle  |  RMB settings  |  ESC close",
                px + panelW / 2, py + panelH - 13, (hintA << 24) | 0xAAAAAA);

        ms.popPose();

        if (closing && anim <= 0.001F) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return false;
        int panelW = 380, panelH = 240;
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;
        for (CategoryTab c : categoryTabs) {
            if (c.contains(mx, my)) {
                current = c.category;
                rebuildCards(px, py);
                return true;
            }
        }
        for (FeatureCard c : cards) {
            if (c.contains(mx, my)) {
                if (button == 0) {
                    CosmeticsState.get().toggle(c.feature);
                } else if (button == 1) {
                    Minecraft.getInstance().setScreen(new SettingsScreen(c.feature));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 344) {
            closing = true; closingAtMs = System.currentTimeMillis(); return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void onClose() {
        if (!closing) { closing = true; closingAtMs = System.currentTimeMillis(); }
        else super.onClose();
    }

    private float animProgress() {
        long now = System.currentTimeMillis();
        if (!closing) {
            float t = Math.min(1F, (now - openedAtMs) / (float) ANIM_MS);
            return easeOut(t);
        } else {
            float t = Math.min(1F, (now - closingAtMs) / (float) ANIM_MS);
            return 1F - easeOut(t);
        }
    }

    private static float easeOut(float t) { return 1F - (1F - t) * (1F - t); }

    // ---- Widgets ---------------------------------------------------------------

    private class CategoryTab {
        final int x, y, w, h;
        final FeatureType.Category category;
        private float hoverAnim = 0F;

        CategoryTab(int x, int y, int w, int h, FeatureType.Category c) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.category = c;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void draw(MatrixStack ms, int mx, int my, float alpha, FeatureType.Category selected) {
            boolean hover = contains(mx, my);
            boolean sel = selected == category;
            hoverAnim += ((hover ? 1F : 0F) - hoverAnim) * 0.25F;

            int base = blendColor(0xFF1A1730, 0xFF2B2550, sel ? 1F : hoverAnim);
            fill(ms, x, y, x + w, y + h, withAlpha(base, alpha));

            // Left accent bar
            if (sel) {
                fill(ms, x, y, x + 3, y + h, withAlpha(0xFF9B6DFF, alpha));
                fill(ms, x + 3, y, x + 4, y + h, withAlpha(0x409B6DFF, alpha));
            } else if (hoverAnim > 0.05F) {
                fill(ms, x, y, x + 2, y + h, withAlpha(0xFF604090, (int)(alpha * hoverAnim)));
            }

            String label = category.name().charAt(0) + category.name().substring(1).toLowerCase();
            int textCol = withAlpha(sel ? 0xFFE8D8FF : 0xFFCCCCCC, alpha);
            drawString(ms, font, label, x + 10, y + (h - 8) / 2, textCol);
        }
    }

    private class FeatureCard {
        final int x, y, w, h;
        final FeatureType feature;
        private float hoverAnim = 0F;

        FeatureCard(int x, int y, int w, int h, FeatureType f) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.feature = f;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void draw(MatrixStack ms, int mx, int my, float alpha) {
            boolean hover = contains(mx, my);
            boolean on = CosmeticsState.get().isOn(feature);
            hoverAnim += ((hover ? 1F : 0F) - hoverAnim) * 0.25F;

            // Card background with hover glow
            int base = blendColor(0xFF16132A, 0xFF201C38, hoverAnim);
            fill(ms, x, y, x + w, y + h, withAlpha(base, alpha));

            // Enabled accent bar
            if (on) {
                fill(ms, x, y, x + 3, y + h, withAlpha(0xFF8A5CFF, alpha));
                fill(ms, x + 3, y, x + 5, y + h, withAlpha(0x508A5CFF, alpha));
            }

            // Hover shimmer on top edge
            if (hoverAnim > 0.05F) {
                fill(ms, x, y, x + w, y + 1, withAlpha(0xFF8A5CFF, alpha * hoverAnim * 0.5F));
            }

            // Feature name
            int nameCol = withAlpha(on ? 0xFFE8D8FF : 0xFFCCCCCC, alpha);
            drawString(ms, font, feature.displayName, x + 10, y + (h - 8) / 2, nameCol);

            // ON/OFF pill toggle
            int pillW = 28, pillH = 12;
            int pX = x + w - pillW - 8;
            int pY = y + (h - pillH) / 2;
            int pillBg = on ? withAlpha(0xFF7A4CFF, alpha) : withAlpha(0xFF3A3650, alpha);
            fill(ms, pX, pY, pX + pillW, pY + pillH, pillBg);

            // Knob
            int knobX = on ? pX + pillW - pillH + 1 : pX + 1;
            int knobY = pY + 1;
            int knobS = pillH - 2;
            fill(ms, knobX, knobY, knobX + knobS, knobY + knobS, withAlpha(0xFFFFFFFF, alpha));

            // Settings hint on hover
            if (hoverAnim > 0.3F && !on) {
                int hintA = (int)(alpha * hoverAnim * 120);
                drawString(ms, font, "RMB", pX - 22, y + (h - 8) / 2, (hintA << 24) | 0x9B6DFF);
            }
        }
    }

    // ---- Colour helpers --------------------------------------------------------

    private static int withAlpha(int argb, float alpha) {
        int a = clamp((int)((argb >>> 24 & 0xFF) * alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int withAlpha(int argb, int a) {
        return (clamp(a) << 24) | (argb & 0x00FFFFFF);
    }

    private static int blendColor(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int)(((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int)(((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int)((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
