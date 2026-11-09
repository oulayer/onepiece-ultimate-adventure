package com.onepiece.ultimate.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onepiece.ultimate.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/**
 * Parameters of a ring particle.
 *
 * @param color    packed 0xRRGGBB
 * @param scale    final radius in blocks
 * @param lifetime ticks
 * @param pitch    orientation of the ring's normal (degrees). 90 = horizontal ground ring.
 * @param yaw      orientation of the ring's normal (degrees).
 */
public record RingParticleOptions(int color, float scale, int lifetime, float pitch, float yaw) implements ParticleOptions {

    public static final Codec<RingParticleOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("color").forGetter(RingParticleOptions::color),
            Codec.FLOAT.fieldOf("scale").forGetter(RingParticleOptions::scale),
            Codec.INT.fieldOf("lifetime").forGetter(RingParticleOptions::lifetime),
            Codec.FLOAT.fieldOf("pitch").forGetter(RingParticleOptions::pitch),
            Codec.FLOAT.fieldOf("yaw").forGetter(RingParticleOptions::yaw)
    ).apply(i, RingParticleOptions::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<RingParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public RingParticleOptions fromCommand(ParticleType<RingParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            int life = reader.readInt();
            reader.expect(' ');
            float pitch = reader.readFloat();
            reader.expect(' ');
            float yaw = reader.readFloat();
            return new RingParticleOptions(color, scale, life, pitch, yaw);
        }

        @Override
        public RingParticleOptions fromNetwork(ParticleType<RingParticleOptions> type, FriendlyByteBuf buf) {
            return new RingParticleOptions(buf.readInt(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat());
        }
    };

    /** Horizontal ring lying on the ground. */
    public static RingParticleOptions ground(int color, float scale, int lifetime) {
        return new RingParticleOptions(color, scale, lifetime, 90f, 0f);
    }

    /** Ring perpendicular to the direction the attacker is facing. */
    public static RingParticleOptions facing(int color, float scale, int lifetime, float pitch, float yaw) {
        return new RingParticleOptions(color, scale, lifetime, pitch, yaw);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.RING.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(scale);
        buf.writeVarInt(lifetime);
        buf.writeFloat(pitch);
        buf.writeFloat(yaw);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "onepiece:ring %d %.2f %d %.1f %.1f", color, scale, lifetime, pitch, yaw);
    }
}
