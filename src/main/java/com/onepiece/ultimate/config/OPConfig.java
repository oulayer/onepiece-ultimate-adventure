package com.onepiece.ultimate.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-side (per world) gameplay configuration. Synced to clients automatically by Forge. */
public final class OPConfig {
    public static final ForgeConfigSpec SPEC;

    // Destruction
    public static final ForgeConfigSpec.BooleanValue DESTRUCTION_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_PER_ATTACK;
    public static final ForgeConfigSpec.IntValue BLOCKS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue PROTECT_BLOCK_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue RESPECT_MOB_GRIEFING;
    public static final ForgeConfigSpec.BooleanValue FIRE_BREAK_EVENTS;
    public static final ForgeConfigSpec.BooleanValue DEBRIS_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS_PER_ATTACK;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue RESTORE_TERRAIN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BLOCK_DROP_CHANCE;

    // Combat
    public static final ForgeConfigSpec.IntValue COMBO_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue PARRY_WINDOW_TICKS;
    public static final ForgeConfigSpec.DoubleValue BLOCK_REDUCTION;
    public static final ForgeConfigSpec.IntValue DASH_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue DODGE_IFRAMES;
    public static final ForgeConfigSpec.DoubleValue PVP_DAMAGE_MULTIPLIER;

    // Fruits
    public static final ForgeConfigSpec.BooleanValue SECOND_FRUIT_KILLS;
    public static final ForgeConfigSpec.BooleanValue SEA_WEAKNESS;
    public static final ForgeConfigSpec.BooleanValue KEEP_FRUIT_ON_DEATH;

    // Haki
    public static final ForgeConfigSpec.IntValue ARMAMENT_UNLOCK_LEVEL;
    public static final ForgeConfigSpec.IntValue OBSERVATION_UNLOCK_LEVEL;
    public static final ForgeConfigSpec.IntValue CONQUEROR_UNLOCK_LEVEL;
    public static final ForgeConfigSpec.DoubleValue CONQUEROR_CHANCE;
    public static final ForgeConfigSpec.DoubleValue HAKI_XP_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue HAOSHOKU_AFFECTS_PLAYERS;

    // Progression
    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MASTERY_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue POINTS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue MAX_LEVEL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Terrain destruction caused by attacks").push("destruction");
        DESTRUCTION_ENABLED = b.comment("Master switch. When false attacks still deal damage and show effects but never change blocks.")
                .define("enabled", true);
        MAX_BLOCKS_PER_ATTACK = b.comment("Hard cap of blocks a single attack may remove.")
                .defineInRange("maxBlocksPerAttack", 600, 0, 20000);
        BLOCKS_PER_TICK = b.comment("Global per-dimension budget of block changes per server tick. Large craters spread over several ticks.")
                .defineInRange("blocksPerTick", 250, 1, 5000);
        PROTECT_BLOCK_ENTITIES = b.comment("Never destroy blocks with block entities (chests, furnaces, signs, ...).")
                .define("protectBlockEntities", true);
        RESPECT_MOB_GRIEFING = b.comment("Attacks from non-player entities (bosses) obey the mobGriefing game rule.")
                .define("respectMobGriefing", true);
        FIRE_BREAK_EVENTS = b.comment("Fire a BlockEvent.BreakEvent for player-caused destruction so claim/protection mods can veto it.")
                .define("fireBreakEvents", true);
        DEBRIS_ENABLED = b.define("debrisEnabled", true);
        MAX_DEBRIS_PER_ATTACK = b.defineInRange("maxDebrisPerAttack", 24, 0, 200);
        MAX_DEBRIS_PER_LEVEL = b.defineInRange("maxDebrisPerLevel", 160, 0, 2000);
        RESTORE_TERRAIN_SECONDS = b.comment("If > 0, destroyed terrain regenerates after this many seconds (only where the space is still empty). 0 = permanent.")
                .defineInRange("restoreTerrainSeconds", 0, 0, 86400);
        BLOCK_DROP_CHANCE = b.comment("Chance that a destroyed block drops as an item.")
                .defineInRange("blockDropChance", 0.0, 0.0, 1.0);
        b.pop();

        b.push("combat");
        COMBO_WINDOW_TICKS = b.comment("Ticks after a light attack during which the next input continues the combo.")
                .defineInRange("comboWindowTicks", 14, 4, 40);
        PARRY_WINDOW_TICKS = b.comment("Ticks after starting to block during which an incoming hit is parried.")
                .defineInRange("parryWindowTicks", 5, 0, 20);
        BLOCK_REDUCTION = b.comment("Fraction of frontal damage absorbed while blocking.")
                .defineInRange("blockReduction", 0.75, 0.0, 1.0);
        DASH_COOLDOWN_TICKS = b.defineInRange("dashCooldownTicks", 16, 0, 200);
        DODGE_IFRAMES = b.comment("Invulnerability ticks at the start of a dash (perfect dodge window).")
                .defineInRange("dodgeIFrames", 6, 0, 20);
        PVP_DAMAGE_MULTIPLIER = b.defineInRange("pvpDamageMultiplier", 0.6, 0.0, 10.0);
        b.pop();

        b.push("fruits");
        SECOND_FRUIT_KILLS = b.comment("Eating a second Devil Fruit kills the eater (as in the original story). If false, the new fruit simply fails.")
                .define("secondFruitKills", true);
        SEA_WEAKNESS = b.comment("Fruit users submerged in water lose their powers, drain energy and sink.")
                .define("seaWeakness", true);
        KEEP_FRUIT_ON_DEATH = b.define("keepFruitOnDeath", true);
        b.pop();

        b.comment("Haki awakening and training").push("haki");
        ARMAMENT_UNLOCK_LEVEL = b.comment("Character level at which Busoshoku (Armament) Haki awakens.")
                .defineInRange("armamentUnlockLevel", 5, 1, 1000);
        OBSERVATION_UNLOCK_LEVEL = b.comment("Character level at which Kenbunshoku (Observation) Haki awakens.")
                .defineInRange("observationUnlockLevel", 8, 1, 1000);
        CONQUEROR_UNLOCK_LEVEL = b.comment("Character level at which players with Conqueror's potential awaken Haoshoku Haki.")
                .defineInRange("conquerorUnlockLevel", 20, 1, 1000);
        CONQUEROR_CHANCE = b.comment("Chance (rolled once per player) to be born with Conqueror's potential. 1.0 = everyone.")
                .defineInRange("conquerorChance", 0.35, 0.0, 1.0);
        HAKI_XP_MULTIPLIER = b.defineInRange("hakiXpMultiplier", 1.0, 0.0, 100.0);
        HAOSHOKU_AFFECTS_PLAYERS = b.comment("Whether Conqueror's Haki can knock out / intimidate other players.")
                .define("haoshokuAffectsPlayers", true);
        b.pop();

        b.push("progression");
        XP_MULTIPLIER = b.defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
        MASTERY_MULTIPLIER = b.defineInRange("masteryMultiplier", 1.0, 0.0, 100.0);
        POINTS_PER_LEVEL = b.defineInRange("pointsPerLevel", 3, 0, 20);
        MAX_LEVEL = b.defineInRange("maxLevel", 100, 1, 1000);
        b.pop();

        SPEC = b.build();
    }

    private OPConfig() {}
}
