package com.onepiece.ultimate.client.animation;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Drop-in replacement for the vanilla player model: runs the vanilla pose first, then layers the keyframe
 * animations on top and keeps the outer skin layers (sleeves, jacket, pants, hat) glued to their parts.
 * The model instance is shared by all players, so every frame starts from a clean state.
 */
public class AnimatedPlayerModel extends PlayerModel<AbstractClientPlayer> {

    public AnimatedPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        resetScale(head);
        resetScale(body);
        resetScale(rightArm);
        resetScale(leftArm);
        resetScale(rightLeg);
        resetScale(leftLeg);
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // PlayerRenderer#renderHand (first person arm) calls setupAnim with all zeros: don't pose that arm.
        boolean firstPersonHand = ageInTicks == 0f && limbSwing == 0f && limbSwingAmount == 0f && netHeadYaw == 0f && headPitch == 0f;
        float partial = ageInTicks - player.tickCount;
        PlayerAnimations.applyToModel(this, player, partial < 0 || partial > 1 ? 0 : partial, !firstPersonHand);
        syncLayer(hat, head);
        syncLayer(jacket, body);
        syncLayer(rightSleeve, rightArm);
        syncLayer(leftSleeve, leftArm);
        syncLayer(rightPants, rightLeg);
        syncLayer(leftPants, leftLeg);
        rightSleeve.visible &= rightArm.visible;
        leftSleeve.visible &= leftArm.visible;
    }

    private static void resetScale(ModelPart p) {
        p.xScale = p.yScale = p.zScale = 1f;
    }

    private static void syncLayer(ModelPart layer, ModelPart part) {
        layer.copyFrom(part);
        layer.xScale = part.xScale;
        layer.yScale = part.yScale;
        layer.zScale = part.zScale;
    }
}
