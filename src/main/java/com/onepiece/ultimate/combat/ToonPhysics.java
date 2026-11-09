package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gear Fifth "toon force": rubberised targets keep bouncing off the ground like cartoon balls for a while.
 * Weak keys + expiry keep it leak-free.
 */
public final class ToonPhysics {
    private static final class Toon {
        int ticksLeft;
        Vec3 lastVelocity = Vec3.ZERO;
        int bounces;

        Toon(int ticks) {
            this.ticksLeft = ticks;
        }
    }

    private static final Map<LivingEntity, Toon> TOONS = new WeakHashMap<>();

    public static void rubberize(LivingEntity e, int ticks) {
        Toon t = TOONS.get(e);
        if (t == null) TOONS.put(e, new Toon(ticks));
        else t.ticksLeft = Math.max(t.ticksLeft, ticks);
    }

    public static boolean isRubber(LivingEntity e) {
        return TOONS.containsKey(e);
    }

    public static void tick() {
        if (TOONS.isEmpty()) return;
        Iterator<Map.Entry<LivingEntity, Toon>> it = TOONS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            LivingEntity e = en.getKey();
            Toon t = en.getValue();
            if (e.isRemoved() || !e.isAlive() || --t.ticksLeft <= 0 || t.bounces > 6) {
                it.remove();
                continue;
            }
            if (e.onGround() && t.lastVelocity.y < -0.3) {
                Vec3 v = t.lastVelocity;
                e.setDeltaMovement(v.x * 0.9, -v.y * 0.82, v.z * 0.9);
                e.hurtMarked = true;
                e.fallDistance = 0;
                t.bounces++;
                if (e.level() instanceof ServerLevel sl) {
                    ServerFx.sound(sl, e.position(), ModSounds.GOMU_BOUNCE.get(), 0.9f, 1.4f - Math.min(0.6f, t.bounces * 0.1f));
                    sl.sendParticles(RingParticleOptions.ground(0xFFFFFF, 1.2f + e.getBbWidth(), 8), e.getX(), e.getY() + 0.05, e.getZ(), 1, 0, 0, 0, 0);
                }
            }
            t.lastVelocity = e.getDeltaMovement();
        }
    }

    public static void clear() {
        TOONS.clear();
    }

    private ToonPhysics() {}
}
