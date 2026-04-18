package com.example.cosmetics.hud;

import com.example.cosmetics.client.CosmeticsState;
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
 * HUD that appears when the crosshair targets a mob/player.
 * Shows: 2D head icon, display name, smoothly animated HP bar,
 * red flash on hit, smooth fade in/out.
 */
public final class TargetHud {

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

        int sw = mc.getWindow().getGuiScaledWidth();

        int w = 160;
        int h = 44;
        int x = (sw - w) / 2;
        int y = 10;

        GuiDraw.roundedPanel(ms, x, y, w, h, shownAlpha);

        int headSize = 28;
        int headX = x + 7;
        int headY = y + (h - headSize) / 2;
        drawHead(ms, draw, headX, headY, headSize, shownAlpha);

        int a = Math.max(0, Math.min(255, (int)(shownAlpha * 255)));
        int textCol = (a << 24) | 0xFFFFFF;

        String name = draw.getDisplayName().getString();
        if (name.length() > 18) name = name.substring(0, 16) + "..";
        mc.font.drawShadow(ms, name, headX + headSize + 6, y + 7, textCol);

        // HP bar
        int barX = headX + headSize + 6;
        int barY = y + 20;
        int barW = w - (barX - x) - 8;
        int barH = 5;

        int bg = ((int)(shownAlpha * 100) << 24) | 0x111111;
        AbstractGui.fill(ms, barX, barY, barX + barW, barY + barH, bg);

        float fillPct = Math.max(0, Math.min(1, shownHp));
        int fillW = (int)(barW * fillPct);

        // Colour: green -> yellow -> red based on HP%, flashes red on hit
        int hr, hg, hb;
        if (flash > 0.05F) {
            hr = 255; hg = (int)(60 + (1 - flash) * 195); hb = (int)(60 + (1 - flash) * 195);
        } else if (fillPct > 0.6F) {
            hr = 50;  hg = 220; hb = 50;
        } else if (fillPct > 0.3F) {
            hr = 240; hg = 200; hb = 30;
        } else {
            hr = 230; hg = 40;  hb = 40;
        }
        int hpCol = (a << 24) | (hr << 16) | (hg << 8) | hb;
        if (fillW > 0) AbstractGui.fill(ms, barX, barY, barX + fillW, barY + barH, hpCol);

        // HP numbers
        String hpText = String.format("%.0f / %.0f", draw.getHealth(), draw.getMaxHealth());
        int smallA = Math.max(0, Math.min(255, (int)(shownAlpha * 180)));
        mc.font.drawShadow(ms, hpText, barX, barY + 8, (smallA << 24) | 0xCCCCCC);
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
