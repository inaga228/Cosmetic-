package com.example.cosmetics.render;

import com.example.cosmetics.CosmeticsMod;
import com.example.cosmetics.client.CosmeticsState;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;

/**
 * Renders a cone-shaped "China Hat" on top of players that have the hat enabled.
 * Drawn directly with a MatrixStack as a pyramid fan of colored quads (no model file
 * required). The texture in textures/entity/china_hat.png is used.
 */
public class HatLayer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CosmeticsMod.MOD_ID, "textures/entity/china_hat.png");

    public HatLayer(IEntityRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrix, IRenderTypeBuffer buffer, int light,
                       AbstractClientPlayerEntity player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!CosmeticsState.get().isHatOn(CosmeticsState.Hat.CHINA)) return;

        matrix.pushPose();

        // Attach to the head pose so the hat follows head rotation.
        this.getParentModel().head.translateAndRotate(matrix);

        // Position just above the head (head model origin is centered at the neck).
        matrix.translate(0.0, -0.6, 0.0);

        float radius = 0.55F;
        float height = 0.45F;
        int sides = 16;

        IVertexBuilder vb = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f pose = matrix.last().pose();

        for (int i = 0; i < sides; i++) {
            float a0 = (float) (i      * (Math.PI * 2 / sides));
            float a1 = (float) ((i + 1) * (Math.PI * 2 / sides));

            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;

            float u0 = i       / (float) sides;
            float u1 = (i + 1) / (float) sides;

            // Triangle as a degenerate quad (apex duplicated).
            vb.vertex(pose, 0.0F, -height, 0.0F).color(255, 255, 255, 255)
              .uv((u0 + u1) * 0.5F, 0.0F).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
            vb.vertex(pose, x0, 0.0F, z0).color(255, 255, 255, 255)
              .uv(u0, 1.0F).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
            vb.vertex(pose, x1, 0.0F, z1).color(255, 255, 255, 255)
              .uv(u1, 1.0F).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
            vb.vertex(pose, 0.0F, -height, 0.0F).color(255, 255, 255, 255)
              .uv((u0 + u1) * 0.5F, 0.0F).overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
        }

        matrix.popPose();
    }
}
