package com.example.cosmetics.particles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Vector3f;

/**
 * Base for client-only custom particles rendered as line-based 3D geometry
 * in RenderWorldLastEvent. Subclasses override renderShape.
 *
 * Improvements:
 * - Smooth alpha fade-in for first few ticks (avoids pop-in)
 * - 3-axis rotation support (rotX, rotY, rotZ speeds)
 * - Air drag coefficient configurable
 */
public abstract class CustomParticle {
    public double x, y, z;
    public double vx, vy, vz;
    public int age = 0;
    public int maxAge = 25;
    public float size = 0.3F;
    public float rotation = 0.0F;
    public float rotationSpeed = 0.0F;
    public float rotX = 0.0F;
    public float rotSpeedX = 0.0F;
    public float drag = 0.95F;   // velocity multiplier per tick (air resistance)
    public int r = 255, g = 255, b = 255;
    public boolean dead = false;

    public CustomParticle(double x, double y, double z, double vx, double vy, double vz) {
        this.x = x; this.y = y; this.z = z;
        this.vx = vx; this.vy = vy; this.vz = vz;
    }

    public void tick() {
        age++;
        if (age >= maxAge) { dead = true; return; }
        x += vx; y += vy; z += vz;
        vx *= drag; vy = vy * 0.97 + gravity(); vz *= drag;
        rotation  += rotationSpeed;
        rotX      += rotSpeedX;
    }

    /** Override to add gravity. Positive = float up (in the internal convention),
     *  negative = fall down. Applied to vy each tick. */
    protected double gravity() { return 0.0; }

    /** Alpha: fade in over first 4 ticks, then fade out. */
    public int alpha() {
        float fadeIn  = Math.min(1F, age / 4F);
        float fadeOut = 1.0F - (float) age / (float) maxAge;
        return Math.max(0, Math.min(255, (int)(fadeIn * fadeOut * 255)));
    }

    public final void render(MatrixStack ms, IRenderTypeBuffer buf, float partialTicks) {
        ms.pushPose();
        if (rotation != 0.0F || rotationSpeed != 0.0F) {
            ms.mulPose(Vector3f.YP.rotationDegrees(rotation));
        }
        if (rotX != 0.0F || rotSpeedX != 0.0F) {
            ms.mulPose(Vector3f.XP.rotationDegrees(rotX));
        }
        renderShape(ms, buf, alpha());
        ms.popPose();
    }

    protected abstract void renderShape(MatrixStack ms, IRenderTypeBuffer buf, int alpha);
}
