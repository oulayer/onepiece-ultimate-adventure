package com.onepiece.ultimate.client.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.core.TransformationEvents;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.registry.ModParticles;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Gear Fifth: everything turns radiant white – skin, clothes and a billowing cloud of hair – with a ring of clouds
 * circling the neck, huge cartoon eyes and a grin that bursts into laughter, rubbery squash-and-stretch, and a
 * springy balloon growth when Gigant is used.
 */
final class GearFifthVisual implements TransformationVisual {
    /** Paint with a solid white texture: tinting the skin texture white would just reproduce the skin's colours. */
    private static final ResourceLocation WHITE = OnePieceMod.id("textures/particle/white.png");
    private static final ResourceLocation FACE = OnePieceMod.id("textures/entity/gear5_face.png");
    private static final ResourceLocation FACE_LAUGH = OnePieceMod.id("textures/entity/gear5_face_laugh.png");
    private static final int CLOUDS = 11;

    /** Per player {previous, current, velocity} of the Gigant balloon spring. */
    private final Int2ObjectOpenHashMap<float[]> size = new Int2ObjectOpenHashMap<>();

    @Override
    public void renderLayer(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> model, float partial, float age) {
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light,
                OverlayTexture.NO_OVERLAY, 0.97f, 0.97f, 0.99f, 0.82f);
        float glow = 0.25f + 0.1f * Mth.sin(age * 0.25f);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE)), light,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, glow);

        if (model.head.visible) {
            // cartoon face on the (inflated) hat layer, then the cloud hair
            ps.pushPose();
            model.head.translateAndRotate(ps);
            ps.pushPose();
            ps.scale(1.13f, 1.13f, 1.13f);
            ModelPart hat = model.hat;
            float hx = hat.x, hy = hat.y, hz = hat.z, rx = hat.xRot, ry = hat.yRot, rz = hat.zRot;
            hat.setPos(0, 0, 0);
            hat.setRotation(0, 0, 0);
            hat.render(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(laughing(player, age) ? FACE_LAUGH : FACE)), light, OverlayTexture.NO_OVERLAY);
            hat.setPos(hx, hy, hz);
            hat.setRotation(rx, ry, rz);
            ps.popPose();
            float puff = 1f + 0.04f * Mth.sin(age * 0.2f);
            ps.translate(0, -0.25f, 0.02f);
            ps.scale(puff, puff, puff);
            ps.translate(0, 0.25f, -0.02f);
            FormModels.cloudHair().render(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 0.95f);
            ps.popPose();
        }

        // ring of cloud puffs around the neck and shoulders, slowly orbiting and breathing
        ps.pushPose();
        model.body.translateAndRotate(ps);
        VertexConsumer cloud = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        ModelPart puff = FormModels.puff();
        for (int i = 0; i < CLOUDS; i++) {
            float a = age * 0.04f + i * Mth.TWO_PI / CLOUDS;
            float s = 1.05f + 0.3f * Mth.sin(age * 0.17f + i * 1.7f);
            ps.pushPose();
            ps.translate(Mth.cos(a) * 6.2f / 16f, (-0.6f + Mth.sin(age * 0.11f + i) * 0.5f) / 16f, Mth.sin(a) * 4.2f / 16f);
            ps.scale(s, s * 0.85f, s);
            puff.render(ps, cloud, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 0.9f);
            ps.popPose();
        }
        ps.popPose();
    }

    /** Bursts out laughing while attacking or hurt, and every few seconds when idle. */
    private static boolean laughing(AbstractClientPlayer player, float age) {
        return player.swingTime > 0 || player.hurtTime > 0 || ((int) age / 20) % 6 == 0;
    }

    @Override
    public void renderFirstPersonArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, boolean right, float partial) {
        if (!arm.visible) return;
        arm.render(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light, OverlayTexture.NO_OVERLAY, 0.97f, 0.97f, 0.99f, 0.82f);
        sleeve.render(ps, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light, OverlayTexture.NO_OVERLAY, 0.97f, 0.97f, 0.99f, 0.82f);
    }

    @Override
    public float rootScale(AbstractClientPlayer player, float partial) {
        float[] s = size.get(player.getId());
        if (s == null) return target(player);
        return Mth.lerp(partial, s[0], s[1]);
    }

    private static float target(AbstractClientPlayer player) {
        OPPlayerData d = OPCapabilities.getOrNull(player);
        return d != null && d.giant() ? TransformationEvents.GIANT_SCALE : 1f;
    }

    @Override
    public String stance(AbstractClientPlayer player, boolean moving) {
        if (!player.onGround() && !player.isInWater() && !player.getAbilities().flying) return "gear_fifth_jump";
        return moving ? "gear_fifth_run" : "gear_fifth_stance";
    }

    @Override
    public void clientTick(AbstractClientPlayer p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // under-damped spring: the body overshoots and wobbles like a balloon when it grows or shrinks
        float[] s = size.computeIfAbsent(p.getId(), id -> new float[]{target(p), target(p), 0f});
        s[0] = s[1];
        s[2] = (s[2] + (target(p) - s[1]) * 0.22f) * 0.72f;
        s[1] = Math.max(0.5f, s[1] + s[2]);
        if (p.tickCount % 100 == 0) size.keySet().removeIf(id -> mc.level.getEntity(id) == null);

        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) return;
        if (p.tickCount % 2 != 0) return;
        double a = p.tickCount * 0.35;
        float k = s[1];
        for (int i = 0; i < 2; i++) {
            double ang = a + i * Math.PI;
            mc.level.addParticle(ModParticles.STEAM.get(), p.getX() + Math.cos(ang) * 0.75 * k, p.getY() + (1.55 + Mth.sin((float) a) * 0.1) * k,
                    p.getZ() + Math.sin(ang) * 0.75 * k, 0, 0.01, 0);
        }
    }
}
