package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.combat.ToonPhysics;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import com.onepiece.ultimate.combat.CombatManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Gear Fifth – Mogura Pistol (mole pistol): the fist dives into the ground, burrows toward the target leaving a trail
 * of churned earth, and erupts right under it as an uppercut that rubberises and throws it skyward.
 */
public final class MoguraPistolAbility extends Ability {
    MoguraPistolAbility() {
        super("gomu_mogura_pistol", Properties.of(Activation.INSTANT).cost(20).cooldown(70)
                .sharesCooldownWith(OnePieceMod.id("gomu_pistol")).icon("gomu_mogura_pistol").color(0xFFFFFF));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        ServerFx.animation(p, "mogura_pistol");
        LivingEntity target = GomuFx.bestTarget(ctx, 26, 55);
        Vec3 start = p.position();
        Vec3 end = target != null ? target.position()
                : start.add(ctx.look().multiply(1, 0, 1).normalize().scale(12));
        double dist = Math.max(1, end.subtract(start).horizontalDistance());
        int travel = (int) Math.ceil(dist / 2.5);
        ctx.schedule(3, () -> {
            ServerFx.sound(level, start, ModSounds.SLAM.get(), 0.9f, 1.4f);
            DestructionManager.crater(level, start.add(ctx.look().multiply(1, 0, 1).normalize().scale(1.2)).add(0, -0.5, 0), 1f, 0.6f, 1.2f, p);
        });
        for (int i = 1; i <= travel; i++) {
            final double k = i / (double) travel;
            ctx.schedule(3 + i, () -> {
                Vec3 at = start.add(end.subtract(start).scale(k));
                BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
                var state = level.getBlockState(below);
                if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + 0.1, at.z, 10, 0.3, 0.1, 0.3, 0.2);
                ServerFx.sound(level, at, ModSounds.RUMBLE.get(), 0.4f, 1.6f);
            });
        }
        ctx.schedule(4 + travel, () -> {
            if (!p.isAlive()) return;
            Vec3 target2 = target != null && target.isAlive() ? target.position() : end;
            Vec3 under = target2.add(0, -1.8, 0);
            DestructionManager.crater(level, target2.add(0, -0.4, 0), 1.6f, 0.7f, 1.5f, p);
            level.addFreshEntity(StretchFistEntity.create(level, p, under, new Vec3(0, 1, 0), 1)
                    .speed(1.8).range(40).scale(2.4f).pierce(3).style(StretchFistEntity.Style.DAWN).bore()
                    .onHitBlock((hit, last) -> {
                        var st = level.getBlockState(hit.getBlockPos());
                        if (!st.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st),
                                hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 12, 0.4, 0.4, 0.4, 0.25);
                    })
                    .onHitEntity(ToonFx.toonHit(ctx, new Vec3(0, 1, 0), 18f, 0.3f, 1.8f, 2f)));
            level.sendParticles(ParticleTypes.CLOUD, target2.x, target2.y + 0.3, target2.z, 25, 0.6, 0.2, 0.6, 0.05);
            ServerFx.sound(level, target2, ModSounds.GOMU_BOUNCE.get(), 1.5f, 0.8f);
            ServerFx.shake(level, target2, 16, 0.6f, 8);
        });
    }
}
