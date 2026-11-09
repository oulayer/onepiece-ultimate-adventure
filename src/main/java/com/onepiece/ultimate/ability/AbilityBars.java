package com.onepiece.ultimate.ability;

import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.haki.HakiAbilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The ability bars a player has: the Devil Fruit loadout and the Haki techniques. */
public final class AbilityBars {
    public static final int FRUIT = 0;
    public static final int HAKI = 1;

    public static List<Ability> bar(OPPlayerData data, int bar) {
        if (bar == HAKI) return HakiAbilities.all();
        DevilFruit fruit = data.fruit();
        return fruit == null ? List.of() : fruit.abilities();
    }

    /** The slot's base ability (before transformation variants). */
    @Nullable
    public static Ability base(OPPlayerData data, int bar, int slot) {
        List<Ability> list = bar(data, bar);
        return slot >= 0 && slot < list.size() ? list.get(slot) : null;
    }

    private AbilityBars() {}
}
