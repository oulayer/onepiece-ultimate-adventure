package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Gomu Gomu no Gatling: hold to unleash a barrage of short-range fists alternating arms with random spread.
 * Each fist applies a short stun, so a caught enemy stays locked in the barrage. Drains energy while held.
 * Jet Gatling (Gear Second) throws a fist every tick.
 */
public final class GatlingAbility extends Ability {
    private final boolean jet;
    @Nullable private final Ability jetVariant;

    GatlingAbility(boolean jet, @Nullable Ability jetVariant) {
        super(jet ? "gomu_jet_gatling" : "gomu_gatling", Properties.of(Activation.HOLD)
                .cost(15).drain(jet ? 0.8f : 0.6f).maxHold(jet ? 50 : 60).cooldown(120).mastery(10)
                .channelAnimation(jet ? "gomu_jet_gatling" : "gomu_gatling")
                .sharesCooldownWith(OnePieceMod.id("gomu_gatling"))
                .color(jet ? GomuFx.JET_COLOR : GomuFx.RUBBER_COLOR));
        this.jet = jet;
        this.jetVariant = jetVariant;
    }

    @Override
    public Ability resolve(OPPlayerData data) {
        return jetVariant != null && data.isToggleActive(GomuAbilities.GEAR_SECOND.id()) ? jetVariant : this;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        GomuFx.stretch(ctx.level(), ctx.player().position(), 1.2f);
    }

    @Override
    public void onHoldTick(AbilityContext ctx, int ticks) {
        if (ticks < 3) return; // short wind-up
        if (!jet && ticks % 2 != 0) return;
        ServerLevel level = ctx.level();
        RandomSource r = ctx.player().getRandom();
        int side = (ticks / (jet ? 1 : 2)) % 2 == 0 ? 1 : -1;
        Vec3 look = ctx.look();
        Vec3 dir = look.add((r.nextFloat() - 0.5f) * 0.28, (r.nextFloat() - 0.5f) * 0.22, (r.nextFloat() - 0.5f) * 0.28).normalize();
        StretchFistEntity fist = StretchFistEntity.create(level, ctx.player(), ctx.shoulder(side), dir, side)
                .speed(jet ? 4.5 : 3.0)
                .range(7 + 3 * ctx.masteryFraction())
                .scale(0.9f)
                .style(jet ? StretchFistEntity.Style.JET : StretchFistEntity.Style.NORMAL)
                .onHitEntity(GomuFx.fistHit(ctx, look, jet ? 3.4f : 2.6f, 0.18f, 0.02f, 6, 0f, 0.6f,
                        jet ? GomuFx.JET_COLOR : 0xFFFFFF, ticks % 6 == 0 ? 1 : 0, false));
        level.addFreshEntity(fist);
        if (ticks % 4 == 0) ServerFx.sound(level, ctx.player().position(), ModSounds.SWING_LIGHT.get(), 0.6f, 1.3f + r.nextFloat() * 0.3f);
        if (jet && ticks % 3 == 0) {
            Vec3 p = ctx.player().position();
            level.sendParticles(ModParticles.STEAM.get(), p.x, p.y + 1.2, p.z, 2, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void onEnd(AbilityContext ctx, boolean interrupted) {
        if (!interrupted) ServerFx.animation(ctx.player(), "gomu_gatling_end");
    }
}
