package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Advanced Observation – Future Sight: for 3–7 seconds the user sees a moment ahead and evades every attack,
 * free of cost, each dodge opening a counter window. Requires Observation to be active.
 */
public final class FutureSightAbility extends HakiAbility {
    FutureSightAbility() {
        super("haki_future_sight", HakiType.OBSERVATION, 5, Properties.of(Activation.INSTANT).cost(35).cooldown(900));
    }

    @Override
    public boolean canActivate(AbilityContext ctx) {
        if (ctx.data().isToggleActive(HakiAbilities.OBSERVATION.id())) return true;
        ctx.player().displayClientMessage(Component.translatable("message.onepiece.requires_active", HakiAbilities.OBSERVATION.displayName())
                .withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        int lvl = ctx.data().hakiLevel(HakiType.OBSERVATION);
        ctx.data().hakiRuntime.futureSightUntil = ctx.now() + 60 + 8L * lvl;
        ctx.data().setFutureSight(true);
        ServerFx.animation(ctx.player(), "future_sight");
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.HAKI_OBSERVATION.get(), 1.2f, 0.6f);
        ServerFx.camera(ctx.player(), CameraEffectPacket.Type.FOV_PUNCH, -0.08f, 20);
        train(ctx, 4);
    }
}
