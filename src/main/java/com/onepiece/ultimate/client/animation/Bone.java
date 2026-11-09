package com.onepiece.ultimate.client.animation;

import org.jetbrains.annotations.Nullable;

/** Animatable parts of the player. ROOT transforms the whole body (lean, spin, squash). */
public enum Bone {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, ROOT;

    public static final Bone[] PARTS = {HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};

    @Nullable
    public static Bone byName(String name) {
        for (Bone b : values()) if (b.name().equalsIgnoreCase(name)) return b;
        return null;
    }
}
