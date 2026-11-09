package com.onepiece.ultimate.fruit.phoenix;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Hybrid form: arms become wings of blue flame and legs become talons. Glides when falling (hold sneak to drop),
 * flaps with jump in mid-air (energy), +25% to phoenix techniques, no fall damage.
 */
public final class PhoenixHybridAbility extends TransformationAbility {
    PhoenixHybridAbility() {
        super("phoenix_hybrid", "zoan_form", 16, Properties.of(Activation.TOGGLE).cost(15).drain(0.03f).cooldown(100).mastery(15)
                .icon("phoenix_hybrid").color(0x3EC7FF));
        modifier(Attributes.MOVEMENT_SPEED, "ae7e61d2-8092-43b4-dfe0-5b6c7d8e9fa6", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.15f;
    }

    @Override
    public boolean ignoresFall() {
        return true;
    }

    @Override
    protected void activate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "phoenix_hybrid_activate");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_FLAME.get(), 1.2f, 1f);
    }

    @Override
    protected void tickActivation(AbilityContext ctx, int ticks) {
        PhoenixFx.body(ctx.level(), ctx.player(), 6);
        if (ticks == activationTicks()) {
            ServerPlayer p = ctx.player();
            ctx.level().sendParticles(RingParticleOptions.ground(0x3EC7FF, 3.5f, 14), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
            ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_CRY.get(), 1f, 1.2f);
        }
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        if (p.onGround()) ctx.data().abilities.airJumps = 0;
        Vec3 v = p.getDeltaMovement();
        if (!p.onGround() && !p.isInWater() && v.y < -0.12 && !p.isShiftKeyDown()) {
            Vec3 look = ctx.look().multiply(1, 0, 1);
            p.setDeltaMovement(v.x * 0.98 + look.x * 0.03, -0.12, v.z * 0.98 + look.z * 0.03);
            p.hurtMarked = true;
            p.fallDistance = 0;
            if (ticks % 4 == 0) PhoenixFx.body(ctx.level(), p, 2);
        } else if (ticks % 10 == 0) {
            PhoenixFx.body(ctx.level(), p, 1);
        }
    }

    @Override
    public boolean onAirJump(AbilityContext ctx) {
        var rt = ctx.data().abilities;
        if (rt.airJumps >= 6 || !ctx.data().consumeEnergy(6f)) return false;
        rt.airJumps++;
        ServerPlayer p = ctx.player();
        Vec3 look = ctx.look();
        p.setDeltaMovement(look.x * 0.7, 0.75, look.z * 0.7);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_WING.get(), 1f, 1f);
        ServerFx.animation(p, "phoenix_flap");
        PhoenixFx.body(ctx.level(), p, 10);
        return true;
    }
}
