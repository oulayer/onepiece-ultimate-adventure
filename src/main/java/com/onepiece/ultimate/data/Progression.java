package com.onepiece.ultimate.data;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/** Character level, experience and attribute points. Pure data – rules live in ProgressionManager. */
public final class Progression {
    private int level = 1;
    private long xp;
    private int unspentPoints;
    private final EnumMap<PlayerStat, Integer> stats = new EnumMap<>(PlayerStat.class);

    public Progression() {
        for (PlayerStat s : PlayerStat.values()) stats.put(s, 0);
    }

    /** Experience required to go from {@code level} to {@code level + 1}. */
    public static long xpForNext(int level) {
        return 100L + (long) (60 * Math.pow(level, 1.35));
    }

    public int level() { return level; }
    public long xp() { return xp; }
    public int unspentPoints() { return unspentPoints; }
    public int get(PlayerStat stat) { return stats.get(stat); }

    public void setLevel(int level) { this.level = Math.max(1, level); }
    public void setXp(long xp) { this.xp = Math.max(0, xp); }
    public void setUnspentPoints(int p) { this.unspentPoints = Math.max(0, p); }
    public void set(PlayerStat stat, int value) { stats.put(stat, Math.max(0, Math.min(PlayerStat.MAX_POINTS, value))); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("level", level);
        t.putLong("xp", xp);
        t.putInt("points", unspentPoints);
        CompoundTag s = new CompoundTag();
        stats.forEach((k, v) -> s.putInt(k.name(), v));
        t.put("stats", s);
        return t;
    }

    public void load(CompoundTag t) {
        level = Math.max(1, t.getInt("level"));
        xp = t.getLong("xp");
        unspentPoints = t.getInt("points");
        CompoundTag s = t.getCompound("stats");
        for (PlayerStat stat : PlayerStat.values()) set(stat, s.getInt(stat.name()));
    }

    public void copyFrom(Progression o) {
        load(o.save());
    }
}
