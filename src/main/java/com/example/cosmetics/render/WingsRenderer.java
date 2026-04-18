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
 * Dragon wings cosmetic — two small translucent wings on the player's back.
 * Flap amplitude is driven by the player's horizontal speed so the wings
 * look like they're propelling the player when running.
 *
 * Hidden in first-person for the local player.
 *
 * Styles:
 *   0 = DRAGON   — classic membrane wing (3 finger bones)
 *   1 = ANGEL    — feather-row silhouette
 *   2 = SPIRIT   — glow-blended translucent double wing
 */
public final class WingsRenderer {

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.DRAGON_WINGS)) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Hidden in first-person.
        if (mc.options.thirdPersonView == 0) return;

        FeatureSettings fs = state.settings(FeatureType.DRAGON_WINGS);

        double px = player.xo + (player.getX() - player.xo) * partialTicks;
        double py = player.yo + (player.getY() - player.yo) * partialTicks;
        double pz = player.zo + (player.getZ() - player.zo) * partialTicks;
        Vector3d cam = mc.gameRenderer.getCamera().getPosition();

        float yaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;

        // Speed in blocks/tick. When walking ~ 0.215. Idle → ~0.
        double vx = player.getX() - player.xo;
        double vz = player.getZ() - player.zo;
        float speed = (float) Math.sqrt(vx * vx + vz * vz);
        float flapBoost = Math.min(1.0F, speed * 10F);

        // Use gameTime so wings still idle-flap gently.
        float t = (player.tickCount + partialTicks) * 0.35F * fs.speed;
        float idleFlap = (float) Math.sin(t) * 0.15F;
        float moveFlap = (float) Math.sin(t * 2.2F) * 0.65F * flapBoost;
        float flap = idleFlap + moveFlap; // [-0.8 .. 0.8] roughly

        // Attach wings on the upper back.
        double dx = px - cam.x + fs.offsetX;
        double dy = py - cam.y + 1.30 + fs.offsetY;
        double dz = pz - cam.z + fs.offsetZ;

        int style = Math.floorMod(fs.style, 3);
        boolean glow = style == 2;
        IRenderTypeBuffer.Impl buf = mc.getRenderTypeBuffers().getBufferSource();
        IVertexBuilder vb = buf.getBuffer(glow ? ModRenderTypes.GLOW_QUADS : ModRenderTypes.COLOR_QUADS);

        int r = clamp255((int)(fs.colorR * 255));
        int g = clamp255((int)(fs.colorG * 255));
        int b = clamp255((int)(fs.colorB * 255));
        int a = style == 2 ? 140 : 170;

        float size = Math.max(0.2F, fs.size);

        ms.pushPose();
        ms.translate(dx, dy, dz);
        ms.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        // Slight lean back so wings sit on the back, not on top.
        ms.mulPose(Vector3f.XP.rotationDegrees(10F));

        // Left wing (rotated +flap around Z), right wing (rotated -flap).
        drawWing(ms, vb, +1F, flap, style, size, r, g, b, a);
        drawWing(ms, vb, -1F, flap, style, size, r, g, b, a);

        ms.popPose();

        buf.endBatch(ModRenderTypes.COLOR_QUADS);
        buf.endBatch(ModRenderTypes.GLOW_QUADS);
    }

    /**
     * @param side  +1 for left wing, -1 for right (mirrors geometry)
     * @param flap  flap angle in radians (positive = wing down stroke? no, up)
     */
    private static void drawWing(MatrixStack ms, IVertexBuilder vb,
                                 float side, float flap, int style, float size,
                                 int r, int g, int b, int a) {
        ms.pushPose();
        // Mount point slightly offset from spine
        ms.translate(0.18F * side, 0, 0.05F);
        // Flap: rotate around Z so wing raises/lowers
        float flapDeg = (float) Math.toDegrees(flap) * side * 0.8F;
        ms.mulPose(Vector3f.ZP.rotationDegrees(flapDeg + side * 8F));
        // A bit of sweep so the wing isn't perfectly flat.
        ms.mulPose(Vector3f.YP.rotationDegrees(side * -14F));

        Matrix4f pose = ms.last().pose();

        // Wing geometry: series of triangular membranes from root to finger tips.
        // Points are defined as if for the LEFT wing; `side` mirrors X.
        // Every "finger" produces two quads (membrane panels); we draw them as
        // degenerate quads (triangle = 4 verts with 2 shared) since the render
        // type uses GL_QUADS.
        float s = size;
        if (style == 0) { // DRAGON
            float[][] pts = {
                    {0.00F, 0.00F, 0.00F},                       // 0 shoulder
                    {0.80F * s, 0.25F * s, -0.10F * s},          // 1 elbow
                    {1.50F * s, 0.45F * s, -0.25F * s},          // 2 wrist
                    {1.80F * s, 0.15F * s, -0.40F * s},          // 3 finger 1 tip
                    {1.55F * s, -0.15F * s, -0.55F * s},         // 4 finger 2 tip
                    {1.10F * s, -0.45F * s, -0.55F * s},         // 5 finger 3 tip
                    {0.55F * s, -0.55F * s, -0.35F * s},         // 6 trailing edge near body
                    {0.00F * s, -0.35F * s, -0.10F * s},         // 7 body trailing
            };
            // Membrane panels: (shoulder, elbow, wrist), (shoulder, wrist, f1),
            // (shoulder, f1, f2), (shoulder, f2, f3), (shoulder, f3, rear), (shoulder, rear, body)
            int[][] tris = {
                    {0, 1, 2}, {0, 2, 3}, {0, 3, 4}, {0, 4, 5}, {0, 5, 6}, {0, 6, 7}
            };
            for (int[] tri : tris) {
                tri3d(vb, pose, pts[tri[0]], pts[tri[1]], pts[tri[2]], side, r, g, b, a);
            }
            // Bone highlights (bright thin edges along fingers)
            int br = clamp255(r + 40), bg = clamp255(g + 40), bb = clamp255(b + 40);
            quadEdge(vb, pose, pts[0], pts[1], side, br, bg, bb, a, 0.015F);
            quadEdge(vb, pose, pts[1], pts[2], side, br, bg, bb, a, 0.015F);
            quadEdge(vb, pose, pts[2], pts[3], side, br, bg, bb, a, 0.015F);
            quadEdge(vb, pose, pts[2], pts[4], side, br, bg, bb, a, 0.015F);
            quadEdge(vb, pose, pts[2], pts[5], side, br, bg, bb, a, 0.015F);
        } else if (style == 1) { // ANGEL — feather rows
            int rows = 4;
            for (int i = 0; i < rows; i++) {
                float x0 = 0.15F * s + i * 0.30F * s;
                float x1 = x0 + 0.45F * s;
                float yTop = 0.10F * s - i * 0.05F * s;
                float yBot = -0.35F * s - i * 0.03F * s;
                float z  = -0.15F * s - i * 0.08F * s;
                tri3d(vb, pose,
                        new float[]{x0, yTop, z * 0.5F},
                        new float[]{x1, yTop * 0.5F, z},
                        new float[]{x0, yBot, z * 0.4F},
                        side, r, g, b, a);
                tri3d(vb, pose,
                        new float[]{x1, yTop * 0.5F, z},
                        new float[]{x1, yBot * 0.5F, z},
                        new float[]{x0, yBot, z * 0.4F},
                        side, r, g, b, a);
            }
        } else { // SPIRIT — two stacked translucent sheets
            float[][] a1 = {
                    {0, 0, 0}, {1.4F * s, 0.3F * s, -0.2F * s},
                    {1.3F * s, -0.2F * s, -0.4F * s}, {0.6F * s, -0.4F * s, -0.2F * s}
            };
            float[][] a2 = {
                    {0.05F, 0.05F, 0.03F}, {1.3F * s, 0.15F * s, -0.3F * s},
                    {1.1F * s, -0.30F * s, -0.5F * s}, {0.5F * s, -0.45F * s, -0.3F * s}
            };
            tri3d(vb, pose, a1[0], a1[1], a1[2], side, r, g, b, a);
            tri3d(vb, pose, a1[0], a1[2], a1[3], side, r, g, b, a);
            int r2 = clamp255(r + 20), g2 = clamp255(g + 20), b2 = clamp255(b + 30);
            tri3d(vb, pose, a2[0], a2[1], a2[2], side, r2, g2, b2, (int)(a * 0.75F));
            tri3d(vb, pose, a2[0], a2[2], a2[3], side, r2, g2, b2, (int)(a * 0.75F));
        }

        ms.popPose();
    }

    /** Draws a triangle as a degenerate GL_QUAD (p1,p2,p3,p3). */
    private static void tri3d(IVertexBuilder vb, Matrix4f pose,
                              float[] p1, float[] p2, float[] p3,
                              float side, int r, int g, int b, int a) {
        vb.vertex(pose, p1[0] * side, p1[1], p1[2]).color(r, g, b, a).endVertex();
        vb.vertex(pose, p2[0] * side, p2[1], p2[2]).color(r, g, b, a).endVertex();
        vb.vertex(pose, p3[0] * side, p3[1], p3[2]).color(r, g, b, a).endVertex();
        vb.vertex(pose, p3[0] * side, p3[1], p3[2]).color(r, g, b, a).endVertex();
    }

    /** Thick line between two points as a thin quad (visible bone). */
    private static void quadEdge(IVertexBuilder vb, Matrix4f pose,
                                  float[] p1, float[] p2,
                                  float side, int r, int g, int b, int a,
                                  float halfW) {
        float dx = p2[0] - p1[0];
        float dy = p2[1] - p1[1];
        float dz = p2[2] - p1[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-5F) return;
        // Perpendicular in XY plane-ish
        float nx = -dy / len, ny = dx / len, nz = 0;
        vb.vertex(pose, (p1[0] + nx * halfW) * side, p1[1] + ny * halfW, p1[2] + nz * halfW).color(r, g, b, a).endVertex();
        vb.vertex(pose, (p1[0] - nx * halfW) * side, p1[1] - ny * halfW, p1[2] - nz * halfW).color(r, g, b, a).endVertex();
        vb.vertex(pose, (p2[0] - nx * halfW) * side, p2[1] - ny * halfW, p2[2] - nz * halfW).color(r, g, b, a).endVertex();
        vb.vertex(pose, (p2[0] + nx * halfW) * side, p2[1] + ny * halfW, p2[2] + nz * halfW).color(r, g, b, a).endVertex();
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private WingsRenderer() {}
}
