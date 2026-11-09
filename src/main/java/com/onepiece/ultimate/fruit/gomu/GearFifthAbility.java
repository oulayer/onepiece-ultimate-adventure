package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Gear Fifth: the drums of liberation. The body turns white and moves with cartoon freedom – everything it hits turns
 * to rubber and bounces. +60% damage, −40% damage taken, huge jumps and five mid-air hops, no fall damage.
 * Moves become Dawn Gun, Dawn Whip, Dawn Rocket and Bajrang Gun. Afterwards the body is drained (heavy fatigue).
 * Requires full mastery (100).
 */
public final class GearFifthAbility extends TransformationAbility {
    GearFifthAbility() {
        super("gomu_gear_fifth", "gomu_gear", 40, Properties.of(Activation.TOGGLE).cost(60).drain(0.3f).cooldown(2400).mastery(100)
                .icon("gomu_gear_fifth").color(0xF4F4F4));
        modifier(Attributes.MOVEMENT_SPEED, "9e6d50c1-7f81-42a3-cedf-4a5b6c7d8e95", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static final java.util.UUID GIANT_STEP = java.util.UUID.fromString("b0f1e2d3-4c5b-4a69-8e7f-6a5b4c3d2e1f");
    private static final java.util.UUID GIANT_REACH = java.util.UUID.fromString("c1a2b3d4-5e6f-4789-9a0b-1c2d3e4f5a6b");
    private static final java.util.UUID GIANT_ENTITY_REACH = java.util.UUID.fromString("d2b3c4e5-6f70-4891-ab1c-2d3e4f5a6b7c");

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return data.giant() ? 2.4f : 1.6f;
    }

    /** Giant body: step over walls and reach further (removed again when it shrinks). */
    static void applyGiantModifiers(ServerPlayer p, boolean on) {
        var step = p.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
        var reach = p.getAttribute(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
        var entityReach = p.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        if (on) {
            com.onepiece.ultimate.progression.ProgressionManager.setModifier(step, GIANT_STEP, "gigant", 1.5, AttributeModifier.Operation.ADDITION);
            com.onepiece.ultimate.progression.ProgressionManager.setModifier(reach, GIANT_REACH, "gigant", 3, AttributeModifier.Operation.ADDITION);
            com.onepiece.ultimate.progression.ProgressionManager.setModifier(entityReach, GIANT_ENTITY_REACH, "gigant", 3, AttributeModifier.Operation.ADDITION);
        } else {
            if (step != null) step.removeModifier(GIANT_STEP);
            if (reach != null) reach.removeModifier(GIANT_REACH);
            if (entityReach != null) entityReach.removeModifier(GIANT_ENTITY_REACH);
        }
    }

    static void shrink(ServerPlayer p, OPPlayerData d) {
        if (!d.giant()) return;
        d.setGiant(false);
        applyGiantModifiers(p, false);
        p.refreshDimensions();
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 40, 1, 1, 1, 0.05);
        ServerFx.sound(p.serverLevel(), p.position(), ModSounds.GOMU_SNAP.get(), 1.2f, 0.5f);
    }

    @Override
    public float defenseMultiplier(OPPlayerData data) {
        return 0.6f;
    }

    @Override
    public boolean ignoresFall() {
        return true;
    }

    @Override
    protected void activate(AbilityContext ctx) {
        ServerFx.animation(ctx.player(), "gear_fifth_activate");
    }

    @Override
    protected void tickActivation(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        if (ticks % 10 == 1) { // heartbeat: the drums of liberation
            ServerFx.sound(level, p.position(), ModSounds.GOMU_GEAR_FIFTH.get(), 1.3f, 1f);
            level.sendParticles(RingParticleOptions.ground(0xFFFFFF, 2f + ticks * 0.06f, 10), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
            ServerFx.shake(level, p.position(), 16, 0.2f + ticks * 0.01f, 6);
        }
        if (ticks == activationTicks()) {
            Vec3 c = p.position();
            level.sendParticles(RingParticleOptions.ground(0xFFFFFF, 7f, 20), c.x, c.y + 0.1, c.z, 1, 0, 0, 0, 0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, c.x, c.y + 1.5, c.z, 60, 1.2, 0.6, 1.2, 0.08);
            ServerFx.camera(p, CameraEffectPacket.Type.FLASH, 0.8f, 14);
            ServerFx.shake(level, c, 40, 1f, 20);
            ServerFx.sound(level, c, ModSounds.GOMU_BOUNCE.get(), 1.5f, 0.6f);
        }
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        OPPlayerData data = ctx.data();
        if (p.onGround()) data.abilities.airJumps = 0;
        if (data.giant()) {
            if (ctx.now() >= data.abilities.giantUntil) shrink(p, data);
            else if (ticks % 10 == 0 && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                // the ground shakes under every giant footstep
                com.onepiece.ultimate.destruction.DestructionManager.shockwave(ctx.level(), p.position(), 3.5f, 4, 3f, 0.5f, 0.6f, p);
                ServerFx.shake(ctx.level(), p.position(), 20, 0.4f, 6);
                ServerFx.sound(ctx.level(), p.position(), ModSounds.SLAM.get(), 0.8f, 1.2f);
            }
        }
        if (ticks % 40 == 0) {
            ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_GEAR_FIFTH.get(), 0.6f, 1f);
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 2, false, false, false));
        }
        if (ticks % 200 == 0) ctx.awardMastery(2);
    }

    @Override
    public boolean onAirJump(AbilityContext ctx) {
        var rt = ctx.data().abilities;
        if (rt.airJumps >= 5) return false;
        rt.airJumps++;
        ServerPlayer p = ctx.player();
        Vec3 look = ctx.look().multiply(1, 0, 1).normalize();
        p.setDeltaMovement(look.x * 0.8, 1.0, look.z * 0.8);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_BOUNCE.get(), 1f, 1.3f);
        ctx.level().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
        ServerFx.animation(p, "gomu_bounce");
        return true;
    }

    @Override
    protected void deactivate(AbilityContext ctx, boolean interrupted) {
        ServerPlayer p = ctx.player();
        shrink(p, ctx.data());
        p.removeEffect(MobEffects.JUMP);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 800, 2, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 800, 2, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 800, 1, false, true, true));
        ctx.data().setEnergy(0);
        p.displayClientMessage(Component.translatable("message.onepiece.gear_fifth_end").withStyle(ChatFormatting.GRAY), true);
        ServerFx.animation(p, "gear_fourth_deflate");
    }
}
