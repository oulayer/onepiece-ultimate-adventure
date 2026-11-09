package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onepiece.ultimate.entity.DebrisEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

/** Tumbling, shrinking chunk of the block it was torn from. */
public class DebrisRenderer extends EntityRenderer<DebrisEntity> {
    public DebrisRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.2f;
    }

    @Override
    public void render(DebrisEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.life() + partial;
        float size = 0.55f * Math.min(1f, (100 - t) / 20f + 0.3f);
        ps.pushPose();
        ps.translate(0, 0.25, 0);
        ps.mulPose(Axis.XP.rotationDegrees(t * e.spinX));
        ps.mulPose(Axis.ZP.rotationDegrees(t * e.spinZ));
        ps.scale(size, size, size);
        ps.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(e.blockState(), ps, buffers, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
        ps.popPose();
        super.render(e, yaw, partial, ps, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(DebrisEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
