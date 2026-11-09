package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.destruction.DestructionRules;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Gomu Gomu no Whip: the leg stretches and sweeps a 200° arc in front over 5 ticks. Each tick checks only the
 * slice the leg is currently passing through, so enemies on the right are hit before those on the left and are
 * knocked sideways in the sweep direction. Mows down grass, flowers and leaves along the arc.
 */
public final class WhipAbility extends Ability {
    private static final int SWEEP_TICKS = 5;
    private static final float ARC = 200f;

    WhipAbility() {
        super("gomu_whip", Properties.of(Activation.INSTANT).cost(12).cooldown(50).mastery(5).color(GomuFx.RUBBER_COLOR));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        ServerFx.animation(player, "gomu_whip");
        GomuFx.stretch(ctx.level(), player.position(), 0.8f);
        float range = 7f + 3f * ctx.masteryFraction();
        float baseYaw = player.getYRot();
        Set<Integer> hit = new HashSet<>();
        for (int i = 0; i < SWEEP_TICKS; i++) {
            final int step = i;
            ctx.schedule(2 + i, () -> sweepSlice(ctx, baseYaw, step, range, hit));
        }
    }

    private void sweepSlice(AbilityContext ctx, float baseYaw, int step, float range, Set<Integer> hit) {
        ServerPlayer player = ctx.player();
        if (!player.isAlive()) return;
        ServerLevel level = ctx.level();
        // Sweep from the right (+ARC/2) to the left (-ARC/2) relative to where the player faced at activation.
        float slice = ARC / SWEEP_TICKS;
        float from = ARC / 2 - slice * step;
        float mid = from - slice / 2;
        double yawRad = Math.toRadians(baseYaw + mid);
        Vec3 sliceDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        // Sweep direction (tangent, pointing toward the left side) – enemies are knocked that way.
        Vec3 tangent = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad)).scale(-1);

        for (LivingEntity t : HitDetector.arc(player, sliceDir, range, slice + 12f)) {
            if (Math.abs(t.getY() - player.getY()) > 2.2 || !hit.add(t.getId())) continue;
            HitInfo info = new HitInfo(player, 6f * ctx.power()).type(ModDamageTypes.MARTIAL)
                    .direction(tangent.add(sliceDir.scale(0.4))).knockback(1.3f).lift(0.35f).stun(14);
            if (CombatHits.apply(info, t)) {
                ServerFx.impact(level, t.getBoundingBox().getCenter(), tangent, 1.1f, 0xFFFFFF);
                ServerFx.sound(level, t.position(), ModSounds.HIT_HEAVY.get(), 0.9f, 1.1f);
                ctx.awardMastery(2);
            }
        }

        // Visual leg arc + mowing vegetation along the slice.
        for (float d = 1.5f; d <= range; d += 0.75f) {
            Vec3 p = player.position().add(sliceDir.scale(d)).add(0, 0.5, 0);
            if (((int) (d * 4)) % 3 == 0) level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            if (DestructionRules.attackMayDestroy(level, player)) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos bp = BlockPos.containing(p.x, player.getY() + dy, p.z);
                    BlockState s = level.getBlockState(bp);
                    boolean soft = s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.CROPS);
                    if (soft && !s.isAir() && s.getFluidState().isEmpty() && DestructionRules.canDestroy(level, bp, s, player, 0.4f)) {
                        level.destroyBlock(bp, false, player);
                    }
                }
            }
        }
        if (step == 0) ServerFx.sound(level, player.position(), ModSounds.SWING_HEAVY.get(), 1f, 0.8f);
    }
}
