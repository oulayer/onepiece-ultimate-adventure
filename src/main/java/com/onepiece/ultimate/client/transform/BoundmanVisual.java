package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Boundman: a hulking inflated upper body with massive arms and short legs (the black comes from the Armament
 * coating it always wears), red flame markings and a constant bounce.
 */
class BoundmanVisual implements TransformationVisual {
    static final ResourceLocation PATTERN = OnePieceMod.id("textures/entity/gear_fourth_pattern.png");

    @Override
    public void applyModel(PlayerModel<?> m, AbstractClientPlayer player, float partial) {
        scale(m.body, 1.55f, 1.12f, 1.5f);
        scale(m.rightArm, 1.8f, 1.3f, 1.8f);
        scale(m.leftArm, 1.8f, 1.3f, 1.8f);
        m.rightArm.x -= 3.2f;
        m.leftArm.x += 3.2f;
        m.rightArm.y -= 1f;
        m.leftArm.y -= 1f;
        scale(m.rightLeg, 1.3f, 0.85f, 1.3f);
        scale(m.leftLeg, 1.3f, 0.85f, 1.3f);
        m.rightLeg.x -= 0.6f;
        m.leftLeg.x += 0.6f;
        m.rightLeg.y += 1.5f;
        m.leftLeg.y += 1.5f;
        m.head.y -= 0.5f;
        scale(m.head, 0.92f, 0.92f, 0.92f);
    }

    static void scale(ModelPart p, float x, float y, float z) {
        p.xScale *= x;
        p.yScale *= y;
        p.zScale *= z;
    }

    @Override
    public float rootScale() {
        return 1.12f;
    }

    @Override
    public void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model, float partial, float age) {
        float glow = 0.75f + 0.2f * Mth.sin(age * 0.2f);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(PATTERN)), light, OverlayTexture.NO_OVERLAY, 1f, 0.2f, 0.15f, glow);
    }

    @Override
    public void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, boolean right, float partial) {
        if (!arm.visible) return;
        arm.render(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(PATTERN)), light, OverlayTexture.NO_OVERLAY, 1f, 0.2f, 0.15f, 0.85f);
    }

    @Override
    public float firstPersonArmScale() {
        return 1.4f;
    }

    @Override
    public String stance(AbstractClientPlayer player, boolean moving) {
        return "boundman_stance";
    }
}
