package com.example.cosmetics.hit;

import com.example.cosmetics.CosmeticsMod;
import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.particles.CustomParticle;
import com.example.cosmetics.particles.ParticleManager;
import com.example.cosmetics.particles.shapes.CritParticle;
import com.example.cosmetics.particles.shapes.SlashParticle;
import com.example.cosmetics.particles.shapes.StarParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Spawns hit effect particles when the local player attacks an entity.
 *
 * Styles:
 *   0 = SLASH  — red diagonal lines burst outward
 *   1 = STARS  — yellow 3D star burst
 *   2 = CRIT   — golden sparks with gravity + outward velocity
 */
@Mod.EventBusSubscriber(modid = CosmeticsMod.MOD_ID, value = Dist.CLIENT)
public final class HitEffectHandler {

    public enum Style { SLASH, STARS, CRIT }

    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (Minecraft.getInstance().level == null) return;
        if (event.getPlayer() != Minecraft.getInstance().player) return;
        burst(event.getTarget());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity ent = event.getEntityLiving();
        if (ent == null || ent.level == null || !ent.level.isClientSide) return;
        if (event.getSource() != null
                && event.getSource().getEntity() == Minecraft.getInstance().player) return;
        burst(ent);
    }

    private static void burst(Entity target) {
        if (target == null) return;
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.HIT_EFFECT)) return;
        FeatureSettings fs = state.settings(FeatureType.HIT_EFFECT);

        Style style = Style.values()[Math.floorMod(fs.style, Style.values().length)];
        int count = Math.max(1, fs.count);

        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.6;
        double cz = target.getZ();

        boolean customColor = fs.colorR != 1.0F || fs.colorG != 1.0F || fs.colorB != 1.0F;

        for (int i = 0; i < count; i++) {
            // Outward burst velocity (explode from center)
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
                case SLASH:
                    p = new SlashParticle(px, py, pz, vx * 0.4, vy, vz * 0.4);
                    p.maxAge = 12;
                    defaultRgb = new int[]{ 255, 35, 35 };
                    break;
                case STARS:
                    p = new StarParticle(px, py, pz, vx, vy, vz);
                    p.maxAge = 22;
                    defaultRgb = new int[]{ 255, 235, 60 };
                    break;
                case CRIT:
                default:
                    CritParticle cp = new CritParticle(px, py, pz, vx, vy, vz);
                    p = cp;
                    p.maxAge = 18;
                    defaultRgb = new int[]{ 255, 195, 30 };
                    break;
            }

            if (customColor) {
                p.r = clamp((int)(fs.colorR * 255));
                p.g = clamp((int)(fs.colorG * 255));
                p.b = clamp((int)(fs.colorB * 255));
            } else {
                // Slight random variation on default color for more life
                p.r = clamp(defaultRgb[0] + RNG.nextInt(20) - 10);
                p.g = clamp(defaultRgb[1] + RNG.nextInt(20) - 10);
                p.b = clamp(defaultRgb[2] + RNG.nextInt(20) - 10);
            }

            p.size = 0.45F * fs.size;
            ParticleManager.get().add(p);
        }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private HitEffectHandler() {}
}
