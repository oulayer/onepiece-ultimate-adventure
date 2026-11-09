package com.onepiece.ultimate.client.animation;

import net.minecraft.client.model.geom.ModelPart;

/**
 * Mutable full-body pose: for each bone rotation (radians), position (pixels) and scale.
 * Index layout per bone: [rx, ry, rz, px, py, pz, sx, sy, sz].
 */
public final class Pose {
    public static final int STRIDE = 9;
    public final float[] v = new float[Bone.values().length * STRIDE];

    public Pose() {
        reset();
    }

    public void reset() {
        java.util.Arrays.fill(v, 0f);
        for (Bone b : Bone.values()) {
            int o = b.ordinal() * STRIDE;
            v[o + 6] = v[o + 7] = v[o + 8] = 1f;
        }
    }

    public void copyFrom(Pose other) {
        System.arraycopy(other.v, 0, v, 0, v.length);
    }

    public void read(Bone bone, ModelPart part) {
        int o = bone.ordinal() * STRIDE;
        v[o] = part.xRot; v[o + 1] = part.yRot; v[o + 2] = part.zRot;
        v[o + 3] = part.x; v[o + 4] = part.y; v[o + 5] = part.z;
        v[o + 6] = part.xScale; v[o + 7] = part.yScale; v[o + 8] = part.zScale;
    }

    public void write(Bone bone, ModelPart part) {
        int o = bone.ordinal() * STRIDE;
        part.xRot = v[o]; part.yRot = v[o + 1]; part.zRot = v[o + 2];
        part.x = v[o + 3]; part.y = v[o + 4]; part.z = v[o + 5];
        part.xScale = v[o + 6]; part.yScale = v[o + 7]; part.zScale = v[o + 8];
    }

    public float get(Bone bone, int index) {
        return v[bone.ordinal() * STRIDE + index];
    }

    /** this = lerp(this, target, w) for every component. */
    public void blendTowards(Pose target, float w) {
        if (w <= 0) return;
        if (w >= 1) {
            copyFrom(target);
            return;
        }
        for (int i = 0; i < v.length; i++) v[i] += (target.v[i] - v[i]) * w;
    }
}
