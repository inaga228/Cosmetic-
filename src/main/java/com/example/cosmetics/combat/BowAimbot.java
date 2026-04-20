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
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.vector.Vector3d;

/**
 * BowAimbot — автоприцеливание для лука/арбалета/трезубца.
 *
 * Учитывает:
 *  1. Гравитацию стрелы (0.05 в тик² по умолчанию)
 *  2. Скорость стрелы зависит от заряда лука
 *  3. Опережение (leading) — куда переместится цель за время полёта стрелы
 *  4. Сопротивление воздуха стрелы (0.99 per tick)
 *  5. Плавное наведение с настраиваемой скоростью
 *
 * Настройки:
 *   bowAimFov           — угол поиска цели (5–180°)
 *   bowAimSpeed         — скорость поворота прицела (1–20)
 *   bowAimLead          — множитель опережения (0=нет, 1=полное)
 *   bowAimGravity       — множитель гравитации стрелы (точная настройка)
 *   bowAimTargetPlayers / Hostile — фильтр
 *   bowAimOnlyCharged   — стрелять только при полном заряде
 *   bowAimPredictSteps  — кол-во шагов симуляции для нахождения угла (точность)
 *   bowAimSilent        — silent aim (поворачивает только пакеты, не голову — сложно в 1.16.5, используем body)
 */
public final class BowAimbot {

    private static final BowAimbot INSTANCE = new BowAimbot();
    public static BowAimbot get() { return INSTANCE; }

    // Физика стрелы Minecraft
    private static final float ARROW_GRAVITY    = 0.05F;   // блоков/тик²
    private static final float ARROW_DRAG       = 0.99F;   // множитель скорости/тик
    private static final float ARROW_SPEED_FULL = 3.0F;    // блоков/тик при 100% заряде лука

    private BowAimbot() {}

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!CosmeticsState.get().isOn(FeatureType.BOW_AIMBOT)) return;

        ClientPlayerEntity player = mc.player;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.BOW_AIMBOT);

        // Проверяем что держим лук / арбалет / трезубец
        if (!isHoldingRangedWeapon(player)) return;

        // Заряд лука
        int using = player.getTicksUsingItem();
        float charge = BowItem.getPowerForTime(using);  // 0..1
        if (fs.bowAimOnlyCharged && charge < 0.9F) return;

        float arrowSpeed = Math.max(0.1F, ARROW_SPEED_FULL * Math.max(0.05F, charge));

        // Поиск ближайшей цели в FOV
        LivingEntity target = findTarget(mc, player, fs);
        if (target == null) return;

        // Позиция глаз игрока
        Vector3d eyePos = player.getEyePosition(1F);

        // Предсказываем где будет цель через time_of_flight тиков
        // Сначала оцениваем время полёта до текущей позиции, потом итерируем
        Vector3d targetPos = predictTargetPosition(target, eyePos, arrowSpeed, fs);

        // Вычисляем нужный угол с учётом гравитации
        float[] angles = solveAimAngles(eyePos, targetPos, arrowSpeed, fs);
        if (angles == null) return; // цель вне досягаемости

        float wantYaw   = angles[0];
        float wantPitch = angles[1];

        // Плавное наведение
        float t = Math.max(0.01F, Math.min(1F, fs.bowAimSpeed / 20F));
        player.yRot = lerpAngle(player.yRot, wantYaw, t);
        player.xRot = Math.max(-90F, Math.min(90F, lerp(player.xRot, wantPitch, t)));
        player.yHeadRot = player.yRot;
        player.yBodyRot = player.yRot;
    }

    /**
     * Предсказывает позицию цели через время полёта стрелы.
     * Итерируем: сначала считаем время до текущей позиции,
     * потом куда переместится цель за это время, повторяем 3 раза для точности.
     */
    private static Vector3d predictTargetPosition(LivingEntity target, Vector3d eyePos,
                                                   float arrowSpeed, FeatureSettings fs) {
        float lead = Math.max(0F, Math.min(2F, fs.bowAimLead));

        // Начальная позиция цели — центр хитбокса
        Vector3d pos = target.position().add(0, target.getBbHeight() * 0.5, 0);

        if (lead < 0.01F) return pos; // опережение выключено

        // Скорость движения цели (блоков/тик)
        Vector3d vel = new Vector3d(
                target.getX() - target.xo,
                target.getY() - target.yo,
                target.getZ() - target.zo
        );

        // Итерируем для уточнения
        int iters = Math.max(1, Math.min(8, fs.bowAimPredictSteps));
        for (int i = 0; i < iters; i++) {
            double dist = pos.distanceTo(eyePos);
            // Симулируем время полёта стрелы (с учётом сопротивления воздуха)
            float tof = estimateTimeOfFlight(dist, arrowSpeed);
            // Новая предсказанная позиция
            pos = target.position()
                    .add(0, target.getBbHeight() * 0.5, 0)
                    .add(vel.scale(tof * lead));
        }

        return pos;
    }

    /**
     * Оценивает время полёта стрелы с учётом замедления воздуха (drag 0.99/тик).
     * dist = arrowSpeed * (1 - drag^t) / (1 - drag)  → решаем численно.
     */
    private static float estimateTimeOfFlight(double dist, float speed) {
        double covered = 0;
        float v = speed;
        int tick = 0;
        while (covered < dist && tick < 200) {
            covered += v;
            v *= ARROW_DRAG;
            tick++;
        }
        return tick;
    }

    /**
     * Решает углы прицеливания с учётом гравитации стрелы.
     * Использует баллистическое уравнение: для заданной горизонтальной дистанции
     * и вертикального смещения находит угол бросания.
     *
     * Формула (без drag, приближение):
     *   pitch = atan(-vy / vxz)
     * где vy нужна чтобы попасть в цель с учётом gravity*t²/2.
     *
     * С drag — итеративная симуляция.
     */
    private static float[] solveAimAngles(Vector3d eyePos, Vector3d targetPos,
                                           float arrowSpeed, FeatureSettings fs) {
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);

        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float gravity = ARROW_GRAVITY * Math.max(0.1F, Math.min(3F, fs.bowAimGravity));

        // Итеративный подбор pitch угла
        // Бинарный поиск по pitch от -90 до 90
        float pitchLow  = -90F;
        float pitchHigh = 90F;
        float bestPitch = 0F;
        double bestErr  = Double.MAX_VALUE;

        for (int i = 0; i < 64; i++) {
            float mid = (pitchLow + pitchHigh) / 2F;
            double[] simResult = simulateArrow(eyePos, yaw, mid, arrowSpeed, gravity, 300);
            double simHoriz = simResult[0];
            double simVert  = simResult[1];

            // Ошибка: хотим попасть в targetPos
            double horizErr = simHoriz - horizDist;

            if (horizErr < 0) {
                pitchLow  = mid; // стреляем ниже → стрела летит дальше по горизонтали
            } else {
                pitchHigh = mid;
            }

            // Вертикальная точность
            double vertErr = Math.abs(simVert - dy);
            if (vertErr < bestErr) {
                bestErr  = vertErr;
                bestPitch = mid;
            }
        }

        // Если ошибка слишком большая — цель вне досягаемости
        if (bestErr > 3.0) return null;

        return new float[]{ yaw, bestPitch };
    }

    /**
     * Симулирует полёт стрелы, возвращает [horizDist, vertDist] когда горизонталь
     * достигнет maxTicks или стрела начнёт лететь назад.
     */
    private static double[] simulateArrow(Vector3d start, float yaw, float pitch,
                                           float speed, float gravity, int maxTicks) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad   = Math.toRadians(yaw + 90); // +90 из-за Minecraft-системы координат

        double vx = speed * Math.cos(pitchRad) * Math.cos(yawRad);
        double vy = -speed * Math.sin(pitchRad);
        double vz = speed * Math.cos(pitchRad) * Math.sin(yawRad);

        double x = 0, y = 0, z = 0;
        double prevHoriz = 0;

        for (int t = 0; t < maxTicks; t++) {
            x += vx; y += vy; z += vz;
            vy -= gravity;
            vx *= ARROW_DRAG;
            vy *= ARROW_DRAG;
            vz *= ARROW_DRAG;

            double horiz = Math.sqrt(x * x + z * z);
            if (horiz < prevHoriz) break; // стрела начала лететь назад
            prevHoriz = horiz;
        }

        return new double[]{ prevHoriz, y };
    }

    private static LivingEntity findTarget(Minecraft mc, ClientPlayerEntity player, FeatureSettings fs) {
        float fov = Math.max(5F, Math.min(180F, fs.bowAimFov));
        Vector3d look = player.getLookAngle();
        Vector3d eye  = player.getEyePosition(1F);

        LivingEntity best = null;
        double bestScore  = Double.MAX_VALUE;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == player || !e.isAlive()) continue;

            if (e instanceof PlayerEntity  && !fs.bowAimTargetPlayers) continue;
            if (e instanceof IMob          && !fs.bowAimTargetHostile)  continue;
            if (!(e instanceof PlayerEntity) && !(e instanceof IMob))   continue;

            Vector3d toEnt = e.position().add(0, e.getBbHeight() * 0.5, 0)
                              .subtract(eye).normalize();
            double dot   = look.dot(toEnt);
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            if (angle > fov / 2F) continue;

            // Сортируем по углу (ближайший к прицелу)
            if (angle < bestScore) { bestScore = angle; best = (LivingEntity) e; }
        }
        return best;
    }

    private static boolean isHoldingRangedWeapon(ClientPlayerEntity player) {
        net.minecraft.item.Item main = player.getMainHandItem().getItem();
        net.minecraft.item.Item off  = player.getOffhandItem().getItem();
        return main instanceof BowItem || main instanceof CrossbowItem || main instanceof TridentItem
            || off  instanceof BowItem || off  instanceof CrossbowItem;
    }

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
}
