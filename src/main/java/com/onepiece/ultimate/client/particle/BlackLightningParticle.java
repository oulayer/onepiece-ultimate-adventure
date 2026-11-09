package com.onepiece.ultimate.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Haoshoku black lightning: a jagged bolt from the spawn point along the particle's "velocity" vector.
 * The path re-randomises every other tick (flicker) and is drawn as camera-facing ribbons: a wide red glow under
 * a thin black core, full-bright.
 */
public class BlackLightningParticle extends TextureSheetParticle {
    private final Vec3 offset;
    private Vec3[] points;
    private final long seedBase;
    private final boolean white;

    protected BlackLightningParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, boolean white) {
        super(level, x, y, z);
        this.offset = new Vec3(dx, dy, dz);
        this.lifetime = 5 + random.nextInt(4);
        this.hasPhysics = false;
        this.gravity = 0;
        this.xd = this.yd = this.zd = 0;
        this.seedBase = random.nextLong();
        this.white = white;
        pickSprite(sprites);
        rebuild(0);
    }

    private void rebuild(int salt) {
        double len = offset.length();
        int n = Mth.clamp((int) (len * 3), 4, 18);
        Vec3 dir = len < 1e-4 ? new Vec3(0, 1, 0) : offset.scale(1 / len);
        Vec3 ref = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(ref).normalize(), v = dir.cross(u).normalize();
        java.util.Random r = new java.util.Random(seedBase + salt * 31L);
        points = new Vec3[n + 1];
        double amp = Math.max(0.08, len * 0.09);
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            Vec3 p = offset.scale(t);
            if (i > 0 && i < n) p = p.add(u.scale((r.nextDouble() - 0.5) * 2 * amp)).add(v.scale((r.nextDouble() - 0.5) * 2 * amp));
            points[i] = p;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (age % 2 == 0) rebuild(age);
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partial) {
        Vec3 cam = camera.getPosition();
        Vec3 origin = new Vec3(x - cam.x, y - cam.y, z - cam.z);
        float fade = 1f - (age + partial) / lifetime;
        float flicker = (age % 2 == 0) ? 1f : 0.75f;
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        for (int pass = 0; pass < 2; pass++) {
            float w = pass == 0 ? 0.075f : 0.03f;
            float r, g, b;
            if (white) { r = pass == 0 ? 0.35f : 1f; g = pass == 0 ? 0.6f : 0.98f; b = pass == 0 ? 1f : 0.8f; }
            else { r = pass == 0 ? 0.9f : 0.02f; g = pass == 0 ? 0.05f : 0f; b = pass == 0 ? 0.05f : 0.03f; }
            float a = (pass == 0 ? 0.45f : 0.95f) * fade * flicker;
            for (int i = 0; i < points.length - 1; i++) {
                Vec3 pa = origin.add(points[i]), pb = origin.add(points[i + 1]);
                Vec3 seg = pb.subtract(pa);
                Vec3 mid = pa.add(pb).scale(0.5);
                float near = (float) Mth.clamp((mid.length() - 1.2) / 2.5, 0.0, 1.0);
                if (near <= 0.01f) continue;
                Vec3 side = seg.cross(mid).normalize().scale(w * (1f - 0.5f * i / points.length));
                Vec3 lift = pass == 1 ? mid.normalize().scale(-0.01) : Vec3.ZERO; // core slightly closer to the camera
                Vec3 c0 = pa.subtract(side).add(lift), c1 = pa.add(side).add(lift), c2 = pb.add(side).add(lift), c3 = pb.subtract(side).add(lift);
                // both windings: particles render with back-face culling and the ribbon's facing varies
                float an = a * near;
                put(vc, c0, u0, v0, r, g, b, an);
                put(vc, c1, u0, v1, r, g, b, an);
                put(vc, c2, u1, v1, r, g, b, an);
                put(vc, c3, u1, v0, r, g, b, an);
                put(vc, c3, u1, v0, r, g, b, an);
                put(vc, c2, u1, v1, r, g, b, an);
                put(vc, c1, u0, v1, r, g, b, an);
                put(vc, c0, u0, v0, r, g, b, an);
            }
        }
    }

    private static void put(VertexConsumer vc, Vec3 p, float u, float v, float r, float g, float b, float a) {
        vc.vertex(p.x, p.y, p.z).uv(u, v).color(r, g, b, a).uv2(0xF000F0).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new BlackLightningParticle(level, x, y, z, dx, dy, dz, sprites, false);
        }
    }

    public record WhiteProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new BlackLightningParticle(level, x, y, z, dx, dy, dz, sprites, true);
        }
    }
}
