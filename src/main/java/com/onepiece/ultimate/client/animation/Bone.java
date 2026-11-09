package com.onepiece.ultimate.client.animation;

import org.jetbrains.annotations.Nullable;

/**
 * Animatable parts of the player. ROOT transforms the whole body (lean, spin, squash).
 * FP_RIGHT / FP_LEFT are the first-person arms, in camera space relative to their resting place:
 * position in pixels (x = outward, y = up, z = forward), rotation in degrees (x = pitch, y = outward yaw,
 * z = outward roll). Left-arm values are mirrored automatically, so symmetric poses share numbers.
 */
public enum Bone {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, ROOT, FP_RIGHT, FP_LEFT;

    public boolean isSpecial() {
        return this == ROOT || this == FP_RIGHT || this == FP_LEFT;
    }

    public static final Bone[] PARTS = {HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};

    @Nullable
    public static Bone byName(String name) {
        for (Bone b : values()) if (b.name().equalsIgnoreCase(name)) return b;
        return null;
    }
}
