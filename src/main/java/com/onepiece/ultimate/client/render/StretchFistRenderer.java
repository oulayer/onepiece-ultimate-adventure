package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepiece.ultimate.entity.StretchFistEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a rubber arm from the owner's shoulder to the fist using the owner's own skin, stretched along its
 * length, plus an oversized fist at the end. Gigant fists get a huge arm and fist.
 */
public class StretchFistRenderer extends EntityRenderer<StretchFistEntity> {

    /** Skin UV regions (pixels on a 64x64 skin) for one arm. */
    private record ArmUV(float[] front, float[] back, float[] outer, float[] inner, float[] top, float[] bottom) {}

    private static final ArmUV RIGHT = new ArmUV(
            new float[]{44, 20, 48, 32}, new float[]{52, 20, 56, 32}, new float[]{40, 20, 44, 32}, new float[]{48, 20, 52, 32},
            new float[]{44, 16, 48, 20}, new float[]{48, 16, 52, 20});
    private static final ArmUV LEFT = new ArmUV(
            new float[]{36, 52, 40, 64}, new float[]{44, 52, 48, 64}, new float[]{40, 52, 44, 64}, new float[]{32, 52, 36, 64},
            new float[]{36, 48, 40, 52}, new float[]{40, 48, 44, 52});

    private static final ResourceLocation WHITE = com.onepiece.ultimate.OnePieceMod.id("textures/particle/white.png");

    public StretchFistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(StretchFistEntity e) {
        return e.getOwner() instanceof AbstractClientPlayer p ? p.getSkinTextureLocation() : DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public void render(StretchFistEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        Entity o = e.getOwner();
        if (!(o instanceof AbstractClientPlayer owner)) return;
        Minecraft mc = Minecraft.getInstance();
        int side = e.side() == 0 ? 1 : e.side();

        Vec3 base = e.getPosition(partial);
        Vec3 fist = base.add(0, e.getBbHeight() / 2, 0);
        Vec3 shoulder = e.shoulder(owner, partial);
        if (owner == mc.player && mc.options.getCameraType().isFirstPerson()) {
            // Start the arm just below the view so it reads as "my arm" without clipping the camera.
            Camera cam = mc.gameRenderer.getMainCamera();
            Vector3f look = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
            shoulder = cam.getPosition()
                    .add(look.x() * 0.5, look.y() * 0.5, look.z() * 0.5)
                    .add(-up.x() * 0.45, -up.y() * 0.45, -up.z() * 0.45)
                    .add(-left.x() * 0.35 * side, -left.y() * 0.35 * side, -left.z() * 0.35 * side);
        }
        Vec3 s = shoulder.subtract(base);
        Vec3 f = fist.subtract(base);
        Vec3 axis = f.subtract(s);
        if (axis.lengthSqr() < 1e-4) return;
        Vec3 dir = axis.normalize();

        float scale = e.fistScale();
        boolean gigant = e.style() == StretchFistEntity.Style.GIGANT || e.style() == StretchFistEntity.Style.DAWN && e.fistScale() > 2f;
        float r = 1f, g = 1f, b = 1f;
        if (e.style() == StretchFistEntity.Style.JET) { g = 0.68f; b = 0.66f; }
        boolean dawn = e.style() == StretchFistEntity.Style.DAWN;
        if (dawn) light = 0xF000F0;
        var ownerData = com.onepiece.ultimate.data.OPCapabilities.getOrNull(owner);
        if (dawn) {
            r = g = b = 1f; // Gear Fifth: radiant white
        } else if (ownerData != null && ownerData.isToggleActive(com.onepiece.ultimate.haki.HakiAbilities.ARMAMENT.id())) {
            r = 0.16f; g = 0.12f; b = 0.22f; // coated with Armament Haki
        }

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(dawn ? WHITE : owner.getSkinTextureLocation()));
        PoseStack.Pose last = ps.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        ArmUV uv = side >= 0 ? RIGHT : LEFT;

        float armRadius = 0.125f * (gigant ? scale * 0.55f : Math.min(1f, scale));
        float fistRadius = 0.17f * (gigant ? scale : Math.max(0.8f, scale));
        Vec3 fistBack = f.subtract(dir.scale(fistRadius * 1.6));
        box(vc, m, n, s, fistBack, armRadius, uv, false, light, r, g, b);
        box(vc, m, n, fistBack, f.add(dir.scale(fistRadius * 0.4)), fistRadius, uv, true, light, r, g, b);
    }

    private static void box(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 from, Vec3 to, float radius, ArmUV uv,
                            boolean fistPart, int light, float r, float g, float b) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 ref = Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 R = axis.cross(ref).normalize().scale(radius);
        Vec3 U = R.cross(axis).normalize().scale(radius);

        float[] front = fistPart ? hand(uv.front) : uv.front;
        float[] back = fistPart ? hand(uv.back) : uv.back;
        float[] outer = fistPart ? hand(uv.outer) : uv.outer;
        float[] inner = fistPart ? hand(uv.inner) : uv.inner;

        // side faces: corners along the length (from -> to)
        face(vc, m, n, from.add(U).subtract(R), to.add(U).subtract(R), to.add(U).add(R), from.add(U).add(R), back, U, light, r, g, b);
        face(vc, m, n, from.subtract(U).add(R), to.subtract(U).add(R), to.subtract(U).subtract(R), from.subtract(U).subtract(R), front, U.scale(-1), light, r, g, b);
        face(vc, m, n, from.add(R).add(U), to.add(R).add(U), to.add(R).subtract(U), from.add(R).subtract(U), outer, R, light, r, g, b);
        face(vc, m, n, from.subtract(R).subtract(U), to.subtract(R).subtract(U), to.subtract(R).add(U), from.subtract(R).add(U), inner, R.scale(-1), light, r, g, b);
        // caps
        face(vc, m, n, to.add(U).subtract(R), to.subtract(U).subtract(R), to.subtract(U).add(R), to.add(U).add(R), uv.bottom, axis, light, r, g, b);
        face(vc, m, n, from.add(U).add(R), from.subtract(U).add(R), from.subtract(U).subtract(R), from.add(U).subtract(R), uv.top, axis.scale(-1), light, r, g, b);
    }

    /** Lowest 4 pixels of a side region = the hand. */
    private static float[] hand(float[] side) {
        return new float[]{side[0], side[3] - 4, side[2], side[3]};
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 a, Vec3 b2, Vec3 c, Vec3 d, float[] uv, Vec3 normal,
                             int light, float r, float g, float b) {
        float u0 = uv[0] / 64f, v0 = uv[1] / 64f, u1 = uv[2] / 64f, v1 = uv[3] / 64f;
        Vec3 nn = normal.normalize();
        vertex(vc, m, n, a, u0, v0, nn, light, r, g, b);
        vertex(vc, m, n, b2, u0, v1, nn, light, r, g, b);
        vertex(vc, m, n, c, u1, v1, nn, light, r, g, b);
        vertex(vc, m, n, d, u1, v0, nn, light, r, g, b);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 p, float u, float v, Vec3 nn, int light, float r, float g, float b) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, 1f).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, (float) nn.x, (float) nn.y, (float) nn.z).endVertex();
    }

    @Override
    public boolean shouldRender(StretchFistEntity e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true; // the arm spans from the owner to the fist; never cull by the fist's small box
    }
}
