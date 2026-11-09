package com.onepiece.ultimate.client.dev;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.client.screen.CharacterScreen;
import com.onepiece.ultimate.combat.CombatAction;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.DevilFruits;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.AbilityInputPacket;
import com.onepiece.ultimate.network.packet.CombatInputPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Development-only scripted play-through used to verify rendering, animations and HUD end to end
 * (enabled only with {@code -Donepiece.autotest=true}, e.g. {@code ./gradlew runClient -Pautotest}).
 * It creates/loads a flat test world, stages a scene on the integrated server, drives abilities through the
 * <em>real</em> network packets the key bindings send, saves screenshots to {@code run/screenshots} and quits.
 */
@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientAutoTest {
    /** "true"/"" = full visual run, "fp" = first-person calibration. */
    private static final String MODE = System.getProperty("onepiece.autotest", "");
    private static final boolean ENABLED = System.getProperty("onepiece.autotest") != null;
    private static final String WORLD = "onepiece_autotest";

    private record Step(int tick, Runnable action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean worldRequested;
    private static int ticksInWorld = -1;
    private static int stepIndex;

    private static void at(int tick, Runnable r) {
        STEPS.add(new Step(tick, r));
    }

    private static void server(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) action.accept(sp);
        });
    }

    private static void refill() {
        server(sp -> OPCapabilities.get(sp).ifPresent(d -> {
            d.setEnergy(d.maxEnergy());
            d.cooldowns().clear();
            d.markFull();
        }));
    }

    private static void press(int slot) {
        OPNetwork.sendToServer(new AbilityInputPacket(slot, true));
    }

    private static void release(int slot) {
        OPNetwork.sendToServer(new AbilityInputPacket(slot, false));
    }

    private static void hakiPress(int slot) {
        OPNetwork.sendToServer(new AbilityInputPacket(com.onepiece.ultimate.ability.AbilityBars.HAKI, slot, true));
    }

    private static void hakiRelease(int slot) {
        OPNetwork.sendToServer(new AbilityInputPacket(com.onepiece.ultimate.ability.AbilityBars.HAKI, slot, false));
    }

    /** Haki scene: high Haki levels, passive animals around, a hostile zombie behind and targets behind the wall. */
    private static void stageHaki(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        OPPlayerData d = OPCapabilities.getOrNull(sp);
        if (d == null) return;
        com.onepiece.ultimate.ability.AbilityManager.cancelAll(sp, d);
        d.progression().setLevel(30);
        d.haki().setConquerorPotential(true);
        for (var type : com.onepiece.ultimate.haki.HakiType.values()) {
            d.haki().awaken(type);
            d.haki().setXp(type, com.onepiece.ultimate.haki.HakiData.totalXpFor(8));
        }
        d.setHakiGauge(d.maxHaki());
        d.cooldowns().clear();
        d.markFull();
        d.markVisual();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        y = (int) sp.getY();
        for (int i = 0; i < 4; i++) {
            var c = EntityType.CHICKEN.create(level);
            if (c != null) { c.moveTo(-4 + i * 2.5, y, 4, 0, 0); level.addFreshEntity(c); }
        }
        var z = EntityType.HUSK.create(level);
        if (z != null) {
            z.moveTo(-6.5, y, -12.5, 0, 0);
            z.setTarget(sp);
            level.addFreshEntity(z);
        }
        for (int i = -1; i <= 1; i++) {
            Husk h = EntityType.HUSK.create(level);
            if (h != null) { h.setNoAi(true); h.moveTo(i * 3 + 0.5, y, 21.5, 180, 0); level.addFreshEntity(h); }
        }
    }

    private static void combat(CombatAction a) {
        OPNetwork.sendToServer(new CombatInputPacket(a, 0, 0));
    }

    private static final List<String> PENDING_SHOTS = new ArrayList<>();

    /** Screenshots are taken after the next rendered frame so they show the state of this tick. */
    private static void shot(String name) {
        PENDING_SHOTS.add(name);
    }

    @SubscribeEvent
    public static void afterFrame(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || PENDING_SHOTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        OnePieceMod.LOGGER.info("[autotest] fps {}", mc.getFps());
        for (String name : PENDING_SHOTS) {
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> OnePieceMod.LOGGER.info("[autotest] {}", msg.getString()));
        }
        PENDING_SHOTS.clear();
    }

    private static void camera(CameraType type) {
        Minecraft.getInstance().options.setCameraType(type);
    }

    static {
        if ("fp".equals(MODE)) firstPersonCalibration();
        else fullRun();
    }

    /** Plays one-parameter first-person poses and photographs them, to map pose values to what the camera sees. */
    private static void firstPersonCalibration() {
        int t = 60;
        at(t, () -> server(ClientAutoTest::stage));
        at(t + 30, () -> camera(CameraType.FIRST_PERSON));
        at(t + 40, () -> shot("fp_00_rest"));
        float[][] cal = {
                {6, 0, 0, 0, 0, 0}, {0, 6, 0, 0, 0, 0}, {0, 0, 6, 0, 0, 0},
                {0, 0, 0, 40, 0, 0}, {0, 0, 0, 0, 40, 0}, {0, 0, 0, 0, 0, 40},
                {0, 0, 0, -40, 0, 0}, {0, 0, 0, 0, -40, 0}, {0, 0, 0, 0, 0, -40}};
        String[] names = {"px+6", "py+6", "pz+6", "rx+40", "ry+40", "rz+40", "rx-40", "ry-40", "rz-40"};
        for (int i = 0; i < cal.length; i++) {
            float[] v = cal[i];
            String name = String.format("fp_%02d_%s", i + 1, names[i]);
            int at = t + 50 + i * 20;
            at(at, () -> playPose(v));
            at(at + 12, () -> shot(name));
        }
        at(t + 50 + cal.length * 20 + 10, () -> Minecraft.getInstance().stop());
    }

    private static void playPose(float[] v) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var rot = new com.onepiece.ultimate.client.animation.Keyframe[]{
                new com.onepiece.ultimate.client.animation.Keyframe(0, v[3], v[4], v[5], com.onepiece.ultimate.client.animation.Easing.LINEAR)};
        var pos = new com.onepiece.ultimate.client.animation.Keyframe[]{
                new com.onepiece.ultimate.client.animation.Keyframe(0, v[0], v[1], v[2], com.onepiece.ultimate.client.animation.Easing.LINEAR)};
        var track = new com.onepiece.ultimate.client.animation.BoneTrack(rot, pos, null, false);
        var tracks = new java.util.EnumMap<com.onepiece.ultimate.client.animation.Bone, com.onepiece.ultimate.client.animation.BoneTrack>(
                com.onepiece.ultimate.client.animation.Bone.class);
        tracks.put(com.onepiece.ultimate.client.animation.Bone.FP_RIGHT, track);
        tracks.put(com.onepiece.ultimate.client.animation.Bone.FP_LEFT, track);
        var anim = new com.onepiece.ultimate.client.animation.PoseAnimation(OnePieceMod.id("dev_cal"), 1, false, true, 0.1f, 0.1f, 1000, 0.9f, tracks);
        com.onepiece.ultimate.client.animation.PlayerAnimations.controller(mc.player.getId()).play(anim, 1f);
    }

    private static void fullRun() {
        int t = 60; // after joining, give chunks time to load
        at(t, () -> server(ClientAutoTest::stage));
        at(t + 40, () -> camera(CameraType.THIRD_PERSON_BACK));
        at(t + 60, () -> shot("01_combat_stance_hud"));
        // Pistol
        at(t + 62, ClientAutoTest::refill);
        at(t + 64, () -> press(0));
        at(t + 65, () -> release(0));
        at(t + 70, () -> shot("02_gomu_pistol"));
        at(t + 73, () -> shot("03_gomu_pistol_return"));
        // Gear Second + Jet Gatling
        at(t + 90, ClientAutoTest::refill);
        at(t + 92, () -> press(5));
        at(t + 93, () -> release(5));
        at(t + 99, () -> shot("04_gear_second_activation"));
        at(t + 125, () -> shot("05_gear_second_stance"));
        at(t + 128, () -> press(2));
        at(t + 140, () -> shot("06_jet_gatling"));
        at(t + 150, () -> release(2));
        at(t + 160, () -> press(5));
        at(t + 161, () -> release(5));
        // Gear Third
        at(t + 175, ClientAutoTest::refill);
        at(t + 177, () -> press(6));
        at(t + 205, () -> shot("07_gigant_charge"));
        at(t + 212, () -> release(6));
        at(t + 216, () -> shot("08_gigant_fist"));
        at(t + 224, () -> shot("09_gigant_impact"));
        // Whip
        at(t + 260, ClientAutoTest::refill);
        at(t + 262, () -> press(1));
        at(t + 263, () -> release(1));
        at(t + 267, () -> shot("10_whip"));
        // Front camera poses: block, heavy charge, bazooka
        at(t + 290, () -> camera(CameraType.THIRD_PERSON_FRONT));
        at(t + 292, () -> combat(CombatAction.BLOCK_PRESS));
        at(t + 302, () -> shot("11_block_front"));
        at(t + 305, () -> combat(CombatAction.BLOCK_RELEASE));
        at(t + 312, () -> combat(CombatAction.HEAVY_PRESS));
        at(t + 335, () -> shot("12_heavy_charge_front"));
        at(t + 338, () -> combat(CombatAction.HEAVY_RELEASE));
        at(t + 341, () -> shot("13_heavy_release_front"));
        at(t + 360, ClientAutoTest::refill);
        at(t + 362, () -> press(4));
        at(t + 386, () -> shot("14_bazooka_charge_front"));
        at(t + 390, () -> release(4));
        at(t + 393, () -> shot("15_bazooka_release_front"));
        // Character sheet
        at(t + 420, () -> camera(CameraType.FIRST_PERSON));
        at(t + 422, () -> Minecraft.getInstance().setScreen(new CharacterScreen()));
        at(t + 432, () -> shot("16_character_screen"));
        at(t + 434, () -> Minecraft.getInstance().setScreen(null));
        at(t + 445, () -> shot("17_first_person"));
        // Slow-motion fists so the stretched-arm renderer is captured mid-flight even at low FPS
        at(t + 446, () -> camera(CameraType.THIRD_PERSON_BACK));
        at(t + 448, () -> server(sp -> {
            for (int side : new int[]{1, -1}) {
                // sideways (perpendicular to the camera) so the stretched arms aren't hidden behind the body
                double yaw = Math.toRadians(sp.yBodyRot);
                var look = new net.minecraft.world.phys.Vec3(-Math.cos(yaw) * side, 0.15, -Math.sin(yaw) * side).normalize();
                var origin = sp.position().add(-Math.cos(Math.toRadians(sp.yBodyRot)) * 0.36 * side, sp.getBbHeight() * 0.76,
                        -Math.sin(Math.toRadians(sp.yBodyRot)) * 0.36 * side);
                sp.level().addFreshEntity(com.onepiece.ultimate.entity.StretchFistEntity.create(sp.level(), sp, origin, look, side)
                        .speed(0.15).range(7));
            }
        }));
        at(t + 480, () -> shot("18_stretched_arms"));
        // ---------------- Haki
        int k = t + 520;
        at(k, () -> server(ClientAutoTest::stageHaki));
        at(k + 5, () -> camera(CameraType.THIRD_PERSON_FRONT));
        at(k + 10, () -> hakiPress(0));
        at(k + 11, () -> hakiRelease(0));
        at(k + 40, () -> shot("20_armament_front"));
        at(k + 45, () -> camera(CameraType.THIRD_PERSON_BACK));
        at(k + 46, () -> server(sp -> {
            for (int side : new int[]{1, -1}) {
                double yaw = Math.toRadians(sp.yBodyRot);
                var dir = new net.minecraft.world.phys.Vec3(-Math.cos(yaw) * side, 0.15, -Math.sin(yaw) * side).normalize();
                var origin = sp.position().add(-Math.cos(yaw) * 0.36 * side, sp.getBbHeight() * 0.76, -Math.sin(yaw) * 0.36 * side);
                sp.level().addFreshEntity(com.onepiece.ultimate.entity.StretchFistEntity.create(sp.level(), sp, origin, dir, side).speed(0.15).range(7));
            }
        }));
        at(k + 76, () -> shot("21_armament_stretch"));
        at(k + 90, () -> hakiPress(1));
        at(k + 91, () -> hakiRelease(1));
        at(k + 96, () -> camera(CameraType.FIRST_PERSON));
        at(k + 104, () -> shot("22a_observation_first_person"));
        at(k + 106, () -> camera(CameraType.THIRD_PERSON_BACK));
        at(k + 120, () -> shot("22_observation_sense"));
        at(k + 125, ClientAutoTest::refill);
        at(k + 127, () -> hakiPress(4));
        at(k + 150, () -> shot("23_conqueror_charge"));
        at(k + 160, () -> hakiRelease(4));
        at(k + 163, () -> shot("24_conqueror_release"));
        at(k + 175, () -> shot("25_conqueror_aftermath"));
        at(k + 200, ClientAutoTest::refill);
        at(k + 202, () -> hakiPress(5));
        at(k + 203, () -> hakiRelease(5));
        at(k + 215, () -> combat(CombatAction.LIGHT));
        at(k + 219, () -> shot("26_infusion_hit"));
        at(k + 222, () -> combat(CombatAction.LIGHT));
        at(k + 226, () -> shot("27_infusion_hit2"));
        at(k + 240, () -> Minecraft.getInstance().setScreen(new CharacterScreen()));
        at(k + 250, () -> shot("28_character_haki"));
        at(k + 252, () -> Minecraft.getInstance().setScreen(null));
        at(k + 260, () -> {
            OnePieceMod.LOGGER.info("[autotest] finished");
            Minecraft.getInstance().stop();
        });
    }

    /** Server side: flat arena with targets, a wall and a tree in front of the player. */
    private static void stage(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        MinecraftServer server = level.getServer();
        server.setDifficulty(Difficulty.NORMAL, true);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        sp.setGameMode(GameType.SURVIVAL);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        sp.teleportTo(level, 0.5, y, 0.5, 0f, 10f);

        OPPlayerData d = OPCapabilities.getOrNull(sp);
        if (d != null) {
            d.setFruit(DevilFruits.GOMU_GOMU_ID);
            d.setMasteryXp(OPPlayerData.totalXpForMastery(60));
            d.progression().setLevel(12);
            d.progression().setUnspentPoints(6);
            d.setCombatMode(true);
            d.setEnergy(d.maxEnergy());
        }
        for (int i = -1; i <= 1; i++) {
            Husk h = EntityType.HUSK.create(level);
            if (h == null) continue;
            h.setNoAi(true);
            h.moveTo(i * 2.5 + 0.5, y, 8.5, 180f, 0f);
            h.setYHeadRot(180f);
            level.addFreshEntity(h);
        }
        for (int x = -5; x <= 5; x++)
            for (int dy = 0; dy < 6; dy++)
                level.setBlock(new BlockPos(x, y + dy, 18), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        for (int dy = 0; dy < 5; dy++) level.setBlock(new BlockPos(7, y + dy, 12), Blocks.OAK_LOG.defaultBlockState(), 3);
        for (BlockPos p : BlockPos.betweenClosed(5, y + 3, 10, 9, y + 5, 14)) {
            if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && mc.screen instanceof TitleScreen title) {
            worldRequested = true;
            if (mc.getLevelSource().levelExists(WORLD)) {
                mc.createWorldOpenFlows().loadLevel(title, WORLD);
            } else {
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20251109L, false, false),
                        reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            }
            return;
        }
        if (mc.level == null || mc.player == null) return;
        ticksInWorld++;
        while (stepIndex < STEPS.size() && STEPS.get(stepIndex).tick <= ticksInWorld) {
            try {
                STEPS.get(stepIndex).action.run();
            } catch (Exception e) {
                OnePieceMod.LOGGER.error("[autotest] step failed", e);
            }
            stepIndex++;
        }
    }

    private ClientAutoTest() {}
}
