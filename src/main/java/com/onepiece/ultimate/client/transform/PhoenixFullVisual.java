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
import net.minecraft.world.phys.Vec3;

/** Full phoenix: the player is drawn as a great bird of blue flame, flapping in flight and gliding with wings spread. */
final class PhoenixFullVisual implements TransformationVisual {
    @Override
    public boolean replacesBody() {
        return true;
    }

    @Override
    public float cameraDistanceBonus(AbstractClientPlayer player, float partial) {
        return 2.5f; // a wingspan of ~6 blocks
    }

    @Override
    public boolean hidesFirstPersonArms() {
        return true;
    }

    private static final int GROW_TICKS = 12;
    /** Per player {tick the form appeared, last tick it was seen}: drives the scale-in transition. */
    private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<int[]> seen = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();

    @Override
    public void renderBody(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial) {
        ModelPart bird = PhoenixModels.bird();
        ModelPart body = bird.getChild("body");
        float age = player.tickCount + partial;
        Vec3 vel = new Vec3(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo);
        double hSpeed = vel.horizontalDistance();
        boolean airborne = player.getAbilities().flying || !player.onGround();

        // flight state → wing motion: fast deep beats climbing, slow beats cruising, wings locked open gliding/diving
        float flap, tipLag, legTuck;
        if (!airborne) {
            flap = -0.35f; tipLag = 0f; legTuck = 0f;        // wings folded against the body, standing on its talons
        } else if (vel.y > 0.06) {
            flap = 0.2f + Mth.sin(age * 0.9f) * 0.8f; tipLag = Mth.sin(age * 0.9f - 0.8f) * 0.5f; legTuck = 0.9f;
        } else if (vel.y < -0.08) {
            flap = 0.12f + Mth.sin(age * 0.3f) * 0.05f; tipLag = -0.1f; legTuck = 1.3f;
        } else if (hSpeed > 0.15) {
            flap = 0.15f + Mth.sin(age * 0.35f) * 0.45f; tipLag = Mth.sin(age * 0.35f - 0.8f) * 0.3f; legTuck = 1.3f;
        } else { // hovering
            flap = 0.3f + Mth.sin(age * 0.6f) * 0.7f; tipLag = Mth.sin(age * 0.6f - 0.8f) * 0.45f; legTuck = 0.5f;
        }
        body.getChild("wing_r").zRot = -flap;
        body.getChild("wing_l").zRot = flap;
        body.getChild("wing_r").getChild("tip").zRot = -tipLag;
        body.getChild("wing_l").getChild("tip").zRot = tipLag;
        // folded: the arm segment lies back along the flank and the hand segment folds forward over it,
        // its long primaries tucked away; spread in the air
        float fold = airborne ? 0f : 2.8f;
        ModelPart wr = body.getChild("wing_r"), wl = body.getChild("wing_l");
        wr.yRot = airborne ? 0 : -1.35f;
        wl.yRot = airborne ? 0 : 1.35f;
        wr.getChild("tip").yRot = fold;
        wl.getChild("tip").yRot = -fold;
        wr.getChild("tip").visible = airborne; // folded under the arm segment
        wl.getChild("tip").visible = airborne;
        ModelPart tail = body.getChild("tail");
        tail.xRot = (airborne ? 0.05f : 0.45f) + Mth.sin(age * 0.2f) * 0.06f;
        for (int i = 0; i < 5; i++) {
            ModelPart plume = tail.getChild("plume" + i);
            plume.getChild("end").xRot = Mth.sin(age * 0.25f + i * 0.9f) * (airborne ? 0.18f : 0.08f) + (airborne ? 0 : 0.3f);
            plume.getChild("end").yRot = Mth.sin(age * 0.18f + i) * 0.1f;
        }
        body.getChild("leg_r").xRot = legTuck;
        body.getChild("leg_l").xRot = legTuck;

        // body pitch follows the flight path (climb nose up, dive nose down); standing, the bird sits upright
        float pitch;
        if (!airborne) pitch = -22f;
        else if (hSpeed > 0.1 || Math.abs(vel.y) > 0.1) pitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(vel.y, Math.max(0.05, hSpeed))), -60, 60);
        else pitch = Mth.clamp(player.getViewXRot(partial), -40, 40) * 0.5f;
        body.xRot = pitch * Mth.DEG_TO_RAD;
        ModelPart neck = body.getChild("neck");
        neck.getChild("head").xRot = 0.45f + (airborne ? -pitch * Mth.DEG_TO_RAD * 0.5f : player.getViewXRot(partial) * Mth.DEG_TO_RAD * 0.5f + 0.39f);

        int[] st = seen.get(player.getId());
        float grow = st == null ? 1f : Mth.clamp((player.tickCount - st[0] + partial) / GROW_TICKS, 0f, 1f);
        float s = easeOutBack(grow);

        ps.pushPose();
        ps.translate(0, (airborne ? 0.55 : 0.62) * Math.max(0.2f, s), 0);
        ps.mulPose(Axis.YP.rotationDegrees(-player.getViewYRot(partial)));
        // ModelPart already works in 1/16 block units. Mirror one axis: boxes are wound for vanilla's flipped model
        // space, so without it every face would be back-face culled (the bird is symmetric, so the mirror is invisible).
        ps.scale(-s, s, s);
        bird.render(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(PhoenixModels.TEXTURE)), 0xF000F0, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 0.95f);
        bird.render(ps, buffers.getBuffer(RenderType.energySwirl(ArmamentVisuals.FLAME, (age * 0.02f) % 1f, (age * 0.05f) % 1f)), 0xF000F0,
                OverlayTexture.NO_OVERLAY, 0.1f, 0.45f, 0.95f, 1f);
        ps.popPose();
    }

    private static float easeOutBack(float t) {
        float c = 1.7f, u = t - 1f;
        return Math.max(0f, 1f + (c + 1f) * u * u * u + c * u * u);
    }

    @Override
    public void clientTick(AbstractClientPlayer p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int[] st = seen.get(p.getId());
        boolean fresh = st == null || p.tickCount - st[1] > 2 || p.tickCount < st[1];
        if (fresh) seen.put(p.getId(), st = new int[]{p.tickCount, p.tickCount});
        st[1] = p.tickCount;
        if (p.tickCount % 100 == 0) seen.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        boolean ownView = p == mc.player && mc.options.getCameraType().isFirstPerson();
        if (ownView) return;
        var r = p.getRandom();
        if (fresh) { // burst of blue fire as the body becomes the bird
            for (int i = 0; i < 30; i++)
                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.8, p.getZ(),
                        (r.nextFloat() - 0.5) * 0.4, r.nextFloat() * 0.2, (r.nextFloat() - 0.5) * 0.4);
        }
        if (p.tickCount % 2 == 0) {
            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + (r.nextFloat() - 0.5) * 2.5, p.getY() + 0.6,
                    p.getZ() + (r.nextFloat() - 0.5) * 2.5, 0, 0.03, 0);
        }
        PhoenixHybridVisual.wind(p, 2.6);
    }
}
