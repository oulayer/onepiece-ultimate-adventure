package com.onepiece.ultimate.client.animation;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * A keyframe animation over the player's bones.
 *
 * @param length   ticks
 * @param loop     repeat forever (state animations)
 * @param hold     when not looping, keep the last frame after the end instead of finishing (charge poses)
 * @param blendIn  ticks to blend from the previous pose
 * @param blendOut ticks to blend back to vanilla after the end
 * @param priority a new one-shot can't interrupt a running one with higher priority
 */
public record PoseAnimation(ResourceLocation id, float length, boolean loop, boolean hold, float blendIn, float blendOut,
                            int priority, float rootPivot, Map<Bone, BoneTrack> tracks) {

    private static final float DEG = (float) (Math.PI / 180.0);

    public PoseAnimation {
        tracks = new EnumMap<>(tracks.isEmpty() ? Map.of() : tracks);
    }

    public float wrap(float time) {
        if (loop && length > 0) return time % length;
        return Math.min(time, length);
    }

    /** Applies this animation at {@code time} onto {@code pose} with weight {@code w}. */
    public void apply(Pose pose, float time, float w, float[] tmp) {
        if (w <= 0) return;
        float t = wrap(time);
        for (Map.Entry<Bone, BoneTrack> e : tracks.entrySet()) {
            int o = e.getKey().ordinal() * Pose.STRIDE;
            BoneTrack track = e.getValue();
            boolean root = e.getKey() == Bone.ROOT;
            if (Keyframe.sample(track.rotation(), t, tmp)) {
                float k = root ? 1f : DEG; // root rotation is kept in degrees for PoseStack
                for (int i = 0; i < 3; i++) {
                    float target = tmp[i] * k;
                    if (track.additive() || root) pose.v[o + i] += target * w;
                    else pose.v[o + i] += (target - pose.v[o + i]) * w;
                }
            }
            if (Keyframe.sample(track.position(), t, tmp)) {
                for (int i = 0; i < 3; i++) pose.v[o + 3 + i] += tmp[i] * w;
            }
            if (Keyframe.sample(track.scale(), t, tmp)) {
                for (int i = 0; i < 3; i++) pose.v[o + 6 + i] += (tmp[i] - pose.v[o + 6 + i]) * w;
            }
        }
    }

    public boolean has(Bone bone) {
        return tracks.containsKey(bone);
    }
}
