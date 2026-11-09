package com.onepiece.ultimate.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepiece.ultimate.client.animation.Easing;
import com.onepiece.ultimate.particle.RingParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Oriented (non-billboard) expanding shock ring. Horizontal for ground shockwaves, perpendicular to the strike
 * direction for punch impacts.
 */
public class RingParticle extends TextureSheetParticle {
    private final float targetScale;
    private final Quaternionf rotation;

    protected RingParticle(ClientLevel level, double x, double y, double z, RingParticleOptions o, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = Math.max(2, o.lifetime());
        this.targetScale = o.scale();
        this.hasPhysics = false;
        this.gravity = 0;
        this.xd = this.yd = this.zd = 0;
        setColor(((o.color() >> 16) & 255) / 255f, ((o.color() >> 8) & 255) / 255f, (o.color() & 255) / 255f);
        this.rotation = new Quaternionf().rotateY((float) Math.toRadians(-o.yaw())).rotateX((float) Math.toRadians(o.pitch()));
        pickSprite(sprites);
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partial) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partial, xo, x) - cam.x());
        float py = (float) (Mth.lerp(partial, yo, y) - cam.y());
        float pz = (float) (Mth.lerp(partial, zo, z) - cam.z());
        float t = Math.min(1f, (age + partial) / lifetime);
        float size = 0.15f + targetScale * Easing.OUT_CUBIC.apply(t);
        float a = 0.9f * (1f - t * t);

        Vector3f[] corners = {new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, -1, 0)};
        for (Vector3f c : corners) {
            c.mul(size);
            rotation.transform(c);
            c.add(px, py, pz);
        }
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = 0xF000F0;
        float[][] uv = {{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};
        for (int i = 0; i < 4; i++) put(vc, corners[i], uv[i][0], uv[i][1], a, light);
        for (int i = 3; i >= 0; i--) put(vc, corners[i], uv[i][0], uv[i][1], a, light); // back face
    }

    private void put(VertexConsumer vc, Vector3f p, float u, float v, float a, int light) {
        vc.vertex(p.x(), p.y(), p.z()).uv(u, v).color(rCol, gCol, bCol, a).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<RingParticleOptions> {
        @Override
        public Particle createParticle(RingParticleOptions o, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new RingParticle(level, x, y, z, o, sprites);
        }
    }
}
