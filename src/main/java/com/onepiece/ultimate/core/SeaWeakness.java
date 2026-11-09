package com.onepiece.ultimate.core;

import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

/**
 * The sea's curse: a fruit user in water deeper than their waist loses their powers (abilities and
 * transformations end), drains energy, is weakened and sinks – they can't swim back up on their own.
 */
public final class SeaWeakness {
    private static final double WAIST_DEPTH = 0.9;

    public static void tick(ServerPlayer player, OPPlayerData data) {
        boolean weak = OPConfig.SEA_WEAKNESS.get() && data.hasFruit() && !player.isCreative() && !player.isSpectator()
                && player.getFluidTypeHeight(ForgeMod.WATER_TYPE.get()) >= WAIST_DEPTH;
        if (weak && !data.seaWeakened()) {
            AbilityManager.cancelAll(player, data);
            player.displayClientMessage(Component.translatable("message.onepiece.sea_curse").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        data.setSeaWeakened(weak);
        if (!weak) return;

        data.setEnergy(data.energy() - 0.6f);
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
        }
        Vec3 v = player.getDeltaMovement();
        if (v.y > -0.12) {
            player.setDeltaMovement(v.x * 0.8, Math.min(v.y, 0) - 0.06, v.z * 0.8);
            player.hurtMarked = true;
        }
        player.setSwimming(false);
    }

    private SeaWeakness() {}
}
