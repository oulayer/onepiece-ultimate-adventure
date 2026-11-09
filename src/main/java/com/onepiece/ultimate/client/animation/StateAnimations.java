package com.onepiece.ultimate.client.animation;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityRegistry;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.gomu.GomuAbilities;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Maps synced player state to the looping/holding pose that should play underneath one-shot actions. */
public final class StateAnimations {

    @Nullable
    public static String resolve(Player player, OPPlayerData data) {
        var channel = data.channeling();
        if (channel != null) {
            if (channel.equals(CombatManager.HEAVY_CHANNEL)) return "heavy_charge";
            Ability a = AbilityRegistry.get(channel);
            if (a != null && a.channelAnimation() != null) return a.channelAnimation();
        }
        if (data.blocking()) return "block";
        if (data.seaWeakened()) return "sea_weak";
        if (player.isSwimming() || player.isFallFlying() || player.isPassenger() || player.isSleeping()) return null;
        boolean moving = player.walkAnimation.speed() > 0.25f;
        if (player instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
            var visual = com.onepiece.ultimate.client.transform.TransformationVisuals.of(player);
            if (visual != null) {
                String stance = visual.stance(acp, moving);
                if (stance != null) return stance;
            }
        }
        if (data.isToggleActive(GomuAbilities.GEAR_SECOND.id())) return moving ? "gear_second_run" : "gear_second_stance";
        if (data.combatMode()) {
            return CombatManager.isBlade(player.getMainHandItem()) ? "combat_stance_sword" : "combat_stance";
        }
        return null;
    }

    private StateAnimations() {}
}
