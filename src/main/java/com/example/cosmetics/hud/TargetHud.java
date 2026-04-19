package com.example.cosmetics.hud;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.config.HudPositionManager;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.GuiDraw;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/**
 * Target HUD — shown when the crosshair points at a mob/player.
 *
 * Styles (via Caps.STYLE):
 *   0 = CLASSIC — head icon + name + HP bar on top of the screen
 *   1 = SLIM    — thin flat card with HP bar only, no head
 *   2 = NOVUS   — wide dark flat card with gradient HP bar and subtle glow
 *   3 = TAB     — right-side vertical tab (head + vertical HP bar)
 */
public final class TargetHud {

    public static final String[] STYLE_NAMES = { "Classic", "Slim", "Novus", "Tab" };

    private static LivingEntity lastTarget;
    private static long lastSeenMs;
    private static float shownAlpha = 0.0F;
    private static float shownHp = 0.0F;
    private static float flash = 0.0F;

    private static final long FADE_OUT_MS = 1200L;
    private static final ResourceLocation FALLBACK_SKIN =
            new ResourceLocation("minecraft", "textures/entity/steve.png");

    public static void onLivingHurt(LivingEntity ent) {
        if (ent == lastTarget) flash = 1.0F;
    }

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.TARGET_HUD)) return;
        if (mc.player == null) return;

        LivingEntity target = resolveTarget();
        long now = System.currentTimeMillis();
        if (target != null) {
            lastTarget = target;
            lastSeenMs = now;
        }

        boolean visible = target != null || (lastTarget != null && (now - lastSeenMs) < FADE_OUT_MS);
        float targetAlpha = visible ? 1.0F : 0.0F;
        shownAlpha += (targetAlpha - shownAlpha) * 0.12F;
        if (shownAlpha < 0.01F || lastTarget == null) return;

        LivingEntity draw = target != null ? target : lastTarget;

        float realHp = draw.getHealth() / Math.max(0.001F, draw.getMaxHealth());
        shownHp += (realHp - shownHp) * 0.12F;
        flash = Math.max(0.0F, flash - 0.04F);

        FeatureSettings fs = state.settings(FeatureType.TARGET_HUD);
        int style = Math.floorMod(fs.style, STYLE_NAMES.length);

        switch (style) {
            case 1: renderSlim(mc, ms, draw); break;
            case 2: renderNovus(mc, ms, draw, fs); break;
            case 3: renderTab(mc, ms, draw); break;
            case 0:
            default: renderClassic(mc, ms, draw); break;
        }
    }

    // --- style 0: classic (original) ---
    private static void renderClassic(Minecraft mc, MatrixStack ms, LivingEntity draw) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int w = 160, h = 44;
        int x = HudPositionManager.get().getTargetX(w);
        int y = HudPositionManager.get().getTargetY();

        GuiDraw.roundedPanel(ms, x, y, w, h, shownAlpha);

        int headSize = 28;
        int headX = x + 7;
        int headY = y + (h - headSize) / 2;
        drawHead(ms, draw, headX, headY, headSize, shownAlpha);

        int a = alpha255(1F);
        int textCol = (a << 24) | 0xFFFFFF;
        String name = safeName(draw, 18);
        mc.font.drawShadow(ms, name, headX + headSize + 6, y + 7, textCol);

        drawHpBar(ms, headX + headSize + 6, y + 20, w - (headX + headSize + 6 - x) - 8, 5);

        String hpText = String.format("%.0f / %.0f", draw.getHealth(), draw.getMaxHealth());
        int smallA = alpha255(0.7F);
        mc.font.drawShadow(ms, hpText, headX + headSize + 6, y + 28, (smallA << 24) | 0xCCCCCC);
    }

    // --- style 1: slim (compact bar, no head) ---
    private static void renderSlim(Minecraft mc, MatrixStack ms, LivingEntity draw) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int w = 140, h = 18;
        int x = HudPositionManager.get().getTargetX(w);
        int y = HudPositionManager.get().getTargetY();

        int a = alpha255(1F);
        AbstractGui.fill(ms, x, y, x + w, y + h, (alpha255(0.85F) << 24) | 0x0E0E14);
        AbstractGui.fill(ms, x, y, x + 2, y + h, (a << 24) | 0xFF4A6BFF);

        String name = safeName(draw, 20);
        mc.font.drawShadow(ms, name, x + 6, y + 2, (a << 24) | 0xFFFFFF);

        drawHpBar(ms, x + 6, y + 12, w - 12, 3);
    }

    // --- style 2: novus (wide flat card with gradient bar) ---
    private static void renderNovus(Minecraft mc, MatrixStack ms, LivingEntity draw, FeatureSettings fs) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int w = 200, h = 36;
        int x = HudPositionManager.get().getTargetX(w);
        int y = HudPositionManager.get().getTargetY();

        int accent = accentFromColor(fs);
        GuiDraw.themedPanel(ms, x, y, w, h, shownAlpha, 0x101018, 0x08080E, accent);

        int headSize = 22;
        int headX = x + 7;
        int headY = y + (h - headSize) / 2;
        drawHead(ms, draw, headX, headY, headSize, shownAlpha);

        int a = alpha255(1F);
        String name = safeName(draw, 22);
        mc.font.drawShadow(ms, name, headX + headSize + 6, y + 6, (a << 24) | 0xFFFFFF);

        // Gradient HP bar
        int barX = headX + headSize + 6;
        int barY = y + 20;
        int barW = w - (barX - x) - 8;
        int barH = 6;
        int bg = (alpha255(0.45F) << 24) | 0x111116;
        AbstractGui.fill(ms, barX, barY, barX + barW, barY + barH, bg);
        int fillW = (int)(barW * Math.max(0, Math.min(1, shownHp)));
        if (fillW > 0) {
            int left = hpColorArgb();
            int right = accent;
            GuiDraw.fillGradientRect(ms, barX, barY, barX + fillW, barY + barH, left, right);
        }

        String hpText = String.format("%.0f / %.0f ♥", draw.getHealth(), draw.getMaxHealth());
        int smallA = alpha255(0.75F);
        mc.font.drawShadow(ms, hpText, barX + barW - mc.font.width(hpText), y + 6, (smallA << 24) | 0xCCCCDD);
    }

    // --- style 3: right-side vertical tab ---
    private static void renderTab(Minecraft mc, MatrixStack ms, LivingEntity draw) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int w = 52, h = 88;
        int x = sw - w - 8, y = (sh - h) / 2;

        GuiDraw.roundedPanel(ms, x, y, w, h, shownAlpha);

        int headSize = 30;
        int headX = x + (w - headSize) / 2;
        int headY = y + 6;
        drawHead(ms, draw, headX, headY, headSize, shownAlpha);

        int a = alpha255(1F);
        String name = safeName(draw, 8);
        int nw = mc.font.width(name);
        mc.font.drawShadow(ms, name, x + (w - nw) / 2, headY + headSize + 4, (a << 24) | 0xFFFFFF);

        // Vertical HP bar
        int barX = x + (w - 6) / 2;
        int barY = headY + headSize + 16;
        int barW = 6;
        int barH = h - (barY - y) - 8;
        int bg = (alpha255(0.45F) << 24) | 0x111116;
        AbstractGui.fill(ms, barX, barY, barX + barW, barY + barH, bg);
        int fillH = (int)(barH * Math.max(0, Math.min(1, shownHp)));
        int hpCol = hpColorArgb();
        if (fillH > 0) {
            AbstractGui.fill(ms, barX, barY + (barH - fillH), barX + barW, barY + barH, hpCol);
        }
    }

    // --- helpers ---

    private static void drawHpBar(MatrixStack ms, int barX, int barY, int barW, int barH) {
        int bg = (alpha255(0.4F) << 24) | 0x111111;
        AbstractGui.fill(ms, barX, barY, barX + barW, barY + barH, bg);
        float pct = Math.max(0, Math.min(1, shownHp));
        int fillW = (int)(barW * pct);
        int col = hpColorArgb();
        if (fillW > 0) AbstractGui.fill(ms, barX, barY, barX + fillW, barY + barH, col);
    }

    private static int hpColorArgb() {
        int a = alpha255(1F);
        int hr, hg, hb;
        float pct = Math.max(0, Math.min(1, shownHp));
        if (flash > 0.05F) {
            hr = 255; hg = (int)(60 + (1 - flash) * 195); hb = (int)(60 + (1 - flash) * 195);
        } else if (pct > 0.6F) { hr = 50;  hg = 220; hb = 50;
        } else if (pct > 0.3F) { hr = 240; hg = 200; hb = 30;
        } else                 { hr = 230; hg = 40;  hb = 40; }
        return (a << 24) | (hr << 16) | (hg << 8) | hb;
    }

    private static int accentFromColor(FeatureSettings fs) {
        int r = Math.max(0, Math.min(255, (int)(fs.colorR * 255)));
        int g = Math.max(0, Math.min(255, (int)(fs.colorG * 255)));
        int b = Math.max(0, Math.min(255, (int)(fs.colorB * 255)));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String safeName(LivingEntity e, int max) {
        String name = e.getDisplayName().getString();
        if (name.length() > max) name = name.substring(0, Math.max(1, max - 2)) + "..";
        return name;
    }

    private static int alpha255(float mul) {
        return Math.max(0, Math.min(255, (int)(shownAlpha * 255 * mul)));
    }

    private static void drawHead(MatrixStack ms, LivingEntity ent, int x, int y, int size, float alpha) {
        ResourceLocation tex = FALLBACK_SKIN;
        if (ent instanceof AbstractClientPlayerEntity) {
            tex = ((AbstractClientPlayerEntity) ent).getSkinTextureLocation();
        }
        Minecraft.getInstance().getTextureManager().bind(tex);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1, 1, 1, alpha);
        AbstractGui.blit(ms, x, y, size, size, 8F, 8F, 8, 8, 64, 64);
        AbstractGui.blit(ms, x, y, size, size, 40F, 8F, 8, 8, 64, 64);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static LivingEntity resolveTarget() {
        Minecraft mc = Minecraft.getInstance();
        RayTraceResult rt = mc.hitResult;
        if (rt == null || rt.getType() != RayTraceResult.Type.ENTITY) return null;
        Entity e = ((EntityRayTraceResult) rt).getEntity();
        if (!(e instanceof LivingEntity)) return null;
        return (LivingEntity) e;
    }

    private TargetHud() {}
}
