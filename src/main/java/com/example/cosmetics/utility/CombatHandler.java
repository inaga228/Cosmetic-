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
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public final class CombatHandler {

    private static final CombatHandler INSTANCE = new CombatHandler();
    public static CombatHandler get() { return INSTANCE; }

    private static final Random RNG = new Random();

    // ---- Kill Aura state ----------------------------------------------------
    private long lastAttackMs           = 0;
    private long lastAttackTick         = 0;
    private long auraTicks              = 0;
    private long lastAttackMsForRotation = 0;

    // Silent rotation
    private float   targetYaw, targetPitch;
    private float   originalYaw, originalPitch;
    private float   clientYaw, clientPitch;
    private boolean isRotating = false;

    // Strafe
    private double circleAngle      = 0;
    private int    strafeSideDir    = 1; // фиксированное направление для Side/Circle
    private int    strafeZigzagDir  = 1;
    private int    strafeZigzagTimer = 0;

    // Switch target queue
    private final Queue<LivingEntity> switchQueue = new ConcurrentLinkedQueue<>();

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

        auraTicks++;
        clientYaw   = player.yRot;
        clientPitch = player.xRot;

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
    // =========================================================================
    private void tickKillAura(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) {
            if (isRotating) restoreRotation(mc, player);
            return;
        }

        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.KILL_AURA);

        // Паузы
        if (mc.screen != null) return;
        if (fs.killAuraNoAttackOnUse && player.isUsingItem()) return;

        // Поиск целей
        List<LivingEntity> targets = getValidTargets(mc, player, fs);
        if (targets.isEmpty()) {
            if (isRotating) restoreRotation(mc, player);
            return;
        }

        // Выбор первичной цели
        LivingEntity primary = selectPrimaryTarget(targets, fs);
        if (primary == null) return;

        // Проверка задержки
        boolean canAttackNow;
        if (fs.killAuraDelayMode == 1) {
            canAttackNow = player.getAttackStrengthScale(-fs.killAuraExtraTicks) >= 1.0F;
        } else {
            int interval = Math.max(1, fs.killAuraTickInterval);
            canAttackNow = (auraTicks - lastAttackTick) >= interval;
        }

        long now = System.currentTimeMillis();
        int delayMs = calculateDelayMs(fs);

        if (!canAttackNow || (now - lastAttackMs) < delayMs) {
            handlePostAttackRotation(mc, player);
            return;
        }

        // Рандом шанс удара
        if (RNG.nextInt(100) >= fs.killAuraHitChance) return;

        // Подготовка поворота
        prepareRotation(mc, player, primary, fs);

        // Auto Crit (микро-прыжок)
        if (fs.killAuraAutoCrit && canCrit(player)) {
            player.setDeltaMovement(
                    player.getDeltaMovement().x, 0.11,
                    player.getDeltaMovement().z);
        }

        // Атака
        performAttack(mc, player, targets, primary, fs);

        lastAttackMs            = now;
        lastAttackTick          = auraTicks;
        lastAttackMsForRotation = now;
    }

    // ---- Поиск целей --------------------------------------------------------
    private static List<LivingEntity> getValidTargets(Minecraft mc,
                                                       ClientPlayerEntity player,
                                                       FeatureSettings fs) {
        float range    = Math.max(0.5F, Math.min(10F, fs.size));
        float minRange = Math.max(0F, fs.killAuraMinRange);
        float fov      = Math.max(10F, Math.min(360F, fs.killAuraFov));
        float vFov     = Math.max(10F, Math.min(360F, fs.killAuraVerticalFov));

        Vector3d eyePos = player.getEyePosition(1F);
        AxisAlignedBB aabb = player.getBoundingBox().inflate(range);

        return mc.level.getEntitiesOfClass(LivingEntity.class, aabb, e -> {
            if (e == player || !e.isAlive() || e.getHealth() <= 0) return false;

            double dist = e.distanceTo(player);
            if (dist > range || dist < minRange) return false;

            boolean isPlayer  = e instanceof PlayerEntity;
            boolean isHostile = e instanceof IMob;
            boolean isPassive = (e instanceof AnimalEntity) && !isHostile;

            if (isPlayer  && !fs.killAuraTargetPlayers) return false;
            if (isHostile && !fs.killAuraTargetHostile) return false;
            if (isPassive && !fs.killAuraTargetPassive) return false;
            if (!isPlayer && !isHostile && !isPassive)  return false;

            if (!fs.killAuraAttackInvisible && e.isInvisible()) return false;
            if (e.getHealth() > fs.killAuraMaxHealthTarget) return false;

            // AntiBot
            if (fs.killAuraAntiBot && isPlayer) {
                if (e.isInvisible()) return false;
                if (e.getName().getString().isEmpty()) return false;
                if (e.getDeltaMovement().lengthSqr() == 0 && !e.hasCustomName()) return false;
            }

            // Raytrace
            if (fs.killAuraRaytrace) {
                Vector3d entPos = e.position().add(0, e.getBbHeight() * 0.5, 0);
                RayTraceContext ctx = new RayTraceContext(eyePos, entPos,
                        RayTraceContext.BlockMode.COLLIDER,
                        RayTraceContext.FluidMode.NONE, player);
                RayTraceResult result = mc.level.clip(ctx);
                if (result.getType() == RayTraceResult.Type.BLOCK) return false;
            }

            // Горизонтальный FOV
            if (fov < 360F) {
                double dx = e.getX() - player.getX();
                double dz = e.getZ() - player.getZ();
                float needYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                if (Math.abs(wrapDeg(needYaw - player.yRot)) > fov / 2F) return false;
            }

            // Вертикальный FOV (исправленный)
            if (vFov < 360F) {
                double dx = e.getX() - player.getX();
                double dy = (e.getY() + e.getBbHeight() * fs.killAuraAimHeight)
                          - (player.getY() + player.getEyeHeight());
                double dz = e.getZ() - player.getZ();
                double distXZ = Math.sqrt(dx * dx + dz * dz);
                float needPitch = (float)(-Math.toDegrees(Math.atan2(dy, distXZ)));
                if (Math.abs(needPitch - player.xRot) > vFov / 2F) return false;
            }

            return true;
        }).stream()
          .sorted(getComparator(player, fs))
          .limit(fs.killAuraAttackAll ? 20 : (fs.killAuraAttackMode == 2 ? 20 : 1))
          .collect(Collectors.toList());
    }

    private static Comparator<LivingEntity> getComparator(ClientPlayerEntity player,
                                                            FeatureSettings fs) {
        switch (Math.floorMod(fs.killAuraSortMode, 4)) {
            case 1: return Comparator.comparingDouble(LivingEntity::getHealth);
            case 2: return Comparator.comparingDouble(e -> -e.getHealth());
            case 3: return Comparator.comparingDouble(e -> {
                Vector3d eye = player.getEyePosition(1F);
                Vector3d pos = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
                double dot = player.getLookAngle().dot(pos.normalize());
                return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
            });
            default: return Comparator.comparingDouble(e -> e.distanceToSqr(player));
        }
    }

    private LivingEntity selectPrimaryTarget(List<LivingEntity> targets, FeatureSettings fs) {
        if (targets.isEmpty()) return null;
        if (fs.killAuraAttackMode == 1) { // SWITCH
            if (switchQueue.isEmpty()) switchQueue.addAll(targets);
            LivingEntity next = switchQueue.poll();
            if (next == null || !targets.contains(next)) return targets.get(0);
            return next;
        }
        return targets.get(0);
    }

    // ---- Поворот ------------------------------------------------------------
    private void prepareRotation(Minecraft mc, ClientPlayerEntity player,
                                  LivingEntity target, FeatureSettings fs) {
        if (fs.killAuraRotMode == 0) return;

        double aimH = MathHelper.clamp(fs.killAuraAimHeight, 0.0F, 1.0F);
        Vector3d targetPos = target.position().add(0, target.getBbHeight() * aimH, 0);
        Vector3d delta = targetPos.subtract(player.getEyePosition(1F));

        float yaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y,
                Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        pitch = MathHelper.clamp(pitch, -90F, 90F);

        // Шум углов — натуральное движение мыши
        if (fs.killAuraRandomizeAngles) {
            yaw   += (RNG.nextFloat() - 0.5F) * fs.killAuraAngleNoise;
            pitch += (RNG.nextFloat() - 0.5F) * fs.killAuraAngleNoise;
        }

        targetYaw   = yaw;
        targetPitch = pitch;

        switch (Math.floorMod(fs.killAuraRotMode, 4)) {
            case 1: // NORMAL — полный поворот камеры
                player.yRot     = targetYaw;
                player.xRot     = targetPitch;
                player.yBodyRot = targetYaw;
                player.yHeadRot = targetYaw;
                break;
            case 2: // SILENT — пакет на сервер, потом восстанавливаем камеру
                sendRotationPacket(mc, player, targetYaw, targetPitch);
                isRotating    = true;
                originalYaw   = player.yRot;
                originalPitch = player.xRot;
                break;
            case 3: // SERVER_ONLY — только пакет, камера не трогается вообще
                sendRotationPacket(mc, player, targetYaw, targetPitch);
                isRotating    = true;
                originalYaw   = clientYaw;
                originalPitch = clientPitch;
                break;
        }
    }

    private static void sendRotationPacket(Minecraft mc, ClientPlayerEntity player,
                                            float yaw, float pitch) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new CPlayerPacket.RotationPacket(yaw, pitch, player.isOnGround()));
    }

    private void restoreRotation(Minecraft mc, ClientPlayerEntity player) {
        if (mc.getConnection() == null) return;
        sendRotationPacket(mc, player, originalYaw, originalPitch);
        player.yRot = originalYaw;
        player.xRot = originalPitch;
        isRotating  = false;
    }

    private void handlePostAttackRotation(Minecraft mc, ClientPlayerEntity player) {
        if (!isRotating) return;
        if (System.currentTimeMillis() - lastAttackMsForRotation > 50) {
            restoreRotation(mc, player);
        }
    }

    // ---- Атака --------------------------------------------------------------
    private void performAttack(Minecraft mc, ClientPlayerEntity player,
                                List<LivingEntity> targets, LivingEntity primary,
                                FeatureSettings fs) {
        if (fs.killAuraAttackAll || fs.killAuraAttackMode == 2) {
            for (LivingEntity t : targets) attackEntity(mc, player, t, fs);
        } else {
            attackEntity(mc, player, primary, fs);
        }
    }

    private static void attackEntity(Minecraft mc, ClientPlayerEntity player,
                                      LivingEntity target, FeatureSettings fs) {
        if (target == null) return;
        if (fs.killAuraSilentAttack) {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new CUseEntityPacket(target, player.isCrouching()));
                if (fs.killAuraSwing) player.swing(Hand.MAIN_HAND);
            }
        } else {
            mc.gameMode.attack(player, target);
            if (fs.killAuraSwing) player.swing(Hand.MAIN_HAND);
        }
    }

    // ---- Задержка (Гаусс) ---------------------------------------------------
    private static int calculateDelayMs(FeatureSettings fs) {
        int min  = Math.max(0, (int) fs.killAuraAttackDelay);
        int rand = Math.max(0, (int) fs.killAuraAttackDelayRand);
        int base = min + (rand > 0 ? RNG.nextInt(rand + 1) : 0);
        if (fs.killAuraRandomizeDelay) {
            double g = RNG.nextGaussian() * 8 + base;
            return (int) MathHelper.clamp(g, min, min + rand + 30);
        }
        return base;
    }

    // ---- Крит ---------------------------------------------------------------
    private static boolean canCrit(ClientPlayerEntity player) {
        return player.isOnGround()
                && !player.isCrouching()
                && !player.isInWater()
                && !player.onClimbable()
                && !player.isSprinting();
    }

    // =========================================================================
    // STANDALONE CRIT
    // =========================================================================
    private void tickStandaloneCrit(ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.CRIT)) return;
        boolean swinging = player.getAttackStrengthScale(0F) < 0.9F;
        if (swinging && canCrit(player)) {
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
    // STRAFE (Circle / Side / Zigzag)
    // =========================================================================
    private void tickStrafe(Minecraft mc, ClientPlayerEntity player) {
        if (!CosmeticsState.get().isOn(FeatureType.STRAFE)) return;
        if (!CosmeticsState.get().isOn(FeatureType.KILL_AURA)) return;
        if (mc.screen != null) return;

        FeatureSettings fs   = CosmeticsState.get().settings(FeatureType.STRAFE);
        FeatureSettings kaFs = CosmeticsState.get().settings(FeatureType.KILL_AURA);
        float range = Math.max(0.5F, Math.min(10F, kaFs.size));

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range + 2.0),
                e -> isValidKillAuraTarget(e, player, kaFs));
        if (candidates.isEmpty()) { circleAngle = 0; return; }

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (target == null) return;
        if (fs.strafeOnlyInCombat && target.distanceTo(player) > range) return;

        double speed   = Math.max(0.5, Math.min(5.0, fs.speed));
        boolean jitter = fs.strafeJitter;

        // Определяем фиксированное направление (не меняем каждый тик)
        int dir = Math.floorMod(fs.strafeDirection, 3);
        if (dir == 0) {
            // Left
            strafeSideDir = -1;
        } else if (dir == 1) {
            // Right
            strafeSideDir = 1;
        }
        // dir==2 Random — strafeSideDir остаётся как есть (меняется только в Zigzag)

        switch (Math.floorMod(fs.strafeMode, 3)) {
            case 0: { // CIRCLE — плавное кружение, угол меняется равномерно
                double noise = 1.0 + (RNG.nextDouble() - 0.5) * 0.10;
                circleAngle += strafeSideDir * speed * 0.045 * noise;
                double radius = Math.max(1.8, Math.min(5.0, fs.strafeRadius));
                double tx = target.getX() + Math.cos(circleAngle) * radius;
                double tz = target.getZ() + Math.sin(circleAngle) * radius;
                double dx = tx - player.getX();
                double dz = tz - player.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.05) {
                    double baseSpd  = 0.26 + (speed - 1) * 0.02;
                    double spdNoise = 1.0 + (RNG.nextDouble() - 0.5) * 0.08;
                    double mv = Math.min(len, baseSpd * spdNoise);
                    player.setDeltaMovement((dx / len) * mv, player.getDeltaMovement().y, (dz / len) * mv);
                }
                double ddx = target.getX() - player.getX();
                double ddz = target.getZ() - player.getZ();
                player.yRot = lerpAngle(player.yRot,
                        (float)(Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0), 0.15F);
                break;
            }
            case 1: { // SIDE
                double ddx = target.getX() - player.getX();
                double ddz = target.getZ() - player.getZ();
                double len  = Math.sqrt(ddx * ddx + ddz * ddz);
                if (len < 0.01) break;
                double sideX =  strafeSideDir * ddz / len;
                double sideZ = -strafeSideDir * ddx / len;
                double baseSpd  = 0.25 + (speed - 1) * 0.02;
                double spdNoise = 1.0 + (RNG.nextDouble() - 0.5) * 0.06;
                double jit = jitter ? (RNG.nextDouble() - 0.5) * 0.07 : 0;
                player.setDeltaMovement(
                        sideX * baseSpd * spdNoise + (ddx / len) * jit,
                        player.getDeltaMovement().y,
                        sideZ * baseSpd * spdNoise + (ddz / len) * jit);
                player.yRot = lerpAngle(player.yRot,
                        (float)(Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0), 0.2F);
                break;
            }
            case 2: { // ZIGZAG
                if (--strafeZigzagTimer <= 0) {
                    strafeZigzagDir   = -strafeZigzagDir;
                    strafeZigzagTimer = 6 + RNG.nextInt(9);
                }
                double ddx = target.getX() - player.getX();
                double ddz = target.getZ() - player.getZ();
                double len  = Math.sqrt(ddx * ddx + ddz * ddz);
                if (len < 0.01) break;
                double sideX =  strafeZigzagDir * ddz / len;
                double sideZ = -strafeZigzagDir * ddx / len;
                double baseSpd  = 0.24 + (speed - 1) * 0.02;
                double spdNoise = 1.0 + (RNG.nextDouble() - 0.5) * 0.08;
                double jit = jitter ? (RNG.nextDouble() - 0.5) * 0.08 : 0;
                player.setDeltaMovement(
                        sideX * baseSpd * spdNoise + (ddx / len) * jit,
                        player.getDeltaMovement().y,
                        sideZ * baseSpd * spdNoise + (ddz / len) * jit);
                player.yRot = lerpAngle(player.yRot,
                        (float)(Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0), 0.2F);
                break;
            }
        }
        if (fs.strafeSprint && !player.isSprinting()) player.setSprinting(true);
    }

    private static boolean isValidKillAuraTarget(LivingEntity e,
                                                   ClientPlayerEntity player,
                                                   FeatureSettings fs) {
        if (e == player || !e.isAlive()) return false;
        boolean isPlayer  = e instanceof PlayerEntity;
        boolean isHostile = e instanceof IMob;
        boolean isPassive = (e instanceof AnimalEntity) && !isHostile;
        if (isPlayer  && !fs.killAuraTargetPlayers) return false;
        if (isHostile && !fs.killAuraTargetHostile) return false;
        if (isPassive && !fs.killAuraTargetPassive) return false;
        if (!isPlayer && !isHostile && !isPassive)  return false;
        return true;
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

    // =========================================================================
    // HELPERS
    // =========================================================================
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
