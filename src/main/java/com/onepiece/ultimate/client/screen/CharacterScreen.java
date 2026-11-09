package com.onepiece.ultimate.client.screen;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.client.input.KeyBindings;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.data.Progression;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.haki.HakiData;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.SpendStatPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Character sheet: level, attribute points, Devil Fruit mastery and the full move list with unlock levels. */
public class CharacterScreen extends Screen {
    private static final int W = 520, H = 220;
    private final List<Button> plusButtons = new ArrayList<>();

    public CharacterScreen() {
        super(Component.translatable("screen.onepiece.character"));
    }

    @Nullable
    private OPPlayerData data() {
        return minecraft == null ? null : OPCapabilities.getOrNull(minecraft.player);
    }

    @Override
    protected void init() {
        plusButtons.clear();
        int left = (width - W) / 2, top = (height - H) / 2;
        PlayerStat[] stats = PlayerStat.values();
        for (int i = 0; i < stats.length; i++) {
            PlayerStat stat = stats[i];
            Button b = Button.builder(Component.literal("+"), btn -> OPNetwork.sendToServer(new SpendStatPacket(stat)))
                    .bounds(left + 150, top + 62 + i * 30, 16, 16)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(stat.description()))
                    .build();
            plusButtons.add(addRenderableWidget(b));
        }
    }

    @Override
    public void tick() {
        OPPlayerData d = data();
        if (d == null) return;
        Progression p = d.progression();
        PlayerStat[] stats = PlayerStat.values();
        for (int i = 0; i < plusButtons.size(); i++) {
            plusButtons.get(i).active = p.unspentPoints() > 0 && p.get(stats[i]) < PlayerStat.MAX_POINTS;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int left = (width - W) / 2, top = (height - H) / 2;
        g.fill(left, top, left + W, top + H, 0xE0101014);
        g.fill(left, top, left + W, top + 1, 0xFFFFD27A);
        g.fill(left + 180, top + 24, left + 181, top + H - 10, 0x40FFFFFF);
        g.fill(left + 360, top + 24, left + 361, top + H - 10, 0x40FFFFFF);
        g.drawCenteredString(font, title, width / 2, top + 8, 0xFFFFD27A);

        OPPlayerData d = data();
        if (d == null) {
            super.render(g, mouseX, mouseY, partial);
            return;
        }
        Progression p = d.progression();

        // ---- left: character
        int x = left + 12, y = top + 26;
        g.drawString(font, Component.translatable("screen.onepiece.level", p.level()), x, y, 0xFFFFFFFF, true);
        long need = Progression.xpForNext(p.level());
        g.fill(x, y + 12, x + 154, y + 16, 0xFF303030);
        g.fill(x, y + 12, x + (int) (154 * Math.min(1f, p.xp() / (float) need)), y + 16, 0xFF7CFF8A);
        g.drawString(font, p.xp() + " / " + need + " XP", x, y + 19, 0xFFAAAAAA, false);
        String pts = Component.translatable("screen.onepiece.points", p.unspentPoints()).getString();
        g.drawString(font, pts, x + 154 - font.width(pts), y, p.unspentPoints() > 0 ? 0xFF7CFF8A : 0xFF888888, true);

        PlayerStat[] stats = PlayerStat.values();
        for (int i = 0; i < stats.length; i++) {
            int sy = top + 62 + i * 30;
            PlayerStat s = stats[i];
            g.drawString(font, s.displayName(), x, sy, 0xFF000000 | s.color, true);
            String v = String.valueOf(p.get(s));
            g.drawString(font, v, left + 142 - font.width(v), sy, 0xFFFFFFFF, true);
            g.fill(x, sy + 11, x + 130, sy + 14, 0xFF303030);
            g.fill(x, sy + 11, x + (int) (130 * p.get(s) / (float) PlayerStat.MAX_POINTS), sy + 14, 0xFF000000 | s.color);
        }
        g.drawString(font, Component.translatable("screen.onepiece.energy", Math.round(d.maxEnergy())), x, top + H - 20, 0xFFFFE27A, false);

        // ---- right: fruit
        int rx = left + 192;
        DevilFruit fruit = d.fruit();
        if (fruit == null) {
            g.drawString(font, Component.translatable("screen.onepiece.no_fruit"), rx, top + 26, 0xFF888888, false);
        } else {
            g.drawString(font, fruit.displayName(), rx, top + 26, 0xFFFFD27A, true);
            g.drawString(font, fruit.type().displayName(), rx, top + 37, 0xFFFFFFFF, false);
            String mastery = Component.translatable("screen.onepiece.mastery", d.mastery()).getString();
            g.drawString(font, mastery, rx + 156 - font.width(mastery), top + 37, 0xFFE0A0FF, false);
            g.fill(rx, top + 48, rx + 156, top + 51, 0xFF303030);
            g.fill(rx, top + 48, rx + (int) (156 * d.masteryProgress()), top + 51, 0xFFB070FF);

            int ay = top + 58;
            Ability hovered = null;
            List<Ability> list = fruit.abilities();
            for (int i = 0; i < list.size(); i++) {
                Ability a = list.get(i);
                boolean locked = d.mastery() < a.requiredMastery();
                int rowY = ay + i * 21;
                if (locked) g.setColor(0.3f, 0.3f, 0.3f, 1f);
                g.blit(a.icon(), rx, rowY, 16, 16, 0, 0, 32, 32, 32, 32);
                g.setColor(1, 1, 1, 1);
                String info = locked ? "M" + a.requiredMastery() : "[" + KeyBindings.shortName(KeyBindings.ABILITIES[i]) + "]";
                String name = font.plainSubstrByWidth(a.displayName().getString(), 156 - 24 - font.width(info));
                g.drawString(font, name, rx + 20, rowY + 1, locked ? 0xFF777777 : 0xFFFFFFFF, false);
                g.drawString(font, info, rx + 156 - font.width(info), rowY + 1, locked ? 0xFFAA5555 : 0xFFFFE27A, false);
                String type = Component.translatable("ability_type.onepiece." + a.activation().name().toLowerCase()).getString()
                        + " · " + (int) a.energyCost() + " EN · " + String.format("%.1fs", a.cooldown() / 20f);
                g.pose().pushPose();
                g.pose().translate(rx + 20, rowY + 11, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.drawString(font, type, 0, 0, 0xFF999999, false);
                g.pose().popPose();
                if (mouseX >= rx && mouseX < rx + 156 && mouseY >= rowY && mouseY < rowY + 20) hovered = a;
            }
            if (hovered != null) {
                g.renderTooltip(font, font.split(hovered.description(), 200), mouseX, mouseY);
            }
        }
        renderHaki(g, d, left + 372, top + 26);
        super.render(g, mouseX, mouseY, partial);
    }

    private void renderHaki(GuiGraphics g, OPPlayerData d, int x, int y) {
        g.drawString(font, Component.translatable("screen.onepiece.haki"), x, y, 0xFFB48CFF, true);
        String gauge = Math.round(d.hakiGauge()) + " / " + Math.round(d.maxHaki());
        g.drawString(font, gauge, x + 136 - font.width(gauge), y, 0xFF999999, false);
        int row = y + 16;
        for (HakiType t : HakiType.values()) {
            g.drawString(font, t.displayName(), x, row, 0xFF000000 | t.color, true);
            HakiData h = d.haki();
            String status;
            if (h.isAwakened(t)) status = Component.translatable("screen.onepiece.haki_level", h.level(t), HakiData.MAX_LEVEL).getString();
            else if (t == HakiType.CONQUEROR && h.potentialRolled() && !h.hasConquerorPotential())
                status = Component.translatable("screen.onepiece.no_potential").getString();
            else status = Component.translatable("screen.onepiece.awakens_at", switch (t) {
                    case ARMAMENT -> OPConfig.ARMAMENT_UNLOCK_LEVEL.get();
                    case OBSERVATION -> OPConfig.OBSERVATION_UNLOCK_LEVEL.get();
                    case CONQUEROR -> OPConfig.CONQUEROR_UNLOCK_LEVEL.get();
                }).getString();
            g.pose().pushPose();
            g.pose().translate(x, row + 11, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, status, 0, 0, h.isAwakened(t) ? 0xFFFFFFFF : 0xFF888888, false);
            g.pose().popPose();
            g.fill(x, row + 20, x + 136, row + 23, 0xFF303030);
            g.fill(x, row + 20, x + (int) (136 * h.progress(t)), row + 23, 0xFF000000 | t.color);
            row += 32;
        }
        g.pose().pushPose();
        g.pose().translate(x, row + 2, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        int ly = 0;
        for (var line : font.split(Component.translatable("screen.onepiece.haki_hint"), 180)) {
            g.drawString(font, line, 0, ly, 0xFF888888, false);
            ly += 10;
        }
        g.pose().popPose();
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
