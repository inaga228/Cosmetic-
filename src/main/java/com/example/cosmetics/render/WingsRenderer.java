package com.example.cosmetics.render;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/**
 * Wings cosmetic — ONE single, hand-tuned, very pretty wing design.
 *
 * The design is an "Ethereal" wing: dragon-shaped membrane skeleton with
 * angelic feathered trailing edge, a bright inner glow sheet, softly
 * pulsing bone highlights, and tip-of-the-finger sparkle dots that drift
 * behind the wing tips. Flap amplitude is driven by the player's
 * horizontal speed so the wings look like they're propelling the player
 * when running.
 *
 * Hidden in first-person for the local player so the view stays clear.
 *
 * Performance: geometry tables are pre-computed as static arrays and we
 * only call begin/endBatch once per pair (solid + glow). Two draw calls
 * total per frame, regardless of how much flapping is happening.
 */
public final class WingsRenderer {

    // Kept so SettingsScreen's lookup still finds a name, but the slider
    // is a no-op — only one style exists.
    public static final int STYLE_COUNT = 1;
    public static final String[] STYLE_NAMES = { "Ethereal" };

    // Pre-computed wing skeleton (LEFT wing; `side` mirrors X for the right).
    // Indices line up with the membrane triangulation and bone list below.
    //
    //   0 = shoulder (root)
    //   1 = elbow
    //   2 = wrist
    //   3 = finger 1 tip (upper)
    //   4 = finger 2 tip
    //   5 = finger 3 tip
    //   6 = finger 4 tip (trailing lower)
    //   7 = body trailing
    private static final float[][] WING_PTS = {
            { 0.00F,  0.00F,  0.00F },   // 0 shoulder
            { 0.85F,  0.35F, -0.10F },   // 1 elbow (raised)
            { 1.55F,  0.55F, -0.25F },   // 2 wrist
            { 1.95F,  0.25F, -0.40F },   // 3 finger 1 tip
            { 1.80F, -0.10F, -0.55F },   // 4 finger 2 tip
            { 1.45F, -0.45F, -0.60F },   // 5 finger 3 tip
            { 0.95F, -0.70F, -0.50F },   // 6 finger 4 tip
            { 0.35F, -0.55F, -0.20F },   // 7 body trailing
    };

    // Triangulation of the membrane. Shoulder-fan style.
    private static final int[][] WING_TRIS = {
            { 0, 1, 2 }, { 0, 2, 3 }, { 0, 3, 4 },
            { 0, 4, 5 }, { 0, 5, 6 }, { 0, 6, 7 },
    };

    // Bones to highlight with bright edges.
    private static final int[][] WING_BONES = {
            { 0, 1 }, { 1, 2 },
            { 2, 3 }, { 2, 4 }, { 2, 5 }, { 2, 6 },
    };

    // Feather tufts along the trailing edge (x, y, z, length).
    private static final float[][] FEATHERS = {
            { 0.30F, -0.50F, -0.15F, 0.20F },
            { 0.55F, -0.60F, -0.25F, 0.22F },
            { 0.80F, -0.68F, -0.40F, 0.22F },
            { 1.05F, -0.72F, -0.50F, 0.22F },
            { 1.30F, -0.65F, -0.55F, 0.22F },
            { 1.55F, -0.50F, -0.55F, 0.20F },
    };

    // Points where we emit the bright sparkle quads.
    private static final int[] SPARKLE_TIPS = { 3, 4, 5, 6 };

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.DRAGON_WINGS)) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Hidden in first-person.
        if (HatRenderer.isFirstPerson(mc)) return;

        FeatureSettings fs = state.settings(FeatureType.DRAGON_WINGS);

        double px = player.xo + (player.getX() - player.xo) * partialTicks;
        double py = player.yo + (player.getY() - player.yo) * partialTicks;
        double pz = player.zo + (player.getZ() - player.zo) * partialTicks;
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();

        float yaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;

        // Speed in blocks/tick. When walking ~ 0.215. Idle → ~0.
        double vx = player.getX() - player.xo;
        double vz = player.getZ() - player.zo;
        float speed = (float) Math.sqrt(vx * vx + vz * vz);
        float flapBoost = Math.min(1.0F, speed * 10F);

        // Animation time. Cached per-frame — avoid double Math.sin.
        float t = (player.tickCount + partialTicks) * 0.35F * fs.speed;
        float idleFlap = (float) Math.sin(t) * 0.15F;
        float moveFlap = (float) Math.sin(t * 2.2F) * 0.65F * flapBoost;
        float flap = idleFlap + moveFlap;

        // Soft pulse for bone/sparkle brightness so it feels alive.
        float pulse = 0.70F + 0.30F * (float) Math.sin(t * 1.6F);

        // Attach wings on the upper back.
        double dx = px - cam.x + fs.offsetX;
        double dy = py - cam.y + 1.30 + fs.offsetY;
        double dz = pz - cam.z + fs.offsetZ;

        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();
        IVertexBuilder solid = buf.getBuffer(ModRenderTypes.COLOR_QUADS);
        IVertexBuilder glow  = buf.getBuffer(ModRenderTypes.GLOW_QUADS);

        int r = clamp255((int) (fs.colorR * 255));
        int g = clamp255((int) (fs.colorG * 255));
        int b = clamp255((int) (fs.colorB * 255));

        // Inner glow sheet is a brighter tint of the user's color.
        int gr = clamp255(r + 60);
        int gg = clamp255(g + 60);
        int gb = clamp255(b + 60);

        // Bone & sparkle color: nearly-white tint of user color, pulsing.
        int br = clamp255((int) ((r + 180) * 0.5F + 40 * pulse));
        int bg = clamp255((int) ((g + 180) * 0.5F + 40 * pulse));
        int bb = clamp255((int) ((b + 255) * 0.5F + 40 * pulse));

        float size = Math.max(0.2F, fs.size);

        ms.pushPose();
        ms.translate(dx, dy, dz);
        ms.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        // Slight lean back so wings sit on the back, not on top.
        ms.mulPose(Vector3f.XP.rotationDegrees(10F));

        drawWing(ms, solid, glow, +1F, flap, size, pulse, r, g, b, gr, gg, gb, br, bg, bb);
        drawWing(ms, solid, glow, -1F, flap, size, pulse, r, g, b, gr, gg, gb, br, bg, bb);

        ms.popPose();

        buf.endBatch(ModRenderTypes.COLOR_QUADS);
        buf.endBatch(ModRenderTypes.GLOW_QUADS);
    }

    private static void drawWing(MatrixStack ms,
                                 IVertexBuilder solid, IVertexBuilder glow,
                                 float side, float flap, float size, float pulse,
                                 int r, int g, int b,
                                 int gr, int gg, int gb,
                                 int br, int bg, int bb) {
        ms.pushPose();
        // Mount point slightly offset from spine
        ms.translate(0.18F * side, 0, 0.05F);
        // Flap: rotate around Z so wing raises/lowers
        float flapDeg = (float) Math.toDegrees(flap) * side * 0.8F;
        ms.mulPose(Vector3f.ZP.rotationDegrees(flapDeg + side * 8F));
        // A bit of sweep so the wing isn't perfectly flat.
        ms.mulPose(Vector3f.YP.rotationDegrees(side * -14F));

        Matrix4f pose = ms.last().pose();

        // Primary membrane (solid, semi-transparent user color).
        int aMain = 185;
        for (int[] tri : WING_TRIS) {
            tri3dScaled(solid, pose, WING_PTS[tri[0]], WING_PTS[tri[1]], WING_PTS[tri[2]],
                    side, size, r, g, b, aMain);
        }

        // Inner glow sheet — smaller, brighter, drawn slightly offset toward
        // the viewer for a frosted-halo look.
        int aGlow = (int) (140 * pulse);
        float gs = size * 0.88F;
        for (int[] tri : WING_TRIS) {
            tri3dScaledOffset(glow, pose, WING_PTS[tri[0]], WING_PTS[tri[1]], WING_PTS[tri[2]],
                    side, gs, 0.04F, gr, gg, gb, aGlow);
        }

        // Bone highlights (bright edges along the skeleton).
        int aBone = (int) (210 * pulse);
        for (int[] bone : WING_BONES) {
            quadEdgeScaled(glow, pose, WING_PTS[bone[0]], WING_PTS[bone[1]],
                    side, size, br, bg, bb, aBone, 0.018F);
        }

        // Feather tufts along trailing edge — thin vertical quads.
        int aFeather = 200;
        for (float[] f : FEATHERS) {
            float[] base = { f[0], f[1], f[2] };
            float[] tip  = { f[0] + 0.02F, f[1] - f[3], f[2] - 0.05F };
            quadEdgeScaled(solid, pose, base, tip, side, size, r, g, b, aFeather, 0.032F);
        }

        // Sparkle dots at the finger tips (tiny bright diamond quads).
        int aSparkle = clamp255((int) (235 * pulse));
        for (int idx : SPARKLE_TIPS) {
            sparkleQuad(glow, pose, WING_PTS[idx], side, size, br, bg, bb, aSparkle, 0.055F);
        }

        ms.popPose();
    }

    // --- low-level primitives ------------------------------------------------

    /** Triangle as a degenerate GL_QUAD with points scaled by `scale`. */
    private static void tri3dScaled(IVertexBuilder vb, Matrix4f pose,
                                    float[] p1, float[] p2, float[] p3,
                                    float side, float scale, int r, int g, int b, int a) {
        float x1 = p1[0] * scale * side, y1 = p1[1] * scale, z1 = p1[2] * scale;
        float x2 = p2[0] * scale * side, y2 = p2[1] * scale, z2 = p2[2] * scale;
        float x3 = p3[0] * scale * side, y3 = p3[1] * scale, z3 = p3[2] * scale;
        vb.vertex(pose, x1, y1, z1).color(r, g, b, a).endVertex();
        vb.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
        vb.vertex(pose, x3, y3, z3).color(r, g, b, a).endVertex();
        vb.vertex(pose, x3, y3, z3).color(r, g, b, a).endVertex();
    }

    /** Same as {@link #tri3dScaled} but shifted by `zOffset` toward the
     * viewer (−z in wing-local space) so the glow sheet sits in front of
     * the membrane without z-fighting. */
    private static void tri3dScaledOffset(IVertexBuilder vb, Matrix4f pose,
                                          float[] p1, float[] p2, float[] p3,
                                          float side, float scale, float zOffset,
                                          int r, int g, int b, int a) {
        float x1 = p1[0] * scale * side, y1 = p1[1] * scale, z1 = p1[2] * scale - zOffset;
        float x2 = p2[0] * scale * side, y2 = p2[1] * scale, z2 = p2[2] * scale - zOffset;
        float x3 = p3[0] * scale * side, y3 = p3[1] * scale, z3 = p3[2] * scale - zOffset;
        vb.vertex(pose, x1, y1, z1).color(r, g, b, a).endVertex();
        vb.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
        vb.vertex(pose, x3, y3, z3).color(r, g, b, a).endVertex();
        vb.vertex(pose, x3, y3, z3).color(r, g, b, a).endVertex();
    }

    /** Thick line between two points as a thin quad (visible bone/feather). */
    private static void quadEdgeScaled(IVertexBuilder vb, Matrix4f pose,
                                       float[] p1, float[] p2,
                                       float side, float scale,
                                       int r, int g, int b, int a,
                                       float halfW) {
        float ax = p1[0] * scale, ay = p1[1] * scale, az = p1[2] * scale;
        float bx = p2[0] * scale, by = p2[1] * scale, bz = p2[2] * scale;
        float dx = bx - ax, dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-5F) return;
        float nx = -dy / len, ny = dx / len;
        vb.vertex(pose, (ax + nx * halfW) * side, ay + ny * halfW, az).color(r, g, b, a).endVertex();
        vb.vertex(pose, (ax - nx * halfW) * side, ay - ny * halfW, az).color(r, g, b, a).endVertex();
        vb.vertex(pose, (bx - nx * halfW) * side, by - ny * halfW, bz).color(r, g, b, a).endVertex();
        vb.vertex(pose, (bx + nx * halfW) * side, by + ny * halfW, bz).color(r, g, b, a).endVertex();
    }

    /** A tiny diamond quad centered on `p` — the sparkle dot at a tip. */
    private static void sparkleQuad(IVertexBuilder vb, Matrix4f pose, float[] p,
                                    float side, float scale, int r, int g, int b, int a,
                                    float half) {
        float cx = p[0] * scale * side, cy = p[1] * scale, cz = p[2] * scale;
        vb.vertex(pose, cx,         cy + half, cz).color(r, g, b, a).endVertex();
        vb.vertex(pose, cx - half,  cy,        cz).color(r, g, b, a).endVertex();
        vb.vertex(pose, cx,         cy - half, cz).color(r, g, b, a).endVertex();
        vb.vertex(pose, cx + half,  cy,        cz).color(r, g, b, a).endVertex();
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private WingsRenderer() {}
}
