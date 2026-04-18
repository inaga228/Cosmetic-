package com.example.cosmetics.render;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.trails.TrailHistory;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D trail ribbon renderer.
 *
 * Builds a camera-facing ribbon out of the player's recent positions stored
 * in {@link TrailHistory}. Each consecutive pair of points produces a
 * translucent quad whose width tapers toward the tail; alpha fades with age.
 *
 * Styles (per trail):
 *   0 = RIBBON     — smooth ribbon facing the camera (classic)
 *   1 = BLADE      — thin vertical ribbon, glow-blended
 *   2 = DOUBLE     — two offset ribbons braiding
 */
public final class TrailRenderer {

    private static final long TRAIL_LIFETIME_MS = 1200L;

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        CosmeticsState s = CosmeticsState.get();

        List<TrailHistory.Point> pts = new ArrayList<>(TrailHistory.points());
        if (pts.size() < 2) return;

        Vector3d cam = mc.gameRenderer.getCamera().getPosition();

        boolean rainbow = s.isOn(FeatureType.RAINBOW_TRAIL);
        boolean flame   = s.isOn(FeatureType.FLAME_TRAIL);
        boolean galaxy  = s.isOn(FeatureType.GALAXY_TRAIL);
        if (!rainbow && !flame && !galaxy) return;

        IRenderTypeBuffer.Impl buf = mc.getRenderTypeBuffers().getBufferSource();

        if (rainbow) drawTrail(ms, buf, pts, cam, s.settings(FeatureType.RAINBOW_TRAIL),
                TrailKind.RAINBOW, partialTicks);
        if (flame)   drawTrail(ms, buf, pts, cam, s.settings(FeatureType.FLAME_TRAIL),
                TrailKind.FLAME, partialTicks);
        if (galaxy)  drawTrail(ms, buf, pts, cam, s.settings(FeatureType.GALAXY_TRAIL),
                TrailKind.GALAXY, partialTicks);

        buf.endBatch(ModRenderTypes.COLOR_QUADS);
        buf.endBatch(ModRenderTypes.GLOW_QUADS);
    }

    private enum TrailKind { RAINBOW, FLAME, GALAXY }

    private static void drawTrail(MatrixStack ms, IRenderTypeBuffer buf,
                                  List<TrailHistory.Point> pts, Vector3d cam,
                                  FeatureSettings fs, TrailKind kind, float partialTicks) {

        int style = Math.floorMod(fs.style, 3);
        boolean glow = style == 1 || kind == TrailKind.GALAXY;
        IVertexBuilder vb = buf.getBuffer(glow ? ModRenderTypes.GLOW_QUADS : ModRenderTypes.COLOR_QUADS);

        float baseW = Math.max(0.05F, fs.size) * (style == 1 ? 0.35F : 0.55F);
        Matrix4f pose = ms.last().pose();

        long now = System.currentTimeMillis();

        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            TrailHistory.Point a = pts.get(i);
            TrailHistory.Point b = pts.get(i + 1);

            float ageA = Math.min(1F, (now - a.timeMs) / (float) TRAIL_LIFETIME_MS);
            float ageB = Math.min(1F, (now - b.timeMs) / (float) TRAIL_LIFETIME_MS);
            // Fade to zero at the tail; full alpha at the head.
            float headA = 1F - ageA;
            float headB = 1F - ageB;

            // Ribbon width tapers from head→tail for a blade look.
            float tA = i / (float) (n - 1);
            float tB = (i + 1) / (float) (n - 1);
            float widthA = baseW * (0.15F + 0.85F * tA);
            float widthB = baseW * (0.15F + 0.85F * tB);

            // Build a "right" vector that is perpendicular to the segment and
            // faces the camera — classic billboard ribbon.
            double sx = b.x - a.x, sy = b.y - a.y, sz = b.z - a.z;
            double slen = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (slen < 1e-6) continue;
            double dnx = sx / slen, dny = sy / slen, dnz = sz / slen;

            double cxm = (a.x + b.x) * 0.5 - cam.x;
            double cym = (a.y + b.y) * 0.5 - cam.y;
            double czm = (a.z + b.z) * 0.5 - cam.z;

            // right = normalize( dir × toCam )
            double rx = dny * czm - dnz * cym;
            double ry = dnz * cxm - dnx * czm;
            double rz = dnx * cym - dny * cxm;
            double rlen = Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (rlen < 1e-6) { rx = 0; ry = 1; rz = 0; rlen = 1; }
            rx /= rlen; ry /= rlen; rz /= rlen;

            // For "blade" style the ribbon is vertical (world-up), not camera-facing.
            if (style == 1) { rx = 0; ry = 1; rz = 0; }

            double ax = a.x - cam.x, ay = a.y - cam.y, az = a.z - cam.z;
            double bx = b.x - cam.x, by = b.y - cam.y, bz = b.z - cam.z;

            // Color per endpoint
            int[] cA = colorFor(kind, fs, tA, headA);
            int[] cB = colorFor(kind, fs, tB, headB);

            emitQuad(vb, pose,
                    (float)(ax + rx * widthA), (float)(ay + ry * widthA), (float)(az + rz * widthA),
                    (float)(ax - rx * widthA), (float)(ay - ry * widthA), (float)(az - rz * widthA),
                    (float)(bx - rx * widthB), (float)(by - ry * widthB), (float)(bz - rz * widthB),
                    (float)(bx + rx * widthB), (float)(by + ry * widthB), (float)(bz + rz * widthB),
                    cA, cB);

            // DOUBLE style: draw a second, narrower, vertically-offset ribbon.
            if (style == 2) {
                float off = widthA * 0.4F;
                emitQuad(vb, pose,
                        (float)(ax + rx * widthA * 0.5F), (float)(ay + ry * widthA * 0.5F + off), (float)(az + rz * widthA * 0.5F),
                        (float)(ax - rx * widthA * 0.5F), (float)(ay - ry * widthA * 0.5F + off), (float)(az - rz * widthA * 0.5F),
                        (float)(bx - rx * widthB * 0.5F), (float)(by - ry * widthB * 0.5F + off), (float)(bz - rz * widthB * 0.5F),
                        (float)(bx + rx * widthB * 0.5F), (float)(by + ry * widthB * 0.5F + off), (float)(bz + rz * widthB * 0.5F),
                        cA, cB);
            }
        }
    }

    private static int[] colorFor(TrailKind kind, FeatureSettings fs, float t, float fade) {
        int r, g, b;
        switch (kind) {
            case RAINBOW: {
                // Cycle hue along ribbon + over time for motion
                float hue = (t * 1.2F + (System.currentTimeMillis() % 4000L) / 4000F) % 1F;
                int rgb = java.awt.Color.HSBtoRGB(hue, 1F, 1F);
                r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF;
                break;
            }
            case FLAME: {
                // Head = white-yellow, middle = orange, tail = dark red
                float k = 1F - t;
                r = 255;
                g = clamp255((int)(230 * k));
                b = clamp255((int)(80 * k * k));
                break;
            }
            case GALAXY:
            default: {
                r = clamp255((int)(fs.colorR * 255));
                g = clamp255((int)(fs.colorG * 255));
                b = clamp255((int)(fs.colorB * 255));
                // twinkle: slight bright flicker along head
                float flick = 0.7F + 0.3F * (float) Math.sin((System.currentTimeMillis() % 1000L) * 0.02 + t * 6);
                r = clamp255((int)(r * flick));
                g = clamp255((int)(g * flick));
                b = clamp255((int)(b * flick));
                break;
            }
        }
        int a = clamp255((int)(fade * 220));
        return new int[]{ r, g, b, a };
    }

    private static void emitQuad(IVertexBuilder vb, Matrix4f pose,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 int[] cA, int[] cB) {
        vb.vertex(pose, x1, y1, z1).color(cA[0], cA[1], cA[2], cA[3]).endVertex();
        vb.vertex(pose, x2, y2, z2).color(cA[0], cA[1], cA[2], cA[3]).endVertex();
        vb.vertex(pose, x3, y3, z3).color(cB[0], cB[1], cB[2], cB[3]).endVertex();
        vb.vertex(pose, x4, y4, z4).color(cB[0], cB[1], cB[2], cB[3]).endVertex();
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private TrailRenderer() {}
}
