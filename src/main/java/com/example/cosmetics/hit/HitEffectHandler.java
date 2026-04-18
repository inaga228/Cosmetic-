package com.example.cosmetics.hit;

import com.example.cosmetics.CosmeticsMod;
import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.particles.ParticleManager;
import com.example.cosmetics.particles.shapes.CritParticle;
import com.example.cosmetics.particles.shapes.CubeParticle;
import com.example.cosmetics.particles.shapes.SlashParticle;
import com.example.cosmetics.particles.shapes.SphereParticle;
import com.example.cosmetics.particles.shapes.StarParticle;
import com.example.cosmetics.particles.shapes.TetraParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Spawns hit effect particles when the local player attacks an entity.
 *
 * Styles:
 *   0 = SLASH   — red diagonal lines burst outward
 *   1 = STARS   — yellow 3D star burst
 *   2 = CRIT    — golden sparks with gravity + outward velocity
 *   3 = RIPPLE  — expanding horizontal ring of small spheres
 *   4 = SHARDS  — jagged 3D shards (cubes/tetras) flying outward
 *   5 = BOLT    — upward zap: a vertical column of fast sparks
 */
@Mod.EventBusSubscriber(modid = CosmeticsMod.MOD_ID, value = Dist.CLIENT)
public final class HitEffectHandler {

    public static final String[] STYLE_NAMES = { "Slash", "Stars", "Crit", "Ripple", "Shards", "Bolt" };

    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (Minecraft.getInstance().level == null) return;
        if (event.getPlayer() != Minecraft.getInstance().player) return;
        burst(event.getTarget());
    }

    private static void burst(Entity target) {
        if (target == null) return;
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.HIT_EFFECT)) return;
        FeatureSettings fs = state.settings(FeatureType.HIT_EFFECT);

        int style = Math.floorMod(fs.style, STYLE_NAMES.length);
        int count = Math.max(1, fs.count);

        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.6;
        double cz = target.getZ();

        boolean customColor = fs.colorR != 1.0F || fs.colorG != 1.0F || fs.colorB != 1.0F;

        switch (style) {
            case 3: spawnRipple(cx, cy, cz, fs, customColor, count); break;
            case 4: spawnShards(cx, cy, cz, fs, customColor, count); break;
            case 5: spawnBolt  (cx, cy, cz, fs, customColor, count); break;
            default: spawnBurst(cx, cy, cz, fs, customColor, count, style); break;
        }
    }

    // ---- classic burst (SLASH/STARS/CRIT) -----------------------------------

    private static void spawnBurst(double cx, double cy, double cz,
                                   FeatureSettings fs, boolean customColor,
                                   int count, int style) {
        for (int i = 0; i < count; i++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double upward = RNG.nextDouble() * 0.12;
            double speed = 0.08 + RNG.nextDouble() * 0.10;
            double vx = Math.cos(angle) * speed;
            double vy = upward;
            double vz = Math.sin(angle) * speed;

            double px = cx + (RNG.nextDouble() - 0.5) * 0.3;
            double py = cy + (RNG.nextDouble() - 0.5) * 0.3;
            double pz = cz + (RNG.nextDouble() - 0.5) * 0.3;

            CustomParticle p;
            int[] defaultRgb;
            switch (style) {
                case 0: // SLASH
                    p = new SlashParticle(px, py, pz, vx * 0.4, vy, vz * 0.4);
                    p.maxAge = 12;
                    defaultRgb = new int[]{ 255, 35, 35 };
                    break;
                case 1: // STARS
                    p = new StarParticle(px, py, pz, vx, vy, vz);
                    p.maxAge = 22;
                    defaultRgb = new int[]{ 255, 235, 60 };
                    break;
                case 2: // CRIT
                default:
                    p = new CritParticle(px, py, pz, vx, vy, vz);
                    p.maxAge = 18;
                    defaultRgb = new int[]{ 255, 195, 30 };
                    break;
            }
            applyColor(p, fs, customColor, defaultRgb);
            p.size = 0.45F * fs.size;
            ParticleManager.get().add(p);
        }
    }

    // ---- RIPPLE: horizontal ring of small spheres ---------------------------

    private static void spawnRipple(double cx, double cy, double cz,
                                    FeatureSettings fs, boolean customColor, int count) {
        int ring = Math.max(8, count * 2);
        for (int i = 0; i < ring; i++) {
            double a = i * Math.PI * 2 / ring;
            double speed = 0.14;
            SphereParticle p = new SphereParticle(
                    cx, cy - 0.3, cz,
                    Math.cos(a) * speed, 0.01, Math.sin(a) * speed);
            p.maxAge = 14;
            p.drag = 0.90F;
            p.size = 0.25F * fs.size;
            applyColor(p, fs, customColor, new int[]{ 110, 220, 255 });
            ParticleManager.get().add(p);
        }
        // A few upward sparks to emphasize the impact
        for (int i = 0; i < Math.max(3, count / 2); i++) {
            StarParticle s = new StarParticle(cx, cy, cz,
                    (RNG.nextDouble() - 0.5) * 0.04,
                    0.12 + RNG.nextDouble() * 0.08,
                    (RNG.nextDouble() - 0.5) * 0.04);
            s.maxAge = 18;
            s.size = 0.22F * fs.size;
            applyColor(s, fs, customColor, new int[]{ 180, 240, 255 });
            ParticleManager.get().add(s);
        }
    }

    // ---- SHARDS: jagged 3D shards flying outward ---------------------------

    private static void spawnShards(double cx, double cy, double cz,
                                    FeatureSettings fs, boolean customColor, int count) {
        for (int i = 0; i < count; i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            double speed = 0.15 + RNG.nextDouble() * 0.12;
            double vx = Math.cos(a) * speed;
            double vz = Math.sin(a) * speed;
            double vy = 0.04 + RNG.nextDouble() * 0.14;

            CustomParticle p = (i % 2 == 0)
                    ? new CubeParticle(cx, cy, cz, vx, vy, vz)
                    : new TetraParticle(cx, cy, cz, vx, vy, vz);
            p.maxAge = 20 + RNG.nextInt(6);
            p.size = 0.28F * fs.size;
            p.rotationSpeed = (RNG.nextFloat() - 0.5F) * 18F;
            p.rotSpeedX    = (RNG.nextFloat() - 0.5F) * 18F;
            applyColor(p, fs, customColor, new int[]{ 230, 230, 255 });
            ParticleManager.get().add(p);
        }
    }

    // ---- BOLT: vertical lightning column -----------------------------------

    private static void spawnBolt(double cx, double cy, double cz,
                                  FeatureSettings fs, boolean customColor, int count) {
        int bolts = Math.max(6, count);
        for (int i = 0; i < bolts; i++) {
            double ox = (RNG.nextDouble() - 0.5) * 0.25;
            double oz = (RNG.nextDouble() - 0.5) * 0.25;
            StarParticle p = new StarParticle(
                    cx + ox, cy - 0.6, cz + oz,
                    ox * 0.5, 0.28 + RNG.nextDouble() * 0.10, oz * 0.5);
            p.maxAge = 10 + RNG.nextInt(4);
            p.drag = 0.85F;
            p.size = 0.20F * fs.size;
            applyColor(p, fs, customColor, new int[]{ 255, 255, 180 });
            ParticleManager.get().add(p);
        }
    }

    private static void applyColor(CustomParticle p, FeatureSettings fs,
                                   boolean customColor, int[] defaultRgb) {
        if (customColor) {
            p.r = clamp((int)(fs.colorR * 255));
            p.g = clamp((int)(fs.colorG * 255));
            p.b = clamp((int)(fs.colorB * 255));
        } else {
            p.r = clamp(defaultRgb[0] + RNG.nextInt(20) - 10);
            p.g = clamp(defaultRgb[1] + RNG.nextInt(20) - 10);
            p.b = clamp(defaultRgb[2] + RNG.nextInt(20) - 10);
        }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private HitEffectHandler() {}
}
