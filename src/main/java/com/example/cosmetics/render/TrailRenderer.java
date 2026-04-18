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
 *
 * Hidden entirely in first-person so the ribbon does not pass through the
 * crosshair / viewmodel.
 */
public final class TrailRenderer {

    private static final long TRAIL_LIFETIME_MS = 1200L;

    // Reusable snapshot buffer — avoids allocating a new ArrayList per
    // frame. Grown on demand; only ever written from the render thread.
    private static TrailHistory.Point[] SNAP = new TrailHistory.Point[TrailHistory.MAX_POINTS + 4];

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Trails belong to the local player — don't render them when the
        // local camera is first-person, otherwise the ribbon passes through
        // the crosshair and blocks the view.
        if (HatRenderer.isFirstPerson(mc)) return;

        CosmeticsState s = CosmeticsState.get();

        boolean rainbow = s.isOn(FeatureType.RAINBOW_TRAIL);
        boolean flame   = s.isOn(FeatureType.FLAME_TRAIL);
        boolean galaxy  = s.isOn(FeatureType.GALAXY_TRAIL);
        if (!rainbow && !flame && !galaxy) return;

        // Snapshot points into our pooled array (no allocation). Both
        // TrailTicker.tick and this render path are on the client thread
        // so the iteration is safe without locking.
        int size = TrailHistory.points().size();
        if (SNAP.length < size) SNAP = new TrailHistory.Point[size];
        int n = 0;
        for (TrailHistory.Point p : TrailHistory.points()) SNAP[n++] = p;
        if (n < 2) return;

        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();

        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();

        if (rainbow) drawTrail(ms, buf, SNAP, n, cam, s.settings(FeatureType.RAINBOW_TRAIL),
                TrailKind.RAINBOW, partialTicks);
        if (flame)   drawTrail(ms, buf, SNAP, n, cam, s.settings(FeatureType.FLAME_TRAIL),
                TrailKind.FLAME, partialTicks);
        if (galaxy)  drawTrail(ms, buf, SNAP, n, cam, s.settings(FeatureType.GALAXY_TRAIL),
                TrailKind.GALAXY, partialTicks);

        buf.endBatch(ModRenderTypes.COLOR_QUADS);
        buf.endBatch(ModRenderTypes.GLOW_QUADS);

        // Clear references so the GC can reclaim expired Point objects.
        for (int i = 0; i < n; i++) SNAP[i] = null;
    }

    private enum TrailKind { RAINBOW, FLAME, GALAXY }

    private static void drawTrail(MatrixStack ms, IRenderTypeBuffer buf,
                                  TrailHistory.Point[] pts, int n, Vector3d cam,
                                  FeatureSettings fs, TrailKind kind, float partialTicks) {

        int style = Math.floorMod(fs.style, 3);
        boolean glow = style == 1 || kind == TrailKind.GALAXY;
        IVertexBuilder vb = buf.getBuffer(glow ? ModRenderTypes.GLOW_QUADS : ModRenderTypes.COLOR_QUADS);

        float baseW = Math.max(0.05F, fs.size) * (style == 1 ? 0.35F : 0.55F);
        Matrix4f pose = ms.last().pose();

        long now = System.currentTimeMillis();

        // Per-frame color statics (used by colorRGB to avoid recomputing).
        float rainbowPhase = (now % 4000L) / 4000F;
        float flickPhase   = (now % 1000L) * 0.02F;
        int baseR = clamp255((int)(fs.colorR * 255));
        int baseG = clamp255((int)(fs.colorG * 255));
        int baseB = clamp255((int)(fs.colorB * 255));

        float invN1 = 1F / (n - 1);
        float invLife = 1F / (float) TRAIL_LIFETIME_MS;

        for (int i = 0; i < n - 1; i++) {
            TrailHistory.Point a = pts[i];
            TrailHistory.Point b = pts[i + 1];

            float ageA = Math.min(1F, (now - a.timeMs) * invLife);
            float ageB = Math.min(1F, (now - b.timeMs) * invLife);
            if (ageA >= 1F && ageB >= 1F) continue;   // fully faded — skip
            float headA = 1F - ageA;
            float headB = 1F - ageB;

            // Ribbon width tapers from head→tail for a blade look.
            float tA = i * invN1;
            float tB = (i + 1) * invN1;
            float widthA = baseW * (0.15F + 0.85F * tA);
            float widthB = baseW * (0.15F + 0.85F * tB);

            // Build a "right" vector that is perpendicular to the segment and
            // faces the camera — classic billboard ribbon.
            double sx = b.x - a.x, sy = b.y - a.y, sz = b.z - a.z;
            double slen = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (slen < 1e-6) continue;
            double invSlen = 1.0 / slen;
            double dnx = sx * invSlen, dny = sy * invSlen, dnz = sz * invSlen;

            double cxm = (a.x + b.x) * 0.5 - cam.x;
            double cym = (a.y + b.y) * 0.5 - cam.y;
            double czm = (a.z + b.z) * 0.5 - cam.z;

            // right = normalize( dir × toCam )
            double rx = dny * czm - dnz * cym;
            double ry = dnz * cxm - dnx * czm;
            double rz = dnx * cym - dny * cxm;
            double rlen2 = rx * rx + ry * ry + rz * rz;
            if (rlen2 < 1e-12) { rx = 0; ry = 1; rz = 0; }
            else {
                double invR = 1.0 / Math.sqrt(rlen2);
                rx *= invR; ry *= invR; rz *= invR;
            }

            // For "blade" style the ribbon is vertical (world-up), not camera-facing.
            if (style == 1) { rx = 0; ry = 1; rz = 0; }

            double ax = a.x - cam.x, ay = a.y - cam.y, az = a.z - cam.z;
            double bx = b.x - cam.x, by = b.y - cam.y, bz = b.z - cam.z;

            // Pack each endpoint's color into an int (0xAARRGGBB) — no allocation.
            int packA = colorRGB(kind, tA, headA, rainbowPhase, flickPhase, baseR, baseG, baseB);
            int packB = colorRGB(kind, tB, headB, rainbowPhase, flickPhase, baseR, baseG, baseB);

            emitQuad(vb, pose,
                    (float)(ax + rx * widthA), (float)(ay + ry * widthA), (float)(az + rz * widthA),
                    (float)(ax - rx * widthA), (float)(ay - ry * widthA), (float)(az - rz * widthA),
                    (float)(bx - rx * widthB), (float)(by - ry * widthB), (float)(bz - rz * widthB),
                    (float)(bx + rx * widthB), (float)(by + ry * widthB), (float)(bz + rz * widthB),
                    packA, packB);

            // DOUBLE style: draw a second, narrower, vertically-offset ribbon.
            if (style == 2) {
                float off = widthA * 0.4F;
                emitQuad(vb, pose,
                        (float)(ax + rx * widthA * 0.5F), (float)(ay + ry * widthA * 0.5F + off), (float)(az + rz * widthA * 0.5F),
                        (float)(ax - rx * widthA * 0.5F), (float)(ay - ry * widthA * 0.5F + off), (float)(az - rz * widthA * 0.5F),
                        (float)(bx - rx * widthB * 0.5F), (float)(by - ry * widthB * 0.5F + off), (float)(bz - rz * widthB * 0.5F),
                        (float)(bx + rx * widthB * 0.5F), (float)(by + ry * widthB * 0.5F + off), (float)(bz + rz * widthB * 0.5F),
                        packA, packB);
            }
        }
    }

    /** Returns the 0xAARRGGBB packed color for an endpoint. No allocation. */
    private static int colorRGB(TrailKind kind, float t, float fade,
                                float rainbowPhase, float flickPhase,
                                int baseR, int baseG, int baseB) {
        int r, g, b;
        switch (kind) {
            case RAINBOW: {
                float hue = (t * 1.2F + rainbowPhase) % 1F;
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
                // twinkle: slight bright flicker along head
                float flick = 0.7F + 0.3F * (float) Math.sin(flickPhase + t * 6F);
                r = clamp255((int)(baseR * flick));
                g = clamp255((int)(baseG * flick));
                b = clamp255((int)(baseB * flick));
                break;
            }
        }
        int a = clamp255((int)(fade * 220));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void emitQuad(IVertexBuilder vb, Matrix4f pose,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 int packA, int packB) {
        int aA = (packA >>> 24) & 0xFF, rA = (packA >> 16) & 0xFF, gA = (packA >> 8) & 0xFF, bA = packA & 0xFF;
        int aB = (packB >>> 24) & 0xFF, rB = (packB >> 16) & 0xFF, gB = (packB >> 8) & 0xFF, bB = packB & 0xFF;
        vb.vertex(pose, x1, y1, z1).color(rA, gA, bA, aA).endVertex();
        vb.vertex(pose, x2, y2, z2).color(rA, gA, bA, aA).endVertex();
        vb.vertex(pose, x3, y3, z3).color(rB, gB, bB, aB).endVertex();
        vb.vertex(pose, x4, y4, z4).color(rB, gB, bB, aB).endVertex();
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private TrailRenderer() {}
}
