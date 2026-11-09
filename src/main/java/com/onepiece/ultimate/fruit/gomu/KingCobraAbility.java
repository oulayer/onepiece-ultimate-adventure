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

/** Snakeman – King Cobra: a giant fist that winds through the air and runs its prey down. */
public final class KingCobraAbility extends Ability {
    KingCobraAbility() {
        super("gomu_king_cobra", Properties.of(Activation.CHARGE).cost(30).cooldown(260).charge(10, 24)
                .channelAnimation("emission_charge").sharesCooldownWith(OnePieceMod.id("gomu_bazooka")).icon("gomu_king_cobra").color(0x8A2040));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 1f, 0.8f);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        float c = chargeTicks / (float) maxCharge();
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        ServerFx.animation(p, "emission_release");
        LivingEntity target = GomuFx.bestTarget(ctx, 32, 45);
        Vec3 dir = ctx.look().add(0, 0.2, 0).normalize();
        level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(1), dir, 1)
                .speed(3.5).range(32).scale(3.5f).homing(target, 0.2).style(StretchFistEntity.Style.GIGANT)
                .onHitEntity(GomuFx.fistHit(ctx, ctx.look(), (22f + 14f * c), 2.8f, 0.6f, 30, 2.6f, 3.2f, 0xFF5070, 5, true))
                .onHitBlock((hit, last) -> {
                    DestructionManager.crater(level, hit.getLocation(), 3f, 0.9f, 3.5f, p);
                    ServerFx.shake(level, hit.getLocation(), 30, 1f, 12);
                }));
        GomuFx.stretch(level, p.position(), 0.8f);
        ServerFx.camera(p, CameraEffectPacket.Type.HITSTOP, 1f, 3);
    }
}
