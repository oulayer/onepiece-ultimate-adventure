package com.onepiece.ultimate.client.animation;

import java.util.Locale;

/** Easing curves for keyframe interpolation (t in [0,1]). */
public enum Easing {
    LINEAR, STEP,
    IN_QUAD, OUT_QUAD, IN_OUT_QUAD,
    IN_CUBIC, OUT_CUBIC, IN_OUT_CUBIC,
    IN_EXPO, OUT_EXPO,
    IN_BACK, OUT_BACK,
    OUT_ELASTIC, OUT_BOUNCE;

    public float apply(float t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return switch (this) {
            case LINEAR -> t;
            case STEP -> 0;
            case IN_QUAD -> t * t;
            case OUT_QUAD -> 1 - (1 - t) * (1 - t);
            case IN_OUT_QUAD -> t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case IN_CUBIC -> t * t * t;
            case OUT_CUBIC -> 1 - (float) Math.pow(1 - t, 3);
            case IN_OUT_CUBIC -> t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
            case IN_EXPO -> (float) Math.pow(2, 10 * t - 10);
            case OUT_EXPO -> 1 - (float) Math.pow(2, -10 * t);
            case IN_BACK -> 2.70158f * t * t * t - 1.70158f * t * t;
            case OUT_BACK -> 1 + 2.70158f * (float) Math.pow(t - 1, 3) + 1.70158f * (float) Math.pow(t - 1, 2);
            case OUT_ELASTIC -> (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1);
            case OUT_BOUNCE -> bounce(t);
        };
    }

    private static float bounce(float t) {
        float n = 7.5625f, d = 2.75f;
        if (t < 1 / d) return n * t * t;
        if (t < 2 / d) return n * (t -= 1.5f / d) * t + 0.75f;
        if (t < 2.5 / d) return n * (t -= 2.25f / d) * t + 0.9375f;
        return n * (t -= 2.625f / d) * t + 0.984375f;
    }

    public static Easing parse(String s) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}
