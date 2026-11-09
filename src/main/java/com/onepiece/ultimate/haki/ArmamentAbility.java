package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Busoshoku Haki: coats the body in invisible armour that hardens to black.
 * <ul>
 *   <li>Outgoing damage +18% … +45% (level), incoming −12% … −30%.</li>
 *   <li>Coated hits ignore rubber's blunt resistance and Logia intangibility (see {@link HakiManager#isImbued}).</li>
 *   <li>Coverage grows: arms (1), arms+legs (4), whole body (8; doubles the defence bonus).</li>
 *   <li>Weapons are coated too (dark aura) and gain the same bonus.</li>
 *   <li>Against another Armament user the stronger coating wins (see CombatEvents).</li>
 * </ul>
 */
public final class ArmamentAbility extends HakiAbility {
    ArmamentAbility() {
        super("haki_armament", HakiType.ARMAMENT, 1, Properties.of(Activation.TOGGLE).cost(10).drain(0.09f).cooldown(60));
    }

    @Override
    public float drainPerTick(OPPlayerData data) {
        return Math.max(0.04f, 0.09f * (1f - 0.06f * (data.hakiLevel(HakiType.ARMAMENT) - 1)));
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.15f + 0.03f * data.hakiLevel(HakiType.ARMAMENT);
    }

    @Override
    public float defenseMultiplier(OPPlayerData data) {
        int lvl = data.hakiLevel(HakiType.ARMAMENT);
        float reduction = 0.10f + 0.02f * lvl;
        if (lvl >= 8) reduction *= 1.5f; // full-body coating
        return 1f - Math.min(0.45f, reduction);
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "armament_activate");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.HAKI_ARMAMENT.get(), 1f, 1f);
        Vec3 c = p.position().add(0, 1.1, 0);
        ctx.level().sendParticles(new DustParticleOptions(new Vector3f(0.12f, 0.04f, 0.2f), 1.6f), c.x, c.y, c.z, 30, 0.4, 0.5, 0.4, 0.02);
        ctx.level().sendParticles(RingParticleOptions.ground(0x2A1040, 2.5f, 12), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    public void onToggleTick(AbilityContext ctx, int ticks) {
        if (ticks % 200 == 0) train(ctx, 1);
    }

    @Override
    public void onEnd(AbilityContext ctx, boolean interrupted) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.HAKI_ARMAMENT.get(), 0.5f, 1.6f);
        // Infusion rides on the armament coating.
        if (ctx.data().isToggleActive(HakiAbilities.CONQUEROR_INFUSION.id())) {
            com.onepiece.ultimate.ability.AbilityManager.endToggle(ctx.player(), ctx.data(), HakiAbilities.CONQUEROR_INFUSION, true);
        }
    }
}
