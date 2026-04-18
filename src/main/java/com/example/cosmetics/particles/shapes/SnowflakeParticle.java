package com.example.cosmetics.particles.shapes;

import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.render.Primitives;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;

/** 3D snowflake: rotates on Y and slowly tilts on X as it falls. */
public class SnowflakeParticle extends CustomParticle {
    public SnowflakeParticle(double x, double y, double z, double vx, double vy, double vz) {
        super(x, y, z, vx, vy, vz);
        this.rotationSpeed = 4.0F + (float)(Math.random() * 4F);
        this.rotSpeedX     = 1.5F;
        this.drag          = 0.98F;
        this.maxAge        = 55 + (int)(Math.random() * 20);
    }

    @Override protected double gravity() { return -0.018; } // falls

    @Override
    protected void renderShape(MatrixStack ms, IRenderTypeBuffer buf, int alpha) {
        IVertexBuilder vb = Primitives.lineBuffer(buf);
        Primitives.snowflake(ms, vb, size * 0.5F, r, g, b, alpha);
    }
}
