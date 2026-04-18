package com.example.cosmetics.auras;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.IParticleData;

import java.util.Random;

/**
 * Spawns aura particles around the local player each tick. The orbiting
 * motion is achieved by emitting on a ring whose angle advances per tick.
 */
public final class AuraTicker {
    private static final Random RNG = new Random();
    private static float angle = 0.0F;

    public static void tick(PlayerEntity player) {
        if (player == null) return;
        angle = (angle + 0.2F) % ((float) (Math.PI * 2));
        CosmeticsState s = CosmeticsState.get();

        if (s.isAuraOn(CosmeticsState.Aura.AURA)) {
            ring(player, ModParticles.AURA.get(), 1.0, 1.2, 3);
        }
        if (s.isAuraOn(CosmeticsState.Aura.SNOW)) {
            rain(player, ModParticles.SNOW.get(), 2.0, 2);
        }
        if (s.isAuraOn(CosmeticsState.Aura.HEARTS)) {
            ring(player, ModParticles.HEART.get(), 0.8, 1.8, 1);
        }
    }

    private static void ring(PlayerEntity player, IParticleData type,
                             double radius, double yOffset, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < count; i++) {
            float a = angle + (float) (i * (Math.PI * 2 / count));
            double x = player.getX() + Math.cos(a) * radius;
            double z = player.getZ() + Math.sin(a) * radius;
            double y = player.getY() + yOffset;
            mc.level.addParticle(type, x, y, z, 0, 0.01, 0);
        }
    }

    private static void rain(PlayerEntity player, IParticleData type,
                             double radius, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < count; i++) {
            double dx = (RNG.nextDouble() - 0.5) * radius * 2;
            double dz = (RNG.nextDouble() - 0.5) * radius * 2;
            double x = player.getX() + dx;
            double z = player.getZ() + dz;
            double y = player.getY() + 2.5;
            mc.level.addParticle(type, x, y, z, 0, -0.03, 0);
        }
    }

    private AuraTicker() {}
}
