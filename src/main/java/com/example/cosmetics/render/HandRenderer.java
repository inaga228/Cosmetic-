package com.example.cosmetics.render;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.vector.Vector3f;

/**
 * Applies View Model offset/rotation and custom swing/place animations
 * to the first-person hand via RenderHandEvent.
 *
 * Custom Attack:
 *   - Fast upswing with wrist roll → hard slam down → bounce recovery
 *
 * Custom Place:
 *   - Quick push forward + tilt → snap back (8 ticks)
 */
public final class HandRenderer {

    public static int placeAnimTicks = 0;

    public static void applyTransforms(MatrixStack ms, float partialTicks) {
        CosmeticsState s = CosmeticsState.get();

        // ---- View Model offset & rotation ----------------------------------------
        if (s.isOn(FeatureType.VIEW_MODEL)) {
            FeatureSettings fs = s.settings(FeatureType.VIEW_MODEL);
            ms.translate(fs.offsetX * 0.5F, fs.offsetY * 0.5F, fs.offsetZ * 0.5F);
            if (fs.rotX != 0) ms.mulPose(Vector3f.XP.rotationDegrees(fs.rotX));
            if (fs.rotY != 0) ms.mulPose(Vector3f.YP.rotationDegrees(fs.rotY));
            if (fs.rotZ != 0) ms.mulPose(Vector3f.ZP.rotationDegrees(fs.rotZ));
        }

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        // ---- Custom Attack animation ----------------------------------------------
        if (s.isOn(FeatureType.CUSTOM_ATTACK)) {
            FeatureSettings fs = s.settings(FeatureType.CUSTOM_ATTACK);
            float swing = p.getAttackAnim(partialTicks); // 0..1, 0 = idle, peaks mid-swing

            if (swing > 0.001F) {
                float str = Math.max(0.5F, fs.size); // size slider controls strength

                // Phase 1 (0→0.4): quick raise with wrist roll
                // Phase 2 (0.4→0.7): slam down hard
                // Phase 3 (0.7→1.0): bounce recovery
                float lift, slam, roll, thrust;
                if (swing < 0.4F) {
                    float t = swing / 0.4F;
                    lift  =  t * 18F * str;
                    slam  =  0;
                    roll  =  t * 8F * str;
                    thrust = -t * 0.04F * str;
                } else if (swing < 0.70F) {
                    float t = (swing - 0.4F) / 0.3F;
                    lift  =  18F * str - t * 36F * str;
                    slam  =  t * 10F * str;
                    roll  =  (1F - t) * 8F * str;
                    thrust = -0.04F * str + t * 0.08F * str;
                } else {
                    float t = (swing - 0.7F) / 0.3F;
                    float ease = 1F - (1F - t) * (1F - t);
                    lift  = -18F * str + ease * 18F * str;
                    slam  =  10F * str * (1F - ease);
                    roll  =  0;
                    thrust = 0.04F * str * (1F - ease);
                }

                ms.mulPose(Vector3f.XP.rotationDegrees(-lift));
                ms.mulPose(Vector3f.ZP.rotationDegrees(roll));
                if (slam != 0) ms.mulPose(Vector3f.XP.rotationDegrees(slam));
                ms.translate(0, 0, thrust);
            }
        }

        // ---- Custom Place animation -----------------------------------------------
        if (s.isOn(FeatureType.CUSTOM_PLACE) && placeAnimTicks > 0) {
            float t = placeAnimTicks / 8.0F; // 1.0→0.0 countdown

            // Quick thrust forward + small downward tilt → snap back
            float phase    = (float) Math.sin(t * Math.PI);
            float thrust   = phase * 0.18F;
            float tiltDown = phase * 12F;
            float rollLeft = phase * 4F;

            ms.translate(0, -phase * 0.04F, thrust);
            ms.mulPose(Vector3f.XP.rotationDegrees(tiltDown));
            ms.mulPose(Vector3f.ZP.rotationDegrees(rollLeft));
        }
    }

    public static void tickPlaceAnim() {
        if (placeAnimTicks > 0) placeAnimTicks--;
    }

    public static void triggerPlaceAnim() { placeAnimTicks = 8; }

    private HandRenderer() {}
}
