package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.haki.HakiAbilities;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Busoshoku coating look, used by the third-person layer and the first-person arms:
 * <ol>
 *   <li><b>Aura</b> – on activation a swirling violet flame engulfs the limbs (scrolling energy texture on an
 *       inflated copy of the part);</li>
 *   <li><b>Hardening</b> – the flames recede as the limb turns solid black;</li>
 *   <li><b>Gloss</b> – emissive highlight streaks on every face give the polished, metallic shine; a faint
 *       violet edge keeps flickering while the coating is held.</li>
 * </ol>
 * The activation timeline is tracked per player on the client (leak-free: entries are dropped when inactive).
 */
public final class ArmamentVisuals {
    public static final ResourceLocation FLAME = OnePieceMod.id("textures/entity/haki_flame.png");
    public static final ResourceLocation GLOSS = OnePieceMod.id("textures/entity/armament_gloss.png");
    private static final int ACTIVATION_TICKS = 18;
    private static final Int2IntOpenHashMap ACTIVATED_AT = new Int2IntOpenHashMap();
    /** Players we have seen without the coating – only for them is the activation animated. */
    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet SEEN_INACTIVE = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    private static final Vector3f DARK_DUST = new Vector3f(0.08f, 0.02f, 0.15f);

    /** Colours of the coating: the classic violet-flame Busoshoku, or Gear Fourth's crimson-edged version. */
    public enum Palette {
        VIOLET(0.42f, 0.14f, 0.8f, 0.92f, 0.9f, 1f, new Vector3f(0.55f, 0.2f, 1f)),
        CRIMSON(0.6f, 0.05f, 0.1f, 0.9f, 0.55f, 0.6f, new Vector3f(0.95f, 0.15f, 0.2f));
        final float fr, fg, fb, gr, gg, gb;
        final Vector3f dust;
        Palette(float fr, float fg, float fb, float gr, float gg, float gb, Vector3f dust) {
            this.fr = fr; this.fg = fg; this.fb = fb; this.gr = gr; this.gg = gg; this.gb = gb; this.dust = dust;
        }
    }

    public static Palette palette(Player p) {
        OPPlayerData d = OPCapabilities.getOrNull(p);
        if (d == null) return Palette.VIOLET;
        boolean gearFourth = d.isToggleActive(com.onepiece.ultimate.fruit.gomu.GomuAbilities.BOUNDMAN.id())
                || d.isToggleActive(com.onepiece.ultimate.fruit.gomu.GomuAbilities.SNAKEMAN.id());
        return gearFourth ? Palette.CRIMSON : Palette.VIOLET;
    }

    public static boolean active(Player p) {
        OPPlayerData d = OPCapabilities.getOrNull(p);
        return d != null && d.isToggleActive(HakiAbilities.ARMAMENT.id());
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVATED_AT.clear();
            return;
        }
        for (Player p : mc.level.players()) {
            boolean on = active(p);
            if (!on) {
                ACTIVATED_AT.remove(p.getId());
                SEEN_INACTIVE.add(p.getId());
                continue;
            }
            if (!ACTIVATED_AT.containsKey(p.getId())) {
                // animate only if we saw it switch on; a player walking into view already coated is shown hardened
                boolean justStarted = SEEN_INACTIVE.remove(p.getId());
                ACTIVATED_AT.put(p.getId(), justStarted ? p.tickCount : p.tickCount - 1000);
            }
            float pr = progress(p, 0);
            boolean ownView = p == mc.player && mc.options.getCameraType().isFirstPerson();
            if (pr < 1f && !ownView) {
                var r = p.getRandom();
                for (int i = 0; i < 3; i++) {
                    double yaw = Math.toRadians(p.yBodyRot);
                    int side = r.nextBoolean() ? 1 : -1;
                    double x = p.getX() - Math.cos(yaw) * 0.38 * side, z = p.getZ() - Math.sin(yaw) * 0.38 * side;
                    double y = p.getY() + 0.7 + r.nextFloat() * 0.8;
                    mc.level.addParticle(new DustParticleOptions(r.nextBoolean() ? palette(p).dust : DARK_DUST, 0.9f + r.nextFloat() * 0.6f),
                            x, y, z, 0, 0.04, 0);
                }
            }
        }
        ACTIVATED_AT.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        SEEN_INACTIVE.removeIf(id -> mc.level.getEntity(id) == null);
    }

    /** 0 → 1 over the activation; 1 = fully hardened. */
    public static float progress(Player p, float partial) {
        if (!ACTIVATED_AT.containsKey(p.getId())) return 1f;
        return Mth.clamp((p.tickCount + partial - ACTIVATED_AT.get(p.getId())) / ACTIVATION_TICKS, 0f, 1f);
    }

    public static void clear() {
        ACTIVATED_AT.clear();
        SEEN_INACTIVE.clear();
    }

    /**
     * Renders the coating over the given parts (pairs of base part and its outer skin layer) in their current pose.
     */
    public static void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial, ModelPart... parts) {
        float pr = progress(player, partial);
        float age = player.tickCount + partial;
        Palette pal = palette(player);

        // Order matters: the aura is an inflated copy that writes depth, so the solid coating must be drawn first.
        // 1. hardening to black, 2. polished gloss
        float hard = smoothstep(0.3f, 1f, pr);
        if (hard > 0f) {
            VertexConsumer black = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
            for (ModelPart part : parts) if (part.visible) part.render(ps, black, light, OverlayTexture.NO_OVERLAY, 0.035f, 0.025f, 0.06f, 0.97f * hard);
            VertexConsumer gloss = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOSS));
            float shine = hard * (0.75f + 0.25f * Mth.sin(age * 0.12f));
            for (ModelPart part : parts) if (part.visible) part.render(ps, gloss, light, OverlayTexture.NO_OVERLAY, pal.gr, pal.gg, pal.gb, shine);
        }

        // 3. flame aura on top (additive) (strong during activation, faint edge afterwards)
        // additive blending: colour is the brightness of the flames
        float idle = pal == Palette.CRIMSON ? 0.11f + 0.05f * Mth.sin(age * 0.5f) : 0.10f + 0.05f * Mth.sin(age * 0.35f);
        float flame = pr < 1f ? Mth.sin(pr * Mth.PI) * 0.75f + (1f - pr) * 0.15f : idle;
        if (flame > 0.01f) {
            VertexConsumer swirl = buffers.getBuffer(RenderType.energySwirl(FLAME, (age * 0.011f) % 1f, (age * 0.035f) % 1f));
            float inflate = 1.05f + 0.12f * Math.min(1f, flame);
            for (ModelPart part : parts) renderScaled(part, ps, swirl, light, inflate, pal.fr * flame, pal.fg * flame, pal.fb * flame, 1f);
        }

    }

    private static void renderScaled(ModelPart part, PoseStack ps, VertexConsumer vc, int light, float s, float r, float g, float b, float a) {
        if (!part.visible) return;
        float x = part.xScale, y = part.yScale, z = part.zScale;
        part.xScale = x * s;
        part.yScale = y * (1f + (s - 1f) * 0.6f);
        part.zScale = z * s;
        part.render(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, a);
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
    }

    private static float smoothstep(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    private ArmamentVisuals() {}
}
