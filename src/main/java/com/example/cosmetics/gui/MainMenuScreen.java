package com.example.cosmetics.gui;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main cosmetics menu.
 * Left side: category tabs. Right side: feature cards.
 * LMB = toggle, RMB = open settings.
 * Polished gradient style: glow panel, animated star-dust background,
 * hue-cycled rainbow title, smooth hover/slide-in transitions.
 */
public class MainMenuScreen extends Screen {

    private FeatureType.Category current = FeatureType.Category.TRAILS;
    private long openedAtMs;
    private boolean closing = false;
    private long closingAtMs;
    private static final long ANIM_MS = 260L;

    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private final List<FeatureCard> cards = new ArrayList<>();

    // Decorative "dust" floating in the background — just for eye candy.
    private static final int DUST_COUNT = 36;
    private final float[] dustX = new float[DUST_COUNT];
    private final float[] dustY = new float[DUST_COUNT];
    private final float[] dustSpeed = new float[DUST_COUNT];
    private final float[] dustPhase = new float[DUST_COUNT];
    private final float[] dustSize = new float[DUST_COUNT];

    public MainMenuScreen() { super(new StringTextComponent("Cosmetics")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        openedAtMs = System.currentTimeMillis();
        closing = false;
        categoryTabs.clear();

        int panelW = 400;
        int panelH = 250;
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        int i = 0;
        for (FeatureType.Category c : FeatureType.Category.values()) {
            categoryTabs.add(new CategoryTab(px + 8, py + 48 + i * 25, 100, 22, c));
            i++;
        }
        rebuildCards(px, py);

        // Seed dust
        Random rnd = new Random(0xC051E7); // stable per-open dust layout
        for (int k = 0; k < DUST_COUNT; k++) {
            dustX[k] = rnd.nextFloat();
            dustY[k] = rnd.nextFloat();
            dustSpeed[k] = 0.03F + rnd.nextFloat() * 0.08F;
            dustPhase[k] = rnd.nextFloat() * (float) (Math.PI * 2);
            dustSize[k] = 1 + rnd.nextInt(2);
        }
    }

    private void rebuildCards(int px, int py) {
        cards.clear();
        int cx = px + 118;
        int cy = py + 48;
        int cw = 272;
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

        // Backdrop dim
        fill(ms, 0, 0, this.width, this.height, (int)(anim * 155) << 24);

        // Floating dust particles (decorative)
        drawDust(ms, anim);

        int panelW = 400;
        int panelH = 250;
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        ms.pushPose();
        float scale = 0.88F + 0.12F * anim;
        ms.translate(this.width / 2f, this.height / 2f, 0);
        ms.scale(scale, scale, 1.0F);
        ms.translate(-this.width / 2f, -this.height / 2f, 0);

        long now = System.currentTimeMillis();
        float hueBase = (now % 6000L) / 6000.0F;
        int glowAccent = GuiDraw.hsv(hueBase, 0.65F, 1.0F);

        GuiDraw.roundedPanelAccent(ms, px, py, panelW, panelH, anim, glowAccent);

        // Title bar
        int titleA = clamp((int)(anim * 255));
        int barTop = (titleA << 24) | 0x1B1530;
        int barBot = (titleA << 24) | 0x120D22;
        fillGradient(ms, px + 2, py + 2, px + panelW - 2, py + 40, barTop, barBot);

        // Title — rainbow gradient per-letter
        String leftStar = "\u2726";  // ✦
        String titleText = " Cosmetics ";
        String rightStar = "\u2726";
        int full = this.font.width(leftStar + titleText + rightStar);
        int titleX = px + panelW / 2 - full / 2;
        int titleY = py + 15;

        // Side stars
        int starCol = GuiDraw.argb(anim, GuiDraw.hsv(hueBase + 0.0F, 0.9F, 1.0F));
        this.font.drawShadow(ms, leftStar, titleX, titleY, starCol);
        titleX += this.font.width(leftStar);

        // Rainbow animated letters
        for (int i = 0; i < titleText.length(); i++) {
            char c = titleText.charAt(i);
            float h = hueBase + i * 0.04F;
            int col = GuiDraw.argb(anim, GuiDraw.hsv(h, 0.85F, 1.0F));
            String s = String.valueOf(c);
            this.font.drawShadow(ms, s, titleX, titleY, col);
            titleX += this.font.width(s);
        }
        int starCol2 = GuiDraw.argb(anim, GuiDraw.hsv(hueBase + 0.4F, 0.9F, 1.0F));
        this.font.drawShadow(ms, rightStar, titleX, titleY, starCol2);

        // Underline glow
        int glowC = (clamp((int)(anim * 200)) << 24) | 0x9B6DFF;
        fill(ms, px + panelW / 2 - 60, py + 31, px + panelW / 2 + 60, py + 33, glowC);
        int inner = (clamp((int)(anim * 140)) << 24) | 0xFF6AA8FF;
        fill(ms, px + panelW / 2 - 40, py + 33, px + panelW / 2 + 40, py + 34, inner);

        // Left sidebar tint strip
        int sideA = clamp((int)(anim * 95));
        fill(ms, px + 4, py + 44, px + 114, py + panelH - 18, (sideA << 24) | 0x130E24);

        for (CategoryTab c : categoryTabs) c.draw(ms, mouseX, mouseY, anim, current);
        for (FeatureCard c : cards) c.draw(ms, mouseX, mouseY, anim);

        // Footer
        int hintA = clamp((int)(anim * 170));
        drawCenteredString(ms, this.font, "LMB toggle  |  RMB settings  |  ESC close",
                px + panelW / 2, py + panelH - 13, (hintA << 24) | 0xB0A4D0);

        // Version tag, bottom-left
        int vA = clamp((int)(anim * 130));
        this.font.draw(ms, "v1.0.0", px + 8, py + panelH - 13, (vA << 24) | 0x7A6CA0);

        ms.popPose();

        if (closing && anim <= 0.001F) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    private void drawDust(MatrixStack ms, float anim) {
        long now = System.currentTimeMillis();
        for (int k = 0; k < DUST_COUNT; k++) {
            float dy = dustY[k] + (now / 1000f) * dustSpeed[k] * 0.06F;
            dy = dy - (float) Math.floor(dy);
            float twinkle = 0.5F + 0.5F * (float) Math.sin(now / 500.0 + dustPhase[k]);
            int col = (int)(anim * twinkle * 80) << 24 | 0x9B6DFF;
            int px = (int)(dustX[k] * this.width);
            int py = (int)(dy * this.height);
            int s = (int) dustSize[k];
            fill(ms, px, py, px + s, py + s, col);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return false;
        int panelW = 400, panelH = 250;
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
                // Slide-in highlight
                int hi = (clamp((int)(alpha * 25)) << 24) | 0xFFFFFF;
                fill(ms, x + 4, y, x + w, y + 1, hi);
            } else if (hoverAnim > 0.05F) {
                fill(ms, x, y, x + 2, y + h, withAlpha(0xFF604090, alpha * hoverAnim));
            }

            String label = category.name().charAt(0) + category.name().substring(1).toLowerCase();
            int textCol = withAlpha(sel ? 0xFFF0E0FF : 0xFFCCBED8, alpha);
            drawString(ms, font, label, x + 12, y + (h - 8) / 2, textCol);
        }
    }

    private class FeatureCard {
        final int x, y, w, h;
        final FeatureType feature;
        private float hoverAnim = 0F;
        private float knobAnim = 0F;

        FeatureCard(int x, int y, int w, int h, FeatureType f) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.feature = f;
            this.knobAnim = CosmeticsState.get().isOn(f) ? 1F : 0F;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void draw(MatrixStack ms, int mx, int my, float alpha) {
            boolean hover = contains(mx, my);
            boolean on = CosmeticsState.get().isOn(feature);
            hoverAnim += ((hover ? 1F : 0F) - hoverAnim) * 0.25F;
            knobAnim  += ((on   ? 1F : 0F) - knobAnim)  * 0.3F;

            // Card background with hover glow
            int base = blendColor(0xFF15122A, 0xFF211C3A, hoverAnim);
            fill(ms, x, y, x + w, y + h, withAlpha(base, alpha));

            // Enabled accent bar (with soft edge)
            if (on) {
                fill(ms, x, y, x + 3, y + h, withAlpha(0xFF8A5CFF, alpha));
                fill(ms, x + 3, y, x + 5, y + h, withAlpha(0x508A5CFF, alpha));
            }

            // Hover shimmer on top edge
            if (hoverAnim > 0.05F) {
                fill(ms, x, y, x + w, y + 1, withAlpha(0xFF8A5CFF, alpha * hoverAnim * 0.5F));
            }

            // Feature name
            int nameCol = withAlpha(on ? 0xFFF0E0FF : 0xFFCCBED8, alpha);
            drawString(ms, font, feature.displayName, x + 12, y + (h - 8) / 2, nameCol);

            // ON/OFF pill toggle (knob position smoothly animated)
            int pillW = 30, pillH = 14;
            int pX = x + w - pillW - 8;
            int pY = y + (h - pillH) / 2;
            int pillBgOff = 0xFF3A3452;
            int pillBgOn  = 0xFF7A4CFF;
            int pillBg = blendColor(pillBgOff, pillBgOn, knobAnim);
            fill(ms, pX, pY, pX + pillW, pY + pillH, withAlpha(pillBg, alpha));

            // Pill inner highlight
            int pillHi = (clamp((int)(alpha * 50)) << 24) | 0xFFFFFF;
            fill(ms, pX + 1, pY + 1, pX + pillW - 1, pY + 2, pillHi);

            // Knob (smooth slide)
            int knobS = pillH - 4;
            int knobX = pX + 2 + (int)((pillW - knobS - 4) * knobAnim);
            int knobY = pY + 2;
            int knobCol = withAlpha(0xFFFFFFFF, alpha);
            fill(ms, knobX, knobY, knobX + knobS, knobY + knobS, knobCol);

            // Settings hint on hover
            if (hoverAnim > 0.3F && !on) {
                int hintA = (int)(alpha * hoverAnim * 120);
                drawString(ms, font, "RMB", pX - 24, y + (h - 8) / 2, (hintA << 24) | 0x9B6DFF);
            }
        }
    }

    // ---- Colour helpers --------------------------------------------------------

    private static int withAlpha(int argb, float alpha) {
        int a = clamp((int)((argb >>> 24 & 0xFF) * alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
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
