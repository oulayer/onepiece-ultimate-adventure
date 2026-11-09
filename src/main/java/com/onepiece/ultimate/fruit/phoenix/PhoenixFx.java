package com.onepiece.ultimate.fruit.phoenix;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Blue-flame particles shared by the phoenix moveset. */
final class PhoenixFx {
    static void flames(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
    }

    static void body(ServerLevel level, LivingEntity e, int count) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), count,
                e.getBbWidth() * 0.5, e.getBbHeight() * 0.35, e.getBbWidth() * 0.5, 0.01);
    }

    private PhoenixFx() {}
}
