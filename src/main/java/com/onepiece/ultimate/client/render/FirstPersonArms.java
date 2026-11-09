package com.onepiece.ultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.client.animation.AnimationController;
import com.onepiece.ultimate.client.animation.Bone;
import com.onepiece.ultimate.client.animation.PlayerAnimations;
import com.onepiece.ultimate.client.animation.Pose;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.gomu.GomuAbilities;
import com.onepiece.ultimate.haki.HakiAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * First-person combat arms. With an empty hand and combat mode / a power active, both arms are drawn and driven
 * by the same keyframe animations as the third-person body (the FP_RIGHT / FP_LEFT bones), including hit-stop and
 * cross-fades. Busoshoku and Gear Second coatings are drawn on the arms too. Holding an item keeps vanilla rendering.
 */
@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonArms {
    private static final Pose POSE = new Pose();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || player.isSpectator() || player.isInvisible() || !player.getMainHandItem().isEmpty()) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null) return;
        AnimationController c = PlayerAnimations.existing(player.getId());
        boolean animated = c != null && c.hasFirstPerson();
        boolean powers = data.isToggleActive(HakiAbilities.ARMAMENT.id()) || data.isToggleActive(GomuAbilities.GEAR_SECOND.id());
        if (!data.combatMode() && !animated && !powers) return;

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        float partial = event.getPartialTick();
        POSE.reset();
        if (c != null) c.applySpecial(POSE, partial, Bone.FP_RIGHT, Bone.FP_LEFT);
        PoseStack ps = event.getPoseStack();
        renderArm(ps, event.getMultiBufferSource(), event.getPackedLight(), player, true, data, partial);
        renderArm(ps, event.getMultiBufferSource(), event.getPackedLight(), player, false, data, partial);
    }

    private static void renderArm(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, boolean right,
                                  OPPlayerData data, float partial) {
        Bone bone = right ? Bone.FP_RIGHT : Bone.FP_LEFT;
        float f = right ? 1f : -1f;
        float rx = POSE.get(bone, 0), ry = POSE.get(bone, 1), rz = POSE.get(bone, 2);
        float px = POSE.get(bone, 3), py = POSE.get(bone, 4), pz = POSE.get(bone, 5);
        float sx = POSE.get(bone, 6), sy = POSE.get(bone, 7), sz = POSE.get(bone, 8);

        ps.pushPose();
        // vanilla resting place of an empty first-person arm
        ps.translate(f * 0.64f, -0.6f, -0.72f);
        // animated offset & rotation in camera space (mirrored for the left arm)
        ps.translate(f * px / 16f, py / 16f, -pz / 16f);
        ps.mulPose(Axis.XP.rotationDegrees(rx));
        ps.mulPose(Axis.YP.rotationDegrees(f * ry));
        ps.mulPose(Axis.ZP.rotationDegrees(f * rz));
        // vanilla arm placement (ItemInHandRenderer#renderPlayerArm with no swing)
        ps.mulPose(Axis.YP.rotationDegrees(f * 45f));
        ps.translate(f * -1.0f, 3.6f, 3.5f);
        ps.mulPose(Axis.ZP.rotationDegrees(f * 120f));
        ps.mulPose(Axis.XP.rotationDegrees(200f));
        ps.mulPose(Axis.YP.rotationDegrees(f * -135f));
        ps.translate(f * 5.6f, 0f, 0f);
        if (sx != 1 || sy != 1 || sz != 1) ps.scale(sx, sy, sz);

        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (right) renderer.renderRightHand(ps, buffers, light, player);
        else renderer.renderLeftHand(ps, buffers, light, player);

        PlayerModel<AbstractClientPlayer> m = renderer.getModel();
        ModelPart arm = right ? m.rightArm : m.leftArm;
        ModelPart sleeve = right ? m.rightSleeve : m.leftSleeve;
        if (data.isToggleActive(HakiAbilities.ARMAMENT.id())) {
            ArmamentVisuals.render(ps, buffers, light, player, partial, arm, sleeve);
        } else if (data.isToggleActive(GomuAbilities.GEAR_SECOND.id()) && arm.visible) {
            float a = 0.32f + 0.12f * Mth.sin((player.tickCount + partial) * 0.35f);
            var vc = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
            arm.render(ps, vc, light, OverlayTexture.NO_OVERLAY, 1f, 0.25f, 0.22f, a);
        }
        ps.popPose();
    }

    private FirstPersonArms() {}
}
