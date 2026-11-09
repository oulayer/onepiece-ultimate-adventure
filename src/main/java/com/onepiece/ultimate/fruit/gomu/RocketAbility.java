package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Gomu Gomu no Rocket: both arms shoot out and grab the block you're looking at (up to 32–48 blocks), then the
 * rubber snaps and slingshots you there. You ram through enemies along the way. Without anything to grab you
 * launch straight up.
 */
public final class RocketAbility extends Ability {
    private static final double ARM_SPEED = 4.0;

    RocketAbility() {
        super("gomu_rocket", Properties.of(Activation.INSTANT).cost(15).cooldown(60).mastery(15).color(GomuFx.RUBBER_COLOR));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        double range = 32 + 16 * ctx.masteryFraction();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hr = level.clip(new ClipContext(eye, eye.add(ctx.look().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        ServerFx.animation(player, "gomu_rocket");
        GomuFx.stretch(level, player.position(), 0.9f);

        if (hr.getType() != HitResult.Type.BLOCK) {
            // Nothing to grab: spring upward.
            ctx.schedule(3, () -> launch(ctx, new Vec3(ctx.look().x * 0.6, 1.0, ctx.look().z * 0.6).normalize(), 1.9));
            return;
        }
        Vec3 anchor = hr.getLocation();
        for (int side : new int[]{1, -1}) {
            Vec3 from = ctx.shoulder(side);
            StretchFistEntity arm = StretchFistEntity.create(level, player, from, anchor.subtract(from), side)
                    .speed(ARM_SPEED).range(range + 2).scale(0.85f);
            level.addFreshEntity(arm);
        }
        int travel = (int) Math.ceil(anchor.distanceTo(eye) / ARM_SPEED) + 1;
        ctx.schedule(travel, () -> {
            Vec3 to = anchor.subtract(player.position());
            double dist = to.length();
            launch(ctx, to.normalize(), Math.min(3.6, 1.4 + dist * 0.07));
            ServerFx.sound(level, anchor, ModSounds.GOMU_SNAP.get(), 1f, 1f);
        });
    }

    private void launch(AbilityContext ctx, Vec3 dir, double speed) {
        ServerPlayer player = ctx.player();
        if (!player.isAlive()) return;
        player.setDeltaMovement(dir.scale(speed).add(0, 0.25, 0));
        player.hurtMarked = true;
        player.fallDistance = 0;
        ServerFx.camera(player, CameraEffectPacket.Type.FOV_PUNCH, 0.3f, 10);
        ServerFx.sound(ctx.level(), player.position(), ModSounds.DASH.get(), 1f, 0.7f);
        // Ram through enemies during the flight.
        Set<Integer> hit = new HashSet<>();
        for (int t = 1; t <= 14; t++) {
            ctx.schedule(t, () -> ram(ctx, hit));
        }
    }

    private void ram(AbilityContext ctx, Set<Integer> hit) {
        ServerPlayer player = ctx.player();
        if (!player.isAlive() || player.getDeltaMovement().lengthSqr() < 0.3) return;
        ServerLevel level = ctx.level();
        Vec3 v = player.getDeltaMovement();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.8, player.getZ(), 1, 0.1, 0.1, 0.1, 0);
        for (LivingEntity t : HitDetector.arc(player, v, 2.0f, 360f)) {
            if (!hit.add(t.getId())) continue;
            HitInfo info = new HitInfo(player, 7f * ctx.power()).type(ModDamageTypes.MARTIAL)
                    .direction(v).knockback(1.6f).lift(0.5f).stun(16).launch(1.2f);
            if (CombatHits.apply(info, t)) {
                ServerFx.impact(level, t.getBoundingBox().getCenter(), v, 1.5f, 0xFFFFFF);
                ServerFx.sound(level, t.position(), ModSounds.HIT_HEAVY.get(), 1f, 0.9f);
                ctx.awardMastery(3);
            }
        }
    }
}
