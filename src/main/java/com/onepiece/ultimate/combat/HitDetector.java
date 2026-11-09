package com.onepiece.ultimate.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Geometric hit detection against entity bounding boxes.
 * <ul>
 *     <li>{@link #arc} – melee cone: distance is measured to the closest point of the target's box,
 *         so large mobs are hittable at their edge, and walls block hits (line of sight).</li>
 *     <li>{@link #sweep} – thick segment (capsule) used by fast projectiles so they never tunnel through targets.</li>
 * </ul>
 */
public final class HitDetector {

    public static List<LivingEntity> arc(LivingEntity attacker, float range, float arcDegrees) {
        return arc(attacker, attacker.getLookAngle(), range, arcDegrees);
    }

    public static List<LivingEntity> arc(LivingEntity attacker, Vec3 direction, float range, float arcDegrees) {
        Level level = attacker.level();
        Vec3 eye = attacker.getEyePosition();
        Vec3 dir = direction.normalize();
        double cosHalf = Math.cos(Math.toRadians(Math.min(360f, arcDegrees) / 2.0));
        List<LivingEntity> result = new ArrayList<>();
        AABB search = attacker.getBoundingBox().inflate(range + 1.0);
        for (Entity e : level.getEntities(attacker, search, x -> x instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity) e;
            if (!isValidTarget(attacker, target)) continue;
            AABB box = target.getBoundingBox();
            Vec3 closest = closestPoint(box, eye);
            double dist = closest.distanceTo(eye);
            if (dist > range) continue;
            if (arcDegrees < 360f && dist > 0.6) {
                // Test both the box centre and the closest point: forgiving for big targets, precise for small ones.
                Vec3 toCenter = box.getCenter().subtract(eye).normalize();
                Vec3 toClosest = closest.subtract(eye).normalize();
                if (toCenter.dot(dir) < cosHalf && toClosest.dot(dir) < cosHalf) continue;
            }
            if (!hasLineOfSight(level, eye, box.getCenter(), attacker)) continue;
            result.add(target);
        }
        result.sort(Comparator.comparingDouble(t -> t.distanceToSqr(attacker)));
        return result;
    }

    /** Entities whose box (inflated by radius) intersects the segment from→to. */
    public static List<LivingEntity> sweep(Level level, Vec3 from, Vec3 to, double radius, @Nullable Entity exclude) {
        List<LivingEntity> out = new ArrayList<>();
        AABB area = new AABB(from, to).inflate(radius + 0.5);
        for (Entity e : level.getEntities(exclude, area, x -> x instanceof LivingEntity)) {
            LivingEntity le = (LivingEntity) e;
            if (!le.isAlive() || le.isSpectator()) continue;
            AABB box = le.getBoundingBox().inflate(radius);
            if (box.contains(from) || box.clip(from, to).isPresent()) out.add(le);
        }
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(from)));
        return out;
    }

    public static boolean isValidTarget(LivingEntity attacker, LivingEntity target) {
        if (target == attacker || !target.isAlive() || target.isSpectator()) return false;
        if (target instanceof ArmorStand stand && stand.isMarker()) return false;
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(attacker)) return false;
        if (target.isAlliedTo(attacker)) return false;
        return !target.isInvulnerable();
    }

    public static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to, Entity viewer) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)).getType() == HitResult.Type.MISS;
    }

    public static Vec3 closestPoint(AABB box, Vec3 p) {
        return new Vec3(clamp(p.x, box.minX, box.maxX), clamp(p.y, box.minY, box.maxY), clamp(p.z, box.minZ, box.maxZ));
    }

    public static Optional<Vec3> segmentHit(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) ? Optional.of(from) : box.clip(from, to);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    private HitDetector() {}
}
