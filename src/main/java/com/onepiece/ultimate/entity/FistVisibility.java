package com.onepiece.ultimate.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side bookkeeping of which arms currently have a stretched fist out, so the player model can hide the
 * real arm while the stretched one is drawn. Entries expire after a few ticks without refresh (leak free).
 * Lives in common code because the fist entity refreshes it from its client tick; it never touches client classes.
 */
public final class FistVisibility {
    private static final Map<Integer, long[]> ACTIVE = new HashMap<>(); // ownerId -> [rightUntil, leftUntil]

    public static void mark(int ownerId, int side, long now) {
        long[] v = ACTIVE.computeIfAbsent(ownerId, k -> new long[2]);
        if (side >= 0) v[0] = now + 2;
        if (side <= 0) v[1] = now + 2;
    }

    public static boolean rightHidden(int ownerId, long now) {
        long[] v = ACTIVE.get(ownerId);
        return v != null && v[0] >= now;
    }

    public static boolean leftHidden(int ownerId, long now) {
        long[] v = ACTIVE.get(ownerId);
        return v != null && v[1] >= now;
    }

    public static void cleanup(long now) {
        ACTIVE.values().removeIf(v -> v[0] < now && v[1] < now);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private FistVisibility() {}
}
