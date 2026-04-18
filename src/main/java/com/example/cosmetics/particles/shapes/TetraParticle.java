package com.example.cosmetics.particles.shapes;

import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.render.Primitives;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;

/** 3D tetrahedron — used for flame trail. Rises and spins rapidly. */
public class TetraParticle extends CustomParticle {
    public TetraParticle(double x, double y, double z, double vx, double vy, double vz) {
        super(x, y, z, vx, vy, vz);
        this.rotationSpeed = 11.0F;
        this.rotSpeedX     = 7.0F;
        this.drag          = 0.93F;
        this.maxAge        = 16 + (int)(Math.random() * 10);
    }

    @Override protected double gravity() { return 0.005; } // rises (flame)

    @Override
    protected void renderShape(MatrixStack ms, IRenderTypeBuffer buf, int alpha) {
        IVertexBuilder vb = Primitives.lineBuffer(buf);
        Primitives.tetra(ms, vb, size * 0.5F, r, g, b, alpha);
    }
}
