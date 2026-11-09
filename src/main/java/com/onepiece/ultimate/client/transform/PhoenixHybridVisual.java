package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onepiece.ultimate.client.render.ArmamentVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;

/** Phoenix hybrid: wings of blue flame trail from both arms, the legs turn into golden talons. */
final class PhoenixHybridVisual implements TransformationVisual {
    @Override
    public void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model, float partial, float age) {
        wing(ps, buffers, model.rightArm, age);
        wing(ps, buffers, model.leftArm, age);
        var talons = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        model.rightLeg.render(ps, talons, light, OverlayTexture.NO_OVERLAY, 1f, 0.8f, 0.25f, 0.55f);
        model.leftLeg.render(ps, talons, light, OverlayTexture.NO_OVERLAY, 1f, 0.8f, 0.25f, 0.55f);
    }

    static void wing(PoseStack ps, MultiBufferSource buffers, ModelPart arm, float age) {
        if (!arm.visible) return;
        ModelPart wing = PhoenixModels.wing();
        ps.pushPose();
        arm.translateAndRotate(ps);
        wing.render(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(PhoenixModels.TEXTURE)), 0xF000F0, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 0.85f);
        wing.render(ps, buffers.getBuffer(RenderType.energySwirl(ArmamentVisuals.FLAME, (age * 0.02f) % 1f, (age * 0.05f) % 1f)), 0xF000F0,
                OverlayTexture.NO_OVERLAY, 0.1f, 0.45f, 0.9f, 1f);
        ps.popPose();
    }

    @Override
    public void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, boolean right, float partial) {
        wing(ps, buffers, arm, player.tickCount + partial);
    }

    @Override
    public String stance(AbstractClientPlayer player, boolean moving) {
        return player.onGround() || player.isInWater() ? null : "phoenix_glide";
    }

    @Override
    public void clientTick(AbstractClientPlayer p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || p.getRandom().nextInt(3) != 0 || (p == mc.player && mc.options.getCameraType().isFirstPerson())) return;
        double yaw = Math.toRadians(p.yBodyRot);
        int side = p.getRandom().nextBoolean() ? 1 : -1;
        mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX() - Math.cos(yaw) * 0.5 * side, p.getY() + 1.2, p.getZ() - Math.sin(yaw) * 0.5 * side,
                0, 0.02, 0);
    }
}
