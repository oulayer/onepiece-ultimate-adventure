package com.onepiece.ultimate.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class KeyBindings {
    public static final String CATEGORY = "key.categories.onepiece";
    public static final int ABILITY_SLOTS = 7;
    private static final int[] DEFAULT_ABILITY_KEYS = {
            GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M
    };

    public static final KeyMapping[] ABILITIES = new KeyMapping[ABILITY_SLOTS];
    public static final int HAKI_SLOTS = 6;
    private static final int[] DEFAULT_HAKI_KEYS = {
            GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O
    };
    /** Armament, Observation, Emission, Future Sight, Conqueror's wave, Conqueror's infusion. */
    public static final KeyMapping[] HAKI = new KeyMapping[HAKI_SLOTS];
    public static final KeyMapping COMBAT_MODE = key("combat_mode", GLFW.GLFW_KEY_G);
    public static final KeyMapping DASH = key("dash", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping BLOCK = key("block", GLFW.GLFW_KEY_R);
    public static final KeyMapping CHARACTER = key("character", GLFW.GLFW_KEY_K);

    static {
        for (int i = 0; i < ABILITY_SLOTS; i++) ABILITIES[i] = key("ability_" + (i + 1), DEFAULT_ABILITY_KEYS[i]);
        for (int i = 0; i < HAKI_SLOTS; i++) HAKI[i] = key("haki_" + (i + 1), DEFAULT_HAKI_KEYS[i]);
    }

    private static KeyMapping key(String name, int key) {
        return new KeyMapping("key.onepiece." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    public static List<KeyMapping> all() {
        List<KeyMapping> list = new ArrayList<>(List.of(ABILITIES));
        list.addAll(List.of(HAKI));
        list.add(COMBAT_MODE);
        list.add(DASH);
        list.add(BLOCK);
        list.add(CHARACTER);
        return list;
    }

    /** Short label for HUD key hints ("Z", "LAlt", "M4"). */
    public static String shortName(KeyMapping key) {
        String s = key.getTranslatedKeyMessage().getString();
        if (s.length() <= 3) return s.toUpperCase();
        return s.replace("Left ", "L").replace("Right ", "R").replace("Button ", "M").substring(0, Math.min(4, s.length()));
    }

    private KeyBindings() {}
}
