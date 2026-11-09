package com.onepiece.ultimate.client;

import com.onepiece.ultimate.client.animation.PlayerAnimations;
import com.onepiece.ultimate.config.OPClientConfig;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Screen shake (trauma based, smooth noise), field-of-view punches, impact flashes and local hit-stop.
 */
public final class CameraEffects {
    private static float trauma;
    private static float fovPunch, fovPunchMax;
    private static int fovTicks, fovDuration;
    private static float flash;
    private static int flashTicks, flashDuration;
    private static long seed = 1234;
    private static float darkPulse;
    private static int darkTicks, darkDuration;

    public static void handle(CameraEffectPacket.Type type, float intensity, int duration) {
        switch (type) {
            case SHAKE -> trauma = Math.min(1.5f, trauma + intensity);
            case HITSTOP -> {
                var p = Minecraft.getInstance().player;
                if (p != null) PlayerAnimations.hitstop(p.getId(), duration);
                trauma = Math.min(1.5f, trauma + intensity * 0.15f);
            }
            case FOV_PUNCH -> {
                fovPunchMax = intensity;
                fovDuration = fovTicks = Math.max(1, duration);
            }
            case FLASH -> {
                flash = intensity;
                flashDuration = flashTicks = Math.max(1, duration);
            }
            case DARK_PULSE -> {
                darkPulse = intensity;
                darkDuration = darkTicks = Math.max(1, duration);
                trauma = Math.min(1.5f, trauma + intensity * 0.6f);
            }
        }
    }

    public static void tick() {
        trauma = Math.max(0, trauma - 0.06f);
        if (fovTicks > 0) fovTicks--;
        if (flashTicks > 0) flashTicks--;
        if (darkTicks > 0) darkTicks--;
        seed++;
    }

    public static void reset() {
        trauma = 0;
        fovTicks = flashTicks = darkTicks = 0;
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float strength = (float) (trauma * trauma * OPClientConfig.CAMERA_SHAKE.get());
        if (strength <= 0.0001f) return;
        double t = (seed + event.getPartialTick()) * 0.9;
        event.setYaw(event.getYaw() + (float) (smooth(t, 1) * 4.0 * strength));
        event.setPitch(event.getPitch() + (float) (smooth(t, 2) * 4.0 * strength));
        event.setRoll(event.getRoll() + (float) (smooth(t, 3) * 6.0 * strength));
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (fovTicks <= 0) return;
        float k = (fovTicks - (float) event.getPartialTick()) / fovDuration;
        k = Math.max(0, k);
        event.setFOV(event.getFOV() * (1.0 + fovPunchMax * Math.sin(k * Math.PI) ));
    }

    /** Conqueror's pressure: dark red vignette that pulses and fades. */
    public static void renderPressure(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        if (darkTicks <= 0) return;
        float k = (darkTicks - partial) / darkDuration;
        float pulse = 0.7f + 0.3f * (float) Math.sin((darkDuration - darkTicks + partial) * 0.9);
        float a = Math.max(0, Math.min(1, k * darkPulse * pulse));
        vignette(g, width, height, (int) (a * 200), 0x200000, 0x000000);
    }

    /** Future Sight: a cool, steady cyan vignette while active. */
    public static void renderFutureSight(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var d = com.onepiece.ultimate.data.OPCapabilities.getOrNull(mc.player);
        if (d == null || !d.futureSight()) return;
        float t = mc.player.tickCount + partial;
        int a = (int) (70 + 25 * Math.sin(t * 0.2));
        vignette(g, width, height, a, 0x40C8FF, 0x103050);
    }

    private static void vignette(GuiGraphics g, int w, int h, int alpha, int edge, int inner) {
        if (alpha <= 0) return;
        int band = Math.max(20, Math.min(w, h) / 4);
        int e = (Math.min(255, alpha) << 24) | (edge & 0xFFFFFF);
        int i = inner & 0xFFFFFF;
        g.fillGradient(0, 0, w, band, e, i);                 // top
        g.fillGradient(0, h - band, w, h, i, e);             // bottom
        // sides: rotate a vertical gradient by drawing thin columns
        for (int x = 0; x < band; x += 2) {
            int aa = (int) (Math.min(255, alpha) * (1f - x / (float) band));
            int c = (aa << 24) | (edge & 0xFFFFFF);
            g.fill(x, band, x + 2, h - band, c);
            g.fill(w - x - 2, band, w - x, h - band, c);
        }
    }

    public static void renderFlash(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        if (flashTicks <= 0) return;
        float k = (flashTicks - partial) / flashDuration;
        int a = (int) (Math.max(0, Math.min(1, k * flash)) * 255);
        if (a > 0) g.fill(0, 0, width, height, (a << 24) | 0xFFFFFF);
    }

    /** Cheap smooth pseudo noise in [-1,1]. */
    private static double smooth(double t, int channel) {
        return Math.sin(t * 1.7 + channel * 12.9) * 0.6 + Math.sin(t * 3.3 + channel * 4.1) * 0.4;
    }

    private CameraEffects() {}
}
