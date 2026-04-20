package com.example.cosmetics.combat;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;

/**
 * HitboxExpander — расширяет хитбоксы сущностей на клиенте.
 *
 * Работает путём подмены результата raytrace: если мы НЕ попали в сущность
 * стандартным raytrace, но попали в расширенный хитбокс — подменяем crosshair-
 * цель на эту сущность. Это позволяет атаковать как будто хитбокс больше.
 *
 * Настройки:
 *   hitboxSize          — множитель размера хитбокса (1.0 = нормальный, 2.0 = двойной)
 *   hitboxX / hitboxY / hitboxZ — отдельно по осям
 *   hitboxTargetPlayers / Hostile / Passive — фильтр
 *   hitboxOnlyInCombat  — только при зажатой ЛКМ
 *   hitboxDebug         — отрисовывать расширенные хитбоксы (для проверки)
 */
public final class HitboxExpander {

    private static final HitboxExpander INSTANCE = new HitboxExpander();
    public static HitboxExpander get() { return INSTANCE; }

    /** Текущая "подменённая" цель (если расширенный raytrace нашёл кого-то) */
    private Entity overrideTarget = null;

    private HitboxExpander() {}

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            overrideTarget = null;
            return;
        }
        if (!CosmeticsState.get().isOn(FeatureType.HITBOX)) {
            overrideTarget = null;
            return;
        }

        ClientPlayerEntity player = mc.player;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.HITBOX);

        // Только при зажатой ЛКМ?
        if (fs.hitboxOnlyInCombat && !mc.options.keyAttack.isDown()) {
            overrideTarget = null;
            return;
        }

        // Стандартный результат уже нашёл сущность — расширение не нужно
        if (mc.hitResult != null && mc.hitResult.getType() == RayTraceResult.Type.ENTITY) {
            overrideTarget = null;
            return;
        }

        // Выполняем расширенный raytrace
        overrideTarget = expandedRaytrace(mc, player, fs);
    }

    /**
     * Возвращает переопределённую цель атаки (если есть).
     * Вызывается из CombatHandler когда игра пытается найти цель.
     */
    public Entity getOverrideTarget() { return overrideTarget; }

    /**
     * Расширяет хитбокс сущности и проверяет попадание луча.
     */
    private static Entity expandedRaytrace(Minecraft mc, ClientPlayerEntity player, FeatureSettings fs) {
        float reach = (float) player.getAttribute(
                net.minecraft.entity.ai.attributes.Attributes.ATTACK_REACH) != 0
                ? 3.0F : 3.0F; // в 1.16.5 нет ATTACK_REACH, используем 3.0

        Vector3d eye  = player.getEyePosition(1F);
        Vector3d look = player.getLookAngle();
        Vector3d end  = eye.add(look.scale(reach + 1.0)); // чуть дальше стандартного

        float scaleX = Math.max(0.1F, Math.min(5F, fs.hitboxX));
        float scaleY = Math.max(0.1F, Math.min(5F, fs.hitboxY));
        float scaleZ = Math.max(0.1F, Math.min(5F, fs.hitboxZ));

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        List<Entity> entities = mc.level.getEntities(player,
                player.getBoundingBox().inflate(reach + 2.0));

        for (Entity e : entities) {
            if (!(e instanceof LivingEntity) || !e.isAlive()) continue;
            if (!isHitboxTarget(e, player, fs)) continue;

            // Расширяем хитбокс
            AxisAlignedBB bb = e.getBoundingBox();
            double w = (bb.maxX - bb.minX);
            double h = (bb.maxY - bb.minY);
            double d = (bb.maxZ - bb.minZ);

            double expandX = w * (scaleX - 1.0) / 2.0;
            double expandY = h * (scaleY - 1.0) / 2.0;
            double expandZ = d * (scaleZ - 1.0) / 2.0;

            AxisAlignedBB expanded = bb.inflate(expandX, expandY, expandZ);

            // Проверяем пересечение луча с расширенным хитбоксом
            java.util.Optional<Vector3d> hit = expanded.clip(eye, end);
            if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }
        return best;
    }

    private static boolean isHitboxTarget(Entity e, ClientPlayerEntity self, FeatureSettings fs) {
        if (e == self) return false;
        if (e instanceof PlayerEntity  && !fs.hitboxTargetPlayers) return false;
        if (e instanceof IMob          && !fs.hitboxTargetHostile)  return false;
        if (!(e instanceof PlayerEntity) && !(e instanceof IMob)
                && !(e instanceof net.minecraft.entity.passive.AnimalEntity)) return false;
        if (e instanceof net.minecraft.entity.passive.AnimalEntity
                && !fs.hitboxTargetPassive) return false;
        return true;
    }
}
