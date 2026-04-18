package com.example.cosmetics.gui;

import com.example.cosmetics.CosmeticsMod;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
// NOTE: vanilla's main-menu screen has the same simple name as our own
// com.example.cosmetics.gui.MainMenuScreen (which sits in THIS package) so
// importing it would be a compile-time ambiguity. We fully-qualify the
// instanceof check instead.
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Beautifies Minecraft's own title screen (the vanilla main menu you see on
 * launch). Draws on top of the vanilla screen in a Post event so the vanilla
 * widgets and background still show through cleanly.
 *
 * What we add:
 *  - A soft, animated aurora gradient band across the top and bottom of the
 *    screen (purple → pink → cyan) that breathes with a slow sine.
 *  - A subtle vignette that darkens the screen edges so the logo pops.
 *  - A centered glowing "Cosmetics" tagline just under the logo.
 *  - A dim "mod active" footer chip in the bottom-left showing the mod name.
 *
 * All drawing is done with {@link AbstractGui#fill} / fillGradient — no
 * texture loading, no extra GL state — so it is cheap and safe.
 */
@Mod.EventBusSubscriber(modid = CosmeticsMod.MOD_ID, value = Dist.CLIENT)
public final class TitleScreenDecor extends AbstractGui {

    private static final TitleScreenDecor I = new TitleScreenDecor();
    private static final long START_MS = System.currentTimeMillis();

    @SubscribeEvent
    public static void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof net.minecraft.client.gui.screen.MainMenuScreen)) return;
        MatrixStack ms = event.getMatrixStack();
        int w = event.getGui().width;
        int h = event.getGui().height;
        I.draw(ms, w, h);
    }

    private void draw(MatrixStack ms, int w, int h) {
        long now = System.currentTimeMillis();
        float t = (now - START_MS) / 1000F;
        // Slow breathing in [0.6 .. 1.0]
        float breathe = 0.80F + 0.20F * (float) Math.sin(t * 0.6F);

        // --- Aurora band at the top -----------------------------------------
        int bandH = Math.max(12, h / 10);
        int purple = 0x9B6DFF;
        int pink   = 0xFF6DC1;
        int cyan   = 0x6DE6FF;
        // Split into thirds to fake a 3-stop horizontal gradient using two
        // fillGradient calls (vanilla fillGradient is horizontal-capable? no —
        // it is vertical only, so we compose it as vertical bands and repeat
        // horizontally with segments).
        drawAuroraBand(ms, 0, 0, w, bandH, purple, pink, cyan, breathe);
        drawAuroraBand(ms, 0, h - bandH, w, bandH, cyan, pink, purple, breathe * 0.9F);

        // --- Vignette -------------------------------------------------------
        int vinA = clamp((int)(100 * breathe));
        fillGradient(ms, 0, 0, w, 6,               (vinA << 24),              0);
        fillGradient(ms, 0, h - 6, w, h,           0,                          (vinA << 24));
        // side vignette — done as very thin columns
        fillGradient(ms, 0, 0,      8,  h,          (vinA << 24) | 0x000000,   0);
        fillGradient(ms, w - 8, 0,  w,  h,          0,                          (vinA << 24));

        // --- Tagline under the logo -----------------------------------------
        FontRenderer font = Minecraft.getInstance().font;
        String tag = "\u2726 Cosmetics Mod \u2726";
        int tagColAlpha = clamp((int)(230 * breathe));
        int tagCol = (tagColAlpha << 24) | 0xE8D8FF;
        // Minecraft's logo sits around y=30..75; we draw our tag at ~85.
        int tx = (w - font.width(tag)) / 2;
        int ty = 88;
        // Soft glow: draw slight blurs (by redraw with alpha).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int glowA = clamp((int)(70 * breathe));
                font.draw(ms, tag, tx + dx, ty + dy, (glowA << 24) | 0x9B6DFF);
            }
        }
        font.drawShadow(ms, tag, tx, ty, tagCol);

        // --- Footer chip (bottom-left): "Cosmetics Mod • v1.0" --------------
        String chip = "\u2726 Cosmetics Mod";
        int chipW = font.width(chip) + 12;
        int chipH = 14;
        int cx = 6;
        int cy = h - chipH - 6;
        int chipBg = (clamp((int)(140 * breathe)) << 24) | 0x1A1430;
        int chipAccent = (clamp((int)(200 * breathe)) << 24) | 0x9B6DFF;
        fill(ms, cx,     cy,     cx + chipW, cy + chipH, chipBg);
        fill(ms, cx,     cy,     cx + 2,     cy + chipH, chipAccent);
        font.drawShadow(ms, chip, cx + 6, cy + 3, 0xFFE8D8FF);
    }

    /**
     * Draws an "aurora" band: three vertical gradients in three horizontal
     * segments, with the transitions tinted to blend.  Not a true 3-stop
     * gradient but visually identical at this scale.
     */
    private void drawAuroraBand(MatrixStack ms, int x, int y, int w, int h,
                                int c1, int c2, int c3, float alpha) {
        int aTop = clamp((int)(200 * alpha));
        int aBot = clamp((int)(10  * alpha));
        int third = w / 3;

        // Segment 1: c1 → c2
        GuiDraw.fillGradientRect(ms, x,              y, x + third,     y + h,
                packAH(c1, aTop), packAH(c2, aTop));
        GuiDraw.fillGradientRect(ms, x,              y, x + third,     y + h,
                packAH(c1, aTop), packAH(c1, aBot));

        // Segment 2: c2 (mid)
        GuiDraw.fillGradientRect(ms, x + third,      y, x + third * 2, y + h,
                packAH(c2, aTop), packAH(c3, aTop));
        GuiDraw.fillGradientRect(ms, x + third,      y, x + third * 2, y + h,
                packAH(c2, aTop), packAH(c2, aBot));

        // Segment 3: c3
        GuiDraw.fillGradientRect(ms, x + third * 2,  y, x + w,         y + h,
                packAH(c3, aTop), packAH(c3, aTop));
        GuiDraw.fillGradientRect(ms, x + third * 2,  y, x + w,         y + h,
                packAH(c3, aTop), packAH(c3, aBot));
    }

    private static int packAH(int rgb, int a) {
        return (clamp(a) << 24) | (rgb & 0xFFFFFF);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private TitleScreenDecor() {}
}
