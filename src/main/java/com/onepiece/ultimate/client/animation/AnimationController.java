package com.onepiece.ultimate.client.animation;

import org.jetbrains.annotations.Nullable;

/**
 * Animation state of one entity: a looping/holding <b>state layer</b> (stances, charge poses) under a one-shot
 * <b>action layer</b> (attacks, dashes). Every switch snapshots the last rendered pose and cross-fades from it,
 * so transitions never pop. Supports hit-stop (time freeze).
 */
public final class AnimationController {
    private static final float STATE_FADE = 4f;

    private static final class Layer {
        final PoseAnimation anim;
        final float speed;
        float time, prevTime;

        Layer(PoseAnimation anim, float speed) {
            this.anim = anim;
            this.speed = speed;
        }

        float time(float partial) {
            return prevTime + (time - prevTime) * partial;
        }
    }

    @Nullable private Layer action;
    @Nullable private Layer state;

    private final Pose lastPose = new Pose();
    private final Pose snapshot = new Pose();
    private boolean hasLast;
    private float snapshotStart = -1000, snapshotDuration = 1;

    private float clock, prevClock;
    private int hitstop;
    private final float[] tmp = new float[3];
    private final Pose work = new Pose();

    public void play(PoseAnimation anim, float speed) {
        if (action != null && action.anim.priority() > anim.priority() && action.time < action.anim.length()) return;
        beginTransition(anim.blendIn());
        action = new Layer(anim, speed <= 0 ? 1f : speed);
    }

    public void setState(@Nullable PoseAnimation anim) {
        if (state == null ? anim == null : state.anim == anim) return;
        beginTransition(anim != null ? anim.blendIn() : STATE_FADE);
        state = anim == null ? null : new Layer(anim, 1f);
    }

    public void hitstop(int ticks) {
        hitstop = Math.max(hitstop, ticks);
    }

    public boolean isPlaying() {
        return action != null;
    }

    @Nullable
    public PoseAnimation currentAction() {
        return action == null ? null : action.anim;
    }

    private void beginTransition(float duration) {
        if (!hasLast) return;
        snapshot.copyFrom(lastPose);
        snapshotStart = clock;
        snapshotDuration = Math.max(0.5f, duration);
    }

    public void tick() {
        prevClock = clock;
        if (hitstop > 0) {
            hitstop--;
            if (action != null) action.prevTime = action.time;
            if (state != null) state.prevTime = state.time;
            return;
        }
        clock += 1;
        if (action != null) {
            action.prevTime = action.time;
            action.time += action.speed;
            PoseAnimation a = action.anim;
            if (!a.loop() && !a.hold() && action.time >= a.length() + a.blendOut()) action = null;
        }
        if (state != null) {
            state.prevTime = state.time;
            state.time += state.speed;
        }
    }

    /** Builds the animated pose on top of the vanilla pose already stored in {@code pose}. */
    public void apply(Pose pose, float partial) {
        if (state != null) state.anim.apply(pose, state.time(partial), 1f, tmp);
        if (action != null) {
            PoseAnimation a = action.anim;
            float t = action.time(partial);
            float w = 1f;
            if (!a.loop() && !a.hold() && t > a.length()) w = 1f - (t - a.length()) / Math.max(0.5f, a.blendOut());
            a.apply(pose, t, Math.max(0f, w), tmp);
        }
        float c = prevClock + (clock - prevClock) * partial;
        float sw = 1f - (c - snapshotStart) / snapshotDuration;
        if (sw > 0) {
            // ease the cross-fade
            sw = sw * sw * (3 - 2 * sw);
            work.copyFrom(pose);
            work.blendTowards(snapshot, sw);
            pose.copyFrom(work);
        }
        lastPose.copyFrom(pose);
        hasLast = true;
    }

    /** Evaluates only the ROOT bone (whole-body transform, applied before the model renders). */
    public void applyRoot(Pose pose, float partial) {
        pose.reset();
        if (state != null) state.anim.apply(pose, state.time(partial), 1f, tmp);
        if (action != null) {
            PoseAnimation a = action.anim;
            float t = action.time(partial);
            float w = 1f;
            if (!a.loop() && !a.hold() && t > a.length()) w = 1f - (t - a.length()) / Math.max(0.5f, a.blendOut());
            a.apply(pose, t, Math.max(0f, w), tmp);
        }
        float c = prevClock + (clock - prevClock) * partial;
        float sw = 1f - (c - snapshotStart) / snapshotDuration;
        if (sw > 0 && hasLast) {
            sw = sw * sw * (3 - 2 * sw);
            int o = Bone.ROOT.ordinal() * Pose.STRIDE;
            for (int i = o; i < o + Pose.STRIDE; i++) pose.v[i] += (snapshot.v[i] - pose.v[i]) * sw;
        }
    }

    public float rootPivot() {
        if (action != null && action.anim.has(Bone.ROOT)) return action.anim.rootPivot();
        if (state != null && state.anim.has(Bone.ROOT)) return state.anim.rootPivot();
        return 0.9f;
    }

    public boolean isIdle() {
        return action == null && state == null && clock - snapshotStart > snapshotDuration;
    }
}
