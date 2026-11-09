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

/** Boundman – Culverin: a fist compressed into the forearm and fired straight; pierces and throws its targets upward. */
public final class CulverinAbility extends Ability {
    CulverinAbility() {
        super("gomu_culverin", Properties.of(Activation.CHARGE).cost(18).cooldown(70).charge(6, 16)
                .channelAnimation("kong_gun_charge").sharesCooldownWith(OnePieceMod.id("gomu_whip")).icon("gomu_culverin").color(0xC03030));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 0.9f, 0.9f);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        float c = chargeTicks / (float) maxCharge();
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        Vec3 dir = ctx.look();
        ServerFx.animation(p, "gomu_jet_pistol");
        level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(1), dir, 1)
                .speed(4.2).range(22).scale(2.4f).pierce(2).style(StretchFistEntity.Style.GIGANT)
                .onHitEntity(GomuFx.fistHit(ctx, dir, 14f + 8f * c, 1.2f, 1.3f, 20, 1.8f, 2.2f, 0xFFFFFF, 3, true))
                .onHitBlock((hit, last) -> DestructionManager.crater(level, hit.getLocation(), 1.8f + c, 0.9f, 3f, p)));
        Vec3 front = p.getEyePosition().add(dir.scale(1.2));
        level.sendParticles(RingParticleOptions.facing(0xFFFFFF, 1.8f, 6, p.getXRot(), p.getYRot()), front.x, front.y, front.z, 1, 0, 0, 0, 0);
        GomuFx.stretch(level, p.position(), 0.8f);
    }
}
