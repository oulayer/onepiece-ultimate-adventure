package com.onepiece.ultimate.destruction;

import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** A ring that travels outward along the ground, hitting each entity once as it passes. */
final class ShockwaveJob {
    private final ServerLevel level;
    private final Vec3 center;
    private final float maxRadius;
    private final int duration;
    private final float damage;
    private final float lift;
    private final float knockback;
    @Nullable private final LivingEntity source;
    private final Set<Integer> hit = new HashSet<>();
    private int age;

    ShockwaveJob(ServerLevel level, Vec3 center, float maxRadius, int duration, float damage, float lift, float knockback, @Nullable LivingEntity source) {
        this.level = level;
        this.center = center;
        this.maxRadius = maxRadius;
        this.duration = Math.max(1, duration);
        this.damage = damage;
        this.lift = lift;
        this.knockback = knockback;
        this.source = source;
    }

    ServerLevel level() {
        return level;
    }

    /** @return true when finished. */
    boolean tick() {
        age++;
        float r = maxRadius * age / duration;
        float prev = maxRadius * (age - 1) / duration;

        AABB box = new AABB(center, center).inflate(r + 1, 3, r + 1);
        for (Entity e : level.getEntities(source, box, x -> x instanceof LivingEntity)) {
            LivingEntity t = (LivingEntity) e;
            if (hit.contains(t.getId())) continue;
            if (source != null && !HitDetector.isValidTarget(source, t)) continue;
            double dx = t.getX() - center.x, dz = t.getZ() - center.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < prev - 1.0 || d > r + 0.6 || Math.abs(t.getY() - center.y) > 2.5) continue;
            hit.add(t.getId());
            float falloff = (float) (1.0 - 0.5 * d / maxRadius);
            if (source != null) {
                HitInfo info = new HitInfo(source, damage * falloff).type(ModDamageTypes.IMPACT)
                        .direction(new Vec3(dx, 0, dz)).knockback(knockback * falloff).lift(lift).stun(12).noCombo();
                CombatHits.apply(info, t);
            } else {
                t.hurt(level.damageSources().generic(), damage * falloff);
            }
        }

        int points = Math.max(8, (int) (r * 5));
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 * i) / points + level.random.nextFloat() * 0.2;
            double x = center.x + Math.cos(a) * r, z = center.z + Math.sin(a) * r;
            BlockPos below = BlockPos.containing(x, center.y - 0.5, z);
            BlockState s = level.getBlockState(below);
            if (s.isAir()) s = level.getBlockState(below.below());
            if (!s.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), x, center.y + 0.1, z, 2, 0.1, 0.05, 0.1, 0.25);
            }
            if (i % 3 == 0) level.sendParticles(ParticleTypes.POOF, x, center.y + 0.2, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        return age >= duration;
    }
}
