package com.example.cosmetics.particles.factories;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;

/**
 * A generic factory that spawns a simple sprite-textured particle which
 * fades out, drifts with its initial velocity, and uses the texture
 * registered for its ParticleType via the particles/<name>.json file.
 */
public class GenericSpriteParticleFactory implements IParticleFactory<BasicParticleType> {
    private final IAnimatedSprite sprites;

    public GenericSpriteParticleFactory(IAnimatedSprite sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(BasicParticleType type, ClientWorld world,
                                   double x, double y, double z,
                                   double vx, double vy, double vz) {
        return new SpriteParticle(world, x, y, z, vx, vy, vz, sprites);
    }

    private static class SpriteParticle extends SpriteTexturedParticle {
        SpriteParticle(ClientWorld world, double x, double y, double z,
                       double vx, double vy, double vz, IAnimatedSprite sprites) {
            super(world, x, y, z, vx, vy, vz);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = 20 + this.random.nextInt(20);
            this.quadSize = 0.15F + this.random.nextFloat() * 0.1F;
            this.hasPhysics = false;
            this.pickSprite(sprites);
            this.rCol = 1.0F;
            this.gCol = 1.0F;
            this.bCol = 1.0F;
            this.alpha = 1.0F;
        }

        @Override
        public void tick() {
            super.tick();
            // Gentle drag + alpha fade-out over lifetime.
            this.xd *= 0.95;
            this.yd *= 0.95;
            this.zd *= 0.95;
            float life = (float) this.age / (float) this.lifetime;
            this.alpha = 1.0F - life;
        }

        @Override
        public IParticleRenderType getRenderType() {
            return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
