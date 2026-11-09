package com.onepiece.ultimate.registry;

import com.mojang.serialization.Codec;
import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.particle.RingParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, OnePieceMod.MOD_ID);

    /** Soft rising puff – Gear Second steam, dash dust. */
    public static final RegistryObject<SimpleParticleType> STEAM = PARTICLES.register("steam", () -> new SimpleParticleType(false));
    /** Bright star flash at the point of impact. */
    public static final RegistryObject<SimpleParticleType> IMPACT_SPARK = PARTICLES.register("impact_spark", () -> new SimpleParticleType(true));
    /**
     * Jagged black lightning (Haoshoku / emission). The particle "velocity" is the bolt's end offset;
     * spawn with count 0 so the vector arrives exactly.
     */
    public static final RegistryObject<SimpleParticleType> BLACK_LIGHTNING = PARTICLES.register("black_lightning", () -> new SimpleParticleType(true));
    /** Same bolt shape, white-gold core with a blue glow (Gear Fifth Kaminari). */
    public static final RegistryObject<SimpleParticleType> WHITE_LIGHTNING = PARTICLES.register("white_lightning", () -> new SimpleParticleType(true));
    /** Expanding coloured ring: impact shock rings (facing a direction) and ground shockwaves (horizontal). */
    public static final RegistryObject<ParticleType<RingParticleOptions>> RING = PARTICLES.register("ring",
            () -> new ParticleType<>(true, RingParticleOptions.DESERIALIZER) {
                @Override
                public Codec<RingParticleOptions> codec() {
                    return RingParticleOptions.CODEC;
                }
            });

    private ModParticles() {}
}
