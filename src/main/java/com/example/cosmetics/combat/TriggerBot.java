package com.example.cosmetics.combat;

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
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Random;

/**
 * TriggerBot — автоматически атакует когда прицел наведён на цель.
 *
 * Настройки:
 *   triggerDelay       — задержка перед кликом (мс), чтобы не выглядело как бот
 *   triggerDelayRand   — рандом ±мс к задержке
 *   triggerFov         — угол прицела (насколько точно надо смотреть на цель)
 *   triggerTargetPlayers / Hostile / Passive — фильтр
 *   triggerNeedWeapon  — только если держишь оружие
 *   triggerOnlyCharged — только при полном заряде атаки
 *   triggerAutoRelease — удерживать ЛКМ или делать одиночные клики
 *   triggerCancelOnMove— отменять если двигаешься
 */
public final class TriggerBot {

    private static final TriggerBot INSTANCE = new TriggerBot();
    public static TriggerBot get() { return INSTANCE; }

    private static final Random RNG = new Random();

    private long pendingClickAt = -1;   // время запланированного клика (мс)
    private boolean wasLooking  = false; // смотрели ли на цель в прошлый тик

    private TriggerBot() {}

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!CosmeticsState.get().isOn(FeatureType.TRIGGER_BOT)) {
            pendingClickAt = -1;
            wasLooking = false;
            return;
        }

        ClientPlayerEntity player = mc.player;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.TRIGGER_BOT);

        // Нужно оружие в руке?
        if (fs.triggerNeedWeapon && !isHoldingWeapon(player)) {
            pendingClickAt = -1;
            return;
        }

        // Отменять при движении?
        if (fs.triggerCancelOnMove) {
            boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                          || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
            if (moving) { pendingClickAt = -1; return; }
        }

        // Ищем что под прицелом
        Entity looked = getLookedEntity(mc, player, fs);
        boolean lookingNow = (looked != null);

        if (lookingNow && !wasLooking) {
            // Только что навели — планируем клик с задержкой
            long delay = Math.max(0, fs.triggerDelay)
                       + (fs.triggerDelayRand > 0 ? (long)(RNG.nextFloat() * fs.triggerDelayRand) : 0);
            pendingClickAt = System.currentTimeMillis() + delay;
        } else if (!lookingNow) {
            // Увели прицел — сбрасываем
            pendingClickAt = -1;
        }

        wasLooking = lookingNow;

        // Пора кликать?
        if (pendingClickAt > 0 && System.currentTimeMillis() >= pendingClickAt) {
            // Только при полном заряде?
            if (fs.triggerOnlyCharged && player.getAttackStrengthScale(0F) < 1.0F) return;

            if (fs.triggerAutoRelease) {
                // Одиночный клик
                KeyBinding.click(mc.options.keyAttack.getKey());
            } else {
                // Симулируем удержание ЛКМ
                KeyBinding.set(mc.options.keyAttack.getKey(), true);
            }

            // После клика — новая задержка перед следующим
            long delay = Math.max(0, fs.triggerDelay)
                       + (fs.triggerDelayRand > 0 ? (long)(RNG.nextFloat() * fs.triggerDelayRand) : 0);
            pendingClickAt = System.currentTimeMillis() + delay + 50; // +50мс минимум между кликами
        } else if (!lookingNow && !fs.triggerAutoRelease) {
            KeyBinding.set(mc.options.keyAttack.getKey(), false);
        }
    }

    /** Возвращает сущность под прицелом, если она подходит под фильтр */
    private static Entity getLookedEntity(Minecraft mc, ClientPlayerEntity player, FeatureSettings fs) {
        // Стандартный раytrace Minecraft
        RayTraceResult hit = mc.hitResult;
        if (hit == null || hit.getType() != RayTraceResult.Type.ENTITY) return null;

        EntityRayTraceResult eHit = (EntityRayTraceResult) hit;
        Entity e = eHit.getEntity();
        if (!(e instanceof LivingEntity)) return null;
        if (!e.isAlive()) return null;

        // FOV фильтр — угол между взглядом и направлением на цель
        if (fs.triggerFov < 180F) {
            Vector3d look = player.getLookAngle();
            Vector3d toEnt = e.position().add(0, e.getBbHeight() * 0.5, 0)
                              .subtract(player.getEyePosition(1F)).normalize();
            double dot = look.dot(toEnt);
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            if (angle > fs.triggerFov / 2F) return null;
        }

        // Фильтр по типу
        if (e instanceof PlayerEntity  && !fs.triggerTargetPlayers) return null;
        if (e instanceof IMob          && !fs.triggerTargetHostile)  return null;
        if (e instanceof AnimalEntity  && !fs.triggerTargetPassive)  return null;
        if (!(e instanceof PlayerEntity) && !(e instanceof IMob) && !(e instanceof AnimalEntity)) return null;

        return e;
    }

    private static boolean isHoldingWeapon(ClientPlayerEntity player) {
        net.minecraft.item.Item item = player.getMainHandItem().getItem();
        return item instanceof net.minecraft.item.SwordItem
            || item instanceof net.minecraft.item.AxeItem
            || item instanceof net.minecraft.item.TridentItem
            || item instanceof net.minecraft.item.PickaxeItem
            || item instanceof net.minecraft.item.ShovelItem;
    }
}
