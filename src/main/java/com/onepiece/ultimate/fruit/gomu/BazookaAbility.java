package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Gomu Gomu no Bazooka: charge by stretching both arms far behind you, release for a double palm strike.
 * Breaks guards, launches enemies (who then take extra damage if they hit a wall) and blasts a hole in terrain
 * in front of you. Damage, reach and blast size scale with charge.
 */
public final class BazookaAbility extends Ability {
    private final boolean jet;

    BazookaAbility(boolean jet) {
        super(jet ? "gomu_jet_bazooka" : "gomu_bazooka", Properties.of(Activation.CHARGE)
                .cost(25).cooldown(jet ? 120 : 160).mastery(22)
                .charge(jet ? 4 : 8, jet ? 14 : 24)
                .channelAnimation("gomu_bazooka_charge")
                .sharesCooldownWith(OnePieceMod.id("gomu_bazooka"))
                .color(jet ? GomuFx.JET_COLOR : GomuFx.RUBBER_COLOR));
        this.jet = jet;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        GomuFx.stretch(ctx.level(), ctx.player().position(), 0.7f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        if (ticks == maxCharge()) {
            ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.CHARGE.get(), 0.9f, 1.5f);
        }
        if (ticks % 4 == 0) {
            Vec3 back = ctx.player().position().add(ctx.look().scale(-1.2)).add(0, 1.2, 0);
            ctx.level().sendParticles(ParticleTypes.CRIT, back.x, back.y, back.z, 2, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        float charge = chargeTicks / (float) maxCharge();
        ServerFx.animation(ctx.player(), "gomu_bazooka_release");
        GomuFx.stretch(ctx.level(), ctx.player().position(), 1.3f);
        ctx.schedule(2, () -> strike(ctx, charge));
    }

    private void strike(AbilityContext ctx, float charge) {
        ServerPlayer player = ctx.player();
        if (!player.isAlive()) return;
        ServerLevel level = ctx.level();
        Vec3 look = ctx.look();
        float range = 4.5f + charge * 2f;
        float dmg = (9f + 9f * charge) * (jet ? 1.25f : 1f) * ctx.power();

        boolean any = false;
        for (LivingEntity t : HitDetector.arc(player, range, 65f)) {
            HitInfo info = new HitInfo(player, dmg).type(ModDamageTypes.MARTIAL).direction(look)
                    .knockback(2.2f + 1.5f * charge).lift(0.5f).stun(20).launch(1.5f + charge).guardBreak();
            if (CombatHits.apply(info, t)) {
                any = true;
                ServerFx.impact(level, t.getBoundingBox().getCenter(), look, 2.2f, jet ? GomuFx.JET_COLOR : 0xFFFFFF);
                ctx.awardMastery(3);
            }
        }

        Vec3 eye = player.getEyePosition();
        Vec3 front = eye.add(look.scale(2.2));
        level.sendParticles(RingParticleOptions.facing(0xFFFFFF, 2.5f + charge * 2f, 10, player.getXRot(), player.getYRot()),
                front.x, front.y - 0.3, front.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y - 0.3, front.z, 2, 0.3, 0.3, 0.3, 0);
        ServerFx.sound(level, front, ModSounds.HIT_HEAVY.get(), 1.4f, 0.6f);
        ServerFx.shake(level, front, 20, 0.6f + charge * 0.6f, 10);
        ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, any ? 4 : 2);

        // Palm blast hits terrain in front.
        BlockHitResult hr = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hr.getType() == HitResult.Type.BLOCK) {
            DestructionManager.crater(level, hr.getLocation(), 1.4f + charge * 1.4f, 0.9f, 2.2f, player);
        }
    }
}
