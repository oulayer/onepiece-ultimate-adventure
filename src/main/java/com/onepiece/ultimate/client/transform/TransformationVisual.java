package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

/** How a transformation looks. Every hook is optional. */
public interface TransformationVisual {
    /** Reshape the posed model (part scales/offsets) after animations ran. */
    default void applyModel(PlayerModel<?> model, AbstractClientPlayer player, float partial) {}

    /** Uniform scale of the whole body, applied about the feet. */
    default float rootScale(AbstractClientPlayer player, float partial) {
        return 1f;
    }

    /** Extra third-person camera distance (blocks) so a big form never swallows the camera. */
    default float cameraDistanceBonus(AbstractClientPlayer player, float partial) {
        return 4f * Math.max(0f, rootScale(player, partial) - 1f);
    }

    /** Extra rendering on top of the model (tints, patterns, wings…). */
    default void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                             PlayerModel<AbstractClientPlayer> model, float partial, float age) {}

    /** Extra rendering on a first-person arm (after the arm itself). */
    default void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                      ModelPart arm, ModelPart sleeve, boolean right, float partial) {}

    default float firstPersonArmScale() {
        return 1f;
    }

    default boolean hidesFirstPersonArms() {
        return false;
    }

    /** Replace the whole player model (full Zoan forms). */
    default boolean replacesBody() {
        return false;
    }

    default void renderBody(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial) {}

    /** Looping stance animation for this form (null = default resolution). */
    @Nullable
    default String stance(AbstractClientPlayer player, boolean moving) {
        return null;
    }

    /** Client-only ambience (particles). */
    default void clientTick(AbstractClientPlayer player) {}
}
