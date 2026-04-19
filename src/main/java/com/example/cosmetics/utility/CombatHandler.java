package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Combat utility features:
 *
 *  Kill Aura
 *  ─────────
 *  size        — attack range in blocks (0.5 – 10)
 *  speed       — minimum ticks between attacks (1 – 40)
 *                when "weapon cooldown" flag is on this becomes a lower bound;
 *                the actual attack only fires once the weapon is fully charged.
 *  style       — camera/rotation mode
 *                  0  BODY_TRACK  body yaw snaps to target;
 *                                 1st-person view stays free (look around freely),
 *                                 3rd-person shows the character visibly turned
 *                  1  FULL_LOCK   yaw + pitch snap in all perspectives
 *                  2  SILENT      no rotation at all (ghost hits)
 *  extraFlags  — bitmask of toggles
 *                  0x01  weapon cooldown gate (attack only when fully charged)
 *                  0x02  auto crit            (micro-jump before each swing)
 *                  0x04  target players
 *                  0x08  target hostile mobs
 *                  0x10  target passive / neutral animals
 *
 *  count       — target sort mode
 *                  0  closest first
 *                  1  lowest HP first
 *                  2  highest HP first
 *
 *  Auto Clicker
 *  ────────────
 *  count  — min CPS, speed — max CPS (1-20)
 */
public final class CombatHandler {

    private static final CombatHandler INSTANCE = new CombatHandler();
    public static CombatHandler get() { return INSTANCE; }

    private static final Random RNG = new Random();

    // ---- Kill Aura state -------------------------------------------------------
    private int    auraCooldown   = 0;
    private float  savedYaw       = 0;   // player's own view yaw when BODY_TRACK
    private float  savedPitch     = 0;
    private boolean auraWasActive = false;

    // ---- Crit state ------------------------------------------------------------
    private int critJumpedTick = -999; // game tick when micro-jump was issued


    // ---- Auto Clicker state ----------------------------------------------------
    private int clickerTick     = 0;
    private int clickerInterval = 4;

    private CombatHandler() {}

    // ============================================================
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientPlayerEntity player = mc.player;

        tickNoFireOverlay(player);
        tickKillAura(mc, player);
        tickAutoClicker(mc, player);
        // standalone Crit (without Kill Aura) handled inside tickKillAura
        // and also standalone below
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) {
            tickStandaloneCrit(player);
        }
    }

    // ============================================================
    // NO FIRE OVERLAY
    // ============================================================
    private static void tickNoFireOverlay(ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY)) return;
        if (player.getRemainingFireTicks() > 0) player.setRemainingFireTicks(0);
    }

    // ============================================================
    // KILL AURA  (full rewrite)
    // ============================================================
    private void tickKillAura(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) {
            // Restore view if we were rotating it
            if (auraWasActive) {
                player.yRot  = savedYaw;
                player.xRot  = savedPitch;
                auraWasActive = false;
            }
            auraCooldown = 0;
            return;
        }

        FeatureSettings fs  = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        int   camMode       = Math.floorMod(fs.style, 3);
        int   flags         = fs.extraFlags;
        int   sortMode      = Math.floorMod(fs.count, 3);
        float range         = Math.max(0.5F, Math.min(10F, fs.size));
        int   minDelay      = Math.max(1, (int) fs.speed);
        boolean useWeaponCD = (flags & 0x01) != 0;
        boolean autoCrit    = (flags & 0x02) != 0;

        // Save the player's own mouse-look so we can restore it in BODY_TRACK
        if (!auraWasActive) {
            savedYaw   = player.yRot;
            savedPitch = player.xRot;
        }
        auraWasActive = true;

        // ---- Find target -------------------------------------------------------
        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> isValidTarget(e, player, flags));

        if (candidates.isEmpty()) {
            // Restore rotation when no targets
            if (camMode == 0) {
                player.yRot = savedYaw;
                player.xRot = savedPitch;
            }
            auraCooldown = 0;
            return;
        }

        LivingEntity target = pickTarget(candidates, player, sortMode);
        if (target == null) return;

        // ---- Rotation ----------------------------------------------------------
        applyRotation(player, target, camMode);

        // ---- Cooldown ----------------------------------------------------------
        if (auraCooldown > 0) {
            auraCooldown--;
            return;
        }

        // ---- Weapon charge gate ------------------------------------------------
        if (useWeaponCD && player.getAttackStrengthScale(0F) < 1.0F) return;

        // ---- Auto Crit: micro-jump one tick before attack ----------------------
        if (autoCrit) {
            boolean canCrit = player.isOnGround()
                    && !player.isCrouching()
                    && !player.isInWater()
                    && !player.onClimbable()
                    && !player.isSprinting();
            if (canCrit) {
                int currentTick = (int)(mc.level.getGameTime() & 0x7FFFFFFF);
                if (currentTick != critJumpedTick) {
                    // Issue micro-jump; real attack next tick when airborne
                    player.setDeltaMovement(
                            player.getDeltaMovement().x,
                            0.11,
                            player.getDeltaMovement().z);
                    critJumpedTick = currentTick;
                    auraCooldown = 1; // wait one tick → airborne → crit
                    return;
                }
                // Second tick: still mark not on ground → vanilla crit fires
            }
        }

        // ---- Attack ------------------------------------------------------------
        mc.gameMode.attack(player, target);
        player.swing(Hand.MAIN_HAND);
        auraCooldown = minDelay;
    }

    // ---- Rotation helper -------------------------------------------------------
    private void applyRotation(ClientPlayerEntity player, LivingEntity target, int camMode) {
        if (camMode == 2) return; // SILENT — no rotation

        double dx   = target.getX() - player.getX();
        double dy   = (target.getY() + target.getBbHeight() * 0.5)
                    - (player.getY() + player.getEyeHeight());
        double dz   = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        if (camMode == 0) {
            // BODY_TRACK: body + head snap, but camera (xRot/yRot used for view)
            // stays at savedYaw / savedPitch so 1st-person view is free.
            // In 3rd-person the model visibly faces the enemy.
            player.yBodyRot  = targetYaw;
            player.yHeadRot  = targetYaw;
            // Update saved values from current mouse input each tick
            savedYaw   = player.yRot;
            savedPitch = player.xRot;
            // Do NOT touch player.yRot / player.xRot → free look
        } else {
            // FULL_LOCK: camera also snaps
            player.yRot      = targetYaw;
            player.xRot      = targetPitch;
            player.yBodyRot  = targetYaw;
            player.yHeadRot  = targetYaw;
        }
    }

    // ---- Target picker ---------------------------------------------------------
    private static LivingEntity pickTarget(List<LivingEntity> list,
                                           ClientPlayerEntity player, int sortMode) {
        switch (sortMode) {
            case 1: // lowest HP
                return list.stream()
                        .min(Comparator.comparingDouble(LivingEntity::getHealth))
                        .orElse(null);
            case 2: // highest HP
                return list.stream()
                        .max(Comparator.comparingDouble(LivingEntity::getHealth))
                        .orElse(null);
            default: // closest
                return list.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                        .orElse(null);
        }
    }

    // ---- Target filter ---------------------------------------------------------
    private static boolean isValidTarget(LivingEntity e, PlayerEntity player, int flags) {
        if (e == player)   return false;
        if (!e.isAlive())  return false;

        boolean wantPlayers  = (flags & 0x04) != 0;
        boolean wantHostile  = (flags & 0x08) != 0;
        boolean wantPassive  = (flags & 0x10) != 0;

        if (e instanceof PlayerEntity) return wantPlayers;

        EntityClassification cat = e.getType().getCategory();
        if (cat == EntityClassification.MONSTER) return wantHostile;
        if (cat == EntityClassification.CREATURE
         || cat == EntityClassification.AMBIENT
         || cat == EntityClassification.WATER_CREATURE
         || cat == EntityClassification.WATER_AMBIENT) return wantPassive;

        return false;
    }

    // ============================================================
    // STANDALONE CRIT  (when Kill Aura is off)
    // ============================================================
    private void tickStandaloneCrit(ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.CRIT)) return;
        boolean swinging = player.getAttackStrengthScale(0F) < 0.9F;
        if (swinging && player.isOnGround() && !player.isCrouching()
                && !player.isInWater() && !player.onClimbable() && !player.isSprinting()) {
            player.setDeltaMovement(
                    player.getDeltaMovement().x,
                    0.11,
                    player.getDeltaMovement().z);
        }
    }

    // ============================================================
    // AUTO CLICKER
    // ============================================================
    private void tickAutoClicker(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_CLICKER)) {
            clickerTick = 0;
            return;
        }
        if (!mc.options.keyAttack.isDown()) {
            clickerTick = 0;
            return;
        }
        clickerTick++;
        if (clickerTick >= clickerInterval) {
            clickerTick = 0;
            FeatureSettings fs = CosmeticsState.get().settings(FeatureType.AUTO_CLICKER);
            int minCps = Math.max(1, Math.min(20, (int) fs.count));
            int maxCps = Math.max(minCps, Math.min(20, (int) fs.speed));
            int cps    = minCps + (maxCps > minCps ? RNG.nextInt(maxCps - minCps + 1) : 0);
            clickerInterval = Math.max(1, 20 / cps);
            KeyBinding.click(mc.options.keyAttack.getKey());
        }
    }

    public static boolean shouldSuppressFireOverlay() {
        return CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY);
    }
}
