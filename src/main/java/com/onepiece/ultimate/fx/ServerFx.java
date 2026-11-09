package com.onepiece.ultimate.fx;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.network.packet.PlayAnimationPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server-side helpers that broadcast visual/audio feedback. Gameplay never depends on these. */
public final class ServerFx {

    public static void animation(Entity entity, String name) {
        animation(entity, name, 1f);
    }

    public static void animation(Entity entity, String name, float speed) {
        OPNetwork.sendToTrackingAndSelf(entity, new PlayAnimationPacket(entity.getId(), OnePieceMod.id(name), speed));
    }

    /** Shake for every player within {@code radius}, fading with distance. */
    public static void shake(ServerLevel level, Vec3 pos, double radius, float intensity, int duration) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(pos);
            if (d > radius) continue;
            float falloff = (float) (1.0 - d / radius);
            OPNetwork.sendTo(p, new CameraEffectPacket(CameraEffectPacket.Type.SHAKE, intensity * falloff, duration));
        }
    }

    public static void camera(Entity entity, CameraEffectPacket.Type type, float intensity, int duration) {
        if (entity instanceof ServerPlayer sp) OPNetwork.sendTo(sp, new CameraEffectPacket(type, intensity, duration));
    }

    public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void particles(ServerLevel level, ParticleOptions type, Vec3 pos, int count, double spread, double speed) {
        level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    /** Standard punch impact: spark flash, a shock ring facing the attack direction and some dust. */
    public static void impact(ServerLevel level, Vec3 pos, Vec3 direction, float size, int color) {
        level.sendParticles(ModParticles.IMPACT_SPARK.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, direction.normalize().y))));
        level.sendParticles(RingParticleOptions.facing(color, size, 8, pitch, yaw), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, (int) (6 * size), 0.2, 0.2, 0.2, 0.4);
        if (size > 1.5f) level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    /** Ground ring plus dust from the block beneath. */
    public static void groundShock(ServerLevel level, Vec3 pos, float radius, int lifetime) {
        level.sendParticles(RingParticleOptions.ground(0xFFFFFF, radius, lifetime), pos.x, pos.y + 0.1, pos.z, 1, 0, 0, 0, 0);
        BlockState below = level.getBlockState(BlockPos.containing(pos.x, pos.y - 0.5, pos.z));
        if (!below.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), pos.x, pos.y + 0.2, pos.z,
                    (int) (radius * 12), radius * 0.4, 0.1, radius * 0.4, 0.3);
        }
    }

    private ServerFx() {}
}
