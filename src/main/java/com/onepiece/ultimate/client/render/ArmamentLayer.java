package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.haki.HakiAbilities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Busoshoku coating: the coated parts turn glossy black with a faint violet sheen. Coverage follows the user's
 * Armament level – arms (1+), arms and legs (4+), whole body (8+).
 */
public class ArmamentLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public ArmamentLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null || !data.isToggleActive(HakiAbilities.ARMAMENT.id())) return;
        int level = Math.max(1, data.armamentLevelForRender());
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        float sheen = 0.10f + 0.06f * Mth.sin(age * 0.15f);

        for (int pass = 0; pass < 2; pass++) {
            float r = pass == 0 ? 0.07f : 0.55f, g = pass == 0 ? 0.05f : 0.40f, b = pass == 0 ? 0.11f : 1.0f;
            float a = pass == 0 ? 0.93f : sheen;
            part(m.rightArm, m.rightSleeve, ps, vc, light, r, g, b, a);
            part(m.leftArm, m.leftSleeve, ps, vc, light, r, g, b, a);
            if (level >= 4) {
                part(m.rightLeg, m.rightPants, ps, vc, light, r, g, b, a);
                part(m.leftLeg, m.leftPants, ps, vc, light, r, g, b, a);
            }
            if (level >= 8) {
                part(m.body, m.jacket, ps, vc, light, r, g, b, a);
                part(m.head, m.hat, ps, vc, light, r, g, b, a * 0.8f);
            }
        }
    }

    private static void part(ModelPart base, ModelPart overlay, PoseStack ps, VertexConsumer vc, int light, float r, float g, float b, float a) {
        if (!base.visible) return;
        base.render(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, a);
        if (overlay.visible) overlay.render(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, a);
    }
}
