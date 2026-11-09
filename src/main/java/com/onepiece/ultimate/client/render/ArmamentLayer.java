package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.ArrayList;
import java.util.List;

/** Busoshoku coating on the third-person model. Coverage follows the Armament level: arms (1+), legs (4+), whole body (8+). */
public class ArmamentLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public ArmamentLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (player.isInvisible() || !ArmamentVisuals.active(player)) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        int level = data == null ? 1 : Math.max(1, data.armamentLevelForRender());
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        List<ModelPart> parts = new ArrayList<>(List.of(m.rightArm, m.rightSleeve, m.leftArm, m.leftSleeve));
        if (level >= 4) parts.addAll(List.of(m.rightLeg, m.rightPants, m.leftLeg, m.leftPants));
        if (level >= 8) parts.addAll(List.of(m.body, m.jacket, m.head, m.hat));
        ArmamentVisuals.render(ps, buffers, light, player, partial, parts.toArray(ModelPart[]::new));
    }
}
