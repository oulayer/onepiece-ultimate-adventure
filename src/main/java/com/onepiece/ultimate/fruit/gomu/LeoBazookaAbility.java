package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Boundman – Leo Bazooka: both inflated fists compress into the arms and fire as one devastating double strike. */
public final class LeoBazookaAbility extends Ability {
    LeoBazookaAbility() {
        super("gomu_leo_bazooka", Properties.of(Activation.CHARGE).cost(35).cooldown(220).charge(10, 26)
                .channelAnimation("gomu_bazooka_charge").sharesCooldownWith(OnePieceMod.id("gomu_bazooka")).icon("gomu_leo_bazooka").color(0xC03030));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 1f, 0.5f);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        float c = chargeTicks / (float) maxCharge();
        ServerFx.animation(ctx.player(), "gomu_bazooka_release");
        ctx.schedule(2, () -> {
            ServerPlayer p = ctx.player();
            if (!p.isAlive()) return;
            ServerLevel level = ctx.level();
            Vec3 look = ctx.look();
            for (LivingEntity t : HitDetector.arc(p, 5.5f + c * 2, 70f)) {
                HitInfo info = new HitInfo(p, (18f + 14f * c) * ctx.power()).type(ModDamageTypes.MARTIAL).direction(look)
                        .knockback(3.2f + 1.5f * c).lift(0.7f).stun(30).launch(2.8f + c).guardBreak();
                if (CombatHits.apply(info, t)) {
                    ServerFx.impact(level, t.getBoundingBox().getCenter(), look, 3.5f, 0xFFFFFF);
                    ctx.awardMastery(5);
                }
            }
            Vec3 eye = p.getEyePosition();
            Vec3 front = eye.add(look.scale(2.5));
            level.sendParticles(RingParticleOptions.facing(0xFFFFFF, 4f + 2f * c, 12, p.getXRot(), p.getYRot()), front.x, front.y, front.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, front.x, front.y, front.z, 1, 0, 0, 0, 0);
            BlockHitResult hr = level.clip(new ClipContext(eye, eye.add(look.scale(8)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (hr.getType() == HitResult.Type.BLOCK) DestructionManager.crater(level, hr.getLocation(), 2.8f + 1.5f * c, 0.9f, 3.5f, p);
            DestructionManager.tunnel(level, eye.add(look.scale(1.5)), eye.add(look.scale(7)), 1.6f, 3f, p);
            ServerFx.sound(level, front, ModSounds.HIT_HEAVY.get(), 2f, 0.5f);
            ServerFx.shake(level, front, 30, 1.1f, 14);
            ServerFx.camera(p, CameraEffectPacket.Type.HITSTOP, 1f, 4);
        });
    }
}
