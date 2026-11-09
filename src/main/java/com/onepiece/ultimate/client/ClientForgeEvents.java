package com.onepiece.ultimate.client;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.client.animation.PlayerAnimations;
import com.onepiece.ultimate.client.input.InputHandler;
import com.onepiece.ultimate.data.OPCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    /** Players whose root transform was pushed in Pre and must be popped in Post. */
    private static final Set<Integer> PUSHED = new HashSet<>();

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.isPaused()) {
            OPCapabilities.get(mc.player).ifPresent(d -> d.cooldowns().tick());
            PlayerAnimations.tick();
            KenVision.tick();
            com.onepiece.ultimate.client.render.ArmamentVisuals.tick();
            com.onepiece.ultimate.client.transform.TransformationVisuals.tick();
            infusionSparks(mc);
            CameraEffects.tick();
            ClientState.tick();
        }
        InputHandler.tick();
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        InputHandler.onInteraction(event);
    }

    /** Full Zoan forms replace the player model entirely. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void replaceBody(RenderPlayerEvent.Pre event) {
        var visual = com.onepiece.ultimate.client.transform.TransformationVisuals.of(event.getEntity());
        if (visual != null && visual.replacesBody() && event.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer p) {
            event.setCanceled(true);
            visual.renderBody(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), p, event.getPartialTick());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderPre(RenderPlayerEvent.Pre event) {
        if (PlayerAnimations.applyRoot(event.getEntity(), event.getPoseStack(), event.getPartialTick())) {
            PUSHED.add(event.getEntity().getId());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void renderPost(RenderPlayerEvent.Post event) {
        if (PUSHED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        var cam = event.getCamera();
        if (cam.isDetached() && cam.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer p) {
            var visual = com.onepiece.ultimate.client.transform.TransformationVisuals.of(p);
            float extra = visual == null ? 0f : visual.cameraDistanceBonus(p, (float) event.getPartialTick());
            if (extra > 0.05f) cam.move(-cam.getMaxZoom(extra), 0, 0); // same wall-aware pull-back vanilla uses
        }
        CameraEffects.onCameraAngles(event);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        CameraEffects.onFov(event);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PlayerAnimations.clear();
        KenVision.clear();
        com.onepiece.ultimate.client.render.ArmamentVisuals.clear();
        CameraEffects.reset();
        ClientState.reset();
        InputHandler.reset();
        PUSHED.clear();
    }

    /** Conqueror's infusion: black lightning crackles around the user's arms (purely client-side). */
    private static void infusionSparks(Minecraft mc) {
        if (mc.level == null) return;
        for (var p : mc.level.players()) {
            var d = OPCapabilities.getOrNull(p);
            if (d == null || !d.isToggleActive(com.onepiece.ultimate.haki.HakiAbilities.CONQUEROR_INFUSION.id())) continue;
            if (p.getRandom().nextInt(3) != 0) continue;
            double yaw = Math.toRadians(p.yBodyRot);
            int side = p.getRandom().nextBoolean() ? 1 : -1;
            double x = p.getX() - Math.cos(yaw) * 0.4 * side, z = p.getZ() - Math.sin(yaw) * 0.4 * side;
            var r = p.getRandom();
            mc.level.addParticle(com.onepiece.ultimate.registry.ModParticles.BLACK_LIGHTNING.get(), x, p.getY() + 0.8 + r.nextFloat() * 0.6, z,
                    (r.nextFloat() - 0.5) * 0.9, (r.nextFloat() - 0.5) * 0.9, (r.nextFloat() - 0.5) * 0.9);
        }
    }

    private ClientForgeEvents() {}
}
