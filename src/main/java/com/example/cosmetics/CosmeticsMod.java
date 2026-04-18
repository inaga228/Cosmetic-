package com.example.cosmetics;

import com.example.cosmetics.client.ClientEvents;
import com.example.cosmetics.particles.ModParticles;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod(CosmeticsMod.MOD_ID)
public class CosmeticsMod {
    public static final String MOD_ID = "cosmeticsmod";

    public CosmeticsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register particle types on the mod event bus
        ModParticles.PARTICLE_TYPES.register(modBus);

        // Client-only setup
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(ClientEvents::onClientSetup);
            modBus.addListener(ClientEvents::onParticleFactoryRegister);
        });
    }
}
