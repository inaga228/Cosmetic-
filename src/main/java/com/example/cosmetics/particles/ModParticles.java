package com.example.cosmetics.particles;

import com.example.cosmetics.CosmeticsMod;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CosmeticsMod.MOD_ID);

    // Trails
    public static final RegistryObject<BasicParticleType> RAINBOW =
            PARTICLE_TYPES.register("rainbow", () -> new BasicParticleType(false));
    public static final RegistryObject<BasicParticleType> FLAME =
            PARTICLE_TYPES.register("flame",   () -> new BasicParticleType(false));
    public static final RegistryObject<BasicParticleType> GALAXY =
            PARTICLE_TYPES.register("galaxy",  () -> new BasicParticleType(false));

    // Auras
    public static final RegistryObject<BasicParticleType> AURA =
            PARTICLE_TYPES.register("aura",    () -> new BasicParticleType(false));
    public static final RegistryObject<BasicParticleType> SNOW =
            PARTICLE_TYPES.register("snow",    () -> new BasicParticleType(false));
    public static final RegistryObject<BasicParticleType> HEART =
            PARTICLE_TYPES.register("heart",   () -> new BasicParticleType(false));

    private ModParticles() {}
}
