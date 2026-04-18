package com.example.cosmetics.client;

import com.example.cosmetics.CosmeticsMod;
import com.example.cosmetics.auras.AuraTicker;
import com.example.cosmetics.effects.JumpCircles;
import com.example.cosmetics.feature.FeatureType;
import com.example.cosmetics.gui.MainMenuScreen;
import com.example.cosmetics.hud.CosmeticsHud;
import com.example.cosmetics.hud.TargetHud;
import com.example.cosmetics.hud.Watermark;
import com.example.cosmetics.particles.ParticleManager;
import com.example.cosmetics.render.HandRenderer;
import com.example.cosmetics.render.HatRenderer;
import com.example.cosmetics.trails.TrailTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CosmeticsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    // Landing detection — tracks previous onGround state + fallDistance for local player.
    private static boolean wasOnGround = true;
    private static float lastFallDistance = 0F;

    // ---- Mod event bus --------------------------------------------------

    public static void onClientSetup(FMLClientSetupEvent event) {
        KeyBindings.register();
    }

    // ---- Forge event bus ------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        while (KeyBindings.OPEN_MENU.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new MainMenuScreen());
        }

        ParticleManager.get().tick();
        JumpCircles.get().tick();
        TrailTicker.tick(mc.player);
        AuraTicker.tick(mc.player);
        HandRenderer.tickPlaceAnim();

        // Landing-ring detection: airborne -> onGround transition
        PlayerEntity p = mc.player;
        boolean nowOnGround = p.isOnGround();
        if (nowOnGround && !wasOnGround) {
            JumpCircles.get().spawnLanding(p, lastFallDistance);
        }
        wasOnGround = nowOnGround;
        lastFallDistance = p.fallDistance;
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntityLiving() != mc.player) return;
        JumpCircles.get().spawnJump(mc.player);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        CosmeticsHud.render(event.getMatrixStack(), event.getPartialTicks());
        TargetHud.render(event.getMatrixStack(), event.getPartialTicks());
        Watermark.render(event.getMatrixStack(), event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        ParticleManager.get().renderAll(event.getMatrixStack(), event.getPartialTicks());
        JumpCircles.get().renderAll(event.getMatrixStack(), event.getPartialTicks());
        HatRenderer.render(event.getMatrixStack(), event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        HandRenderer.applyTransforms(event.getMatrixStack(), event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() != null && event.getEntityLiving().level != null
                && event.getEntityLiving().level.isClientSide) {
            TargetHud.onLivingHurt(event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.ClickInputEvent event) {
        // Detect right-click with a block item to trigger the custom place animation.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!event.isUseItem()) return;
        ItemStack stack = mc.player.getItemInHand(Hand.MAIN_HAND);
        if (stack.getItem() instanceof BlockItem) {
            if (CosmeticsState.get().isOn(FeatureType.CUSTOM_PLACE)) {
                HandRenderer.triggerPlaceAnim();
            }
        }
    }

    private ClientEvents() {}
}
