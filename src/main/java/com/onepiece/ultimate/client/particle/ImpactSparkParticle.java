package com.onepiece.ultimate.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Very short, full-bright starburst at the exact point of impact. */
public class ImpactSparkParticle extends TextureSheetParticle {
    protected ImpactSparkParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = 4;
        this.hasPhysics = false;
        this.gravity = 0;
        this.xd = this.yd = this.zd = 0;
        this.roll = random.nextFloat() * (float) Math.PI * 2;
        this.oRoll = roll;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = age / (float) lifetime;
        quadSize = 0.5f + 1.4f * t;
        setAlpha(1f - t);
    }

    @Override
    protected int getLightColor(float partial) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new ImpactSparkParticle(level, x, y, z, sprites);
        }
    }
}
