package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Combat utility features.
 *
 * Kill Aura logic:
 *  - Always uses weapon cooldown gating (attacks only when charge = 1.0)
 *  - Rotation mode: 0 = BODY_TRACK, 1 = FULL_LOCK, 2 = SILENT
 *  - Aim is INSTANT (no lerp) — snaps to target immediately
 *  - All target filters and sort mode exposed as booleans / cycle settings
 */
public final class CombatHandler {

    private static final CombatHandler INSTANCE = new CombatHandler();
    public static CombatHandler get() { return INSTANCE; }

    private static final Random RNG = new Random();

    // ---- Kill Aura state ----------------------------------------------------
    private int critJumpedTick = -999;

    // ---- Auto Clicker state -------------------------------------------------
    private int clickerTick     = 0;
    private int clickerInterval = 4;

    // ---- Auto Pot / Auto Gap state ------------------------------------------
    private int potCooldown = 0;
    private int gapCooldown = 0;

    private CombatHandler() {}

    // =========================================================================
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientPlayerEntity player = mc.player;

        tickNoFireOverlay(player);
        tickKillAura(mc, player);
        tickAutoClicker(mc, player);
        tickSmoothAim(mc, player);
        tickStrafe(mc, player);
        tickAutoPot(mc, player);
        tickAutoGap(mc, player);
        if (potCooldown > 0) potCooldown--;
        if (gapCooldown > 0) gapCooldown--;

        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) {
            tickStandaloneCrit(player);
        }
    }

    // =========================================================================
    // NO FIRE OVERLAY
    // =========================================================================
    private static void tickNoFireOverlay(ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY)) return;
        if (player.getRemainingFireTicks() > 0) player.setRemainingFireTicks(0);
    }

    // =========================================================================
    // KILL AURA
    //
    // Attack fires ONLY when weapon is fully charged (cooldown = 1.0).
    // No manual delay setting — the weapon's own cooldown IS the delay.
    // Aim is INSTANT — no smoothing.
    // =========================================================================
    private void tickKillAura(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) return;

        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.KILL_AURA);

        float range = Math.max(0.5F, Math.min(10F, fs.size));

        // ---- Find target ----------------------------------------------------
        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> isValidKillAuraTarget(e, player, fs));

        if (candidates.isEmpty()) return;

        LivingEntity target = pickTarget(candidates, player, fs.killAuraSortMode);
        if (target == null) return;

        // ---- Instant aim (no lerp / smoothing) ------------------------------
        double dx = target.getX() - player.getX();
        double dy = (target.getY() + target.getBbHeight() * 0.5)
                  - (player.getY() + player.getEyeHeight());
        double dz = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float wantYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        wantPitch = Math.max(-90F, Math.min(90F, wantPitch));

        int rotMode = Math.floorMod(fs.killAuraRotMode, 3);
        switch (rotMode) {
            case 0: // BODY_TRACK — snap body, keep camera free
                player.yBodyRot = wantYaw;
                player.yHeadRot = wantYaw;
                break;
            case 1: // FULL_LOCK — snap camera too
                player.yRot     = wantYaw;
                player.xRot     = wantPitch;
                player.yBodyRot = wantYaw;
                player.yHeadRot = wantYaw;
                break;
            case 2: // SILENT — no rotation at all
                break;
        }

        // ---- Weapon cooldown gate (always ON) --------------------------------
        if (player.getAttackStrengthScale(0F) < 1.0F) return;

        // ---- Auto Crit: micro-jump before swing ------------------------------
        if (fs.killAuraAutoCrit) {
            boolean canCrit = player.isOnGround()
                    && !player.isCrouching()
                    && !player.isInWater()
                    && !player.onClimbable()
                    && !player.isSprinting();
            if (canCrit) {
                int tick = (int)(mc.level.getGameTime() & 0x7FFFFFFF);
                if (tick != critJumpedTick) {
                    player.setDeltaMovement(
                            player.getDeltaMovement().x,
                            0.11,
                            player.getDeltaMovement().z);
                    critJumpedTick = tick;
                    return; // attack next tick after the jump
                }
            }
        }

        // ---- Attack! ---------------------------------------------------------
        mc.gameMode.attack(player, target);
        player.swing(Hand.MAIN_HAND);
    }

    // ---- Target filter -------------------------------------------------------
    private static boolean isValidKillAuraTarget(LivingEntity e,
                                                  ClientPlayerEntity player,
                                                  FeatureSettings fs) {
        if (e == player)  return false;
        if (!e.isAlive()) return false;

        // Category filters
        boolean isPlayer  = e instanceof PlayerEntity;
        boolean isHostile = e instanceof IMob;
        boolean isPassive = e instanceof AnimalEntity && !isHostile;

        if (isPlayer  && !fs.killAuraTargetPlayers)  return false;
        if (isHostile && !fs.killAuraTargetHostile)  return false;
        if (isPassive && !fs.killAuraTargetPassive)  return false;
        // If none of the three categories matched, skip
        if (!isPlayer && !isHostile && !isPassive)   return false;

        // Anti Bot filter
        if (fs.killAuraAntiBot && isPlayer) {
            if (e.isInvisible()) return false;
            if (e.getName().getString().isEmpty()) return false;
            if (e.getDeltaMovement().lengthSqr() == 0 && !e.hasCustomName()) return false;
        }
        return true;
    }

    // ---- Target picker -------------------------------------------------------
    private static LivingEntity pickTarget(List<LivingEntity> list,
                                           ClientPlayerEntity player, int sortMode) {
        switch (Math.floorMod(sortMode, 3)) {
            case 1: return list.stream()
                        .min(Comparator.comparingDouble(LivingEntity::getHealth))
                        .orElse(null);
            case 2: return list.stream()
                        .max(Comparator.comparingDouble(LivingEntity::getHealth))
                        .orElse(null);
            default: return list.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                        .orElse(null);
        }
    }

    // =========================================================================
    // STANDALONE CRIT (when Kill Aura is off)
    // =========================================================================
    private void tickStandaloneCrit(ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.CRIT)) return;
        boolean swinging = player.getAttackStrengthScale(0F) < 0.9F;
        if (swinging && player.isOnGround() && !player.isCrouching()
                && !player.isInWater() && !player.onClimbable() && !player.isSprinting()) {
            player.setDeltaMovement(
                    player.getDeltaMovement().x, 0.11, player.getDeltaMovement().z);
        }
    }

    // =========================================================================
    // AUTO CLICKER
    // =========================================================================
    private void tickAutoClicker(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_CLICKER)) { clickerTick = 0; return; }
        if (!mc.options.keyAttack.isDown())                        { clickerTick = 0; return; }
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

    // =========================================================================
    // SMOOTH AIM — плавная наводка мышкой (отдельная от Kill Aura)
    // =========================================================================
    private void tickSmoothAim(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.SMOOTH_AIM)) return;
        if (mc.screen != null) return;

        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.SMOOTH_AIM);
        float fov   = Math.max(10F, Math.min(180F, fs.size));
        float speed = Math.max(1F,  Math.min(20F,  fs.speed));

        PlayerEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof PlayerEntity) || e == player || !e.isAlive()) continue;
            double dx = e.getX() - player.getX();
            double dy = (e.getY() + e.getBbHeight() * 0.5) - (player.getY() + player.getEyeHeight());
            double dz = e.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            float needYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float needPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
            float dYaw   = Math.abs(wrapDegrees(needYaw - player.yRot));
            float dPitch = Math.abs(needPitch - player.xRot);
            if (dYaw > fov / 2F || dPitch > fov / 2F) continue;
            if (dist < bestDist) { bestDist = dist; target = (PlayerEntity) e; }
        }
        if (target == null) return;

        double dx = target.getX() - player.getX();
        double dy = (target.getY() + target.getBbHeight() * 0.5) - (player.getY() + player.getEyeHeight());
        double dz = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float tYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        float lerpT = speed / 20F;
        player.yRot = lerpAngle(player.yRot, tYaw, lerpT);
        player.xRot = lerp(player.xRot, tPitch, lerpT);
        player.xRot = Math.max(-90F, Math.min(90F, player.xRot));
    }

    // =========================================================================
    // STRAFE
    // =========================================================================
    private double strafeAngle = 0;

    private void tickStrafe(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.STRAFE)) return;
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) return;
        if (mc.screen != null) return;

        FeatureSettings fs   = CosmeticsState.get().settings(FeatureType.STRAFE);
        FeatureSettings kaFs = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        float speed = Math.max(0.5F, Math.min(5F, fs.speed));
        float range = Math.max(0.5F, Math.min(10F, kaFs.size));

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> isValidKillAuraTarget(e, player, kaFs));
        if (candidates.isEmpty()) return;

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (target == null) return;

        strafeAngle += speed * 0.05;
        double radius = Math.max(2.0, range * 0.6);
        double tx = target.getX() + Math.cos(strafeAngle) * radius;
        double tz = target.getZ() + Math.sin(strafeAngle) * radius;

        double moveX = tx - player.getX();
        double moveZ = tz - player.getZ();
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0.01) {
            moveX /= len; moveZ /= len;
            player.setDeltaMovement(moveX * 0.28, player.getDeltaMovement().y, moveZ * 0.28);
        }
    }

    // =========================================================================
    // AUTO POT
    // =========================================================================
    private void tickAutoPot(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_POT)) return;
        if (potCooldown > 0) return;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.AUTO_POT);
        float threshold = fs.count * 2F;
        if (player.getHealth() >= player.getMaxHealth() - 0.5F) return;
        if (player.getHealth() > threshold) return;

        NonNullList<ItemStack> inv = player.inventory.items;
        int potSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.get(i);
            net.minecraft.item.Item it = stack.getItem();
            if (it != Items.SPLASH_POTION && it != Items.LINGERING_POTION) continue;
            Potion pot = PotionUtils.getPotion(stack);
            if (pot == Potions.HEALING || pot == Potions.STRONG_HEALING) { potSlot = i; break; }
            for (EffectInstance eff : PotionUtils.getMobEffects(stack))
                if (eff.getEffect() == Effects.HEAL) { potSlot = i; break; }
            if (potSlot != -1) break;
        }
        if (potSlot == -1) return;

        int prev       = player.inventory.selected;
        int hotbarSlot = potSlot < 9 ? potSlot : 0;
        if (potSlot >= 9) {
            ItemStack tmp = inv.get(hotbarSlot).copy();
            inv.set(hotbarSlot, inv.get(potSlot).copy());
            inv.set(potSlot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }
        float origPitch = player.xRot;
        player.xRot = 90F;
        player.inventory.selected = hotbarSlot;
        mc.getConnection().send(new CHeldItemChangePacket(hotbarSlot));
        mc.gameMode.useItem(player, mc.level, Hand.MAIN_HAND);
        player.xRot = origPitch;
        player.inventory.selected = prev;
        mc.getConnection().send(new CHeldItemChangePacket(prev));
        potCooldown = 30;
    }

    // =========================================================================
    // AUTO GAP
    // =========================================================================
    private void tickAutoGap(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_GAP)) return;
        if (gapCooldown > 0) return;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.AUTO_GAP);
        float threshold = fs.count * 2F;
        if (player.getHealth() > threshold) return;

        NonNullList<ItemStack> inv = player.inventory.items;
        int gapSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.GOLDEN_APPLE
             || inv.get(i).getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                gapSlot = i; break;
            }
        }
        if (gapSlot == -1) return;

        int prev       = player.inventory.selected;
        int hotbarSlot = gapSlot < 9 ? gapSlot : 0;
        if (gapSlot >= 9) {
            ItemStack tmp = inv.get(hotbarSlot).copy();
            inv.set(hotbarSlot, inv.get(gapSlot).copy());
            inv.set(gapSlot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }
        player.inventory.selected = hotbarSlot;
        mc.getConnection().send(new CHeldItemChangePacket(hotbarSlot));
        mc.gameMode.useItem(player, mc.level, Hand.MAIN_HAND);
        player.inventory.selected = prev;
        mc.getConnection().send(new CHeldItemChangePacket(prev));
        gapCooldown = 40;
    }

    // ---- helpers ---------------------------------------------------------------
    private static float lerpAngle(float from, float to, float t) {
        return from + wrapDegrees(to - from) * t;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float wrapDegrees(float d) {
        d %= 360F;
        if (d >= 180F)  d -= 360F;
        if (d < -180F)  d += 360F;
        return d;
    }

    public static boolean shouldSuppressFireOverlay() {
        return CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY);
    }
}
