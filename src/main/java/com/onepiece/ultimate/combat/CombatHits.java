package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Applies a {@link HitInfo} to a target: damage, knockback/launch, stun and combo bookkeeping. */
public final class CombatHits {
    private static final ThreadLocal<HitInfo> CURRENT = new ThreadLocal<>();

    @Nullable
    public static HitInfo current() {
        return CURRENT.get();
    }

    /** @return true if the hit dealt damage (it may be blocked, parried or dodged). */
    public static boolean apply(HitInfo hit, LivingEntity target) {
        if (target.level().isClientSide || !target.isAlive()) return false;
        HitInfo previous = CURRENT.get();
        CURRENT.set(hit);
        boolean hurt;
        try {
            // Our combos land faster than vanilla's 10 tick damage immunity; each move de-duplicates its own hits.
            target.invulnerableTime = 0;
            hurt = target.hurt(ModDamageTypes.source(target.level(), hit.type, hit.attacker), hit.damage);
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
        if (!hurt) return false;
        target.invulnerableTime = 0;

        Vec3 dir = new Vec3(hit.direction.x, 0, hit.direction.z);
        if (dir.lengthSqr() > 1e-6) dir = dir.normalize();
        double resist = 1.0 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        if (hit.launch) {
            Vec3 v = dir.scale(hit.knockback * resist).add(0, hit.lift * resist, 0);
            target.setDeltaMovement(v);
            target.hasImpulse = true;
            LaunchTracker.track(target, hit.attacker, hit.launchPower);
        } else if (hit.knockback > 0 || hit.lift > 0) {
            Vec3 old = target.getDeltaMovement();
            target.setDeltaMovement(old.x * 0.5 + dir.x * hit.knockback * resist,
                    Math.max(old.y, 0) + hit.lift * resist,
                    old.z * 0.5 + dir.z * hit.knockback * resist);
            target.hasImpulse = true;
        }
        target.hurtMarked = true; // sync velocity to players

        if (hit.stun > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.STUNNED.get(), hit.stun, 0, false, false, true));
        }
        if (target instanceof Player victim) {
            OPCapabilities.get(victim).ifPresent(d -> d.combat.comboStep = 0);
        }
        if (hit.countsForCombo && hit.attacker instanceof ServerPlayer sp) {
            OPCapabilities.get(sp).ifPresent(d -> {
                long now = sp.level().getGameTime();
                d.combat.comboHits = now <= d.combat.comboHitsExpire ? d.combat.comboHits + 1 : 1;
                d.combat.comboHitsExpire = now + 50;
                d.combat.markInCombat(now);
                d.setComboHits(d.combat.comboHits);
            });
        }
        return true;
    }

    private CombatHits() {}
}
