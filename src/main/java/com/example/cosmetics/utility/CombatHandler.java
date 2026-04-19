package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Handles combat utility features:
 *  - Auto Block     (shield up when enemy nearby)
 *  - No Fire Overlay
 *  - Kill Aura      (auto-attack nearest entity)
 *  - Crit           (micro-jump before swing to force critical hit)
 *  - Auto Clicker   (simulate LMB clicks at configurable CPS)
 */
public final class CombatHandler {

    private static final CombatHandler INSTANCE = new CombatHandler();
    public static CombatHandler get() { return INSTANCE; }

    private static final Random RNG = new Random();

    // Auto Block
    private int blockCooldown = 0;
    private static final int BLOCK_LOWER_TICKS = 6;
    private static final double BLOCK_RANGE = 6.0;

    // Kill Aura
    private int killAuraCooldown = 0;

    // Crit
    private boolean critPending = false;

    // Auto Clicker
    private int clickerTick = 0;
    private int clickerInterval = 4; // ticks between clicks, recalculated each cycle

    private CombatHandler() {}

    // -------------------------------------------------------------------------
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientPlayerEntity player = mc.player;

        tickAutoBlock(mc, player);
        tickNoFireOverlay(player);
        tickKillAura(mc, player);
        tickCrit(mc, player);
        tickAutoClicker(mc, player);
    }

    // -------------------------------------------------------------------------
    // No Fire Overlay
    // -------------------------------------------------------------------------
    private static void tickNoFireOverlay(ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY)) return;
        if (player.getRemainingFireTicks() > 0) {
            player.setRemainingFireTicks(0);
        }
    }

    // -------------------------------------------------------------------------
    // Kill Aura
    //
    // style 0 = mobs only, style 1 = players only, style 2 = all living entities
    // speed = attack cooldown in ticks (min 1)
    // -------------------------------------------------------------------------
    private void tickKillAura(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) {
            killAuraCooldown = 0;
            return;
        }

        if (killAuraCooldown > 0) {
            killAuraCooldown--;
            return;
        }

        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        int style = Math.floorMod(fs.style, 3);
        double range = 4.5; // standard melee range

        List<LivingEntity> targets = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> isValidTarget(e, player, style));

        if (targets.isEmpty()) return;

        // Attack closest
        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (target == null) return;

        // Face target (snap yaw/pitch smoothly)
        faceEntity(player, target);

        // Attack
        mc.gameMode.attack(player, target);
        player.swing(Hand.MAIN_HAND);

        int delay = Math.max(1, (int) fs.speed);
        killAuraCooldown = delay;
    }

    /** Rotate player's yaw and pitch toward the target entity. */
    private static void faceEntity(ClientPlayerEntity player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dy = (target.getY() + target.getBbHeight() * 0.5)
                  - (player.getY() + player.getEyeHeight());
        double dz = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        player.yRot  = yaw;
        player.xRot  = pitch;
        player.yBodyRot = yaw;
        player.yHeadRot = yaw;
    }

    private static boolean isValidTarget(LivingEntity e, PlayerEntity player, int style) {
        if (e == player) return false;
        if (!e.isAlive()) return false;
        switch (style) {
            case 0: // mobs only
                return !(e instanceof PlayerEntity)
                    && e.getType().getCategory() == net.minecraft.entity.EntityClassification.MONSTER;
            case 1: // players only
                return e instanceof PlayerEntity;
            default: // all living
                return true;
        }
    }

    // -------------------------------------------------------------------------
    // Crit
    //
    // In vanilla, a critical hit requires the player to be falling (not on ground,
    // not climbing, not in water, not sprinting). We trigger a micro-jump of 0.1
    // blocks so the player is briefly airborne before the attack lands.
    // Works by hooking into Kill Aura's attack timing OR the player's own swing.
    // -------------------------------------------------------------------------
    private void tickCrit(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.CRIT)) return;

        // If Kill Aura is also on it handles the swing; we just set up the jump.
        // Detect a swing starting: attackStrengthScale drops below 1 (swing began).
        float swingProgress = player.getAttackStrengthScale(0F);
        boolean swinging = swingProgress < 0.9F;

        if (swinging && player.isOnGround() && !player.isCrouching()
                && !player.isInWater() && !player.isSprinting()) {
            // Micro-jump: enough to make the next tick "falling"
            player.setDeltaMovement(
                    player.getDeltaMovement().x,
                    0.1,
                    player.getDeltaMovement().z);
            critPending = true;
        }

        if (critPending && !player.isOnGround()) {
            // We're airborne — next attack will crit automatically
            critPending = false;
        }
    }

    // -------------------------------------------------------------------------
    // Auto Clicker
    //
    // count = min CPS, speed = max CPS (both clamped 1–20).
    // Each cycle picks a random interval between min and max for humanisation.
    // Only clicks when LMB is held down (mc.options.keyAttack.isDown()).
    // -------------------------------------------------------------------------
    private void tickAutoClicker(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_CLICKER)) {
            clickerTick = 0;
            return;
        }

        // Only fire when the player is holding LMB
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

            // Randomise next interval for humanisation (20 ticks/sec)
            int cps = minCps + (maxCps > minCps ? RNG.nextInt(maxCps - minCps + 1) : 0);
            clickerInterval = Math.max(1, 20 / cps);

            // Simulate the click
            KeyBinding.click(mc.options.keyAttack.getKey());
        }
    }

    // -------------------------------------------------------------------------
    // Auto Block
    // -------------------------------------------------------------------------
    private void tickAutoBlock(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_BLOCK)) {
            if (player.isUsingItem() && isShield(player.getUseItem())) {
                player.stopUsingItem();
            }
            blockCooldown = 0;
            return;
        }

        Hand shieldHand = getShieldHand(player);
        if (shieldHand == null) {
            blockCooldown = 0;
            return;
        }

        boolean justSwung = player.getAttackStrengthScale(0F) < 0.9F && !player.isUsingItem();
        if (justSwung) blockCooldown = BLOCK_LOWER_TICKS;

        if (blockCooldown > 0) {
            blockCooldown--;
            if (player.isUsingItem() && isShield(player.getUseItem())) {
                player.stopUsingItem();
            }
            return;
        }

        boolean enemyNearby = hasEnemyNearby(mc, player);
        if (enemyNearby) {
            if (!player.isUsingItem()) mc.gameMode.useItem(player, mc.level, shieldHand);
        } else {
            if (player.isUsingItem() && isShield(player.getUseItem())) player.stopUsingItem();
        }
    }

    private static Hand getShieldHand(ClientPlayerEntity player) {
        if (isShield(player.getItemInHand(Hand.OFF_HAND)))  return Hand.OFF_HAND;
        if (isShield(player.getItemInHand(Hand.MAIN_HAND))) return Hand.MAIN_HAND;
        return null;
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
    }

    private static boolean hasEnemyNearby(Minecraft mc, ClientPlayerEntity player) {
        List<LivingEntity> entities = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(BLOCK_RANGE),
                e -> isHostile(e, player));
        return !entities.isEmpty();
    }

    private static boolean isHostile(LivingEntity e, PlayerEntity player) {
        if (e == player || !e.isAlive()) return false;
        if (e instanceof PlayerEntity) return true;
        net.minecraft.entity.MobEntity mob = (e instanceof net.minecraft.entity.MobEntity)
                ? (net.minecraft.entity.MobEntity) e : null;
        if (mob != null && mob.getTarget() == player) return true;
        return e.getType().getCategory() == net.minecraft.entity.EntityClassification.MONSTER;
    }

    public static boolean shouldSuppressFireOverlay() {
        return CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY);
    }
}
