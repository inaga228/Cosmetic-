package com.example.cosmetics.render;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * ESP — подсветка игроков/мобов в мире.
 *
 * Режимы (fs.espMode / style):
 *   0 = Box       — проволочная рамка вокруг хитбокса
 *   1 = Cube      — куб по центру сущности
 *   2 = Corners   — только угловые сегменты рамки (красиво)
 *   3 = Glow      — заполненный полупрозрачный ящик
 *
 * Настройки (fs поля):
 *   espShowHealth   — полоска HP над головой
 *   espShowName     — имя над головой
 *   espShowDistance — расстояние
 *   espShowLine     — линия от камеры до цели
 *   espSkeleton     — скелет (кости тела)
 *   espTargetPlayers/ espTargetHostile/ espTargetPassive — фильтр
 *   size            — maxRange (дальность)
 *   colorR/G/B      — цвет ESP
 */
public final class EspRenderer {

    private EspRenderer() {}

    public static void render(RenderWorldLastEvent event) {
        if (!CosmeticsState.get().isOn(FeatureType.ESP)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.ESP);
        float maxRange = Math.max(5F, Math.min(256F, fs.size));

        ClientPlayerEntity self = mc.player;
        MatrixStack ms = event.getMatrixStack();
        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();

        // Камера
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();

        List<LivingEntity> targets = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                self.getBoundingBox().inflate(maxRange),
                e -> isEspTarget(e, self, fs)
        );

        for (LivingEntity entity : targets) {
            float partials = event.getPartialTicks();
            double ex = entity.xo + (entity.getX() - entity.xo) * partials;
            double ey = entity.yo + (entity.getY() - entity.yo) * partials;
            double ez = entity.zo + (entity.getZ() - entity.zo) * partials;

            // Цвет — для игроков красный, для мобов по настройке, можно по HP
            int[] col = pickColor(entity, fs);
            int r = col[0], g = col[1], b = col[2], a = 200;

            ms.pushPose();
            ms.translate(ex - cam.x, ey - cam.y, ez - cam.z);

            IVertexBuilder lines = buf.getBuffer(RenderType.lines());

            // Хитбокс
            AxisAlignedBB bb = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
            float w = (float)(bb.maxX - bb.minX);
            float h = (float)(bb.maxY - bb.minY);

            int mode = Math.floorMod(fs.espMode, 4);
            switch (mode) {
                case 0: drawBox(ms, lines, bb, r, g, b, a);         break;
                case 1: drawCubeCenter(ms, lines, h, r, g, b, a);   break;
                case 2: drawCorners(ms, lines, bb, r, g, b, a);     break;
                case 3: drawFilledBox(ms, buf, bb, r, g, b, 40);    break;
            }

            // Скелет
            if (fs.espSkeleton) {
                drawSkeleton(ms, lines, entity, partials, r, g, b, a);
            }

            ms.popPose();

            // Линия от центра экрана до цели
            if (fs.espShowLine) {
                drawTracerLine(ms, buf, cam, ex, ey + h * 0.5, ez, r, g, b, 160);
            }
        }

        buf.endBatch(RenderType.lines());

        // HUD-метки (имя, HP, дистанция) — через 2D overlay
        if (fs.espShowName || fs.espShowHealth || fs.espShowDistance) {
            for (LivingEntity entity : targets) {
                float partials = event.getPartialTicks();
                double ex = entity.xo + (entity.getX() - entity.xo) * partials;
                double ey = entity.yo + (entity.getY() - entity.yo) * partials;
                double ez = entity.zo + (entity.getZ() - entity.zo) * partials;
                int[] col = pickColor(entity, fs);
                drawLabels(ms, buf, mc, entity, cam, ex, ey, ez, col, fs);
            }
        }
    }

    // ─── Рисование форм ───────────────────────────────────────────────────────

    /** Полный wireframe хитбокс */
    private static void drawBox(MatrixStack ms, IVertexBuilder vb, AxisAlignedBB bb,
                                int r, int g, int b, int a) {
        float x0 = (float) bb.minX, y0 = (float) bb.minY, z0 = (float) bb.minZ;
        float x1 = (float) bb.maxX, y1 = (float) bb.maxY, z1 = (float) bb.maxZ;
        // Bottom
        line(ms, vb, x0,y0,z0, x1,y0,z0, r,g,b,a);
        line(ms, vb, x1,y0,z0, x1,y0,z1, r,g,b,a);
        line(ms, vb, x1,y0,z1, x0,y0,z1, r,g,b,a);
        line(ms, vb, x0,y0,z1, x0,y0,z0, r,g,b,a);
        // Top
        line(ms, vb, x0,y1,z0, x1,y1,z0, r,g,b,a);
        line(ms, vb, x1,y1,z0, x1,y1,z1, r,g,b,a);
        line(ms, vb, x1,y1,z1, x0,y1,z1, r,g,b,a);
        line(ms, vb, x0,y1,z1, x0,y1,z0, r,g,b,a);
        // Verticals
        line(ms, vb, x0,y0,z0, x0,y1,z0, r,g,b,a);
        line(ms, vb, x1,y0,z0, x1,y1,z0, r,g,b,a);
        line(ms, vb, x1,y0,z1, x1,y1,z1, r,g,b,a);
        line(ms, vb, x0,y0,z1, x0,y1,z1, r,g,b,a);
    }

    /** Только уголки рамки (красиво выглядит) */
    private static void drawCorners(MatrixStack ms, IVertexBuilder vb, AxisAlignedBB bb,
                                    int r, int g, int b, int a) {
        float x0 = (float) bb.minX, y0 = (float) bb.minY, z0 = (float) bb.minZ;
        float x1 = (float) bb.maxX, y1 = (float) bb.maxY, z1 = (float) bb.maxZ;
        float cx = (x1 - x0) * 0.25F;
        float cy = (y1 - y0) * 0.20F;
        float cz = (z1 - z0) * 0.25F;

        // 8 углов × 3 линии
        for (int ix = 0; ix <= 1; ix++) {
            float sx = ix == 0 ? x0 : x1;
            float dx = ix == 0 ? cx : -cx;
            for (int iy = 0; iy <= 1; iy++) {
                float sy = iy == 0 ? y0 : y1;
                float dy = iy == 0 ? cy : -cy;
                for (int iz = 0; iz <= 1; iz++) {
                    float sz = iz == 0 ? z0 : z1;
                    float dz2 = iz == 0 ? cz : -cz;
                    line(ms, vb, sx, sy, sz, sx+dx, sy, sz, r,g,b,a);
                    line(ms, vb, sx, sy, sz, sx, sy+dy, sz, r,g,b,a);
                    line(ms, vb, sx, sy, sz, sx, sy, sz+dz2, r,g,b,a);
                }
            }
        }
    }

    /** Куб по центру сущности */
    private static void drawCubeCenter(MatrixStack ms, IVertexBuilder vb, float entityH,
                                       int r, int g, int b, int a) {
        float s = Math.min(0.5F, entityH * 0.3F);
        float cy = entityH * 0.5F;
        ms.pushPose();
        ms.translate(0, cy, 0);
        Primitives.cube(ms, vb, s * 2, r, g, b, a);
        ms.popPose();
    }

    /** Заполненный прозрачный ящик */
    private static void drawFilledBox(MatrixStack ms, IRenderTypeBuffer.Impl buf,
                                      AxisAlignedBB bb, int r, int g, int b, int a) {
        IVertexBuilder vb = buf.getBuffer(ModRenderTypes.COLOR_QUADS);
        float x0 = (float)bb.minX, y0 = (float)bb.minY, z0 = (float)bb.minZ;
        float x1 = (float)bb.maxX, y1 = (float)bb.maxY, z1 = (float)bb.maxZ;
        Matrix4f pose = ms.last().pose();
        // 6 граней
        quad(vb, pose, x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, r,g,b,a); // North
        quad(vb, pose, x0,y0,z1, x0,y1,z1, x1,y1,z1, x1,y0,z1, r,g,b,a); // South
        quad(vb, pose, x0,y0,z0, x0,y1,z0, x0,y1,z1, x0,y0,z1, r,g,b,a); // West
        quad(vb, pose, x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, r,g,b,a); // East
        quad(vb, pose, x0,y0,z0, x0,y0,z1, x1,y0,z1, x1,y0,z0, r,g,b,a); // Bottom
        quad(vb, pose, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, r,g,b,a); // Top
        buf.endBatch(ModRenderTypes.COLOR_QUADS);
        // Wireframe поверх
        IVertexBuilder lines = buf.getBuffer(RenderType.lines());
        drawBox(ms, lines, bb, r, g, b, 255);
    }

    /** Упрощённый скелет (туловище + голова + руки + ноги) */
    private static void drawSkeleton(MatrixStack ms, IVertexBuilder vb,
                                     LivingEntity e, float partials,
                                     int r, int g, int b, int a) {
        float h = e.getBbHeight();
        float headY   = h - 0.25F;          // верх головы (примерно)
        float neckY   = h * 0.85F;
        float shouldY = h * 0.70F;
        float waistY  = h * 0.50F;
        float hipY    = h * 0.38F;
        float kneeY   = h * 0.20F;
        float footY   = 0.02F;
        float armW    = h * 0.18F;
        float legW    = h * 0.12F;

        // Голова (крестик)
        line(ms, vb, -0.1F, neckY, 0, 0.1F, neckY, 0, r,g,b,a);
        line(ms, vb, 0, neckY, 0, 0, headY, 0, r,g,b,a);

        // Позвоночник
        line(ms, vb, 0, neckY, 0, 0, waistY, 0, r,g,b,a);

        // Плечи → кисти
        line(ms, vb, 0, shouldY, 0, -armW, shouldY, 0, r,g,b,a);
        line(ms, vb, -armW, shouldY, 0, -armW, waistY, 0, r,g,b,a);
        line(ms, vb, 0, shouldY, 0, armW, shouldY, 0, r,g,b,a);
        line(ms, vb, armW, shouldY, 0, armW, waistY, 0, r,g,b,a);

        // Таз → ноги
        line(ms, vb, 0, waistY, 0, -legW, hipY, 0, r,g,b,a);
        line(ms, vb, -legW, hipY, 0, -legW, kneeY, 0, r,g,b,a);
        line(ms, vb, -legW, kneeY, 0, -legW, footY, 0, r,g,b,a);
        line(ms, vb, 0, waistY, 0, legW, hipY, 0, r,g,b,a);
        line(ms, vb, legW, hipY, 0, legW, kneeY, 0, r,g,b,a);
        line(ms, vb, legW, kneeY, 0, legW, footY, 0, r,g,b,a);
    }

    /** Линия от центра экрана до сущности */
    private static void drawTracerLine(MatrixStack ms, IRenderTypeBuffer.Impl buf,
                                       Vector3d cam,
                                       double ex, double ey, double ez,
                                       int r, int g, int b, int a) {
        IVertexBuilder vb = buf.getBuffer(RenderType.lines());
        ms.pushPose();
        float dx = (float)(ex - cam.x);
        float dy = (float)(ey - cam.y);
        float dz = (float)(ez - cam.z);
        line(ms, vb, 0, 0, 0, dx, dy, dz, r, g, b, a);
        ms.popPose();
    }

    /** Метки (имя, HP, дистанция) над сущностью — через Minecraft font */
    private static void drawLabels(MatrixStack ms, IRenderTypeBuffer.Impl buf,
                                   Minecraft mc,
                                   LivingEntity entity,
                                   Vector3d cam,
                                   double ex, double ey, double ez,
                                   int[] col, FeatureSettings fs) {
        float h = entity.getBbHeight();
        double labelX = ex - cam.x;
        double labelY = ey + h + 0.3 - cam.y;
        double labelZ = ez - cam.z;

        // Проверяем что цель в поле зрения (грубо — если z > 0 за камерой пропускаем)
        // Для точного экранного проецирования используем матрицы — слишком сложно,
        // поэтому ограничимся 3D billboarded-текстом через font.drawInBatch

        ms.pushPose();
        ms.translate(labelX, labelY, labelZ);

        // Billboard — поворот к камере по Y
        double yaw = Math.atan2(labelX, labelZ);
        ms.mulPose(net.minecraft.util.math.vector.Vector3f.YP.rotation((float)(-yaw)));
        ms.scale(-0.025F, -0.025F, 0.025F);

        float yOff = 0;
        net.minecraft.client.gui.FontRenderer font = mc.font;

        if (fs.espShowName) {
            String name = entity.getName().getString();
            int textCol = 0xFF000000 | (col[0] << 16) | (col[1] << 8) | col[2];
            font.drawInBatch(name, -font.width(name) / 2f, yOff, textCol,
                    false, ms.last().pose(), buf, true, 0, 0xF000F0);
            yOff -= 10;
        }

        if (fs.espShowHealth) {
            float hp  = entity.getHealth();
            float max = entity.getMaxHealth();
            float pct = hp / max;
            // Цвет HP: зелёный → жёлтый → красный
            int hr = (int)(Math.min(1F, 2F - pct * 2F) * 255);
            int hg = (int)(Math.min(1F, pct * 2F) * 255);
            int hpCol = 0xFF000000 | (hr << 16) | (hg << 8);
            String hpStr = String.format("%.1f / %.0f HP", hp, max);
            font.drawInBatch(hpStr, -font.width(hpStr) / 2f, yOff, hpCol,
                    false, ms.last().pose(), buf, true, 0, 0xF000F0);
            yOff -= 10;
        }

        if (fs.espShowDistance) {
            double dist = entity.distanceTo(mc.player);
            String distStr = String.format("%.1fm", dist);
            font.drawInBatch(distStr, -font.width(distStr) / 2f, yOff, 0xFFAAAAAA,
                    false, ms.last().pose(), buf, true, 0, 0xF000F0);
        }

        ms.popPose();
    }

    // ─── Цвет ─────────────────────────────────────────────────────────────────

    /** Выбор цвета: игрок=красный, враг=оранжевый, пассив=зелёный. Если HP-режим — по HP */
    private static int[] pickColor(LivingEntity e, FeatureSettings fs) {
        if (fs.espColorMode == 1) {
            // По здоровью
            float pct = e.getHealth() / e.getMaxHealth();
            int r2 = (int)(Math.min(1F, 2F - pct * 2F) * 255);
            int g2 = (int)(Math.min(1F, pct * 2F) * 255);
            return new int[]{r2, g2, 0};
        }
        if (fs.espColorMode == 2) {
            // По типу
            if (e instanceof PlayerEntity) return new int[]{255, 80,  80};
            if (e instanceof IMob)         return new int[]{255, 160, 0};
            return new int[]{80, 220, 80};
        }
        // Пользовательский цвет
        return new int[]{
            Math.max(0, Math.min(255, (int)(fs.colorR * 255))),
            Math.max(0, Math.min(255, (int)(fs.colorG * 255))),
            Math.max(0, Math.min(255, (int)(fs.colorB * 255)))
        };
    }

    // ─── Фильтр ───────────────────────────────────────────────────────────────

    private static boolean isEspTarget(LivingEntity e, ClientPlayerEntity self, FeatureSettings fs) {
        if (e == self) return false;
        if (!e.isAlive()) return false;
        if (e instanceof PlayerEntity)  return fs.espTargetPlayers;
        if (e instanceof IMob)          return fs.espTargetHostile;
        if (e instanceof AnimalEntity)  return fs.espTargetPassive;
        return false;
    }

    // ─── Primitive helpers ────────────────────────────────────────────────────

    private static void line(MatrixStack ms, IVertexBuilder vb,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b, int a) {
        Matrix4f pose   = ms.last().pose();
        Matrix3f normal = ms.last().normal();
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-6f) { dx=0; dy=1; dz=0; } else { dx/=len; dy/=len; dz/=len; }
        vb.vertex(pose,x1,y1,z1).color(r,g,b,a).normal(normal,dx,dy,dz).endVertex();
        vb.vertex(pose,x2,y2,z2).color(r,g,b,a).normal(normal,dx,dy,dz).endVertex();
    }

    private static void quad(IVertexBuilder vb, Matrix4f pose,
                             float x1,float y1,float z1,
                             float x2,float y2,float z2,
                             float x3,float y3,float z3,
                             float x4,float y4,float z4,
                             int r, int g, int b, int a) {
        vb.vertex(pose,x1,y1,z1).color(r,g,b,a).endVertex();
        vb.vertex(pose,x2,y2,z2).color(r,g,b,a).endVertex();
        vb.vertex(pose,x3,y3,z3).color(r,g,b,a).endVertex();
        vb.vertex(pose,x4,y4,z4).color(r,g,b,a).endVertex();
    }
}
