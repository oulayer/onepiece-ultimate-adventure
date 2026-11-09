package com.onepiece.ultimate.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityBars;
import com.onepiece.ultimate.client.input.KeyBindings;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.AbilityInputPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radial transformation selector. Opens while the transformation key is held; point the mouse at a form and release
 * the key (or click) to toggle it. Shows locked forms with their requirement, cooldowns and the active form.
 */
public class TransformationWheelScreen extends Screen {
    private static final int RADIUS = 72;
    private int hovered = -1;
    private float open;

    public TransformationWheelScreen() {
        super(Component.translatable("screen.onepiece.transformations"));
    }

    @Nullable
    private OPPlayerData data() {
        return minecraft == null ? null : OPCapabilities.getOrNull(minecraft.player);
    }

    @Override
    public void tick() {
        open = Math.min(1f, open + 0.34f);
        if (minecraft == null) return;
        var key = KeyBindings.TRANSFORM.getKey();
        boolean scripted = System.getProperty("onepiece.autotest") != null; // dev visual test keeps the wheel open
        if (!scripted && key.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue())) {
            confirm();
        }
    }

    private void confirm() {
        OPPlayerData d = data();
        if (d != null && hovered >= 0) {
            OPNetwork.sendToServer(new AbilityInputPacket(AbilityBars.TRANSFORM, hovered, true));
            OPNetwork.sendToServer(new AbilityInputPacket(AbilityBars.TRANSFORM, hovered, false));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        OPPlayerData d = data();
        if (d == null) return;
        List<Ability> forms = AbilityBars.bar(d, AbilityBars.TRANSFORM);
        int cx = width / 2, cy = height / 2;
        float k = Mth.clamp(open + partial * 0.34f, 0f, 1f);
        float ease = 1f - (1f - k) * (1f - k);
        g.fill(0, 0, width, height, ((int) (110 * ease) << 24));

        // hovered by direction
        double dx = mx - cx, dy = my - cy;
        hovered = -1;
        if (!forms.isEmpty() && dx * dx + dy * dy > 18 * 18) {
            double ang = Math.atan2(dy, dx) + Math.PI / 2;
            if (ang < 0) ang += Math.PI * 2;
            double sector = Math.PI * 2 / forms.size();
            hovered = (int) Math.floor((ang + sector / 2) / sector) % forms.size();
        }

        g.drawCenteredString(font, title, cx, cy - RADIUS - 36, 0xFFFFD27A);
        for (int i = 0; i < forms.size(); i++) {
            Ability a = forms.get(i);
            double ang = -Math.PI / 2 + i * Math.PI * 2 / forms.size();
            int x = cx + (int) (Math.cos(ang) * RADIUS * ease), y = cy + (int) (Math.sin(ang) * RADIUS * ease);
            boolean locked = !a.isUnlocked(d);
            boolean active = d.isToggleActive(a.id());
            boolean hover = i == hovered;
            int size = hover ? 34 : 28;
            int border = active ? 0xFFFFFFFF : hover ? 0xFF000000 | a.color() : 0xFF3A3A44;
            g.fill(x - size / 2 - 2, y - size / 2 - 2, x + size / 2 + 2, y + size / 2 + 2, border);
            g.fill(x - size / 2, y - size / 2, x + size / 2, y + size / 2, 0xE0141418);
            if (locked) g.setColor(0.3f, 0.3f, 0.3f, 1f);
            g.blit(a.icon(), x - size / 2 + 3, y - size / 2 + 3, size - 6, size - 6, 0, 0, 32, 32, 32, 32);
            g.setColor(1, 1, 1, 1);
            float cd = d.cooldowns().progress(a.cooldownKey());
            if (cd > 0 && !active) {
                int h = (int) ((size) * cd);
                g.fill(x - size / 2, y - size / 2, x + size / 2, y - size / 2 + h, 0xA0000000);
            }
            String label = a.displayName().getString();
            int lw = font.width(label);
            g.drawString(font, label, x - lw / 2, y + size / 2 + 4, hover ? 0xFFFFFFFF : locked ? 0xFF777777 : 0xFFCCCCCC, true);
            if (locked) g.drawCenteredString(font, a.requirementLabel(), x, y - 4, 0xFFFF7070);
            else if (active) g.drawCenteredString(font, Component.translatable("screen.onepiece.active"), x, y - size / 2 - 12, 0xFF7CFF8A);
        }
        // centre
        g.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0xFFFFD27A);
        if (hovered >= 0) {
            Ability a = forms.get(hovered);
            var lines = font.split(a.isUnlocked(d) ? a.description() : a.lockedMessage(), 220);
            int ty = cy + RADIUS + 34;
            for (var line : lines) {
                g.drawCenteredString(font, line, cx, ty, 0xFFDDDDDD);
                ty += 10;
            }
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            confirm();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
