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

import java.util.List;

/**
 * Snakeman – Hydra: six fists leave in quick succession, each weaving its own serpentine path to a different enemy
 * in front (doubling up when there are fewer targets).
 */
public final class HydraAbility extends Ability {
    private static final int HEADS = 6;

    HydraAbility() {
        super("gomu_hydra", Properties.of(Activation.INSTANT).cost(30).cooldown(160)
                .sharesCooldownWith(OnePieceMod.id("gomu_whip")).icon("gomu_hydra").color(0x8A2040));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "gomu_jet_gatling");
        List<LivingEntity> targets = HitDetector.arc(p, 22f, 120f);
        for (int i = 0; i < HEADS; i++) {
            final int head = i;
            ctx.schedule(1 + i, () -> {
                if (!p.isAlive()) return;
                ServerLevel level = ctx.level();
                LivingEntity target = targets.isEmpty() ? null : targets.get(head % targets.size());
                int side = head % 2 == 0 ? 1 : -1;
                Vec3 look = ctx.look();
                Vec3 dir = look.add(new Vec3(-look.z, 0, look.x).scale(side * (0.3 + 0.15 * head))).add(0, 0.15, 0).normalize();
                level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(side), dir, side)
                        .speed(3.6).range(24).homing(target, 0.3).serpentine(0.9, 0.9, head * 1.1).style(StretchFistEntity.Style.JET)
                        .onHitEntity(GomuFx.fistHit(ctx, look, 12f, 1f, 0.3f, 15, 0f, 1.3f, 0xFF5070, 2, false)));
                ServerFx.sound(level, p.position(), ModSounds.GOMU_STRETCH.get(), 0.7f, 1.2f + head * 0.08f);
            });
        }
    }
}
