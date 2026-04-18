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
 * China Hat — only on the LOCAL player, pitch-fixed (Y-rotation only),
 * translucent (alpha blending), configurable color/style/size/offset.
 *
 * Style 0 = classic cone, 1 = flat disc, 2 = wide cone.
 */
public final class HatRenderer {

    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.CHINA_HAT)) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Hide the hat in first-person so it does not block the view.
        // mc.options.thirdPersonView: 0 = first-person, 1 = back, 2 = front.
        if (mc.options.thirdPersonView == 0) return;

        FeatureSettings fs = state.settings(FeatureType.CHINA_HAT);

        // Smooth interpolated player position
        double px = player.xo + (player.getX() - player.xo) * partialTicks;
        double py = player.yo + (player.getY() - player.yo) * partialTicks;
        double pz = player.zo + (player.getZ() - player.zo) * partialTicks;

        Vector3d cam = mc.gameRenderer.getCamera().getPosition();

        double eye = player.getEyeHeight();
        double dx = px - cam.x + fs.offsetX;
        double dy = py - cam.y + eye + 0.38 + fs.offsetY;
        double dz = pz - cam.z + fs.offsetZ;

        // Y-rotation only (body yaw — no pitch, no roll)
        float yaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;

        ms.pushPose();
        ms.translate(dx, dy, dz);
        ms.mulPose(Vector3f.YP.rotationDegrees(-yaw));  // pitch = 0, fixed

        int style = Math.floorMod(fs.style, 3);
        float size = Math.max(0.1F, fs.size);

        int r = clamp((int)(fs.colorR * 255));
        int g = clamp((int)(fs.colorG * 255));
        int b = clamp((int)(fs.colorB * 255));
        int a = 150; // semi-transparent

        IRenderTypeBuffer.Impl buf = mc.getRenderTypeBuffers().getBufferSource();
        IVertexBuilder vb = buf.getBuffer(ModRenderTypes.COLOR_QUADS);

        float radius, height;
        switch (style) {
            case 1: radius = 0.60F * size; height = 0.10F * size; break; // flat disc
            case 2: radius = 0.95F * size; height = 0.30F * size; break; // wide cone
            default: radius = 0.55F * size; height = 0.48F * size; break; // classic cone
        }

        drawCone(ms, vb, radius, height, r, g, b, a);

        ms.popPose();
        buf.endBatch(ModRenderTypes.COLOR_QUADS);
    }

    private static void drawCone(MatrixStack ms, IVertexBuilder vb,
                                  float radius, float height,
                                  int r, int g, int b, int a) {
        int sides = 24;
        Matrix4f pose = ms.last().pose();

        for (int i = 0; i < sides; i++) {
            float a0 = (float)(i       * (Math.PI * 2 / sides));
            float a1 = (float)((i + 1) * (Math.PI * 2 / sides));
            float x0 = (float)Math.cos(a0) * radius;
            float z0 = (float)Math.sin(a0) * radius;
            float x1 = (float)Math.cos(a1) * radius;
            float z1 = (float)Math.sin(a1) * radius;

            // Side face (apex as degenerate quad = triangle)
            vb.vertex(pose, 0.0F, height, 0.0F).color(r, g, b, a).endVertex();
            vb.vertex(pose, x0,   0.0F,   z0  ).color(r, g, b, a).endVertex();
            vb.vertex(pose, x1,   0.0F,   z1  ).color(r, g, b, a).endVertex();
            vb.vertex(pose, 0.0F, height, 0.0F).color(r, g, b, a).endVertex();

            // Bottom cap
            vb.vertex(pose, 0.0F, 0.0F, 0.0F).color(r, g, b, a).endVertex();
            vb.vertex(pose, x1,   0.0F, z1   ).color(r, g, b, a).endVertex();
            vb.vertex(pose, x0,   0.0F, z0   ).color(r, g, b, a).endVertex();
            vb.vertex(pose, 0.0F, 0.0F, 0.0F).color(r, g, b, a).endVertex();
        }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private HatRenderer() {}
}
