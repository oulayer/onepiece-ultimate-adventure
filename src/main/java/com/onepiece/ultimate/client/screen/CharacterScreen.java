package com.onepiece.ultimate.client.screen;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityBars;
import com.onepiece.ultimate.client.input.KeyBindings;
import com.onepiece.ultimate.combat.DamageCalculator;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.data.Progression;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.fruit.DevilFruitItem;
import com.onepiece.ultimate.haki.HakiAbilities;
import com.onepiece.ultimate.haki.HakiAbility;
import com.onepiece.ultimate.haki.HakiData;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.SpendStatPacket;
import com.onepiece.ultimate.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Character sheet with three tabs:
 * <ul>
 *   <li><b>Status</b> – spend attribute points (shift-click = 5) and see the resulting derived stats;</li>
 *   <li><b>Devil Fruit</b> – fruit card, mastery, and one card per technique (key, type, cost, cooldown, unlock);</li>
 *   <li><b>Haki</b> – a card per Haki type with its level, progress, awakening condition and techniques.</li>
 * </ul>
 * A live 3D preview of the character (following the mouse) and the main gauges stay on the left.
 */
public class CharacterScreen extends Screen {
    private enum Tab { STATUS, FRUIT, HAKI }

    private static final int GOLD = 0xFFFFD27A, TEXT = 0xFFE8E8E8, MUTED = 0xFF8A8A92, PANEL = 0xE0121218, CARD = 0xC01C1C26;
    private static Tab tab = Tab.STATUS;

    private int left, top, w, h;
    private final List<Runnable> clickActions = new ArrayList<>();
    private final List<int[]> clickAreas = new ArrayList<>();
    @Nullable private List<FormattedCharSequence> tooltip;

    public CharacterScreen() {
        super(Component.translatable("screen.onepiece.character"));
    }

    @Nullable
    private OPPlayerData data() {
        return minecraft == null ? null : OPCapabilities.getOrNull(minecraft.player);
    }

    @Override
    protected void init() {
        w = Math.min(520, width - 16);
        h = Math.min(270, height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;
    }

    // ================================================================== frame

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        clickActions.clear();
        clickAreas.clear();
        tooltip = null;
        OPPlayerData d = data();

        // window
        g.fill(left - 1, top - 1, left + w + 1, top + h + 1, 0xFF6A5326);
        g.fill(left, top, left + w, top + h, PANEL);
        g.fillGradient(left, top, left + w, top + 26, 0xFF2A2216, 0xE0121218);
        g.fill(left, top + 26, left + w, top + 27, 0x806A5326);
        g.drawString(font, title, left + 10, top + 9, GOLD, true);

        // tabs
        int tx = left + w - 8;
        Tab[] tabs = Tab.values();
        for (int i = tabs.length - 1; i >= 0; i--) {
            Tab t = tabs[i];
            Component label = Component.translatable("screen.onepiece.tab." + t.name().toLowerCase());
            int tw = font.width(label) + 16;
            tx -= tw;
            boolean sel = tab == t, hover = in(mx, my, tx, top + 5, tw, 18);
            g.fill(tx, top + 5, tx + tw, top + 23, sel ? 0xFF3A2E18 : hover ? 0x80302818 : 0x40000000);
            if (sel) g.fill(tx, top + 22, tx + tw, top + 23, GOLD);
            g.drawString(font, label, tx + 8, top + 10, sel ? GOLD : hover ? TEXT : MUTED, false);
            final Tab target = t;
            click(tx, top + 5, tw, 18, () -> tab = target);
            tx -= 3;
        }

        if (d == null) {
            super.render(g, mx, my, partial);
            return;
        }
        renderSidebar(g, d, mx, my);
        int cx = left + 132, cy = top + 34, cw = w - 140, ch = h - 42;
        switch (tab) {
            case STATUS -> renderStatus(g, d, cx, cy, cw, ch, mx, my);
            case FRUIT -> renderFruit(g, d, cx, cy, cw, ch, mx, my);
            case HAKI -> renderHaki(g, d, cx, cy, cw, ch, mx, my);
        }
        super.render(g, mx, my, partial);
        if (tooltip != null) g.renderTooltip(font, tooltip, mx, my);
    }

    private void renderSidebar(GuiGraphics g, OPPlayerData d, int mx, int my) {
        int x = left + 8, y = top + 34, sw = 116;
        card(g, x, y, sw, h - 42, 0xFF2A2A36);
        // live preview
        if (minecraft != null && minecraft.player != null) {
            g.enableScissor(x + 1, y + 1, x + sw - 1, y + 118);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + sw / 2, y + 110, 46, x + sw / 2f - mx, y + 50f - my, minecraft.player);
            g.disableScissor();
            g.drawCenteredString(font, minecraft.player.getName(), x + sw / 2, y + 4, TEXT);
        }
        Progression p = d.progression();
        int by = y + 122;
        String lvl = Component.translatable("screen.onepiece.level", p.level()).getString();
        g.drawString(font, lvl, x + 6, by, GOLD, true);
        long need = Progression.xpForNext(p.level());
        bar(g, x + 6, by + 11, sw - 12, 3, p.xp() / (float) need, 0xFF7CFF8A);
        small(g, p.xp() + " / " + need + " XP", x + 6, by + 16, MUTED);
        bar(g, x + 6, by + 28, sw - 12, 4, d.energy() / d.maxEnergy(), 0xFFFFE27A);
        small(g, Component.translatable("screen.onepiece.energy_bar", Math.round(d.energy()), Math.round(d.maxEnergy())).getString(), x + 6, by + 34, MUTED);
        if (d.haki().anyAwakened()) {
            bar(g, x + 6, by + 46, sw - 12, 4, d.hakiGauge() / d.maxHaki(), 0xFFB48CFF);
            small(g, Component.translatable("screen.onepiece.haki_bar", Math.round(d.hakiGauge()), Math.round(d.maxHaki())).getString(), x + 6, by + 52, MUTED);
        }
        DevilFruit fruit = d.fruit();
        if (fruit != null) {
            small(g, fruit.displayName().getString(), x + 6, by + 66, 0xFFFFB0E0);
            small(g, Component.translatable("screen.onepiece.mastery", d.mastery()).getString(), x + 6, by + 74, MUTED);
        }
    }

    // ================================================================== status

    private void renderStatus(GuiGraphics g, OPPlayerData d, int x, int y, int cw, int ch, int mx, int my) {
        Progression p = d.progression();
        String pts = Component.translatable("screen.onepiece.points", p.unspentPoints()).getString();
        g.drawString(font, pts, x, y, p.unspentPoints() > 0 ? 0xFF7CFF8A : MUTED, true);
        small(g, Component.translatable("screen.onepiece.shift_hint").getString(), x + font.width(pts) + 8, y + 2, MUTED);

        PlayerStat[] stats = PlayerStat.values();
        int colW = (cw - 6) / 2;
        for (int i = 0; i < stats.length; i++) {
            PlayerStat s = stats[i];
            int cx = x + (i % 2) * (colW + 6), cy = y + 14 + (i / 2) * 44;
            card(g, cx, cy, colW, 40, 0xFF000000 | s.color);
            g.fill(cx + 4, cy + 4, cx + 18, cy + 18, 0xFF000000 | s.color);
            g.drawCenteredString(font, s.name().substring(0, 1), cx + 11, cy + 7, 0xFF101010);
            g.drawString(font, s.displayName(), cx + 22, cy + 4, TEXT, false);
            String v = p.get(s) + "/" + PlayerStat.MAX_POINTS;
            small(g, v, cx + 22, cy + 14, MUTED);
            bar(g, cx + 4, cy + 22, colW - 30, 3, p.get(s) / (float) PlayerStat.MAX_POINTS, 0xFF000000 | s.color);
            wrapSmall(g, s.description(), cx + 4, cy + 28, colW - 30, MUTED, 1);
            boolean can = p.unspentPoints() > 0 && p.get(s) < PlayerStat.MAX_POINTS;
            int bx = cx + colW - 22, byy = cy + 12;
            boolean hover = can && in(mx, my, bx, byy, 18, 18);
            g.fill(bx, byy, bx + 18, byy + 18, can ? (hover ? 0xFF7CFF8A : 0xFF3A6A40) : 0xFF303030);
            g.drawCenteredString(font, "+", bx + 9, byy + 5, can ? 0xFF101010 : MUTED);
            if (can) click(bx, byy, 18, 18, () -> {
                int n = hasShiftDown() ? Math.min(5, p.unspentPoints()) : 1;
                for (int k = 0; k < n; k++) OPNetwork.sendToServer(new SpendStatPacket(s));
            });
            if (in(mx, my, cx, cy, colW, 40)) tooltip = font.split(s.description(), 200);
        }

        // derived stats
        int ry = y + 14 + ((stats.length + 1) / 2) * 44 + 2;
        card(g, x, ry, cw, ch - (ry - y), 0xFF3A3A46);
        g.drawString(font, Component.translatable("screen.onepiece.derived"), x + 6, ry + 5, GOLD, false);
        String[] lines = {
                tr("screen.onepiece.stat.health", Math.round(minecraft.player.getMaxHealth())),
                tr("screen.onepiece.stat.damage", pct(DamageCalculator.outgoingMultiplier(d))),
                tr("screen.onepiece.stat.reduction", pct(1f / Math.max(0.01f, DamageCalculator.incomingMultiplier(d)))),
                tr("screen.onepiece.stat.speed", "+" + Math.round(p.get(PlayerStat.AGILITY) * 0.4f) + "%"),
                tr("screen.onepiece.stat.energy", Math.round(d.maxEnergy())),
                tr("screen.onepiece.stat.haki", Math.round(d.maxHaki())),
        };
        for (int i = 0; i < lines.length; i++) {
            small(g, lines[i], x + 6 + (i % 2) * (cw / 2), ry + 18 + (i / 2) * 10, TEXT);
        }
    }

    // ================================================================== fruit

    private void renderFruit(GuiGraphics g, OPPlayerData d, int x, int y, int cw, int ch, int mx, int my) {
        DevilFruit fruit = d.fruit();
        if (fruit == null) {
            card(g, x, y, cw, 40, 0xFF3A3A46);
            g.drawString(font, Component.translatable("screen.onepiece.no_fruit"), x + 8, y + 8, MUTED, false);
            wrapSmall(g, Component.translatable("screen.onepiece.no_fruit_hint"), x + 8, y + 20, cw - 16, MUTED, 2);
            return;
        }
        card(g, x, y, cw, 44, 0xFF000000 | fruit.color());
        ItemStack icon = fruitIcon(fruit);
        g.pose().pushPose();
        g.pose().translate(x + 6, y + 6, 0);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();
        g.drawString(font, fruit.displayName(), x + 42, y + 5, 0xFF000000 | fruit.rarity().color.getColor(), true);
        g.drawString(font, fruit.type().displayName().copy().append(" · ").append(fruit.rarity().displayName()), x + 42, y + 16, TEXT, false);
        String m = Component.translatable("screen.onepiece.mastery", d.mastery()).getString();
        g.drawString(font, m, x + cw - 6 - font.width(m), y + 5, 0xFFE0A0FF, false);
        bar(g, x + 42, y + 30, cw - 50, 4, d.masteryProgress(), 0xFFB070FF);
        if (in(mx, my, x, y, cw, 44)) tooltip = font.split(fruit.description(), 220);

        List<Ability> list = fruit.abilities();
        int colW = (cw - 6) / 2;
        int rows = (list.size() + 1) / 2;
        int cardH = Math.min(32, (ch - 50) / Math.max(1, rows) - 3);
        for (int i = 0; i < list.size(); i++) {
            int ax = x + (i % 2) * (colW + 6), ay = y + 50 + (i / 2) * (cardH + 3);
            abilityCard(g, d, list.get(i), AbilityBars.FRUIT, i, ax, ay, colW, cardH, mx, my);
        }
    }

    private ItemStack fruitIcon(DevilFruit fruit) {
        for (RegistryObject<Item> e : ModItems.ITEMS.getEntries()) {
            if (e.get() instanceof DevilFruitItem dfi && dfi.fruitId().equals(fruit.id())) return new ItemStack(dfi);
        }
        return ItemStack.EMPTY;
    }

    private void abilityCard(GuiGraphics g, OPPlayerData d, Ability a, int bar, int slot, int x, int y, int cw, int ch, int mx, int my) {
        boolean locked = !a.isUnlocked(d);
        boolean hover = in(mx, my, x, y, cw, ch);
        card(g, x, y, cw, ch, hover ? 0xFF000000 | a.color() : 0xFF2E2E3A);
        int is = Math.min(24, ch - 6);
        if (locked) g.setColor(0.3f, 0.3f, 0.3f, 1f);
        g.blit(a.icon(), x + 4, y + (ch - is) / 2, is, is, 0, 0, 32, 32, 32, 32);
        g.setColor(1, 1, 1, 1);
        var keys = bar == AbilityBars.HAKI ? KeyBindings.HAKI : KeyBindings.ABILITIES;
        String info = locked ? a.requirementLabel() : "[" + KeyBindings.shortName(keys[slot]) + "]";
        int nx = x + is + 8;
        String name = font.plainSubstrByWidth(a.displayName().getString(), cw - (nx - x) - font.width(info) - 6);
        g.drawString(font, name, nx, y + 3, locked ? MUTED : TEXT, false);
        g.drawString(font, info, x + cw - 4 - font.width(info), y + 3, locked ? 0xFFAA5555 : GOLD, false);
        String meta = tr("ability_type.onepiece." + a.activation().name().toLowerCase()) + " · " + (int) a.energyCost()
                + (a.resource() == com.onepiece.ultimate.ability.AbilityResource.HAKI ? " HK" : " EN") + " · " + String.format("%.1fs", a.cooldown() / 20f);
        small(g, meta, nx, y + 14, MUTED);
        if (hover) {
            List<FormattedCharSequence> tip = new ArrayList<>(font.split(a.displayName().copy().withStyle(net.minecraft.ChatFormatting.GOLD), 220));
            tip.addAll(font.split(a.description(), 220));
            if (locked) tip.addAll(font.split(a.lockedMessage().copy().withStyle(net.minecraft.ChatFormatting.RED), 220));
            tooltip = tip;
        }
    }

    // ================================================================== haki

    private void renderHaki(GuiGraphics g, OPPlayerData d, int x, int y, int cw, int ch, int mx, int my) {
        HakiData h = d.haki();
        HakiType[] types = HakiType.values();
        int cardW = (cw - 12) / 3;
        for (int i = 0; i < types.length; i++) {
            HakiType t = types[i];
            int cx = x + i * (cardW + 6);
            boolean awake = h.isAwakened(t);
            card(g, cx, y, cardW, ch, awake ? 0xFF000000 | t.color : 0xFF2E2E3A);
            g.fillGradient(cx + 1, y + 1, cx + cardW - 1, y + 30, 0x40000000 | (t.color & 0xFFFFFF), 0x00000000);
            g.drawString(font, font.plainSubstrByWidth(t.displayName().getString(), cardW - 8), cx + 5, y + 5, 0xFF000000 | t.color, true);
            String status;
            if (awake) status = tr("screen.onepiece.haki_level", h.level(t), HakiData.MAX_LEVEL);
            else if (t == HakiType.CONQUEROR && h.potentialRolled() && !h.hasConquerorPotential()) status = tr("screen.onepiece.no_potential");
            else status = tr("screen.onepiece.awakens_at", switch (t) {
                case ARMAMENT -> OPConfig.ARMAMENT_UNLOCK_LEVEL.get();
                case OBSERVATION -> OPConfig.OBSERVATION_UNLOCK_LEVEL.get();
                case CONQUEROR -> OPConfig.CONQUEROR_UNLOCK_LEVEL.get();
            });
            wrapSmall(g, Component.literal(status), cx + 5, y + 16, cardW - 10, awake ? TEXT : MUTED, 2);
            // level pips
            for (int k = 0; k < HakiData.MAX_LEVEL; k++) {
                int px = cx + 5 + k * ((cardW - 10) / HakiData.MAX_LEVEL);
                g.fill(px, y + 34, px + (cardW - 10) / HakiData.MAX_LEVEL - 2, y + 38, k < h.level(t) ? 0xFF000000 | t.color : 0xFF303038);
            }
            bar(g, cx + 5, y + 41, cardW - 10, 2, h.progress(t), 0xFFFFFFFF);

            int ty = y + 50;
            List<Ability> all = HakiAbilities.all();
            for (int s = 0; s < all.size(); s++) {
                if (!(all.get(s) instanceof HakiAbility ha) || ha.type() != t) continue;
                abilityCard(g, d, ha, AbilityBars.HAKI, s, cx + 4, ty, cardW - 8, 26, mx, my);
                ty += 29;
            }
            wrapSmall(g, Component.translatable("screen.onepiece.haki_desc." + t.name().toLowerCase()), cx + 5, ty + 2, cardW - 10, MUTED, 8);
        }
    }

    // ================================================================== helpers

    private void card(GuiGraphics g, int x, int y, int w, int h, int border) {
        g.fill(x, y, x + w, y + h, CARD);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, 0x60000000 | (border & 0xFFFFFF));
        g.fill(x, y, x + 1, y + h, 0x60000000 | (border & 0xFFFFFF));
        g.fill(x + w - 1, y, x + w, y + h, 0x60000000 | (border & 0xFFFFFF));
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, 0xFF2A2A30);
        int fw = (int) (w * Mth.clamp(frac, 0f, 1f));
        g.fillGradient(x, y, x + fw, y + h, color, (color & 0xFF000000) | ((color & 0xFEFEFE) >> 1));
    }

    private void small(GuiGraphics g, String s, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    private void wrapSmall(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        int ly = 0, n = 0;
        for (FormattedCharSequence line : font.split(text, (int) (width / 0.75f))) {
            if (n++ >= maxLines) break;
            g.drawString(font, line, 0, ly, color, false);
            ly += 10;
        }
        g.pose().popPose();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String pct(float mult) {
        int p = Math.round((mult - 1f) * 100f);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void click(int x, int y, int w, int h, Runnable action) {
        clickAreas.add(new int[]{x, y, w, h});
        clickActions.add(action);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = clickAreas.size() - 1; i >= 0; i--) {
                int[] a = clickAreas.get(i);
                if (in((int) mx, (int) my, a[0], a[1], a[2], a[3])) {
                    clickActions.get(i).run();
                    if (minecraft != null) minecraft.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (KeyBindings.CHARACTER.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
