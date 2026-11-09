package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.progression.ProgressionManager;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Gear Second: pumps blood through the legs at high speed. While active: +35% damage, +35% movement and attack
 * speed, faster and further dashes, Jet variants of Pistol/Gatling/Bazooka, constant steam and red skin.
 * Drains energy; ending it leaves the body fatigued for a few seconds.
 */
public final class GearSecondAbility extends Ability {
    private static final UUID SPEED_ID = UUID.fromString("c3a8d1e2-6f47-4b9a-8c15-2e7d9f0a1b33");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("d4b9e2f3-7a58-4cab-9d26-3f8eaf1b2c44");

    GearSecondAbility() {
        super("gomu_gear_second", Properties.of(Activation.TOGGLE).cost(25).drain(0.12f).cooldown(300).mastery(30)
                .icon("gomu_gear_second").color(0xFF5A5A));
    }

    @Override
    public float damageMultiplier(com.onepiece.ultimate.data.OPPlayerData data) {
        return 1.35f;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        ServerFx.animation(player, "gear_second_activate");
        ServerFx.sound(level, player.position(), ModSounds.GOMU_GEAR_SECOND.get(), 1.2f, 1f);
        ServerFx.camera(player, CameraEffectPacket.Type.FOV_PUNCH, 0.15f, 12);
        ServerFx.shake(level, player.position(), 10, 0.3f, 12);
        // Pumping: steam bursts in rhythm with the animation, then a final blast.
        for (int i = 0; i < 3; i++) ctx.schedule(2 + i * 4, () -> GomuFx.steamBurst(level, player, 8));
        ctx.schedule(14, () -> {
            GomuFx.steamBurst(level, player, 30);
            Vec3 p = player.position();
            level.sendParticles(RingParticleOptions.ground(0xFF7A6A, 3.5f, 14), p.x, p.y + 0.1, p.z, 1, 0, 0, 0, 0);
        });
        ProgressionManager.setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, "gear second", 0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
        ProgressionManager.setModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID, "gear second", 0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void onToggleTick(AbilityContext ctx, int ticks) {
        if (ticks % 2 == 0) {
            ServerPlayer p = ctx.player();
            ctx.level().sendParticles(ModParticles.STEAM.get(), p.getX(), p.getY() + 0.4 + p.getRandom().nextFloat() * 1.3, p.getZ(),
                    1, 0.3, 0.2, 0.3, 0.01);
        }
        ctx.awardMastery(ticks % 200 == 0 ? 1 : 0);
    }

    @Override
    public void onEnd(AbilityContext ctx, boolean interrupted) {
        ServerPlayer player = ctx.player();
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        var atk = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atk != null) atk.removeModifier(ATTACK_SPEED_ID);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
        GomuFx.steamBurst(ctx.level(), player, 12);
    }

    /** Removes leftover modifiers (e.g. after a crash or when the toggle got cleared without onEnd). */
    public static void cleanup(ServerPlayer player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        var atk = player.getAttribute(Attributes.ATTACK_SPEED);
        if (atk != null) atk.removeModifier(ATTACK_SPEED_ID);
    }
}
