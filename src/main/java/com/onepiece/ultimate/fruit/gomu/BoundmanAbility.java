package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Gear Fourth – Boundman: a hulking, armament-black rubber body that never stops bouncing.
 * +80% damage, −40% damage taken, heavy knockback resistance, bigger hitbox, up to 3 mid-air bounces.
 * Moves become Kong Gun, Kong Organ, Leo Bazooka and King Kong Gun.
 */
public final class BoundmanAbility extends GearFourthAbility {
    BoundmanAbility() {
        super("gomu_boundman", 30, Properties.of(Activation.TOGGLE).cost(50).drain(0.28f).cooldown(1200).mastery(60)
                .icon("gomu_boundman").color(0xC03030));
        modifier(Attributes.MOVEMENT_SPEED, "5a2f1c8e-3b4d-4e6f-8a9b-0c1d2e3f4a51", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(Attributes.KNOCKBACK_RESISTANCE, "6b3a2d9f-4c5e-4f70-9bac-1d2e3f4a5b62", 0.7, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.8f;
    }

    @Override
    public float defenseMultiplier(OPPlayerData data) {
        return 0.6f;
    }

    @Override
    public EntityDimensions dimensions() {
        return EntityDimensions.scalable(0.95f, 2.3f);
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        super.tickActive(ctx, ticks);
        ServerPlayer p = ctx.player();
        // Boundman never stands still: it bounces every time it lands (sneak to stay grounded).
        if (p.onGround() && !p.isShiftKeyDown() && ticks > 2) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.38, v.z);
            p.hurtMarked = true;
        }
    }

    @Override
    public boolean onAirJump(AbilityContext ctx) {
        var rt = ctx.data().abilities;
        if (rt.airJumps >= 3) return false;
        rt.airJumps++;
        ServerPlayer p = ctx.player();
        Vec3 look = ctx.look().multiply(1, 0, 1).normalize();
        p.setDeltaMovement(look.x * 0.6, 0.85, look.z * 0.6);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_BOUNCE.get(), 1f, 0.7f);
        ctx.level().sendParticles(RingParticleOptions.ground(0xFFFFFF, 1.8f, 8), p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
        ServerFx.animation(p, "gomu_bounce");
        return true;
    }
}
