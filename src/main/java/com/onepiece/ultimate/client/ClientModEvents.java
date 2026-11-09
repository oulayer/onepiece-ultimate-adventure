package com.onepiece.ultimate.client;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.client.animation.AnimatedPlayerModel;
import com.onepiece.ultimate.client.animation.AnimationLoader;
import com.onepiece.ultimate.client.hud.AbilityHud;
import com.onepiece.ultimate.client.hud.CombatHud;
import com.onepiece.ultimate.client.input.KeyBindings;
import com.onepiece.ultimate.client.particle.ImpactSparkParticle;
import com.onepiece.ultimate.client.particle.RingParticle;
import com.onepiece.ultimate.client.particle.SteamParticle;
import com.onepiece.ultimate.client.render.DebrisRenderer;
import com.onepiece.ultimate.client.render.GearSecondLayer;
import com.onepiece.ultimate.client.render.StretchFistRenderer;
import com.onepiece.ultimate.registry.ModEntities;
import com.onepiece.ultimate.registry.ModParticles;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        KeyBindings.all().forEach(event::register);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "abilities", AbilityHud::render);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "combat", CombatHud::render);
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "conqueror_pressure", CameraEffects::renderPressure);
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "future_sight", CameraEffects::renderFutureSight);
        event.registerAboveAll("impact_flash", CameraEffects::renderFlash);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.STEAM.get(), SteamParticle.Provider::new);
        event.registerSpriteSet(ModParticles.IMPACT_SPARK.get(), ImpactSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RING.get(), RingParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BLACK_LIGHTNING.get(), com.onepiece.ultimate.client.particle.BlackLightningParticle.Provider::new);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STRETCH_FIST.get(), StretchFistRenderer::new);
        event.registerEntityRenderer(ModEntities.DEBRIS.get(), DebrisRenderer::new);
    }

    @SubscribeEvent
    public static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(AnimationLoader.INSTANCE);
    }

    /** Swap the player model for the animated one and add transformation layers. */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<?, ?> r = event.getSkin(skin);
            if (!(r instanceof PlayerRenderer renderer)) continue;
            boolean slim = "slim".equals(skin);
            AnimatedPlayerModel model = new AnimatedPlayerModel(
                    event.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
            ((LivingEntityRenderer) renderer).model = model;
            renderer.addLayer(new GearSecondLayer(renderer));
            renderer.addLayer(new com.onepiece.ultimate.client.render.ArmamentLayer(renderer));
        }
    }

    private ClientModEvents() {}
}
