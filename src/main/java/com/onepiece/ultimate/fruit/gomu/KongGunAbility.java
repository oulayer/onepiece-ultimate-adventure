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

/**
 * Boundman – Kong Gun: the fist is pulled back <em>into</em> the inflated arm, building pressure, then released as a
 * huge armament-hard fist. Sends targets flying through walls.
 */
public final class KongGunAbility extends Ability {
    KongGunAbility() {
        super("gomu_kong_gun", Properties.of(Activation.CHARGE).cost(20).cooldown(60).charge(6, 20)
                .channelAnimation("kong_gun_charge").sharesCooldownWith(OnePieceMod.id("gomu_pistol")).icon("gomu_kong_gun").color(0xC03030));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 1f, 0.6f);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        float c = chargeTicks / (float) maxCharge();
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        Vec3 dir = ctx.look();
        ServerFx.animation(p, "gomu_jet_pistol");
        StretchFistEntity fist = StretchFistEntity.create(level, p, ctx.shoulder(1), dir, 1)
                .speed(3.0).range(18).scale(3.2f).pierce(1).style(StretchFistEntity.Style.GIGANT)
                .onHitEntity(GomuFx.fistHit(ctx, dir, 26f * (0.6f + 0.4f * c), 3.0f, 0.5f, 25, 2.4f, 2.8f, 0xFFFFFF, 4, true))
                .onHitBlock((hit, last) -> {
                    DestructionManager.crater(level, hit.getLocation(), 2.4f + c, 0.9f, 3.5f, p);
                    ServerFx.impact(level, hit.getLocation(), dir, 2.8f, 0xFFFFFF);
                    ServerFx.shake(level, hit.getLocation(), 24, 0.9f, 12);
                });
        level.addFreshEntity(fist);
        Vec3 front = p.getEyePosition().add(dir.scale(1.5));
        level.sendParticles(RingParticleOptions.facing(0xFFFFFF, 2.5f, 8, p.getXRot(), p.getYRot()), front.x, front.y, front.z, 1, 0, 0, 0, 0);
        ServerFx.sound(level, front, ModSounds.HIT_HEAVY.get(), 1.4f, 0.6f);
        GomuFx.stretch(level, p.position(), 0.6f);
        ServerFx.camera(p, CameraEffectPacket.Type.HITSTOP, 1f, 2);
    }
}
