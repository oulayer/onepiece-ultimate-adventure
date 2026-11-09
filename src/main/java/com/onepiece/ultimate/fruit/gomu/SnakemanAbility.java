package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Gear Fourth – Snakeman: a leaner, faster form whose punches bend and chase their target.
 * +50% damage, +45% speed, +60% attack speed, one mid-air dash. Moves become Jet Culverin, Black Mamba, King Cobra.
 */
public final class SnakemanAbility extends GearFourthAbility {
    SnakemanAbility() {
        super("gomu_snakeman", 26, Properties.of(Activation.TOGGLE).cost(45).drain(0.24f).cooldown(1200).mastery(70)
                .icon("gomu_snakeman").color(0x8A2040));
        modifier(Attributes.MOVEMENT_SPEED, "7c4b3eaf-5d6f-4081-acbd-2e3f4a5b6c73", 0.45, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(Attributes.ATTACK_SPEED, "8d5c4fb0-6e70-4192-bdce-3f4a5b6c7d84", 0.6, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    protected String activationAnimation() {
        return "snakeman_activate";
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.5f;
    }

    @Override
    public float defenseMultiplier(OPPlayerData data) {
        return 0.8f;
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        super.tickActive(ctx, ticks);
        if (ticks % 4 == 0) {
            ServerPlayer p = ctx.player();
            ctx.level().sendParticles(new DustParticleOptions(new Vector3f(0.75f, 0.1f, 0.15f), 1.1f),
                    p.getX(), p.getY() + 1.5, p.getZ(), 2, 0.35, 0.2, 0.35, 0.01);
        }
    }

    @Override
    public boolean onAirJump(AbilityContext ctx) {
        var rt = ctx.data().abilities;
        if (rt.airJumps >= 1) return false;
        rt.airJumps++;
        ServerPlayer p = ctx.player();
        Vec3 look = ctx.look();
        p.setDeltaMovement(look.x * 1.4, Math.max(0.3, look.y * 1.0), look.z * 1.4);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerFx.sound(ctx.level(), p.position(), ModSounds.DASH.get(), 1f, 0.8f);
        ServerFx.animation(p, "dash_forward");
        return true;
    }
}
