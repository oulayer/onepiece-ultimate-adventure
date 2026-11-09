package com.onepiece.ultimate.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Purely visual, per-player client configuration. */
public final class OPClientConfig {
    public enum HudAnchor { BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ForgeConfigSpec.DoubleValue HUD_SCALE;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ForgeConfigSpec.BooleanValue SHOW_KEY_HINTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_COMBO_COUNTER;
    public static final ForgeConfigSpec.BooleanValue HIDE_HUD_WITHOUT_POWERS;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_DENSITY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hud");
        HUD_ANCHOR = b.defineEnum("anchor", HudAnchor.BOTTOM_RIGHT);
        HUD_SCALE = b.defineInRange("scale", 1.0, 0.5, 2.0);
        HUD_OFFSET_X = b.defineInRange("offsetX", 0, -2000, 2000);
        HUD_OFFSET_Y = b.defineInRange("offsetY", 0, -2000, 2000);
        SHOW_KEY_HINTS = b.define("showKeyHints", true);
        SHOW_COMBO_COUNTER = b.define("showComboCounter", true);
        HIDE_HUD_WITHOUT_POWERS = b.comment("Hide the ability bar entirely when the player has no fruit and no combat mode enabled.")
                .define("hideWithoutPowers", true);
        b.pop();
        b.push("effects");
        CAMERA_SHAKE = b.comment("Multiplier for screen shake. 0 disables it.").defineInRange("cameraShake", 1.0, 0.0, 2.0);
        PARTICLE_DENSITY = b.comment("Multiplier for the mod's particle counts.").defineInRange("particleDensity", 1.0, 0.1, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private OPClientConfig() {}
}
