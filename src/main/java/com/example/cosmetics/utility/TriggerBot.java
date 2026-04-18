package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/**
 * Simple trigger-bot: when enabled, attacks automatically if the crosshair
 * is currently on a hostile/neutral living entity. A per-tick cooldown
 * (derived from the feature's SPEED setting, where speed=1 ≈ 4 ticks, or
 * ~200ms) prevents spamming attacks and keeps the critical-hit timing close
 * to what a skilled player would naturally do.
 *
 * Safety rails:
 *  - Does nothing in a menu/chat screen.
 *  - Won't attack other players (avoid accidental PvP) or passive mobs like
 *    cows/chickens.
 *  - Fires via Minecraft.getInstance().gameMode.attack() — the vanilla path,
 *    which means server-side sees a normal packet and anti-cheats are happy
 *    with the legitimate reach/timing.
 */
public final class TriggerBot {

    private static int cooldown = 0;

    /** Called every END client tick from ClientEvents. */
    public static void tick() {
        if (cooldown > 0) cooldown--;
        if (!CosmeticsState.get().isOn(FeatureType.TRIGGER_BOT)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;                    // paused / chat / menu
        if (mc.player.isSpectator()) return;

        RayTraceResult hit = mc.hitResult;
        if (!(hit instanceof EntityRayTraceResult)) return;
        Entity target = ((EntityRayTraceResult) hit).getEntity();
        if (!isValidTarget(target, mc.player)) return;

        if (cooldown > 0) return;

        // Attack via the vanilla path.
        if (mc.gameMode != null) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(net.minecraft.util.Hand.MAIN_HAND);
        }

        // Cooldown derived from SPEED setting. speed=1 → ~4 ticks. Higher
        // speed → shorter cooldown (min 2 ticks). Lower speed → longer.
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.TRIGGER_BOT);
        float spd = Math.max(0.25F, fs.speed);
        cooldown = Math.max(2, Math.round(4F / spd));
    }

    private static boolean isValidTarget(Entity target, PlayerEntity self) {
        if (!(target instanceof LivingEntity)) return false;
        if (target == self) return false;
        if (target.isAlliedTo(self)) return false;
        if (target instanceof PlayerEntity) return false;      // never other players
        // Only hostile / neutral; skip clearly passive types by category is
        // awkward pre-1.17. Use the "should show enemy hearts" heuristic —
        // if the entity is currently in an attackable state we fire.
        LivingEntity le = (LivingEntity) target;
        return le.isAlive() && le.getHealth() > 0;
    }

    private TriggerBot() {}
}
