package com.onepiece.ultimate.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityRegistry;
import com.onepiece.ultimate.client.ClientState;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.config.OPClientConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Crosshair-area feedback: charge meter, combat-mode brackets and the combo hit counter. */
public final class CombatHud {
    private static final int HEAVY_MAX = 25;

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        OPPlayerData data = OPCapabilities.getOrNull(mc.player);
        if (data == null) return;
        Font font = mc.font;
        int cx = w / 2, cy = h / 2;
        RenderSystem.enableBlend();

        if (data.combatMode()) {
            int c = 0x90FF7A5A;
            g.fill(cx - 9, cy - 9, cx - 6, cy - 8, c);
            g.fill(cx - 9, cy - 9, cx - 8, cy - 6, c);
            g.fill(cx + 6, cy - 9, cx + 9, cy - 8, c);
            g.fill(cx + 8, cy - 9, cx + 9, cy - 6, c);
            g.fill(cx - 9, cy + 8, cx - 6, cy + 9, c);
            g.fill(cx - 9, cy + 6, cx - 8, cy + 9, c);
            g.fill(cx + 6, cy + 8, cx + 9, cy + 9, c);
            g.fill(cx + 8, cy + 6, cx + 9, cy + 9, c);
        }

        // ---- charge meter
        ResourceLocation channel = data.channeling();
        if (channel != null) {
            float p;
            int color;
            boolean charge = true;
            if (channel.equals(CombatManager.HEAVY_CHANNEL)) {
                p = data.channelTicks() / (float) HEAVY_MAX;
                color = 0xFFFFFFFF;
            } else {
                Ability a = AbilityRegistry.get(channel);
                if (a == null) {
                    p = 0;
                    color = 0xFFFFFFFF;
                } else if (a.activation() == Ability.Activation.CHARGE) {
                    p = data.channelTicks() / (float) Math.max(1, a.maxCharge());
                    color = 0xFF000000 | a.color();
                } else {
                    p = 1f - data.channelTicks() / (float) Math.max(1, a.maxHold());
                    color = 0xFF000000 | a.color();
                    charge = false;
                }
            }
            p = Mth.clamp(p, 0, 1);
            int bw = 50;
            int bx = cx - bw / 2, by = cy + 14;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 4, 0xA0000000);
            g.fill(bx, by, bx + (int) (bw * p), by + 3, color);
            if (charge && p >= 1f) {
                float t = mc.player.tickCount + partial;
                int a = (int) (180 + 75 * Mth.sin(t * 0.8f));
                String max = "MAX";
                g.drawString(font, max, cx - font.width(max) / 2, by + 6, (a << 24) | 0xFFE27A, true);
            }
        }

        // ---- Observation Haki: direction of creatures with hostile intent
        if (ClientState.threats.length > 0 && mc.level != null) {
            float t = mc.player.tickCount + partial;
            int alpha = (int) (170 + 60 * Mth.sin(t * 0.4f));
            for (int id : ClientState.threats) {
                var e = mc.level.getEntity(id);
                if (e == null) continue;
                double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
                float angle = (float) Math.toDegrees(Math.atan2(-dx, dz)) - mc.player.getViewYRot(partial);
                angle = Mth.wrapDegrees(angle);
                g.pose().pushPose();
                g.pose().translate(cx, cy, 0);
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
                int col = (alpha << 24) | 0xFF3A3A;
                // chevron pointing outward
                g.fill(-1, -46, 1, -40, col);
                g.fill(-3, -44, -1, -41, col);
                g.fill(1, -44, 3, -41, col);
                g.fill(-5, -42, -3, -40, col);
                g.fill(3, -42, 5, -40, col);
                g.pose().popPose();
            }
        }

        // ---- combo counter
        if (OPClientConfig.SHOW_COMBO_COUNTER.get() && ClientState.comboFadeTicks > 0 && ClientState.comboDisplay >= 2) {
            float alpha = Math.min(1f, (ClientState.comboFadeTicks - partial) / 15f);
            float pop = 1f + Math.max(0, ClientState.comboPopTicks - partial) / 6f * 0.6f;
            int a = (int) (alpha * 255) << 24;
            String n = String.valueOf(ClientState.comboDisplay);
            int color = ClientState.comboDisplay >= 20 ? 0xFF5A3A : ClientState.comboDisplay >= 10 ? 0xFFB347 : 0xFFE27A;
            g.pose().pushPose();
            g.pose().translate(cx + 48, cy - 26, 0);
            g.pose().scale(2.2f * pop, 2.2f * pop, 1f);
            g.drawString(font, n, -font.width(n) / 2, -4, a | color, true);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(cx + 48, cy - 10, 0);
            g.drawString(font, "HITS", -font.width("HITS") / 2, 0, a | 0xFFFFFF, true);
            g.pose().popPose();
        }
        RenderSystem.disableBlend();
    }

    private CombatHud() {}
}
