package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onepiece.ultimate.client.transform.TransformationVisual;
import com.onepiece.ultimate.client.transform.TransformationVisuals;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Draws the active transformation's extra look (tints, markings, wings) on top of the player model. */
public class TransformationLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public TransformationLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        TransformationVisual v = TransformationVisuals.of(player);
        if (v != null) v.renderLayer(ps, buffers, light, player, getParentModel(), partial, age);
    }
}
