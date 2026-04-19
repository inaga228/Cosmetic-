package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
    private int   auraCooldown = 0;
    private float bodyYawSmooth = 0;  // плавный поворот тела

    // ---- Crit state ------------------------------------------------------------
    private int critJumpedTick = -999; // game tick when micro-jump was issued


    // ---- Auto Clicker state ----------------------------------------------------
    private int clickerTick     = 0;
    private int clickerInterval = 4;

    // ---- Smooth Aim state -------------------------------------------------------
    private float smoothYaw   = 0;
    private float smoothPitch = 0;

    // ---- Strafe state -----------------------------------------------------------
    private double strafeAngle = 0;

    // ---- Auto Pot / Auto Gap state ---------------------------------------------
    private int potCooldown = 0;
    private int gapCooldown = 0;

    private CombatHandler() {}

    // ============================================================
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
        // standalone Crit (without Kill Aura) handled inside tickKillAura
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
            auraCooldown = 0;
            return;
        }

        FeatureSettings fs  = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        int   sortMode      = Math.floorMod(fs.count, 3);
        float range         = Math.max(0.5F, Math.min(10F, fs.size));
        int   minDelay      = Math.max(1, (int) fs.speed);
        // style: 0 = weapon cooldown, 1 = auto crit, 2 = both
        boolean useWeaponCD = fs.style == 0 || fs.style == 2;
        boolean autoCrit    = fs.style == 1 || fs.style == 2;

        // ---- Find target — атакуем всех живых кроме себя ----------------------
        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> isValidTarget(e, player));

        if (candidates.isEmpty()) {
            auraCooldown = 0;
            return;
        }

        LivingEntity target = pickTarget(candidates, player, sortMode);
        if (target == null) return;

        // ---- Плавный поворот тела к цели (не трогаем камеру!) -----------------
        double dx  = target.getX() - player.getX();
        double dz  = target.getZ() - player.getZ();
        float wantYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        bodyYawSmooth = lerpAngle(bodyYawSmooth, wantYaw, 0.15F);
        player.yBodyRot = bodyYawSmooth;
        player.yHeadRot = bodyYawSmooth;
        // yRot и xRot (камера) — не трогаем!

        // ---- Cooldown ----------------------------------------------------------
        if (auraCooldown > 0) {
            auraCooldown--;
            return;
        }

        // ---- Weapon charge gate ------------------------------------------------
        if (useWeaponCD && player.getAttackStrengthScale(0F) < 1.0F) return;

        // ---- Auto Crit: micro-jump перед ударом --------------------------------
        if (autoCrit) {
            boolean canCrit = player.isOnGround()
                    && !player.isCrouching()
                    && !player.isInWater()
                    && !player.onClimbable()
                    && !player.isSprinting();
            if (canCrit) {
                int currentTick = (int)(mc.level.getGameTime() & 0x7FFFFFFF);
                if (currentTick != critJumpedTick) {
                    player.setDeltaMovement(
                            player.getDeltaMovement().x,
                            0.11,
                            player.getDeltaMovement().z);
                    critJumpedTick = currentTick;
                    auraCooldown = 1;
                    return;
                }
            }
        }

        // ---- Attack ------------------------------------------------------------
        mc.gameMode.attack(player, target);
        player.swing(Hand.MAIN_HAND);
        auraCooldown = minDelay;
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
    private static boolean isValidTarget(LivingEntity e, PlayerEntity player) {
        if (e == player)  return false;
        if (!e.isAlive()) return false;
        // Anti Bot фильтр
        if (CosmeticsState.get().isOn(FeatureType.ANTI_BOT) && e instanceof PlayerEntity) {
            if (e.isInvisible()) return false;
            if (e.getName().getString().isEmpty()) return false;
            if (e.getDeltaMovement().lengthSqr() == 0 && !e.hasCustomName()) return false;
        }
        return true;
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

    // ============================================================
    // SMOOTH AIM — плавная наводка на ближайшего игрока
    // ============================================================
    private void tickSmoothAim(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.SMOOTH_AIM)) return;
        if (mc.screen != null) return;

        com.example.cosmetics.feature.FeatureSettings fs =
                CosmeticsState.get().settings(FeatureType.SMOOTH_AIM);
        float fov   = Math.max(10F, Math.min(180F, fs.size));   // угол поиска
        float speed = Math.max(1F,  Math.min(20F,  fs.speed));  // скорость наводки

        // Ищем ближайшего игрока в FOV
        PlayerEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof PlayerEntity)) continue;
            if (e == player) continue;
            if (!e.isAlive()) continue;

            double dx = e.getX() - player.getX();
            double dy = (e.getY() + e.getBbHeight() * 0.5) - (player.getY() + player.getEyeHeight());
            double dz = e.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            // Угол между направлением взгляда и целью
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

        float targetYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        // Плавное движение к цели
        float lerpT = speed / 20F;
        player.yRot = lerpAngle(player.yRot, targetYaw, lerpT);
        player.xRot = lerp(player.xRot, targetPitch, lerpT);
        player.xRot = Math.max(-90F, Math.min(90F, player.xRot));
    }

    private static float lerpAngle(float from, float to, float t) {
        float diff = wrapDegrees(to - from);
        return from + diff * t;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float wrapDegrees(float deg) {
        deg = deg % 360F;
        if (deg >= 180F)  deg -= 360F;
        if (deg < -180F)  deg += 360F;
        return deg;
    }

    // ============================================================
    // STRAFE — кружение вокруг ближайшей цели Kill Aura
    // ============================================================
    private void tickStrafe(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.STRAFE)) return;
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) return;
        if (mc.screen != null) return;

        com.example.cosmetics.feature.FeatureSettings fs =
                CosmeticsState.get().settings(FeatureType.STRAFE);
        float speed = Math.max(0.5F, Math.min(5F, fs.speed));

        // Находим ту же цель что и Kill Aura (ближайшую)
        com.example.cosmetics.feature.FeatureSettings kaFs =
                CosmeticsState.get().settings(FeatureType.KILL_AURA);
        float range = Math.max(0.5F, Math.min(10F, kaFs.size));
        int flags = kaFs.extraFlags;

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> isValidTarget(e, player));
        if (candidates.isEmpty()) return;

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (target == null) return;

        // Вращаем угол и двигаемся по окружности вокруг цели
        strafeAngle += speed * 0.05;
        double radius = Math.max(2.0, range * 0.6);
        double tx = target.getX() + Math.cos(strafeAngle) * radius;
        double tz = target.getZ() + Math.sin(strafeAngle) * radius;

        double moveX = tx - player.getX();
        double moveZ = tz - player.getZ();
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0.01) {
            moveX /= len;
            moveZ /= len;
            player.setDeltaMovement(
                    moveX * 0.28,
                    player.getDeltaMovement().y,
                    moveZ * 0.28);
        }
    }

    // ============================================================
    // ANTI BOT — фильтр ботов (используется в isValidTarget)
    // ============================================================
    // Интегрирован в isValidTarget ниже через флаг ANTI_BOT

    // ============================================================
    // AUTO POT — бросить зелье лечения при низком HP
    // ============================================================
    private void tickAutoPot(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_POT)) return;
        if (potCooldown > 0) return;

        com.example.cosmetics.feature.FeatureSettings fs =
                CosmeticsState.get().settings(FeatureType.AUTO_POT);
        float threshold = fs.count * 2F; // сердца -> HP

        if (player.getHealth() >= player.getMaxHealth() - 0.5F) return;
        if (player.getHealth() > threshold) return;

        // Ищем splash/lingering зелье лечения в инвентаре
        NonNullList<ItemStack> inv = player.inventory.items;
        int potSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.get(i);
            // В 1.16.5: Items.SPLASH_POTION и LINGERING_POTION это отдельные предметы
            net.minecraft.item.Item it = stack.getItem();
            if (it != Items.SPLASH_POTION && it != Items.LINGERING_POTION) continue;
            Potion pot = PotionUtils.getPotion(stack);
            if (pot == Potions.HEALING || pot == Potions.STRONG_HEALING) {
                potSlot = i; break;
            }
            for (EffectInstance eff : PotionUtils.getMobEffects(stack)) {
                if (eff.getEffect() == Effects.HEAL) { potSlot = i; break; }
            }
            if (potSlot != -1) break;
        }
        if (potSlot == -1) return;

        int prev = player.inventory.selected;
        int hotbarSlot = potSlot < 9 ? potSlot : 0;

        // Если зелье не в хотбаре — временно свапаем
        if (potSlot >= 9) {
            ItemStack tmp = inv.get(hotbarSlot).copy();
            inv.set(hotbarSlot, inv.get(potSlot).copy());
            inv.set(potSlot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }

        // Смотрим вниз чтобы зелье упало под ноги
        float origPitch = player.xRot;
        float origYaw   = player.yRot;
        player.xRot = 90F; // смотрим прямо вниз

        player.inventory.selected = hotbarSlot;
        mc.getConnection().send(new CHeldItemChangePacket(hotbarSlot));
        mc.gameMode.useItem(player, mc.level, Hand.MAIN_HAND);

        // Восстанавливаем взгляд и слот
        player.xRot = origPitch;
        player.yRot = origYaw;
        player.inventory.selected = prev;
        mc.getConnection().send(new CHeldItemChangePacket(prev));

        potCooldown = 30; // кулдаун 1.5 секунды
    }

    // ============================================================
    // AUTO GAP — съесть золотое яблоко при низком HP
    // ============================================================
    private void tickAutoGap(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_GAP)) return;
        if (gapCooldown > 0) return;

        com.example.cosmetics.feature.FeatureSettings fs =
                CosmeticsState.get().settings(FeatureType.AUTO_GAP);
        float threshold = fs.count * 2F;

        if (player.getHealth() > threshold) return;

        // Ищем золотое яблоко (обычное или зачарованное)
        NonNullList<ItemStack> inv = player.inventory.items;
        int gapSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.GOLDEN_APPLE || inv.get(i).getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                gapSlot = i; break;
            }
        }
        if (gapSlot == -1) return;

        int prev = player.inventory.selected;
        int hotbarSlot = gapSlot < 9 ? gapSlot : 0;

        if (gapSlot >= 9) {
            ItemStack tmp = inv.get(hotbarSlot).copy();
            inv.set(hotbarSlot, inv.get(gapSlot).copy());
            inv.set(gapSlot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }

        player.inventory.selected = hotbarSlot;
        mc.getConnection().send(new CHeldItemChangePacket(hotbarSlot));
        // Начинаем есть (удерживаем ПКМ — один тик достаточно для инициации)
        mc.gameMode.useItem(player, mc.level, Hand.MAIN_HAND);

        player.inventory.selected = prev;
        mc.getConnection().send(new CHeldItemChangePacket(prev));

        gapCooldown = 40; // кулдаун 2 секунды
    }

    public static boolean shouldSuppressFireOverlay() {
        return CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY);
    }
}
