package com.onepiece.ultimate.haki;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/**
 * Persistent Haki progression: experience per type (level 0 = not awakened, 1..10) and whether the player
 * carries the rare Conqueror's potential.
 */
public final class HakiData {
    public static final int MAX_LEVEL = 10;

    private final EnumMap<HakiType, Integer> xp = new EnumMap<>(HakiType.class);
    private final EnumMap<HakiType, Boolean> awakened = new EnumMap<>(HakiType.class);
    /** null = not rolled yet (rolled on first login). */
    private Boolean conquerorPotential;

    public HakiData() {
        for (HakiType t : HakiType.values()) {
            xp.put(t, 0);
            awakened.put(t, false);
        }
    }

    public static int xpForNext(int level) {
        return 60 + 45 * level;
    }

    public static int totalXpFor(int level) {
        int s = 0;
        for (int i = 1; i < Math.min(level, MAX_LEVEL); i++) s += xpForNext(i);
        return s;
    }

    public boolean isAwakened(HakiType t) {
        return awakened.get(t);
    }

    public void awaken(HakiType t) {
        awakened.put(t, true);
    }

    public void setAwakened(HakiType t, boolean v) {
        awakened.put(t, v);
        if (!v) xp.put(t, 0);
    }

    /** 0 if not awakened, otherwise 1..10. */
    public int level(HakiType t) {
        if (!isAwakened(t)) return 0;
        int lvl = 1;
        long rem = xp.get(t);
        while (lvl < MAX_LEVEL && rem >= xpForNext(lvl)) {
            rem -= xpForNext(lvl);
            lvl++;
        }
        return lvl;
    }

    public float progress(HakiType t) {
        if (!isAwakened(t)) return 0f;
        int lvl = 1;
        long rem = xp.get(t);
        while (lvl < MAX_LEVEL && rem >= xpForNext(lvl)) {
            rem -= xpForNext(lvl);
            lvl++;
        }
        return lvl >= MAX_LEVEL ? 1f : rem / (float) xpForNext(lvl);
    }

    public int xp(HakiType t) {
        return xp.get(t);
    }

    public void setXp(HakiType t, int value) {
        xp.put(t, Math.max(0, value));
    }

    public int totalLevels() {
        int s = 0;
        for (HakiType t : HakiType.values()) s += level(t);
        return s;
    }

    public boolean anyAwakened() {
        for (HakiType t : HakiType.values()) if (isAwakened(t)) return true;
        return false;
    }

    public boolean potentialRolled() {
        return conquerorPotential != null;
    }

    public boolean hasConquerorPotential() {
        return Boolean.TRUE.equals(conquerorPotential);
    }

    public void setConquerorPotential(boolean v) {
        conquerorPotential = v;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        for (HakiType type : HakiType.values()) {
            CompoundTag c = new CompoundTag();
            c.putBoolean("awakened", awakened.get(type));
            c.putInt("xp", xp.get(type));
            t.put(type.name(), c);
        }
        if (conquerorPotential != null) t.putBoolean("potential", conquerorPotential);
        return t;
    }

    public void load(CompoundTag t) {
        for (HakiType type : HakiType.values()) {
            CompoundTag c = t.getCompound(type.name());
            awakened.put(type, c.getBoolean("awakened"));
            xp.put(type, c.getInt("xp"));
        }
        conquerorPotential = t.contains("potential") ? t.getBoolean("potential") : null;
    }
}
