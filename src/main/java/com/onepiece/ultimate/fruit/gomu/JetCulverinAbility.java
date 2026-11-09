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

/** Snakeman – Jet Culverin: a fist that bends around obstacles and chases the target you aim near. */
public final class JetCulverinAbility extends Ability {
    JetCulverinAbility() {
        super("gomu_jet_culverin", Properties.of(Activation.INSTANT).cost(16).cooldown(40)
                .sharesCooldownWith(OnePieceMod.id("gomu_pistol")).icon("gomu_jet_culverin").color(0x8A2040));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        ServerFx.animation(p, "gomu_jet_pistol");
        LivingEntity target = GomuFx.bestTarget(ctx, 30, 40);
        Vec3 dir = ctx.look();
        if (target != null) dir = dir.add(target.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().scale(0.3)).normalize();
        level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(1), dir.add(0, 0.25, 0), 1)
                .speed(4.5).range(30).homing(target, 0.28).serpentine(0.6, 0.8, 0).style(StretchFistEntity.Style.JET)
                .onHitEntity(GomuFx.fistHit(ctx, ctx.look(), 18f, 2f, 0.3f, 16, 1.6f, 2f, 0xFF5070, 3, false))
                .onTravel(pos -> level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.8f, 0.1f, 0.2f), 1.2f),
                        pos.x, pos.y + 0.3, pos.z, 2, 0.1, 0.1, 0.1, 0))
                .onHitBlock((hit, last) -> DestructionManager.crater(level, hit.getLocation(), 1.6f, 0.8f, 2.6f, p)));
        GomuFx.stretch(level, p.position(), 1.5f);
    }
}
