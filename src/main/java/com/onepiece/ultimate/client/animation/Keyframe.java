package com.onepiece.ultimate.client.animation;

/** A value at a time. Rotation values are degrees, position values model pixels, scale factors. */
public record Keyframe(float time, float x, float y, float z, Easing easing) {

    /** Samples a sorted keyframe array at time t into out[0..2]. Returns false if the track is empty. */
    public static boolean sample(Keyframe[] frames, float t, float[] out) {
        if (frames == null || frames.length == 0) return false;
        Keyframe first = frames[0];
        if (frames.length == 1 || t <= first.time) {
            out[0] = first.x; out[1] = first.y; out[2] = first.z;
            return true;
        }
        for (int i = 1; i < frames.length; i++) {
            Keyframe b = frames[i];
            if (t <= b.time) {
                Keyframe a = frames[i - 1];
                float span = b.time - a.time;
                float k = span <= 0 ? 1 : b.easing.apply((t - a.time) / span);
                out[0] = a.x + (b.x - a.x) * k;
                out[1] = a.y + (b.y - a.y) * k;
                out[2] = a.z + (b.z - a.z) * k;
                return true;
            }
        }
        Keyframe last = frames[frames.length - 1];
        out[0] = last.x; out[1] = last.y; out[2] = last.z;
        return true;
    }
}
