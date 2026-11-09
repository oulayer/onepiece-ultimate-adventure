package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Advanced Conqueror's – Infusion: wraps the armament coating in Conqueror's Haki. Every hit (punches, blades,
 * fruit attacks) crackles with black lightning, pushes harder, deals +22% … +40% and lands slightly before
 * contact (+0.8 reach). Requires and ends with Armament.
 */
public final class ConquerorInfusionAbility extends HakiAbility {
    ConquerorInfusionAbility() {
        super("haki_conqueror_infusion", HakiType.CONQUEROR, 5, Properties.of(Activation.TOGGLE).cost(30).drain(0.35f).cooldown(400));
    }

    @Override
    public boolean canActivate(AbilityContext ctx) {
        if (ctx.data().isToggleActive(HakiAbilities.ARMAMENT.id())) return true;
        ctx.player().displayClientMessage(Component.translatable("message.onepiece.requires_active", HakiAbilities.ARMAMENT.displayName())
                .withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    @Override
    public float damageMultiplier(OPPlayerData data) {
        return 1.2f + 0.02f * data.hakiLevel(HakiType.CONQUEROR);
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "infusion_activate");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.HAKI_LIGHTNING.get(), 1.3f, 0.9f);
        Vec3 c = p.position().add(0, 1, 0);
        for (int i = 0; i < 8; i++) HakiManager.bolt(ctx.level(), c, HakiManager.randomDir(p).scale(2.5));
        ctx.level().sendParticles(RingParticleOptions.ground(0x200000, 3.5f, 14), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
        ServerFx.shake(ctx.level(), p.position(), 14, 0.4f, 10);
    }

    @Override
    public void onToggleTick(AbilityContext ctx, int ticks) {
        if (!ctx.data().isToggleActive(HakiAbilities.ARMAMENT.id())) {
            AbilityManager.endToggle(ctx.player(), ctx.data(), this, true);
        }
    }
}
