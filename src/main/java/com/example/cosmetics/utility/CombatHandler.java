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
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.TridentItem;
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

public final class CombatHandler {

    private static final CombatHandler INSTANCE = new CombatHandler();
    public static CombatHandler get() { return INSTANCE; }

    private static final Random RNG = new Random();

    // ---- Kill Aura state ----------------------------------------------------
    private int  auraAttackCooldown = 0;
    private boolean critJumpPending = false;
    private int  critJumpTick       = -999;
    private long auraNextAttackMs   = 0; // для ms-точной задержки

    // ---- Auto Clicker -------------------------------------------------------
    private int clickerTick     = 0;
    private int clickerInterval = 4;

    // ---- Auto Pot / Auto Gap ------------------------------------------------
    private int potCooldown = 0;
    private int gapCooldown = 0;

    private CombatHandler() {}

    // =========================================================================
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientPlayerEntity player = mc.player;

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
    // KILL AURA
    //
    // Логика cooldown:
    //   - Получаем скорость атаки оружия (attackSpeed атрибут)
    //   - Рассчитываем количество тиков для полного заряда (20 / attackSpeed)
    //   - После каждого удара ставим свой таймер auraAttackCooldown
    //   - Бьём только когда таймер = 0 И getAttackStrengthScale >= 1.0
    //   - Так удары гарантированно идут и не застревают
    // =========================================================================
    private void tickKillAura(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) {
            auraAttackCooldown = 0;
            critJumpPending    = false;
            auraNextAttackMs   = 0;
            return;
        }

        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        float range    = Math.max(0.5F, Math.min(10F, fs.size));
        float minRange = Math.max(0F, Math.min(range - 0.1F, fs.killAuraMinRange));
        float fov      = Math.max(10F, Math.min(360F, fs.killAuraFov));

        if (auraAttackCooldown > 0) { auraAttackCooldown--; return; }

        // ---- Поиск цели -------------------------------------------------------
        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> {
                    if (!isValidKillAuraTarget(e, player, fs)) return false;
                    double dist = e.distanceTo(player);
                    if (dist < minRange) return false;
                    // FOV фильтр
                    if (fov < 360F) {
                        double dx = e.getX() - player.getX();
                        double dz = e.getZ() - player.getZ();
                        float needYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                        if (Math.abs(wrapDeg(needYaw - player.yRot)) > fov / 2F) return false;
                    }
                    // Raytrace — только при прямой видимости
                    if (fs.killAuraRaytrace) {
                        net.minecraft.util.math.vector.Vector3d eyePos =
                                player.getEyePosition(1F);
                        net.minecraft.util.math.vector.Vector3d entPos =
                                e.position().add(0, e.getBbHeight() * 0.5, 0);
                        net.minecraft.util.math.BlockRayTraceResult brt =
                                mc.level.clip(new net.minecraft.util.math.RayTraceContext(
                                        eyePos, entPos,
                                        net.minecraft.util.math.RayTraceContext.BlockMode.COLLIDER,
                                        net.minecraft.util.math.RayTraceContext.FluidMode.NONE,
                                        player));
                        if (brt.getType() == net.minecraft.util.math.RayTraceResult.Type.BLOCK)
                            return false;
                    }
                    return true;
                });

        if (candidates.isEmpty()) { critJumpPending = false; return; }

        LivingEntity target = pickTarget(candidates, player, fs.killAuraSortMode);
        if (target == null) return;

        // ---- Наведение -------------------------------------------------------
        double dx   = target.getX() - player.getX();
        double dy   = (target.getY() + target.getBbHeight() * 0.5)
                    - (player.getY() + player.getEyeHeight());
        double dz   = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float wantYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = Math.max(-90F, Math.min(90F,
                          (float)(-Math.toDegrees(Math.atan2(dy, dist)))));

        switch (Math.floorMod(fs.killAuraRotMode, 3)) {
            case 0: // BODY_TRACK
                player.yBodyRot = wantYaw;
                player.yHeadRot = wantYaw;
                break;
            case 1: // FULL_LOCK
                player.yRot     = wantYaw;
                player.xRot     = wantPitch;
                player.yBodyRot = wantYaw;
                player.yHeadRot = wantYaw;
                break;
            // case 2: SILENT — ничего
        }

        // ---- Ждём полного заряда --------------------------------------------
        float charge = player.getAttackStrengthScale(0F);
        if (charge < 1.0F) return;

        // ---- Кастомная задержка атаки ----------------------------------------
        if (fs.killAuraAttackDelay > 0 || fs.killAuraAttackDelayRand > 0) {
            long now = System.currentTimeMillis();
            if (now < auraNextAttackMs) return;
            long delay = fs.killAuraAttackDelay
                    + (long)(STRAFE_RNG.nextFloat() * fs.killAuraAttackDelayRand);
            auraNextAttackMs = now + delay;
        }

        // ---- Auto Crit -------------------------------------------------------
        if (fs.killAuraAutoCrit) {
            if (!critJumpPending) {
                boolean canCrit = player.isOnGround()
                        && !player.isCrouching()
                        && !player.isInWater()
                        && !player.onClimbable()
                        && !player.isSprinting();
                if (canCrit) {
                    int tick = (int)(mc.level.getGameTime() & 0x7FFFFFFF);
                    if (tick != critJumpTick) {
                        player.setDeltaMovement(
                                player.getDeltaMovement().x, 0.11,
                                player.getDeltaMovement().z);
                        critJumpTick    = tick;
                        critJumpPending = true;
                        auraAttackCooldown = 1;
                        return;
                    }
                }
            }
            critJumpPending = false;
        }

        // ---- Атака -----------------------------------------------------------
        mc.gameMode.attack(player, target);
        if (fs.killAuraSwing) player.swing(Hand.MAIN_HAND);

        double atkSpeed = player.getAttributeValue(
                net.minecraft.entity.ai.attributes.Attributes.ATTACK_SPEED);
        int cooldownTicks = Math.max(1, (int) Math.ceil(20.0 / atkSpeed));
        auraAttackCooldown = cooldownTicks;
    }

    // ---- Фильтр целей --------------------------------------------------------
    private static boolean isValidKillAuraTarget(LivingEntity e,
                                                  ClientPlayerEntity player,
                                                  FeatureSettings fs) {
        if (e == player)  return false;
        if (!e.isAlive()) return false;

        boolean isPlayer  = e instanceof PlayerEntity;
        boolean isHostile = e instanceof IMob;
        boolean isPassive = (e instanceof AnimalEntity) && !isHostile;

        if (isPlayer  && !fs.killAuraTargetPlayers) return false;
        if (isHostile && !fs.killAuraTargetHostile) return false;
        if (isPassive && !fs.killAuraTargetPassive) return false;
        if (!isPlayer && !isHostile && !isPassive)  return false;

        // Anti Bot
        if (fs.killAuraAntiBot && isPlayer) {
            if (e.isInvisible()) return false;
            if (e.getName().getString().isEmpty()) return false;
            if (e.getDeltaMovement().lengthSqr() == 0 && !e.hasCustomName()) return false;
        }
        return true;
    }

    // ---- Выбор цели ----------------------------------------------------------
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
    // STANDALONE CRIT
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
    // SMOOTH AIM
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
            double dx   = e.getX() - player.getX();
            double dy   = (e.getY() + e.getBbHeight() * 0.5) - (player.getY() + player.getEyeHeight());
            double dz   = e.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            float needYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float needPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
            if (Math.abs(wrapDeg(needYaw - player.yRot)) > fov / 2F) continue;
            if (Math.abs(needPitch - player.xRot)        > fov / 2F) continue;
            if (dist < bestDist) { bestDist = dist; target = (PlayerEntity) e; }
        }
        if (target == null) return;

        double dx   = target.getX() - player.getX();
        double dy   = (target.getY() + target.getBbHeight() * 0.5) - (player.getY() + player.getEyeHeight());
        double dz   = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float tYaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        float t = speed / 20F;
        player.yRot = lerpAngle(player.yRot, tYaw, t);
        player.xRot = Math.max(-90F, Math.min(90F, lerp(player.xRot, tPitch, t)));
    }

    // =========================================================================
    // STRAFE
    // Режимы:
    //   0 = Circle  — плавное вращение вокруг цели
    //   1 = Side    — шаг в сторону на каждый тик
    //   2 = Zigzag  — чередует стороны, сложнее предсказать
    // =========================================================================
    private double strafeAngle    = 0;
    private int    strafeZigzagDir = 1;
    private int    strafeZigzagTimer = 0;
    private static final Random STRAFE_RNG = new Random();

    private void tickStrafe(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.STRAFE)) return;
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) return;
        if (mc.screen != null) return;

        FeatureSettings fs   = CosmeticsState.get().settings(FeatureType.STRAFE);
        FeatureSettings kaFs = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        float speed  = Math.max(0.5F, Math.min(5F, fs.speed));
        float range  = Math.max(0.5F, Math.min(10F, kaFs.size));

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range + 2.0),
                e -> isValidKillAuraTarget(e, player, kaFs));
        if (candidates.isEmpty()) { strafeAngle = 0; return; }

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (target == null) return;

        // Если нужно только в бою (есть цель в зоне атаки)
        if (fs.strafeOnlyInCombat && target.distanceTo(player) > range) return;

        int mode = Math.floorMod(fs.strafeMode, 3);
        int dir  = Math.floorMod(fs.strafeDirection, 3);
        // dir: 0=Left(-1) 1=Right(+1) 2=Random
        double dirSign;
        if (dir == 2) {
            // Рандом при смене цели — один раз выбираем
            dirSign = (STRAFE_RNG.nextBoolean() ? 1 : -1);
        } else {
            dirSign = (dir == 1) ? 1.0 : -1.0;
        }

        switch (mode) {
            case 0: { // ---- CIRCLE — плавно вращаемся вокруг цели
                // Скорость: добавляем небольшой рандом ±5% чтобы не выглядеть ботом
                double noise  = 1.0 + (STRAFE_RNG.nextDouble() - 0.5) * 0.10;
                strafeAngle  += dirSign * speed * 0.045 * noise;
                double radius = Math.max(1.8, Math.min(5.0, fs.strafeRadius));
                double tx = target.getX() + Math.cos(strafeAngle) * radius;
                double tz = target.getZ() + Math.sin(strafeAngle) * radius;
                double dx = tx - player.getX();
                double dz = tz - player.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.05) {
                    double baseSpd = 0.26 + (speed - 1) * 0.02;
                    double spdNoise = 1.0 + (STRAFE_RNG.nextDouble() - 0.5) * 0.08;
                    double mv = Math.min(len, baseSpd * spdNoise);
                    player.setDeltaMovement(
                            (dx / len) * mv,
                            player.getDeltaMovement().y,
                            (dz / len) * mv);
                }
                // Плавный поворот к цели при CIRCLE
                double ddx = target.getX() - player.getX();
                double ddz = target.getZ() - player.getZ();
                float wantYaw = (float)(Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0);
                player.yRot = lerpAngle(player.yRot, wantYaw, 0.15F);
                break;
            }
            case 1: { // ---- SIDE — шаг строго в сторону от направления к цели
                double ddx = target.getX() - player.getX();
                double ddz = target.getZ() - player.getZ();
                double len  = Math.sqrt(ddx * ddx + ddz * ddz);
                if (len < 0.01) break;
                // Перпендикуляр
                double sideX =  dirSign * ddz / len;
                double sideZ = -dirSign * ddx / len;
                double baseSpd = 0.25 + (speed - 1) * 0.02;
                double spdNoise = 1.0 + (STRAFE_RNG.nextDouble() - 0.5) * 0.06;
                // Jitter — небольшое смещение вперёд/назад против предиктора
                double jitterFwd = 0;
                if (fs.strafeJitter) jitterFwd = (STRAFE_RNG.nextDouble() - 0.5) * 0.07;
                double mvX = sideX * baseSpd * spdNoise + (ddx / len) * jitterFwd;
                double mvZ = sideZ * baseSpd * spdNoise + (ddz / len) * jitterFwd;
                player.setDeltaMovement(mvX, player.getDeltaMovement().y, mvZ);
                float wantYaw = (float)(Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0);
                player.yRot = lerpAngle(player.yRot, wantYaw, 0.2F);
                break;
            }
            case 2: { // ---- ZIGZAG — чередует Left/Right каждые N тиков
                strafeZigzagTimer--;
                if (strafeZigzagTimer <= 0) {
                    strafeZigzagDir = -strafeZigzagDir;
                    // Случайный интервал 6-14 тиков — сложнее предсказать
                    strafeZigzagTimer = 6 + STRAFE_RNG.nextInt(9);
                }
                double ddx = target.getX() - player.getX();
                double ddz = target.getZ() - player.getZ();
                double len  = Math.sqrt(ddx * ddx + ddz * ddz);
                if (len < 0.01) break;
                double sideX =  strafeZigzagDir * ddz / len;
                double sideZ = -strafeZigzagDir * ddx / len;
                double baseSpd = 0.24 + (speed - 1) * 0.02;
                double spdNoise = 1.0 + (STRAFE_RNG.nextDouble() - 0.5) * 0.08;
                double jitterFwd = 0;
                if (fs.strafeJitter) jitterFwd = (STRAFE_RNG.nextDouble() - 0.5) * 0.08;
                player.setDeltaMovement(
                        sideX * baseSpd * spdNoise + (ddx / len) * jitterFwd,
                        player.getDeltaMovement().y,
                        sideZ * baseSpd * spdNoise + (ddz / len) * jitterFwd);
                float wantYaw = (float)(Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0);
                player.yRot = lerpAngle(player.yRot, wantYaw, 0.2F);
                break;
            }
        }
        // Sprint toggle
        if (fs.strafeSprint && !player.isSprinting()) player.setSprinting(true);
    }

    // =========================================================================
    // AUTO POT
    // =========================================================================
    private void tickAutoPot(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.AUTO_POT)) return;
        if (potCooldown > 0) return;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.AUTO_POT);
        if (player.getHealth() >= player.getMaxHealth() - 0.5F) return;
        if (player.getHealth() > fs.count * 2F) return;

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

        int prev = player.inventory.selected;
        int slot = potSlot < 9 ? potSlot : 0;
        if (potSlot >= 9) {
            ItemStack tmp = inv.get(slot).copy();
            inv.set(slot, inv.get(potSlot).copy());
            inv.set(potSlot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }
        float origPitch = player.xRot;
        player.xRot = 90F;
        player.inventory.selected = slot;
        mc.getConnection().send(new CHeldItemChangePacket(slot));
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
        if (player.getHealth() > fs.count * 2F) return;

        NonNullList<ItemStack> inv = player.inventory.items;
        int slot = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).getItem() == Items.GOLDEN_APPLE
             || inv.get(i).getItem() == Items.ENCHANTED_GOLDEN_APPLE) { slot = i; break; }
        }
        if (slot == -1) return;

        int prev = player.inventory.selected;
        int hSlot = slot < 9 ? slot : 0;
        if (slot >= 9) {
            ItemStack tmp = inv.get(hSlot).copy();
            inv.set(hSlot, inv.get(slot).copy());
            inv.set(slot, tmp.isEmpty() ? ItemStack.EMPTY : tmp);
        }
        player.inventory.selected = hSlot;
        mc.getConnection().send(new CHeldItemChangePacket(hSlot));
        mc.gameMode.useItem(player, mc.level, Hand.MAIN_HAND);
        player.inventory.selected = prev;
        mc.getConnection().send(new CHeldItemChangePacket(prev));
        gapCooldown = 40;
    }

    // ---- helpers -------------------------------------------------------------
    private static float lerpAngle(float from, float to, float t) {
        return from + wrapDeg(to - from) * t;
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float wrapDeg(float d) {
        d %= 360F;
        if (d >= 180F) d -= 360F;
        if (d < -180F) d += 360F;
        return d;
    }

    public static boolean shouldSuppressFireOverlay() {
        return CosmeticsState.get().isOn(FeatureType.NO_FIRE_OVERLAY);
    }
}
