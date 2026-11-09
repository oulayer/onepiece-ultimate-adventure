package com.onepiece.ultimate.fruit.phoenix;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;

/**
 * Full Phoenix: the body becomes a great bird of blue flame. Real flight (fast), a smaller hitbox, +35% to phoenix
 * techniques and stronger regeneration (handled by the fruit while the form is active).
 */
public final class PhoenixFullAbility extends TransformationAbility {
    PhoenixFullAbility() {
        super("phoenix_full", "zoan_form", 20, Properties.of(Activation.TOGGLE).cost(25).drain(0.06f).cooldown(200).mastery(30)
                .icon("phoenix_full").color(0x6AE0FF));
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.35f;
    }

    @Override
    public boolean grantsFlight() {
        return true;
    }

    @Override
    public boolean ignoresFall() {
        return true;
    }

    @Override
    public EntityDimensions dimensions() {
        return EntityDimensions.scalable(1.1f, 1.1f);
    }

    @Override
    protected void activate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        if (!p.isCreative()) {
            p.getAbilities().setFlyingSpeed(0.09f);
            p.onUpdateAbilities();
        }
        ServerFx.animation(p, "phoenix_full_activate");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_FLAME.get(), 1.4f, 0.8f);
    }

    @Override
    protected void tickActivation(AbilityContext ctx, int ticks) {
        PhoenixFx.body(ctx.level(), ctx.player(), 10);
        if (ticks == activationTicks()) {
            ServerPlayer p = ctx.player();
            ctx.level().sendParticles(RingParticleOptions.ground(0x6AE0FF, 5f, 18), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
            PhoenixFx.flames(ctx.level(), p.position().add(0, 0.8, 0), 60, 1.2);
            ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_CRY.get(), 1.6f, 0.9f);
            ServerFx.camera(p, CameraEffectPacket.Type.FOV_PUNCH, 0.2f, 14);
        }
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        if (ticks % 3 == 0) PhoenixFx.body(ctx.level(), ctx.player(), 2);
        if (ticks % 200 == 0) ctx.awardMastery(2);
    }
}
