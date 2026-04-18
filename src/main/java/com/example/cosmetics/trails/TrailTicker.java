package com.example.cosmetics.trails;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Random;

/**
 * Spawns trail particles behind the local player each client tick based on
 * which trails are enabled. Runs only on the client.
 */
public final class TrailTicker {
    private static final Random RNG = new Random();

    public static void tick(PlayerEntity player) {
        CosmeticsState s = CosmeticsState.get();
        if (player == null) return;

        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();

        // Spawn at feet, slightly behind facing direction for a nicer trail feel.
        Vector3d look = player.getLookAngle();
        double bx = x - look.x * 0.3;
        double bz = z - look.z * 0.3;

        if (s.isTrailOn(CosmeticsState.Trail.RAINBOW)) {
            spawn(ModParticles.RAINBOW.get(), bx, y, bz, 0.02, 0.02, 0.02, 3);
        }
        if (s.isTrailOn(CosmeticsState.Trail.FLAME)) {
            spawn(ModParticles.FLAME.get(), bx, y, bz, 0.01, 0.06, 0.01, 2);
        }
        if (s.isTrailOn(CosmeticsState.Trail.GALAXY)) {
            spawn(ModParticles.GALAXY.get(), bx, y + 0.2, bz, 0.04, 0.04, 0.04, 2);
        }
    }

    private static void spawn(IParticleData type, double x, double y, double z,
                              double sx, double sy, double sz, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < count; i++) {
            double vx = (RNG.nextDouble() - 0.5) * sx;
            double vy = RNG.nextDouble() * sy;
            double vz = (RNG.nextDouble() - 0.5) * sz;
            mc.level.addParticle(type, x, y, z, vx, vy, vz);
        }
    }

    private TrailTicker() {}
}
