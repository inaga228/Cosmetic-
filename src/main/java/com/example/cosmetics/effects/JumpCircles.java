package com.example.cosmetics.effects;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.render.Primitives;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Jump/landing ring effects: when the local player jumps, an expanding ring
 * is spawned at their feet on the ground. The ring grows outward, fades out,
 * and is drawn flat on the XZ plane.
 *
 * <p>Styles (from {@link FeatureSettings#style}, floor-mod 3):
 * <ul>
 *   <li>0 — SOLID single ring, gentle grow</li>
 *   <li>1 — DOUBLE concentric rings (fast + slow)</li>
 *   <li>2 — RAINBOW hue-shifted ring</li>
 * </ul>
 *
 * <p>Also exposes {@link #spawnLanding} for the landing ring effect, which
 * uses a different default colour and smaller max radius.
 */
public final class JumpCircles {

    private static final JumpCircles INSTANCE = new JumpCircles();
    public static JumpCircles get() { return INSTANCE; }

    private static final int HARD_CAP = 64;
    private final List<Ring> rings = new ArrayList<>();

    public void tick() {
        Iterator<Ring> it = rings.iterator();
        while (it.hasNext()) {
            Ring r = it.next();
            r.age++;
            if (r.age >= r.maxAge) it.remove();
        }
    }

    /** Called when the local player jumps. Spawns the configured jump ring(s). */
    public void spawnJump(PlayerEntity player) {
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.JUMP_CIRCLES)) return;
        if (rings.size() >= HARD_CAP) return;

        FeatureSettings fs = state.settings(FeatureType.JUMP_CIRCLES);
        int style = Math.floorMod(fs.style, 3);
        float size = clampPos(fs.size, 0.25F, 3.0F);

        double px = player.getX();
        double py = player.getY() + 0.02;           // just above the feet
        double pz = player.getZ();

        int rgb = toRgb(fs.colorR, fs.colorG, fs.colorB);

        switch (style) {
            case 0:
                rings.add(new Ring(px, py, pz, 0.35F * size, 2.6F * size, 22, rgb, false));
                break;
            case 1:
                rings.add(new Ring(px, py, pz, 0.35F * size, 2.8F * size, 24, rgb, false));
                rings.add(new Ring(px, py, pz, 0.20F * size, 1.6F * size, 18, rgb, false));
                break;
            case 2: default:
                rings.add(new Ring(px, py, pz, 0.35F * size, 2.6F * size, 26, rgb, true));
                break;
        }
    }

    /** Called when the local player lands on the ground after being airborne. */
    public void spawnLanding(PlayerEntity player, float fallDistance) {
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.LANDING_RING)) return;
        if (fallDistance < 0.6F) return;           // skip micro-landings
        if (rings.size() >= HARD_CAP) return;

        FeatureSettings fs = state.settings(FeatureType.LANDING_RING);
        float size = clampPos(fs.size, 0.25F, 3.0F);
        float intensity = Math.min(1.0F, fallDistance / 5.0F);

        double px = player.getX();
        double py = player.getY() + 0.02;
        double pz = player.getZ();

        int rgb = toRgb(fs.colorR, fs.colorG, fs.colorB);
        float startR = 0.25F * size;
        float endR   = (1.6F + 2.2F * intensity) * size;
        int   life   = 16 + (int)(intensity * 8);
        rings.add(new Ring(px, py, pz, startR, endR, life, rgb, false));
    }

    public void renderAll(MatrixStack ms, float partialTicks) {
        if (rings.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();
        IVertexBuilder vb = buf.getBuffer(RenderType.lines());

        long now = System.currentTimeMillis();
        float hueBase = (now % 4000L) / 4000.0F;

        for (Ring r : rings) {
            float life = (r.age + partialTicks) / (float) r.maxAge;
            if (life < 0) life = 0; else if (life > 1) life = 1;

            float radius = r.startR + (r.endR - r.startR) * easeOut(life);
            int alpha = (int)(Math.max(0F, 1F - life) * 220);
            if (alpha <= 4) continue;

            int rgb = r.rainbow ? GuiDraw2.hsv(hueBase + life * 0.3F, 0.8F, 1.0F) : r.rgb;
            int cr = (rgb >> 16) & 0xFF;
            int cg = (rgb >>  8) & 0xFF;
            int cb =  rgb        & 0xFF;

            ms.pushPose();
            ms.translate(r.x - cam.x, r.y - cam.y, r.z - cam.z);

            int segs = 48;
            // Primary bright ring
            hollowRing(ms, vb, radius, segs, cr, cg, cb, alpha);
            // Inner faded ring for depth
            if (radius > 0.25F) {
                hollowRing(ms, vb, radius * 0.92F, segs, cr, cg, cb, alpha / 3);
            }

            ms.popPose();
        }

        buf.endBatch(RenderType.lines());
    }

    private static void hollowRing(MatrixStack ms, IVertexBuilder vb, float radius,
                                    int segs, int r, int g, int b, int a) {
        for (int i = 0; i < segs; i++) {
            double t0 = i * Math.PI * 2 / segs;
            double t1 = (i + 1) * Math.PI * 2 / segs;
            float x0 = (float)(Math.cos(t0) * radius);
            float z0 = (float)(Math.sin(t0) * radius);
            float x1 = (float)(Math.cos(t1) * radius);
            float z1 = (float)(Math.sin(t1) * radius);
            Primitives.line(ms, vb, x0, 0F, z0, x1, 0F, z1, r, g, b, a);
        }
    }

    private static float easeOut(float t) { return 1F - (1F - t) * (1F - t); }

    private static int toRgb(float r, float g, float b) {
        int ir = clamp((int)(r * 255));
        int ig = clamp((int)(g * 255));
        int ib = clamp((int)(b * 255));
        return (ir << 16) | (ig << 8) | ib;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static float clampPos(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private JumpCircles() {}

    private static final class Ring {
        final double x, y, z;
        final float startR, endR;
        final int maxAge;
        final int rgb;
        final boolean rainbow;
        int age = 0;

        Ring(double x, double y, double z, float startR, float endR, int maxAge,
             int rgb, boolean rainbow) {
            this.x = x; this.y = y; this.z = z;
            this.startR = startR; this.endR = endR;
            this.maxAge = maxAge; this.rgb = rgb; this.rainbow = rainbow;
        }
    }

    // Local helper so this class does not pull the gui package into world rendering.
    private static final class GuiDraw2 {
        static int hsv(float h, float s, float v) {
            return com.example.cosmetics.gui.GuiDraw.hsv(h, s, v);
        }
    }
}
