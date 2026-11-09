package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Follows entities that were launched by a heavy hit. If one smashes into a wall (or the ground) at speed
 * it takes impact damage and leaves a dent in the terrain – the "thrown through a wall" moment.
 * Weak keys + expiry keep this leak-free.
 */
public final class LaunchTracker {
    private static final int TRACK_TICKS = 30;
    private static final double WALL_SPEED = 0.55;
    private static final double GROUND_SPEED = 0.9;

    private static final class Launch {
        final LivingEntity attacker;
        final float power;
        final long start;
        Vec3 lastVelocity = Vec3.ZERO;
        int age;

        Launch(LivingEntity attacker, float power, long start) {
            this.attacker = attacker;
            this.power = power;
            this.start = start;
        }
    }

    private static final Map<LivingEntity, Launch> LAUNCHES = new WeakHashMap<>();

    public static void track(LivingEntity target, LivingEntity attacker, float power) {
        Launch l = new Launch(attacker, power, target.level().getGameTime());
        l.lastVelocity = target.getDeltaMovement();
        LAUNCHES.put(target, l);
    }

    public static boolean isTracked(LivingEntity e) {
        return LAUNCHES.containsKey(e);
    }

    /** Called at the end of every server tick. */
    public static void tick() {
        if (LAUNCHES.isEmpty()) return;
        Iterator<Map.Entry<LivingEntity, Launch>> it = LAUNCHES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Launch> e = it.next();
            LivingEntity target = e.getKey();
            Launch l = e.getValue();
            if (target.isRemoved() || !target.isAlive() || ++l.age > TRACK_TICKS || !(target.level() instanceof ServerLevel level)) {
                it.remove();
                continue;
            }
            Vec3 v = l.lastVelocity;
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
            boolean wall = target.horizontalCollision && horizontal > WALL_SPEED;
            boolean ground = l.age > 2 && target.onGround() && -v.y > GROUND_SPEED;
            if (wall || ground) {
                impact(level, target, l, wall ? horizontal : -v.y, wall ? new Vec3(v.x, 0, v.z) : new Vec3(0, -1, 0));
                it.remove();
                continue;
            }
            l.lastVelocity = target.getDeltaMovement();
        }
    }

    private static void impact(ServerLevel level, LivingEntity target, Launch l, double speed, Vec3 dir) {
        float damage = (float) (2.5 + speed * 4.0 * l.power);
        target.invulnerableTime = 0;
        target.hurt(ModDamageTypes.source(level, ModDamageTypes.IMPACT, l.attacker), damage);
        Vec3 n = dir.normalize();
        Vec3 point = target.getBoundingBox().getCenter().add(n.scale(target.getBbWidth() * 0.6 + 0.3));
        float radius = (float) Math.min(3.5, 0.9 + speed * 0.9 * l.power);
        DestructionManager.crater(level, point, radius, 0.9f, 1.6f * l.power, l.attacker);
        ServerFx.impact(level, point, n, radius, 0xFFFFFF);
        ServerFx.sound(level, point, ModSounds.HIT_HEAVY.get(), 1.2f, 0.7f);
        ServerFx.shake(level, point, 18, 0.5f * l.power, 8);
    }

    public static void clear() {
        LAUNCHES.clear();
    }

    private LaunchTracker() {}
}
