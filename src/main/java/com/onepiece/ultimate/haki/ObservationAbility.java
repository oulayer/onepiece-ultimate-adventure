package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kenbunshoku Haki: senses every living presence within 18–40 blocks (shown through walls on the client),
 * warns about creatures with hostile intent (direction markers on the HUD) and reads attacks before they
 * land – a level-based chance to sidestep automatically ({@link HakiManager#tryAutoDodge}).
 */
public final class ObservationAbility extends HakiAbility {
    ObservationAbility() {
        super("haki_observation", HakiType.OBSERVATION, 1, Properties.of(Activation.TOGGLE).cost(10).drain(0.07f).cooldown(60));
    }

    @Override
    public float drainPerTick(OPPlayerData data) {
        return Math.max(0.03f, 0.07f * (1f - 0.06f * (data.hakiLevel(HakiType.OBSERVATION) - 1)));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "observation_activate");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.HAKI_OBSERVATION.get(), 1f, 1f);
        float r = HakiManager.observationRadius(ctx.data());
        ctx.level().sendParticles(RingParticleOptions.ground(0x4BD8FF, r * 0.6f, 24), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    public void onEnd(AbilityContext ctx, boolean interrupted) {
        HakiManager.reset(ctx.data());
    }
}
