package com.example.cosmetics.particles.shapes;

import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.render.Primitives;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;

/** 3D heart — slowly floats upward with gentle Y-rotation. */
public class HeartParticle extends CustomParticle {
    public HeartParticle(double x, double y, double z, double vx, double vy, double vz) {
        super(x, y, z, vx, vy, vz);
        this.rotationSpeed = 1.8F;
        this.drag          = 0.99F;
        this.maxAge        = 38 + (int)(Math.random() * 15);
    }

    @Override protected double gravity() { return 0.008; } // floats up

    @Override
    protected void renderShape(MatrixStack ms, IRenderTypeBuffer buf, int alpha) {
        IVertexBuilder vb = Primitives.lineBuffer(buf);
        Primitives.heart(ms, vb, size * 0.5F, r, g, b, alpha);
    }
}
