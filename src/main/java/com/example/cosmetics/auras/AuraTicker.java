package com.example.cosmetics.auras;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.particles.ParticleManager;
import com.example.cosmetics.particles.shapes.HeartParticle;
import com.example.cosmetics.particles.shapes.SnowflakeParticle;
import com.example.cosmetics.particles.shapes.SphereParticle;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

/**
 * Aura ticker - spawns orbital / rain particles around the local player.
 *
 * AURA      - spheres orbit on multiple rings
 * SNOW_AURA - snowflakes fall from above in a cone around the player
 * HEART_AURA - hearts pulse on orbit
 */
public final class AuraTicker {
    private static final Random RNG = new Random();
    private static float angle = 0.0F;
    private static int tick = 0;

    public static void tick(PlayerEntity player) {
        if (player == null) return;
        CosmeticsState s = CosmeticsState.get();

        tick++;
        angle += 0.18F;
        if (angle > (float)(Math.PI * 2)) angle -= (float)(Math.PI * 2);

        if (s.isOn(FeatureType.AURA))       tickAura      (player, s.settings(FeatureType.AURA));
        if (s.isOn(FeatureType.SNOW_AURA))  tickSnow      (player, s.settings(FeatureType.SNOW_AURA));
        if (s.isOn(FeatureType.HEART_AURA)) tickHearts    (player, s.settings(FeatureType.HEART_AURA));
    }

    // ---- Aura (sphere orbits) ---------------------------------------------------

    private static void tickAura(PlayerEntity player, FeatureSettings fs) {
        // Two rings at different heights and radii for more depth
        spawnOrbit(player, fs, SphereParticle.class, 1.1, 1.0, 0,  3);
        spawnOrbit(player, fs, SphereParticle.class, 0.7, 1.6, (float)(Math.PI / 3), 2);
    }

    private static void spawnOrbit(PlayerEntity player, FeatureSettings fs, Class<?> type,
                                   double radius, double yOff, float angleOffset, int count) {
        int n = Math.max(1, (int) Math.round(count * fs.density));
        for (int i = 0; i < n; i++) {
            float a = angle * fs.speed + angleOffset + (float)(i * (Math.PI * 2 / n));
            double x = player.getX() + Math.cos(a) * radius;
            double z = player.getZ() + Math.sin(a) * radius;
            double y = player.getY() + yOff + Math.sin(angle * 2 + i) * 0.15;

            // Tiny velocity tangent to orbit (looks like spinning)
            double vx = -Math.sin(a) * 0.008 * fs.speed;
            double vz =  Math.cos(a) * 0.008 * fs.speed;

            CustomParticle p = new SphereParticle(x, y, z, vx, 0, vz);
            applyColor(p, fs);
            p.size = 0.45F * fs.size;
            p.maxAge = 8;
            ParticleManager.get().add(p);
        }
    }

    // ---- Snow aura (falling cone) -----------------------------------------------

    private static void tickSnow(PlayerEntity player, FeatureSettings fs) {
        int count = Math.max(1, (int) Math.round(2 * fs.density));
        for (int i = 0; i < count; i++) {
            // Cone radius grows as it falls — spawn at random position in wide circle
            double rad = 0.5 + RNG.nextDouble() * 2.5;
            double ang = RNG.nextDouble() * Math.PI * 2;
            double x = player.getX() + Math.cos(ang) * rad;
            double z = player.getZ() + Math.sin(ang) * rad;
            double y = player.getY() + 3.5 + RNG.nextDouble() * 0.5;

            SnowflakeParticle p = new SnowflakeParticle(
                    x, y, z,
                    (RNG.nextDouble() - 0.5) * 0.01,
                    -0.025 * fs.speed - RNG.nextDouble() * 0.015,
                    (RNG.nextDouble() - 0.5) * 0.01);
            applyColor(p, fs);
            p.size = 0.4F * fs.size;
            p.maxAge = 50 + RNG.nextInt(20);
            ParticleManager.get().add(p);
        }
    }

    // ---- Heart aura (pulsing orbit) --------------------------------------------

    private static void tickHearts(PlayerEntity player, FeatureSettings fs) {
        // Spawn every other tick to pulse
        if (tick % 2 != 0) return;
        int count = Math.max(1, (int) Math.round(2 * fs.density));
        for (int i = 0; i < count; i++) {
            float a = angle * fs.speed + (float)(i * (Math.PI * 2 / Math.max(1, count)));
            double radius = 0.9 + 0.15 * Math.sin(tick * 0.15); // pulsing radius
            double x = player.getX() + Math.cos(a) * radius;
            double z = player.getZ() + Math.sin(a) * radius;
            double y = player.getY() + 1.5 + Math.sin(angle * 1.5 + i) * 0.2;

            HeartParticle p = new HeartParticle(x, y, z, 0, 0.012 * fs.speed, 0);
            applyColor(p, fs);
            p.size = 0.5F * fs.size;
            p.maxAge = 30;
            ParticleManager.get().add(p);
        }
    }

    // ---- Helpers ---------------------------------------------------------------

    private static void applyColor(CustomParticle p, FeatureSettings fs) {
        p.r = clamp((int)(fs.colorR * 255));
        p.g = clamp((int)(fs.colorG * 255));
        p.b = clamp((int)(fs.colorB * 255));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private AuraTicker() {}
}
