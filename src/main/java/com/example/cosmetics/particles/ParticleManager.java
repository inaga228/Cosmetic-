package com.example.cosmetics.particles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-only singleton that owns live {@link CustomParticle}s, ticks them
 * each client tick, and renders them in {@code RenderWorldLastEvent}.
 */
public final class ParticleManager {
    private static final ParticleManager INSTANCE = new ParticleManager();
    public static ParticleManager get() { return INSTANCE; }

    private static final int HARD_CAP = 2000;

    private final List<CustomParticle> particles = new ArrayList<>();

    public void add(CustomParticle p) {
        if (particles.size() >= HARD_CAP) return;
        particles.add(p);
    }

    public void tick() {
        Iterator<CustomParticle> it = particles.iterator();
        while (it.hasNext()) {
            CustomParticle p = it.next();
            p.tick();
            if (p.dead) it.remove();
        }
    }

    public void renderAll(MatrixStack ms, float partialTicks) {
        if (particles.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();

        for (CustomParticle p : particles) {
            ms.pushPose();
            double ix = p.x + p.vx * partialTicks - cam.x;
            double iy = p.y + p.vy * partialTicks - cam.y;
            double iz = p.z + p.vz * partialTicks - cam.z;
            ms.translate(ix, iy, iz);
            p.render(ms, buf, partialTicks);
            ms.popPose();
        }
        buf.endBatch(RenderType.lines());
    }

    private ParticleManager() {}
}
