package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Gear Second: the body flushes red with pulsing blood flow. */
final class GearSecondVisual implements TransformationVisual {
    @Override
    public void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model, float partial, float age) {
        float alpha = 0.32f + 0.12f * Mth.sin(age * 0.35f);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())), light,
                OverlayTexture.NO_OVERLAY, 1.0f, 0.25f, 0.22f, alpha);
    }

    @Override
    public void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, boolean right, float partial) {
        if (!arm.visible) return;
        float a = 0.32f + 0.12f * Mth.sin((player.tickCount + partial) * 0.35f);
        arm.render(ps, buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())), light, OverlayTexture.NO_OVERLAY, 1f, 0.25f, 0.22f, a);
    }
}
