package com.onepiece.ultimate.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityBars;
import com.onepiece.ultimate.haki.HakiAbilities;
import com.onepiece.ultimate.haki.HakiAbility;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.ability.AbilityRegistry;
import com.onepiece.ultimate.client.input.KeyBindings;
import com.onepiece.ultimate.config.OPClientConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.Progression;
import com.onepiece.ultimate.fruit.DevilFruit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact ability panel (default: bottom-right, configurable anchor/scale/offset). Leaves the vanilla hotbar
 * untouched. Shows: active transformation badge, fruit + mastery, level, energy bar, and one slot per ability with
 * icon, key hint, cooldown sweep and seconds, charge progress, toggle glow, lock state and energy warning.
 */
public final class AbilityHud {
    private static final int SLOT = 22;
    private static final int HSLOT = 18;
    private static final int GAP = 2;
    private static final int MARGIN = 6;

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        OPPlayerData data = OPCapabilities.getOrNull(mc.player);
        if (data == null) return;
        DevilFruit fruit = data.fruit();
        boolean haki = data.haki().anyAwakened();
        if (fruit == null && !haki && !data.combatMode() && OPClientConfig.HIDE_HUD_WITHOUT_POWERS.get()) return;

        Font font = mc.font;
        int slots = fruit == null ? 0 : fruit.abilities().size();
        int hakiSlots = haki ? HakiAbilities.all().size() : 0;
        int width = Math.max(120, Math.max(slots * SLOT + Math.max(0, slots - 1) * GAP, hakiSlots * HSLOT + Math.max(0, hakiSlots - 1) * GAP));
        int height = 10 + 2 + 5 + (haki ? 4 : 0) + (hakiSlots > 0 ? 4 + HSLOT : 0) + (slots > 0 ? 4 + SLOT : 0);
        float scale = OPClientConfig.HUD_SCALE.get().floatValue();

        int sw = (int) (screenW / scale), sh = (int) (screenH / scale);
        int x, y;
        switch (OPClientConfig.HUD_ANCHOR.get()) {
            case BOTTOM_LEFT -> { x = MARGIN; y = sh - height - MARGIN; }
            case TOP_RIGHT -> { x = sw - width - MARGIN; y = MARGIN + 12; }
            case TOP_LEFT -> { x = MARGIN; y = MARGIN + 12; }
            default -> { x = sw - width - MARGIN; y = sh - height - MARGIN; }
        }
        x += OPClientConfig.HUD_OFFSET_X.get();
        y += OPClientConfig.HUD_OFFSET_Y.get();

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1f);
        RenderSystem.enableBlend();

        float time = mc.player.tickCount + partial;

        // ---- transformation badges
        List<Ability> toggles = new ArrayList<>();
        for (ResourceLocation id : data.activeToggles()) {
            Ability a = AbilityRegistry.get(id);
            if (a != null) toggles.add(a);
        }
        int bx = x, by = y - 13;
        for (Ability a : toggles) {
            String label = a.displayName().getString().toUpperCase();
            int w = font.width(label) + 10;
            if (bx > x && bx + w > x + width) { // wrap to a new row above
                bx = x;
                by -= 13;
            }
            float pulse = 0.65f + 0.35f * Mth.sin(time * 0.25f);
            int alpha = (int) (200 * pulse);
            g.fill(bx, by, bx + w, by + 11, (alpha << 24) | (a.color() & 0xFFFFFF));
            g.fill(bx, by + 11, bx + w, by + 12, 0xFFFFFFFF);
            g.drawString(font, label, bx + 5, by + 2, 0xFFFFFFFF, true);
            bx += w + 3;
        }
        if (data.seaWeakened()) {
            String label = Component.translatable("hud.onepiece.weakened").getString();
            g.fill(bx, by, bx + font.width(label) + 10, by + 11, 0xC0206080);
            g.drawString(font, label, bx + 5, by + 2, 0xFF9FE8FF, true);
        }

        // ---- header: fruit + mastery | level
        g.fill(x - 3, y - 1, x + width + 3, y + height + 2, 0x70000000);
        g.fill(x - 3, y - 1, x + width + 3, y, 0x40FFFFFF);
        Progression prog = data.progression();
        String right = "Lv " + prog.level();
        if (prog.unspentPoints() > 0) right += " +" + prog.unspentPoints();
        if (fruit != null) {
            String left = fruit.displayName().getString() + " §7M" + data.mastery();
            g.drawString(font, left, x, y + 1, 0xFFFFD27A, true);
        } else {
            g.drawString(font, Component.translatable(haki ? "hud.onepiece.haki" : "hud.onepiece.combat_mode"), x, y + 1,
                    haki ? 0xFFB48CFF : 0xFFFF7A5A, true);
        }
        g.drawString(font, right, x + width - font.width(right), y + 1, prog.unspentPoints() > 0 ? 0xFF7CFF8A : 0xFFDDDDDD, true);
        // xp line
        float xpFrac = Math.min(1f, prog.xp() / (float) Progression.xpForNext(prog.level()));
        g.fill(x, y + 10, x + width, y + 11, 0x60FFFFFF);
        g.fill(x, y + 10, x + (int) (width * xpFrac), y + 11, 0xFF7CFF8A);

        // ---- energy bar
        int ey = y + 12;
        float frac = data.energy() / Math.max(1f, data.maxEnergy());
        g.fill(x, ey, x + width, ey + 5, 0xA0101010);
        int fillW = (int) (width * frac);
        int top = frac < 0.25f ? 0xFFFF5A3A : 0xFFFFE27A;
        int bottom = frac < 0.25f ? 0xFFB02010 : 0xFFE09A20;
        g.fillGradient(x, ey, x + fillW, ey + 5, top, bottom);
        if (data.combatMode() && fruit != null) {
            g.fill(x - 3, ey, x - 1, ey + 5, 0xFFFF5A3A); // combat mode marker
        }
        int cy = ey + 5;

        // ---- haki gauge + haki bar
        if (haki) {
            float hf = data.hakiGauge() / Math.max(1f, data.maxHaki());
            g.fill(x, cy + 1, x + width, cy + 4, 0xA0101010);
            g.fillGradient(x, cy + 1, x + (int) (width * hf), cy + 4, 0xFFB48CFF, 0xFF6A3AD0);
            cy += 4;
            int sy = cy + 4;
            for (int i = 0; i < hakiSlots; i++) drawSlot(g, font, data, AbilityBars.HAKI, i, x + i * (HSLOT + GAP), sy, HSLOT, time);
            cy = sy + HSLOT;
        }

        // ---- ability slots
        if (fruit != null) {
            int sy = cy + 4;
            for (int i = 0; i < slots; i++) {
                int sx = x + i * (SLOT + GAP);
                drawSlot(g, font, data, AbilityBars.FRUIT, i, sx, sy, SLOT, time);
            }
        }

        RenderSystem.disableBlend();
        g.pose().popPose();
    }

    private static void drawSlot(GuiGraphics g, Font font, OPPlayerData data, int bar, int slot, int x, int y, int SLOT, float time) {
        Ability base = AbilityBars.base(data, bar, slot);
        if (base == null) return;
        Ability ability = AbilityManager.abilityInSlot(data, bar, slot);
        if (ability == null) ability = base;
        boolean locked = !ability.isUnlocked(data);
        boolean toggled = data.isToggleActive(base.id());
        boolean variant = ability != base;
        float cd = data.cooldowns().progress(base.cooldownKey());
        boolean poor = !locked && data.resource(ability.resource()) < ability.energyCost() && !toggled;
        int icon = SLOT - 6;

        // frame
        int border = toggled ? pulseColor(ability.color(), time) : variant ? 0xFFFF6B5A : 0xFF3A3A3A;
        g.fill(x, y, x + SLOT, y + SLOT, border);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xE0181818);
        g.fillGradient(x + 1, y + 1, x + SLOT - 1, y + SLOT / 2, 0x30FFFFFF, 0x00FFFFFF);

        // icon
        if (locked) g.setColor(0.25f, 0.25f, 0.25f, 1f);
        else if (poor || data.seaWeakened()) g.setColor(1f, 0.45f, 0.45f, 1f);
        g.blit(ability.icon(), x + 3, y + 3, icon, icon, 0, 0, 32, 32, 32, 32);
        g.setColor(1f, 1f, 1f, 1f);

        // cooldown sweep (fills from the top and drains downward)
        if (cd > 0 && !toggled) {
            int h = (int) Math.ceil((SLOT - 2) * cd);
            g.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + h, 0xB0000000);
            int seconds = (int) Math.ceil(data.cooldowns().remaining(base.cooldownKey()) / 20f);
            String s = String.valueOf(seconds);
            g.drawString(font, s, x + (SLOT - font.width(s)) / 2, y + SLOT / 2 - 4, 0xFFFFFFFF, true);
        }

        // channel progress (charge or hold)
        if (data.channeling() != null && data.channeling().equals(ability.id())) {
            float p = ability.activation() == Ability.Activation.CHARGE
                    ? data.channelTicks() / (float) Math.max(1, ability.maxCharge())
                    : 1f - data.channelTicks() / (float) Math.max(1, ability.maxHold());
            p = Mth.clamp(p, 0f, 1f);
            g.fill(x + 1, y + SLOT - 4, x + SLOT - 1, y + SLOT - 1, 0xC0000000);
            g.fill(x + 1, y + SLOT - 4, x + 1 + (int) ((SLOT - 2) * p), y + SLOT - 1, 0xFF000000 | ability.color());
            if (p >= 1f && ability.activation() == Ability.Activation.CHARGE) {
                g.fill(x, y, x + SLOT, y + 1, 0xFFFFFFFF);
            }
        }

        if (locked) {
            boolean noPotential = ability instanceof HakiAbility ha && ha.type() == HakiType.CONQUEROR && !data.haki().hasConquerorPotential();
            String m = noPotential ? "-" : ability.requirementLabel();
            g.pose().pushPose();
            g.pose().translate(x + SLOT / 2f, y + SLOT / 2f - 3, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.drawString(font, m, -font.width(m) / 2, 0, 0xFFAAAAAA, true);
            g.pose().popPose();
        }

        // key hint
        var keys = bar == AbilityBars.HAKI ? KeyBindings.HAKI : KeyBindings.ABILITIES;
        if (OPClientConfig.SHOW_KEY_HINTS.get() && slot < keys.length) {
            String key = KeyBindings.shortName(keys[slot]);
            g.pose().pushPose();
            g.pose().translate(x + 2, y + 2, 200);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.drawString(font, key, 0, 0, 0xFFFFE27A, true);
            g.pose().popPose();
        }
    }

    private static int pulseColor(int rgb, float time) {
        float k = 0.6f + 0.4f * Mth.sin(time * 0.3f);
        int r = (int) (((rgb >> 16) & 255) * k + 255 * (1 - k) * 0.3f);
        int gg = (int) (((rgb >> 8) & 255) * k + 255 * (1 - k) * 0.3f);
        int b = (int) ((rgb & 255) * k + 255 * (1 - k) * 0.3f);
        return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, gg) << 8) | Math.min(255, b);
    }

    private AbilityHud() {}
}
