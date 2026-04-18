package com.example.cosmetics.trails;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.particles.ParticleManager;
import com.example.cosmetics.particles.shapes.CubeParticle;
import com.example.cosmetics.particles.shapes.StarParticle;
import com.example.cosmetics.particles.shapes.TetraParticle;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

/**
 * Spawns trail particles at the local player's feet each tick.
 *
 * Rainbow Trail  — радужные 3D кубики
 * Flame Trail    — оранжевые 3D тетраэдры, тянутся вверх
 * Galaxy Trail   — маленькие 3D звёздочки, плавают вокруг
 */
public final class TrailTicker {
    private static final Random RNG = new Random();
    private static float hue = 0.0F;

    // Track previous position for velocity-based spawning direction
    private static double lastX, lastY, lastZ;
    private static boolean hasPrev = false;

    public static void tick(PlayerEntity player) {
        if (player == null) { hasPrev = false; return; }
        CosmeticsState s = CosmeticsState.get();

        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();

        // Direction of travel (for trail to come from behind)
        double dvx = hasPrev ? (x - lastX) : 0;
        double dvz = hasPrev ? (z - lastZ) : 0;
        lastX = x; lastY = y; lastZ = z;
        hasPrev = true;

        if (s.isOn(FeatureType.RAINBOW_TRAIL)) spawnRainbow(s.settings(FeatureType.RAINBOW_TRAIL), x, y, z, dvx, dvz);
        if (s.isOn(FeatureType.FLAME_TRAIL))   spawnFlame  (s.settings(FeatureType.FLAME_TRAIL),   x, y, z, dvx, dvz);
        if (s.isOn(FeatureType.GALAXY_TRAIL))  spawnGalaxy (s.settings(FeatureType.GALAXY_TRAIL),  x, y, z);
    }

    // ---- Rainbow ---------------------------------------------------------------

    private static void spawnRainbow(FeatureSettings fs, double x, double y, double z,
                                     double dvx, double dvz) {
        int count = Math.max(1, (int) Math.round(2.5 * fs.density));
        for (int i = 0; i < count; i++) {
            hue = (hue + 0.025F) % 1.0F;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F);

            CubeParticle p = new CubeParticle(
                    x + (RNG.nextDouble() - 0.5) * 0.3,
                    y + RNG.nextDouble() * 0.2,
                    z + (RNG.nextDouble() - 0.5) * 0.3,
                    -dvx * 0.5 + (RNG.nextDouble() - 0.5) * 0.04 * fs.speed,
                     0.015 * fs.speed,
                    -dvz * 0.5 + (RNG.nextDouble() - 0.5) * 0.04 * fs.speed);
            p.r = (rgb >> 16) & 0xFF;
            p.g = (rgb >> 8)  & 0xFF;
            p.b =  rgb        & 0xFF;
            p.size = 0.22F * fs.size;
            p.maxAge = 28 + RNG.nextInt(12);
            ParticleManager.get().add(p);
        }
    }

    // ---- Flame -----------------------------------------------------------------

    private static void spawnFlame(FeatureSettings fs, double x, double y, double z,
                                   double dvx, double dvz) {
        int count = Math.max(1, (int) Math.round(2 * fs.density));
        for (int i = 0; i < count; i++) {
            // Slightly vary the orange hue for richer flame
            float flicker = RNG.nextFloat() * 0.15F;
            TetraParticle p = new TetraParticle(
                    x + (RNG.nextDouble() - 0.5) * 0.2,
                    y + RNG.nextDouble() * 0.15,
                    z + (RNG.nextDouble() - 0.5) * 0.2,
                    -dvx * 0.4 + (RNG.nextDouble() - 0.5) * 0.025 * fs.speed,
                     0.045 * fs.speed + RNG.nextDouble() * 0.02,
                    -dvz * 0.4 + (RNG.nextDouble() - 0.5) * 0.025 * fs.speed);
            // Flame colour: bright orange → deep red flicker
            if (fs.colorR == 1F && fs.colorG == 0.55F) { // default flame
                p.r = 255;
                p.g = clamp((int)((0.35F + flicker) * 255));
                p.b = clamp((int)(flicker * 80));
            } else {
                applyColor(p, fs);
            }
            p.size = 0.30F * fs.size;
            p.maxAge = 18 + RNG.nextInt(10);
            ParticleManager.get().add(p);
        }
    }

    // ---- Galaxy ----------------------------------------------------------------

    private static void spawnGalaxy(FeatureSettings fs, double x, double y, double z) {
        int count = Math.max(1, (int) Math.round(1.5 * fs.density));
        for (int i = 0; i < count; i++) {
            // Spiral outward in a ring
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist  = 0.3 + RNG.nextDouble() * 0.6;
            double sx = x + Math.cos(angle) * dist;
            double sz = z + Math.sin(angle) * dist;
            double sy = y + 0.3 + RNG.nextDouble() * 0.8;

            StarParticle p = new StarParticle(
                    sx, sy, sz,
                    Math.cos(angle + Math.PI / 2) * 0.012 * fs.speed,
                    (RNG.nextDouble() - 0.3) * 0.01 * fs.speed,
                    Math.sin(angle + Math.PI / 2) * 0.012 * fs.speed);
            applyColor(p, fs);
            // Twinkle: vary brightness slightly
            float bright = 0.7F + RNG.nextFloat() * 0.3F;
            p.r = clamp((int)(p.r * bright));
            p.g = clamp((int)(p.g * bright));
            p.b = clamp((int)(p.b * bright));
            p.size = 0.18F * fs.size;
            p.maxAge = 40 + RNG.nextInt(25);
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

    private TrailTicker() {}
}
