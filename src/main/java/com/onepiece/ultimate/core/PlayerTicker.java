package com.onepiece.ultimate.core;

import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.data.DataSync;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.fruit.DevilFruit;
import net.minecraft.server.level.ServerPlayer;

/** The one place where per-player systems are ticked, in a fixed order. */
public final class PlayerTicker {

    public static void tick(ServerPlayer player, OPPlayerData data) {
        long now = player.level().getGameTime();
        data.cooldowns().tick();
        SeaWeakness.tick(player, data);
        CombatManager.tick(player, data);
        AbilityManager.tick(player, data);
        com.onepiece.ultimate.haki.HakiManager.tick(player, data);

        DevilFruit fruit = data.fruit();
        if (fruit != null && !data.seaWeakened()) fruit.passiveTick(player, data);

        if (!data.seaWeakened() && !data.blocking() && data.channeling() == null) {
            float regen = 0.25f + 0.006f * data.progression().get(PlayerStat.STAMINA);
            if (!data.combat.inCombat(now)) regen *= 2.2f;
            data.setEnergy(data.energy() + regen);
        }
        DataSync.flush(player, data);
    }

    private PlayerTicker() {}
}
