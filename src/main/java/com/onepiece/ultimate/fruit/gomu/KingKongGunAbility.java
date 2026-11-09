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
 * Boundman – King Kong Gun: the arm is blown up to colossal size and fired. It ploughs through everything in its path,
 * bores through terrain and ends in a huge crater – and it spends what's left of Gear Fourth.
 */
public final class KingKongGunAbility extends Ability {
    KingKongGunAbility() {
        super("gomu_king_kong_gun", Properties.of(Activation.CHARGE).cost(40).cooldown(1200).charge(30, 50)
                .channelAnimation("gomu_gigant_charge").sharesCooldownWith(OnePieceMod.id("gomu_gigant_pistol")).icon("gomu_king_kong_gun").color(0xFF4040));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.animation(ctx.player(), "gomu_gigant_bite");
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 1.2f, 0.6f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        if (ticks % 5 == 0) ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 0.9f, 0.5f + ticks * 0.01f);
        if (ticks % 10 == 0) ServerFx.shake(ctx.level(), ctx.player().position(), 20, 0.3f + ticks * 0.01f, 10);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        float c = chargeTicks / (float) maxCharge();
        Vec3 dir = ctx.look();
        ServerFx.animation(p, "gomu_gigant_release");
        ServerFx.camera(p, CameraEffectPacket.Type.FLASH, 0.6f, 10);
        ServerFx.shake(level, p.position(), 50, 1.4f, 24);
        StretchFistEntity fist = StretchFistEntity.create(level, p, ctx.shoulder(1).add(dir.scale(2)), dir, 1)
                .speed(1.8).range(32).scale(10f).pierce(99).bore().style(StretchFistEntity.Style.GIGANT)
                .onHitEntity(GomuFx.fistHit(ctx, dir, 55f * (0.7f + 0.3f * c), 4f, 0.8f, 40, 3f, 5f, 0xFFFFFF, 8, true))
                .onHitBlock((hit, last) -> {
                    Vec3 at = hit.getLocation();
                    if (last) {
                        DestructionManager.crater(level, at, 7f, 0.9f, 6f, p);
                        DestructionManager.cracks(level, at, 7, 12, p);
                        ServerFx.impact(level, at, dir, 7f, 0xFFFFFF);
                        ServerFx.shake(level, at, 70, 1.5f, 28);
                    } else {
                        DestructionManager.tunnel(level, at, at.add(dir.scale(2.5)), 3.6f, 6f, p);
                    }
                    ServerFx.sound(level, at, ModSounds.RUMBLE.get(), 2.5f, 0.6f);
                });
        level.addFreshEntity(fist);
        // King Kong Gun empties Gear Fourth.
        ctx.schedule(40, () -> AbilityManager.endToggle(p, ctx.data(), GomuAbilities.BOUNDMAN, true));
    }
}
