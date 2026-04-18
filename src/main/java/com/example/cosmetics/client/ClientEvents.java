package com.example.cosmetics.client;

import com.example.cosmetics.CosmeticsMod;
import com.example.cosmetics.auras.AuraTicker;
import com.example.cosmetics.gui.MainMenuScreen;
import com.example.cosmetics.hud.CosmeticsHud;
import com.example.cosmetics.particles.ModParticles;
import com.example.cosmetics.particles.factories.GenericSpriteParticleFactory;
import com.example.cosmetics.render.HatLayer;
import com.example.cosmetics.trails.TrailTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@Mod.EventBusSubscriber(modid = CosmeticsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    // ---- Mod event bus listeners (wired from CosmeticsMod constructor) ----

    public static void onClientSetup(FMLClientSetupEvent event) {
        KeyBindings.register();

        // Install HatLayer on both player renderers. Must run on the client
        // thread because it touches Minecraft's render manager.
        event.enqueueWork(() -> {
            EntityRendererManager mgr = Minecraft.getInstance().getEntityRenderDispatcher();
            Map<String, PlayerRenderer> skins = mgr.getSkinMap();
            for (PlayerRenderer r : skins.values()) {
                r.addLayer(new HatLayer(r));
            }
        });
    }

    public static void onParticleFactoryRegister(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(ModParticles.RAINBOW.get(), GenericSpriteParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.FLAME.get(),   GenericSpriteParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.GALAXY.get(),  GenericSpriteParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.AURA.get(),    GenericSpriteParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.SNOW.get(),    GenericSpriteParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.HEART.get(),   GenericSpriteParticleFactory::new);
    }

    // ---- Forge event bus listeners (auto-registered via @EventBusSubscriber) ----

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Open menu on Right Shift
        while (KeyBindings.OPEN_MENU.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new MainMenuScreen());
            }
        }

        // Drive particle tickers
        TrailTicker.tick(mc.player);
        AuraTicker.tick(mc.player);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        CosmeticsHud.render(event.getMatrixStack(), event.getPartialTicks());
    }
}
