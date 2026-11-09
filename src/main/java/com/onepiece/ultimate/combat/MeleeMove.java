package com.onepiece.ultimate.combat;

/**
 * Frame data of a melee move. {@code windup} is synchronised with the impact keyframe of {@code animation}
 * so the hit lands exactly when the fist/blade visually connects.
 */
public record MeleeMove(String animation, int windup, int recovery, float range, float arc,
                        float damage, float knockback, float lift, int stun, boolean launch, boolean heavy) {

    public static final MeleeMove[] FIST_COMBO = {
            new MeleeMove("punch_1", 2, 5, 3.0f, 70f, 1.0f, 0.25f, 0.0f, 8, false, false),
            new MeleeMove("punch_2", 2, 5, 3.0f, 70f, 1.0f, 0.25f, 0.0f, 8, false, false),
            new MeleeMove("punch_3", 3, 7, 3.2f, 100f, 1.3f, 0.40f, 0.05f, 10, false, false),
            new MeleeMove("kick_finisher", 4, 12, 3.5f, 90f, 1.9f, 1.5f, 0.45f, 14, true, true),
    };

    public static final MeleeMove[] SWORD_COMBO = {
            new MeleeMove("slash_1", 3, 6, 3.6f, 120f, 1.0f, 0.3f, 0.0f, 8, false, false),
            new MeleeMove("slash_2", 3, 6, 3.6f, 120f, 1.0f, 0.3f, 0.0f, 8, false, false),
            new MeleeMove("slash_3", 4, 8, 3.8f, 60f, 1.45f, 0.45f, 0.1f, 10, false, false),
            new MeleeMove("slash_spin", 5, 12, 3.6f, 360f, 1.7f, 1.3f, 0.35f, 14, true, true),
    };

    public static final MeleeMove AERIAL = new MeleeMove("aerial_punch", 2, 5, 3.0f, 90f, 0.9f, 0.15f, 0.32f, 10, false, false);

    /** Damage/knockback of the heavy attack are scaled by charge at execution time. */
    public static final MeleeMove HEAVY = new MeleeMove("heavy_release", 2, 14, 3.6f, 75f, 1.6f, 1.0f, 0.3f, 20, true, true);
}
