package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Gomu Gomu no Pistol: the arm winds back, then the fist shoots out up to ~14–22 blocks and snaps back.
 * Launches what it hits and dents the wall it strikes. In Gear Second it becomes <b>Jet Pistol</b>: no wind-up,
 * more than twice as fast, harder hitting, with a steam trail.
 */
public final class PistolAbility extends Ability {
    private final boolean jet;

    PistolAbility(boolean jet) {
        super(jet ? "gomu_jet_pistol" : "gomu_pistol", Properties.of(Activation.INSTANT)
                .cost(jet ? 16 : 12).cooldown(jet ? 20 : 26).mastery(0)
                .sharesCooldownWith(OnePieceMod.id("gomu_pistol"))
                .color(jet ? GomuFx.JET_COLOR : GomuFx.RUBBER_COLOR));
        this.jet = jet;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.animation(ctx.player(), jet ? "gomu_jet_pistol" : "gomu_pistol");
        ctx.schedule(jet ? 1 : 4, () -> fire(ctx));
    }

    private void fire(AbilityContext ctx) {
        if (!ctx.player().isAlive()) return;
        ServerLevel level = ctx.level();
        Vec3 dir = ctx.look();
        float m = ctx.masteryFraction();
        StretchFistEntity fist = StretchFistEntity.create(level, ctx.player(), ctx.shoulder(1), dir, 1)
                .speed(jet ? 5.0 : 2.3)
                .range((jet ? 18 : 14) + 8 * m)
                .style(jet ? StretchFistEntity.Style.JET : StretchFistEntity.Style.NORMAL)
                .onHitEntity(GomuFx.fistHit(ctx, dir, jet ? 10f : 7f, jet ? 2.0f : 1.4f, 0.25f, 12,
                        jet ? 1.4f : 1.0f, jet ? 1.6f : 1.2f, jet ? GomuFx.JET_COLOR : 0xFFFFFF, 2, false))
                .onHitBlock((hit, last) -> {
                    DestructionManager.crater(level, hit.getLocation(), jet ? 1.5f : 1.1f, 0.8f, jet ? 2.2f : 1.6f, ctx.player());
                    ServerFx.impact(level, hit.getLocation(), dir, jet ? 1.6f : 1.1f, 0xFFFFFF);
                });
        if (jet) {
            fist.onTravel(p -> level.sendParticles(ModParticles.STEAM.get(), p.x, p.y + 0.3, p.z, 2, 0.1, 0.1, 0.1, 0.01));
        }
        level.addFreshEntity(fist);
        GomuFx.stretch(level, ctx.player().position(), jet ? 1.4f : 1.0f);
        if (jet) GomuFx.steamBurst(level, ctx.player(), 6);
    }
}
