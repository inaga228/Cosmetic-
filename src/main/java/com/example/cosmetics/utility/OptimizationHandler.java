package com.example.cosmetics.utility;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.settings.GraphicsFanciness;

/**
 * Обрабатывает все оптимизационные функции клиента.
 * Применяет настройки мгновенно при включении/выключении.
 */
public final class OptimizationHandler {

    private static final OptimizationHandler INSTANCE = new OptimizationHandler();
    public static OptimizationHandler get() { return INSTANCE; }

    // Saved originals
    private int    savedRenderDist   = -1;
    private int    savedFps          = -1;
    private boolean savedVsync       = false;
    private GraphicsFanciness savedGraphics = null;
    private boolean savedClouds      = true;
    private boolean savedEntityShadows = true;

    // Active states
    private boolean farViewActive    = false;
    private boolean fpsBoostActive   = false;
    private boolean fastGraphicsActive = false;
    private boolean noCloudsActive   = false;
    private boolean noShadowActive   = false;

    private int tickCounter = 0;

    private OptimizationHandler() {}

    public void tick(Minecraft mc) {
        // Проверяем каждые 20 тиков (1 сек) чтобы не нагружать каждый кадр
        if (++tickCounter < 20) return;
        tickCounter = 0;

        tickFarView(mc);
        tickFpsBoost(mc);
        tickFastGraphics(mc);
        tickNoClouds(mc);
        tickNoEntityShadow(mc);
    }

    // ── Увеличенная дальность прорисовки ────────────────────────────────────
    private void tickFarView(Minecraft mc) {
        boolean want = CosmeticsState.get().isOn(FeatureType.FAR_VIEW);
        if (want && !farViewActive) {
            savedRenderDist = mc.options.renderDistance;
            mc.options.renderDistance = 32;
            mc.levelRenderer.allChanged();
            farViewActive = true;
        } else if (!want && farViewActive) {
            if (savedRenderDist > 0) {
                mc.options.renderDistance = savedRenderDist;
                mc.levelRenderer.allChanged();
            }
            farViewActive = false;
        }
    }

    // ── FPS Boost: снять лимит FPS ───────────────────────────────────────────
    private void tickFpsBoost(Minecraft mc) {
        boolean want = CosmeticsState.get().isOn(FeatureType.FPS_BOOST);
        if (want && !fpsBoostActive) {
            savedFps   = mc.options.framerateLimit;
            savedVsync = mc.options.enableVsync;
            mc.options.framerateLimit = 260;
            mc.options.enableVsync   = false;
            mc.getWindow().updateVsync(false);
            fpsBoostActive = true;
        } else if (!want && fpsBoostActive) {
            mc.options.framerateLimit = savedFps > 0 ? savedFps : 120;
            mc.options.enableVsync   = savedVsync;
            mc.getWindow().updateVsync(savedVsync);
            fpsBoostActive = false;
        }
    }

    // ── Fast Graphics: переключить в «Fast» режим ────────────────────────────
    private void tickFastGraphics(Minecraft mc) {
        boolean want = CosmeticsState.get().isOn(FeatureType.FAST_GRAPHICS);
        if (want && !fastGraphicsActive) {
            savedGraphics = mc.options.graphicsMode;
            if (mc.options.graphicsMode != GraphicsFanciness.FAST) {
                mc.options.graphicsMode = GraphicsFanciness.FAST;
                mc.levelRenderer.allChanged();
            }
            fastGraphicsActive = true;
        } else if (!want && fastGraphicsActive) {
            if (savedGraphics != null && mc.options.graphicsMode != savedGraphics) {
                mc.options.graphicsMode = savedGraphics;
                mc.levelRenderer.allChanged();
            }
            fastGraphicsActive = false;
        }
    }

    // ── No Clouds ────────────────────────────────────────────────────────────
    private void tickNoClouds(Minecraft mc) {
        boolean want = CosmeticsState.get().isOn(FeatureType.NO_CLOUDS);
        if (want && !noCloudsActive) {
            savedClouds = mc.options.renderClouds != net.minecraft.client.settings.CloudOption.OFF;
            mc.options.renderClouds = net.minecraft.client.settings.CloudOption.OFF;
            noCloudsActive = true;
        } else if (!want && noCloudsActive) {
            mc.options.renderClouds = savedClouds
                    ? net.minecraft.client.settings.CloudOption.FANCY
                    : net.minecraft.client.settings.CloudOption.OFF;
            noCloudsActive = false;
        }
    }

    // ── No Entity Shadows ────────────────────────────────────────────────────
    private void tickNoEntityShadow(Minecraft mc) {
        boolean want = CosmeticsState.get().isOn(FeatureType.NO_ENTITY_SHADOW);
        if (want && !noShadowActive) {
            savedEntityShadows = mc.options.entityShadows;
            mc.options.entityShadows = false;
            noShadowActive = true;
        } else if (!want && noShadowActive) {
            mc.options.entityShadows = savedEntityShadows;
            noShadowActive = false;
        }
    }
}
