package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Shared feedback and hit helpers for the Gomu Gomu moveset. */
final class GomuFx {
    static final int RUBBER_COLOR = 0xFFC9A0;
    static final int JET_COLOR = 0xFF6B5A;

    static void stretch(ServerLevel level, Vec3 pos, float pitch) {
        ServerFx.sound(level, pos, ModSounds.GOMU_STRETCH.get(), 0.9f, pitch);
    }

    static void steamBurst(ServerLevel level, ServerPlayer player, int count) {
        Vec3 p = player.position();
        level.sendParticles(ModParticles.STEAM.get(), p.x, p.y + 1.0, p.z, count, 0.35, 0.6, 0.35, 0.02);
    }

    /** Builds the on-hit behaviour of a fist: damage, knockback along the punch, FX and mastery. */
    static Consumer<LivingEntity> fistHit(AbilityContext ctx, Vec3 dir, float damage, float knockback, float lift, int stun,
                                          float launchPower, float impactSize, int color, int mastery, boolean guardBreak) {
        ServerPlayer player = ctx.player();
        return target -> {
            HitInfo hit = new HitInfo(player, damage * ctx.power()).type(ModDamageTypes.MARTIAL)
                    .direction(dir).knockback(knockback).lift(lift).stun(stun);
            if (launchPower > 0) hit.launch(launchPower);
            if (guardBreak) hit.guardBreak();
            if (CombatHits.apply(hit, target)) {
                ServerLevel level = ctx.level();
                Vec3 at = target.getBoundingBox().getCenter().subtract(dir.scale(target.getBbWidth() * 0.5));
                ServerFx.impact(level, at, dir, impactSize, color);
                ServerFx.sound(level, at, impactSize > 1.2f ? ModSounds.HIT_HEAVY.get() : ModSounds.HIT_LIGHT.get(), 1f,
                        0.9f + level.random.nextFloat() * 0.2f);
                if (impactSize > 1.2f) {
                    ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, 2);
                    ServerFx.shake(level, at, 16, Math.min(1.2f, impactSize * 0.3f), 6);
                }
                ctx.awardMastery(mastery);
            }
        };
    }

    /** The living target closest to the crosshair inside a cone, for homing attacks. */
    @org.jetbrains.annotations.Nullable
    static LivingEntity bestTarget(AbilityContext ctx, double range, double coneDegrees) {
        ServerPlayer p = ctx.player();
        Vec3 eye = p.getEyePosition(), look = ctx.look();
        double cos = Math.cos(Math.toRadians(coneDegrees));
        LivingEntity best = null;
        double bestScore = -1;
        for (var e : ctx.level().getEntities(p, p.getBoundingBox().inflate(range), x -> x instanceof LivingEntity)) {
            LivingEntity le = (LivingEntity) e;
            if (!com.onepiece.ultimate.combat.HitDetector.isValidTarget(p, le)) continue;
            Vec3 to = le.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range || d < 1e-3) continue;
            double dot = to.scale(1 / d).dot(look);
            if (dot < cos || !com.onepiece.ultimate.combat.HitDetector.hasLineOfSight(ctx.level(), eye, le.getBoundingBox().getCenter(), p)) continue;
            double score = dot * 2 - d / range;
            if (score > bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    private GomuFx() {}
}
