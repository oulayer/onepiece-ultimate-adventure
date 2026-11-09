package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onepiece.ultimate.client.render.ArmamentVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

/** Full phoenix: the player is drawn as a great bird of blue flame, flapping in flight and gliding with wings spread. */
final class PhoenixFullVisual implements TransformationVisual {
    @Override
    public boolean replacesBody() {
        return true;
    }

    @Override
    public boolean hidesFirstPersonArms() {
        return true;
    }

    @Override
    public void renderBody(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial) {
        ModelPart bird = PhoenixModels.bird();
        ModelPart body = bird.getChild("body");
        float age = player.tickCount + partial;
        boolean flying = player.getAbilities().flying || !player.onGround();
        float flap = flying ? 0.25f + Mth.sin(age * 0.45f) * 0.55f : -0.9f;
        body.getChild("wing_r").zRot = -flap;
        body.getChild("wing_l").zRot = flap;
        body.getChild("wing_r").getChild("tip").zRot = -flap * 0.6f;
        body.getChild("wing_l").getChild("tip").zRot = flap * 0.6f;
        body.getChild("tail").xRot = 0.1f + Mth.sin(age * 0.2f) * 0.08f;
        float pitch = flying ? Mth.clamp(player.getViewXRot(partial), -50, 50) : 0;
        body.xRot = pitch * Mth.DEG_TO_RAD * 0.7f;
        body.getChild("head").xRot = flying ? 0 : player.getViewXRot(partial) * Mth.DEG_TO_RAD * 0.5f;

        ps.pushPose();
        ps.translate(0, flying ? 0.55 : 0.45, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-player.getViewYRot(partial)));
        ps.scale(1 / 16f, 1 / 16f, 1 / 16f);
        bird.render(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(PhoenixModels.TEXTURE)), 0xF000F0, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 0.95f);
        bird.render(ps, buffers.getBuffer(RenderType.energySwirl(ArmamentVisuals.FLAME, (age * 0.02f) % 1f, (age * 0.05f) % 1f)), 0xF000F0,
                OverlayTexture.NO_OVERLAY, 0.1f, 0.45f, 0.95f, 1f);
        ps.popPose();
    }

    @Override
    public void clientTick(AbstractClientPlayer p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || (p == mc.player && mc.options.getCameraType().isFirstPerson())) return;
        for (int i = 0; i < 2; i++) {
            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + (p.getRandom().nextFloat() - 0.5) * 2.5, p.getY() + 0.6,
                    p.getZ() + (p.getRandom().nextFloat() - 0.5) * 2.5, 0, 0.03, 0);
        }
    }
}
