package com.onepiece.ultimate.client.particle;

import com.onepiece.ultimate.config.OPClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Soft, rising, expanding puff (Gear Second steam, dash dust). */
public class SteamParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;

    protected SteamParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.lifetime = 18 + random.nextInt(14);
        this.xd = dx + (random.nextFloat() - 0.5) * 0.02;
        this.yd = dy + 0.02 + random.nextFloat() * 0.03;
        this.zd = dz + (random.nextFloat() - 0.5) * 0.02;
        this.gravity = -0.015f;
        this.friction = 0.9f;
        this.startSize = 0.12f + random.nextFloat() * 0.1f;
        this.quadSize = startSize;
        float shade = 0.9f + random.nextFloat() * 0.1f;
        setColor(shade, shade, shade);
        setAlpha(0.65f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
            float t = age / (float) lifetime;
            quadSize = startSize * (1f + 2.2f * t);
            setAlpha(0.65f * (1f - t * t));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            if (level.random.nextFloat() > OPClientConfig.PARTICLE_DENSITY.get()) return null;
            return new SteamParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
