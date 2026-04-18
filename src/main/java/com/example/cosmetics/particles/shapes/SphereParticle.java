package com.example.cosmetics.particles.shapes;

import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.render.Primitives;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;

/** 3D sphere with dual-axis spin. */
public class SphereParticle extends CustomParticle {
    public SphereParticle(double x, double y, double z, double vx, double vy, double vz) {
        super(x, y, z, vx, vy, vz);
        this.rotationSpeed = 5.0F;
        this.rotSpeedX     = 3.0F;
        this.drag          = 0.97F;
        this.maxAge        = 12;
    }

    @Override
    protected void renderShape(MatrixStack ms, IRenderTypeBuffer buf, int alpha) {
        IVertexBuilder vb = Primitives.lineBuffer(buf);
        Primitives.sphere(ms, vb, size * 0.35F, r, g, b, alpha);
    }
}
