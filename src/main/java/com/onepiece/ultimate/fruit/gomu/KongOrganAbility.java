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

/** Boundman – Kong Organ: a barrage of giant armament fists. */
public final class KongOrganAbility extends Ability {
    KongOrganAbility() {
        super("gomu_kong_organ", Properties.of(Activation.HOLD).cost(20).drain(1.0f).maxHold(40).cooldown(180)
                .channelAnimation("gomu_gatling").sharesCooldownWith(OnePieceMod.id("gomu_gatling")).icon("gomu_kong_organ").color(0xC03030));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        GomuFx.stretch(ctx.level(), ctx.player().position(), 0.7f);
    }

    @Override
    public void onHoldTick(AbilityContext ctx, int ticks) {
        if (ticks < 3 || ticks % 2 != 0) return;
        ServerLevel level = ctx.level();
        var r = ctx.player().getRandom();
        int side = (ticks / 2) % 2 == 0 ? 1 : -1;
        Vec3 look = ctx.look();
        Vec3 dir = look.add((r.nextFloat() - 0.5) * 0.35, (r.nextFloat() - 0.5) * 0.25, (r.nextFloat() - 0.5) * 0.35).normalize();
        level.addFreshEntity(StretchFistEntity.create(level, ctx.player(), ctx.shoulder(side), dir, side)
                .speed(3.2).range(13).scale(2.2f).style(StretchFistEntity.Style.GIGANT)
                .onHitEntity(GomuFx.fistHit(ctx, look, 8f, 0.5f, 0.05f, 8, 0f, 1.4f, 0xFFFFFF, 1, false))
                .onHitBlock((hit, last) -> DestructionManager.crater(level, hit.getLocation(), 1.4f, 0.8f, 2.5f, ctx.player())));
        if (ticks % 4 == 0) ServerFx.sound(level, ctx.player().position(), ModSounds.SWING_HEAVY.get(), 0.7f, 0.8f + r.nextFloat() * 0.2f);
    }
}
