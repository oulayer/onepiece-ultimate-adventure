package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.gomu.GomuAbilities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Gear Second: the body flushes red with pulsing blood flow. */
public class GearSecondLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public GearSecondLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null || !data.isToggleActive(GomuAbilities.GEAR_SECOND.id())) return;
        float alpha = 0.32f + 0.12f * Mth.sin(age * 0.35f);
        var vc = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        getParentModel().renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, 1.0f, 0.25f, 0.22f, alpha);
    }
}
