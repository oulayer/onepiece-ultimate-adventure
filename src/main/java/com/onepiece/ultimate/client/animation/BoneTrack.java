package com.onepiece.ultimate.client.animation;

import org.jetbrains.annotations.Nullable;

/**
 * Keyframes of one bone. Rotation either replaces the vanilla pose (blended by weight) or, when
 * {@link #additive}, is added on top of it. Position is always an offset; scale replaces.
 */
public record BoneTrack(@Nullable Keyframe[] rotation, @Nullable Keyframe[] position, @Nullable Keyframe[] scale, boolean additive) {
}
