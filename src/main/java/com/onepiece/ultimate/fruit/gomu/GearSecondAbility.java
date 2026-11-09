package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Gear Second: pumps blood through the legs at high speed. +35% damage, movement and attack speed, faster dashes,
 * Jet versions of Pistol/Gatling/Bazooka, constant steam and red skin. Drains energy; leaves the body fatigued.
 */
public final class GearSecondAbility extends TransformationAbility {
    GearSecondAbility() {
        super("gomu_gear_second", "gomu_gear", 14, Properties.of(Activation.TOGGLE).cost(25).drain(0.12f).cooldown(300).mastery(30)
                .icon("gomu_gear_second").color(0xFF5A5A));
        modifier(Attributes.MOVEMENT_SPEED, "c3a8d1e2-6f47-4b9a-8c15-2e7d9f0a1b33", 0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(Attributes.ATTACK_SPEED, "d4b9e2f3-7a58-4cab-9d26-3f8eaf1b2c44", 0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.35f;
    }

    @Override
    protected void activate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        ServerFx.animation(player, "gear_second_activate");
        ServerFx.sound(level, player.position(), ModSounds.GOMU_GEAR_SECOND.get(), 1.2f, 1f);
        ServerFx.camera(player, CameraEffectPacket.Type.FOV_PUNCH, 0.15f, 12);
        ServerFx.shake(level, player.position(), 10, 0.3f, 12);
    }

    @Override
    protected void tickActivation(AbilityContext ctx, int ticks) {
        if (ticks % 4 == 2) GomuFx.steamBurst(ctx.level(), ctx.player(), 8);
        if (ticks == activationTicks()) {
            ServerPlayer p = ctx.player();
            GomuFx.steamBurst(ctx.level(), p, 30);
            Vec3 c = p.position();
            ctx.level().sendParticles(RingParticleOptions.ground(0xFF7A6A, 3.5f, 14), c.x, c.y + 0.1, c.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        if (ticks % 2 == 0) {
            ServerPlayer p = ctx.player();
            ctx.level().sendParticles(ModParticles.STEAM.get(), p.getX(), p.getY() + 0.4 + p.getRandom().nextFloat() * 1.3, p.getZ(),
                    1, 0.3, 0.2, 0.3, 0.01);
        }
        if (ticks % 200 == 0) ctx.awardMastery(1);
    }

    @Override
    protected void deactivate(AbilityContext ctx, boolean interrupted) {
        ServerPlayer player = ctx.player();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
        GomuFx.steamBurst(ctx.level(), player, 12);
    }
}
