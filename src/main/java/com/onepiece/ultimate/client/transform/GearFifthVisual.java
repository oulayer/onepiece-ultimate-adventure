package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onepiece.ultimate.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Gear Fifth: everything turns radiant white – hair, clothes, skin – with cloud-like steam swirling around the shoulders. */
final class GearFifthVisual implements TransformationVisual {
    /** Paint with a solid white texture: tinting the skin texture white would just reproduce the skin's colours. */
    private static final net.minecraft.resources.ResourceLocation WHITE = com.onepiece.ultimate.OnePieceMod.id("textures/particle/white.png");
    @Override
    public void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model, float partial, float age) {
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light,
                OverlayTexture.NO_OVERLAY, 0.97f, 0.97f, 0.99f, 0.82f);
        float glow = 0.25f + 0.1f * Mth.sin(age * 0.25f);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE)), light,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, glow);
    }

    @Override
    public void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, boolean right, float partial) {
        if (!arm.visible) return;
        arm.render(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light, OverlayTexture.NO_OVERLAY, 0.97f, 0.97f, 0.99f, 0.82f);
        sleeve.render(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light, OverlayTexture.NO_OVERLAY, 0.97f, 0.97f, 0.99f, 0.82f);
    }

    @Override
    public String stance(AbstractClientPlayer player, boolean moving) {
        return moving ? "gear_fifth_run" : "gear_fifth_stance";
    }

    @Override
    public void clientTick(AbstractClientPlayer p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || (p == mc.player && mc.options.getCameraType().isFirstPerson())) return;
        if (p.tickCount % 2 != 0) return;
        double a = p.tickCount * 0.35;
        for (int i = 0; i < 2; i++) {
            double ang = a + i * Math.PI;
            mc.level.addParticle(ModParticles.STEAM.get(), p.getX() + Math.cos(ang) * 0.75, p.getY() + 1.55 + Mth.sin((float) a) * 0.1,
                    p.getZ() + Math.sin(ang) * 0.75, 0, 0.01, 0);
        }
    }
}
