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
 * Snakeman: its own silhouette, not a recoloured Boundman – a slim torso with narrow hips, long thin coated arms,
 * rows of red dots instead of flames, and black hair swept back into spikes.
 */
final class SnakemanVisual extends BoundmanVisual {
    private static final ResourceLocation DOTS = OnePieceMod.id("textures/entity/snakeman_pattern.png");
    private static final ResourceLocation SOLID = OnePieceMod.id("textures/particle/white.png");

    @Override
    public void applyModel(PlayerModel<?> m, AbstractClientPlayer player, float partial) {
        scale(m.body, 1.08f, 1.08f, 0.9f);
        scale(m.rightArm, 0.92f, 1.32f, 0.92f);
        scale(m.leftArm, 0.92f, 1.32f, 0.92f);
        m.rightArm.x -= 0.3f;
        m.leftArm.x += 0.3f;
        scale(m.rightLeg, 0.95f, 1.08f, 0.95f);
        scale(m.leftLeg, 0.95f, 1.08f, 0.95f);
        // a slow serpentine sway of the arms on top of whatever pose is playing
        float t = player.tickCount + partial;
        m.rightArm.zRot += Mth.sin(t * 0.15f) * 0.06f;
        m.leftArm.zRot -= Mth.sin(t * 0.15f + 1f) * 0.06f;
    }

    @Override
    public float rootScale(AbstractClientPlayer player, float partial) {
        return 1.06f;
    }

    @Override
    public void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model, float partial, float age) {
        float glow = 0.7f + 0.25f * Mth.sin(age * 0.3f);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(DOTS)), light, OverlayTexture.NO_OVERLAY, 1f, 0.18f, 0.2f, glow);
        if (!model.head.visible) return;
        ps.pushPose();
        model.head.translateAndRotate(ps);
        FormModels.spikyHair().render(ps, buffers.getBuffer(RenderType.entitySolid(SOLID)), light, OverlayTexture.NO_OVERLAY, 0.06f, 0.05f, 0.07f, 1f);
        ps.popPose();
    }

    @Override
    public void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, boolean right, float partial) {
        if (!arm.visible) return;
        arm.render(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(DOTS)), light, OverlayTexture.NO_OVERLAY, 1f, 0.18f, 0.2f, 0.85f);
    }

    @Override
    public float firstPersonArmScale() {
        return 1.0f;
    }

    @Override
    public String stance(AbstractClientPlayer player, boolean moving) {
        return "snakeman_stance";
    }
}
