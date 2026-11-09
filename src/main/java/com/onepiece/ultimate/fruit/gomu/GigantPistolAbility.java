package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Gear Third – Gigant Pistol: bite the thumb and blow air into the bones until the arm is gigantic, then fire.
 * The giant fist is slow but unstoppable – it hits <em>every</em> enemy in its path, bores a tunnel through
 * terrain and ends in a crater. Afterwards the body shrinks for a while (slowness &amp; weakness).
 */
public final class GigantPistolAbility extends Ability {
    GigantPistolAbility() {
        super("gomu_gigant_pistol", Properties.of(Activation.CHARGE).cost(45).cooldown(400).mastery(45)
                .charge(20, 30).channelAnimation("gomu_gigant_charge").color(0xFFB347));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.animation(ctx.player(), "gomu_gigant_bite");
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 1f, 1f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        if (ticks % 6 == 0) ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_INFLATE.get(), 0.7f, 0.8f + ticks * 0.02f);
        if (ticks == maxCharge()) ServerFx.shake(ctx.level(), p.position(), 12, 0.4f, 10);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        float charge = chargeTicks / (float) maxCharge();
        Vec3 dir = ctx.look();
        ServerFx.animation(player, "gomu_gigant_release");
        ServerFx.camera(player, CameraEffectPacket.Type.FLASH, 0.4f, 8);
        ServerFx.shake(level, player.position(), 30, 1.0f, 16);
        GomuFx.stretch(level, player.position(), 0.5f);

        float damage = 22f * (0.7f + 0.3f * charge);
        StretchFistEntity fist = StretchFistEntity.create(level, player, ctx.shoulder(1).add(dir.scale(1.0)), dir, 1)
                .speed(1.6)
                .range(24 + 10 * ctx.masteryFraction())
                .scale(5f)
                .pierce(64)
                .bore()
                .style(StretchFistEntity.Style.GIGANT)
                .onHitEntity(GomuFx.fistHit(ctx, dir, damage, 3.0f, 0.6f, 30, 2.0f, 3.5f, 0xFFB347, 5, true))
                .onHitBlock((hit, last) -> {
                    Vec3 at = hit.getLocation();
                    if (last) {
                        DestructionManager.crater(level, at, 4.0f, 0.9f, 4.0f, player);
                        DestructionManager.cracks(level, at, 4, 8, player);
                        ServerFx.impact(level, at, dir, 4f, 0xFFB347);
                        ServerFx.shake(level, at, 40, 1.2f, 18);
                    } else {
                        DestructionManager.tunnel(level, at, at.add(dir.scale(2.0)), 1.9f, 4.0f, player);
                        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.5, 0.5, 0.5, 0);
                    }
                    ServerFx.sound(level, at, ModSounds.RUMBLE.get(), 1.5f, 0.7f);
                });
        level.addFreshEntity(fist);

        // Side effect of Gear Third: the body shrinks.
        ctx.schedule(40, () -> {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true, true));
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.15, player.getZ(), 12, 0.5, 0.05, 0.5, 0.03);
        });
    }
}
