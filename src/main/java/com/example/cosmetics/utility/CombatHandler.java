package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

import java.util.List;

/**
 * Handles combat utility features:
 *  - Auto Block  (shield up when enemy nearby, lower to attack, raise again)
 *  - No Hurt Cam (cancel screen shake on damage)
 *  - No Fire Overlay (cancel fire texture render when burning)
 */
public final class CombatHandler {

    private static final CombatHandler INSTANCE = new CombatHandler();
    public static CombatHandler get() { return INSTANCE; }

    // Auto Block: how many ticks to keep shield lowered after an attack swing
    private int blockCooldown = 0;
    private static final int BLOCK_LOWER_TICKS = 6; // ~0.3s — enough for hit to register
    private static final double BLOCK_RANGE = 6.0; // blocks distance to trigger

    private CombatHandler() {}

    // -------------------------------------------------------------------------
    // Called every client tick from ClientEvents
    // -------------------------------------------------------------------------
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        tickAutoBlock(mc, mc.player);
        tickNoHurtCam(mc.player);
    }

    // -------------------------------------------------------------------------
    // No Hurt Cam
    //
    // Vanilla applies a camera roll/tilt while player.hurtTime > 0.
    // We simply zero hurtTime every tick when the feature is on — the damage
    // is already applied, only the visual shake is cancelled.
    // -------------------------------------------------------------------------
    private static void tickNoHurtCam(net.minecraft.client.entity.player.ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.NO_HURT_CAM)) return;
        if (player.hurtTime > 0) {
            player.hurtTime = 0;
        }
    }

    // -------------------------------------------------------------------------
    // Auto Block
    //
    // Logic:
    //  1. Player must have a shield in main or off hand.
    //  2. If an enemy is within BLOCK_RANGE — start using the shield (raise it).
    //  3. When the player swings (attackStrengthTicker resets to 0) — stop using
    //     for BLOCK_LOWER_TICKS ticks so the hit registers, then raise again.
    // -------------------------------------------------------------------------
    private void tickAutoBlock(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_BLOCK)) {
            // Release shield if we were holding it
            if (player.isUsingItem() && isShield(player.getUseItem())) {
                player.stopUsingItem();
            }
            blockCooldown = 0;
            return;
        }

        // Need a shield somewhere
        Hand shieldHand = getShieldHand(player);
        if (shieldHand == null) {
            blockCooldown = 0;
            return;
        }

        // Detect attack swing: attackStrengthTicker < 1 means just swung
        boolean justSwung = player.getAttackStrengthScale(0F) < 0.9F
                && !player.isUsingItem();
        if (justSwung) {
            blockCooldown = BLOCK_LOWER_TICKS;
        }

        if (blockCooldown > 0) {
            blockCooldown--;
            // Keep shield down during cooldown
            if (player.isUsingItem() && isShield(player.getUseItem())) {
                player.stopUsingItem();
            }
            return;
        }

        // Check if there's a hostile entity nearby
        boolean enemyNearby = hasEnemyNearby(mc, player);

        if (enemyNearby) {
            if (!player.isUsingItem()) {
                // Raise shield
                mc.gameMode.useItem(player, mc.level, shieldHand);
            }
        } else {
            // No enemy — lower shield
            if (player.isUsingItem() && isShield(player.getUseItem())) {
                player.stopUsingItem();
            }
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
        if (e == player) return false;
        if (!e.isAlive()) return false;
        // Other players count as threats
        if (e instanceof PlayerEntity) return true;
        // Mobs that have a target set to the player
        net.minecraft.entity.MobEntity mob = (e instanceof net.minecraft.entity.MobEntity)
                ? (net.minecraft.entity.MobEntity) e : null;
        if (mob != null && mob.getTarget() == player) return true;
        // Classic hostile mob categories
        net.minecraft.entity.EntityType<?> type = e.getType();
        return type.getCategory() == net.minecraft.entity.EntityClassification.MONSTER;
    }

    // -------------------------------------------------------------------------
    // No Hurt Cam — called from RenderGameOverlayEvent or camera event
    // Returns true if the camera tilt from damage should be suppressed
    // -------------------------------------------------------------------------
    public static boolean shouldSuppressHurtCam() {
        return CosmeticsState.get().isOn(FeatureType.NO_HURT_CAM);
    }

    // -------------------------------------------------------------------------
    // No Fire Overlay — called from RenderGameOverlayEvent.Pre
    // Returns true if the fire overlay render should be cancelled
    // -------------------------------------------------------------------------
    public static boolean shouldSuppressFireOverlay() {
        return CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY);
    }
}
