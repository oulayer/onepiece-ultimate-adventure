package com.onepiece.ultimate.data;

import net.minecraft.network.chat.Component;

/** Spendable character attributes. Each one has a concrete gameplay effect (see ProgressionManager / DamageCalculator). */
public enum PlayerStat {
    /** +3% melee and fruit damage per point, +0.06 vanilla attack damage. */
    STRENGTH(0xE8503A),
    /** +0.5 max health per point and up to 35% damage reduction. */
    DURABILITY(0x5A9BE8),
    /** +0.4% movement speed per point, longer dashes, shorter dash cooldown. */
    AGILITY(0x5BE87A),
    /** +5 max energy per point and faster regeneration. */
    STAMINA(0xF2C94C),
    /** +6 max haki per point, faster haki regeneration, +1% haki potency and +2% haki experience. */
    HAKI(0xA070FF);

    public static final int MAX_POINTS = 100;
    public final int color;

    PlayerStat(int color) {
        this.color = color;
    }

    public Component displayName() {
        return Component.translatable("stat.onepiece." + name().toLowerCase());
    }

    public Component description() {
        return Component.translatable("stat.onepiece." + name().toLowerCase() + ".desc");
    }

    public static PlayerStat byIndex(int i) {
        PlayerStat[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}
