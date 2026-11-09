package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityRegistry;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Single source of truth for damage scaling. Outgoing multipliers are applied to <em>every</em> hit a player
 * deals (vanilla attacks, combos and fruit abilities) in {@code CombatEvents#onHurt}, so ability code only
 * declares base damage values.
 */
public final class DamageCalculator {

    public static float strengthMultiplier(OPPlayerData data) {
        return 1f + 0.03f * data.progression().get(PlayerStat.STRENGTH);
    }

    public static float transformationMultiplier(OPPlayerData data) {
        float m = 1f;
        for (ResourceLocation id : data.activeToggles()) {
            Ability a = AbilityRegistry.get(id);
            if (a != null) m *= a.damageMultiplier(data);
        }
        return m;
    }

    /** Applied to all outgoing damage of the player. */
    public static float outgoingMultiplier(OPPlayerData data) {
        return strengthMultiplier(data) * transformationMultiplier(data);
    }

    /** Fruit abilities additionally scale with mastery. */
    public static float fruitMultiplier(Player player, OPPlayerData data) {
        return 1f + data.mastery() * 0.006f;
    }

    /** Applied to all incoming damage of the player. */
    public static float incomingMultiplier(OPPlayerData data) {
        float durability = Math.min(0.35f, data.progression().get(PlayerStat.DURABILITY) * 0.0035f);
        float m = 1f - durability;
        for (ResourceLocation id : data.activeToggles()) {
            Ability a = AbilityRegistry.get(id);
            if (a != null) m *= a.defenseMultiplier(data);
        }
        return m;
    }

    private DamageCalculator() {}
}
