package com.onepiece.ultimate.gametest;

import com.mojang.authlib.GameProfile;
import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.core.PlayerTicker;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fruit.DevilFruits;
import com.onepiece.ultimate.fruit.gomu.GomuAbilities;
import com.onepiece.ultimate.haki.ConquerorWaveAbility;
import com.onepiece.ultimate.haki.HakiAbilities;
import com.onepiece.ultimate.haki.HakiData;
import com.onepiece.ultimate.haki.HakiManager;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Server-side functional tests. Run headless with {@code ./gradlew runGameTestServer}.
 * They exercise the real systems (capability, ability pipeline, entities, combat events, destruction jobs)
 * inside a live world rather than mocks.
 */
@GameTestHolder(OnePieceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OPGameTests {
    private static final String ARENA = "arena";
    /** Relative y of the arena's stone floor (GameTest places structures one block above the test origin). */
    private static final int FLOOR = 1;
    private static final int FORMS = com.onepiece.ultimate.ability.AbilityBars.TRANSFORM;

    // ------------------------------------------------------------------ helpers

    private static FakePlayer fake(GameTestHelper h, double x, double y, double z, float yaw) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "op_test"));
        Vec3 pos = h.absoluteVec(new Vec3(x, y, z));
        p.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        p.yBodyRot = yaw;
        return p;
    }

    /**
     * A fake player that takes damage like a survival player (FakePlayer is normally invulnerable and new
     * players have spawn protection).
     */
    private static ServerPlayer mock(GameTestHelper h, double x, double y, double z, float yaw) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "op_test_victim")) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }

            @Override
            public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                return true;
            }
        };
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, p, 0, "f_8921_"); // spawnInvulnerableTime
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(new Vec3(x, y, z));
        p.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        p.yBodyRot = yaw;
        p.setHealth(p.getMaxHealth());
        return p;
    }

    private static void discard(GameTestHelper h, ServerPlayer p) {
        p.discard();
    }

    private static OPPlayerData data(GameTestHelper h, net.minecraft.world.entity.player.Player p) {
        OPPlayerData d = OPCapabilities.getOrNull(p);
        if (d == null) h.fail("player data capability missing");
        return d;
    }

    /** Spawns a target that keeps still (no AI, no gravity drift) so geometry in the test is exact. */
    private static Husk target(GameTestHelper h, double x, double y, double z) {
        Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, y, z));
        husk.setNoAi(true);
        return husk;
    }

    private static void check(GameTestHelper h, boolean cond, String msg) {
        if (!cond) h.fail(msg);
    }

    // ------------------------------------------------------------------ data

    @GameTest(template = ARENA)
    public static void arenaFloor(GameTestHelper h) {
        h.assertBlockPresent(Blocks.STONE, new BlockPos(4, FLOOR, 10));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(4, FLOOR + 1, 10));
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void dataRoundTrip(GameTestHelper h) {
        OPPlayerData a = new OPPlayerData();
        a.setFruit(DevilFruits.GOMU_GOMU_ID);
        a.setMasteryXp(OPPlayerData.totalXpForMastery(37) + 5);
        a.progression().setLevel(12);
        a.progression().setUnspentPoints(4);
        a.progression().set(PlayerStat.STRENGTH, 9);
        a.cooldowns().start(GomuAbilities.PISTOL.cooldownKey(), 40);
        a.setCombatMode(true);

        OPPlayerData b = new OPPlayerData();
        b.load(a.save());
        check(h, DevilFruits.GOMU_GOMU_ID.equals(b.fruitId()), "fruit not restored");
        check(h, b.mastery() == 37, "mastery not restored: " + b.mastery());
        check(h, b.progression().level() == 12 && b.progression().unspentPoints() == 4, "progression not restored");
        check(h, b.progression().get(PlayerStat.STRENGTH) == 9, "stats not restored");
        check(h, b.cooldowns().remaining(GomuAbilities.PISTOL.cooldownKey()) == 40, "cooldown not restored");
        check(h, b.combatMode(), "combat mode not restored");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void abilityGates(GameTestHelper h) {
        FakePlayer p = fake(h, 3.5, 2, 3.5, 0);
        OPPlayerData d = data(h, p);
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.NO_ABILITY, "ability usable without a fruit");
        d.setFruit(DevilFruits.GOMU_GOMU_ID);
        d.setEnergy(d.maxEnergy());

        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.SUCCESS, "Pistol should fire");
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.COOLDOWN, "Pistol should be on cooldown");
        check(h, AbilityManager.press(p, d, 5) == AbilityManager.Result.LOCKED, "Gigant Pistol must need mastery 45");

        d.cooldowns().clear();
        d.setEnergy(0);
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.NO_ENERGY, "energy is not enforced");

        // Gear Second turns Pistol into Jet Pistol (variant shares the slot and cooldown)
        d.setMasteryXp(OPPlayerData.totalXpForMastery(30));
        d.setEnergy(d.maxEnergy());
        check(h, AbilityManager.press(p, d, FORMS, 0) == AbilityManager.Result.SUCCESS, "Gear Second should activate");
        check(h, AbilityManager.abilityInSlot(d, 0) == GomuAbilities.JET_PISTOL, "Pistol should resolve to Jet Pistol");
        check(h, AbilityManager.press(p, d, FORMS, 0) == AbilityManager.Result.TOGGLED_OFF, "Gear Second should toggle off");
        check(h, AbilityManager.abilityInSlot(d, 0) == GomuAbilities.PISTOL, "Pistol should be back to normal");
        h.succeed();
    }

    // ------------------------------------------------------------------ abilities in the world

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void pistolHitsTarget(GameTestHelper h) {
        FakePlayer p = fake(h, 4.5, 2, 2.5, 0); // facing +Z
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.GOMU_GOMU_ID);
        d.setEnergy(d.maxEnergy());
        Husk target = target(h, 4.5, 2, 10.5);
        float start = target.getHealth();
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.SUCCESS, "Pistol refused");
        h.onEachTick(() -> PlayerTicker.tick(p, d));
        h.succeedWhen(() -> {
            check(h, target.getHealth() < start, "the rubber fist never hit the target");
            check(h, d.masteryXp() > 0, "hitting with a fruit ability should grant mastery (health " + target.getHealth()
                    + "/" + start + ", xp " + d.progression().xp() + ", mastery xp " + d.masteryXp() + ")");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void lightComboChains(GameTestHelper h) {
        FakePlayer p = fake(h, 4.5, 2, 4.5, 0);
        p.setOnGround(true); // fake players are never ticked by the server, so set ground state explicitly
        OPPlayerData d = data(h, p);
        d.setCombatMode(true);
        Husk target = target(h, 4.5, 2, 6.3);
        float start = target.getHealth();
        h.onEachTick(() -> PlayerTicker.tick(p, d));
        int[] maxStep = {0};
        for (int t : new int[]{1, 9, 17}) {
            h.runAfterDelay(t, () -> {
                CombatManager.light(p, d);
                maxStep[0] = Math.max(maxStep[0], d.combat.comboStep);
            });
        }
        h.succeedWhen(() -> {
            check(h, maxStep[0] >= 3, "combo did not advance (step " + maxStep[0] + ")");
            check(h, target.getHealth() <= start - 5f, "combo hits did not land: " + target.getHealth());
        });
    }

    // ------------------------------------------------------------------ defence

    @GameTest(template = ARENA)
    public static void parryNegatesAndStuns(GameTestHelper h) {
        ServerPlayer p = mock(h, 4.5, 2, 4.5, 0);
        try {
            OPPlayerData d = data(h, p);
            Husk attacker = target(h, 4.5, 2, 6.0);
            CombatManager.startBlocking(p, d);
            boolean hurt = p.hurt(p.damageSources().mobAttack(attacker), 6f);
            check(h, !hurt && p.getHealth() == p.getMaxHealth(), "parry did not negate the hit");
            check(h, attacker.hasEffect(ModEffects.STUNNED.get()), "parry did not stun the attacker");
            check(h, d.combat.counterUntil > h.getLevel().getGameTime(), "parry did not open a counter window");
            // A stunned mob cannot hit
            CombatManager.stopBlocking(p, d);
            check(h, !p.hurt(p.damageSources().mobAttack(attacker), 6f), "stunned attacker still landed a hit");
        } finally {
            discard(h, p);
        }
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void blockReducesDamage(GameTestHelper h) {
        ServerPlayer p = mock(h, 4.5, 2, 4.5, 0);
        try {
            OPPlayerData d = data(h, p);
            Husk attacker = target(h, 4.5, 2, 6.0);
            CombatManager.startBlocking(p, d);
            d.combat.blockStart = h.getLevel().getGameTime() - 100; // outside the parry window
            float energy = d.energy();
            p.hurt(p.damageSources().mobAttack(attacker), 8f);
            float expected = p.getMaxHealth() - 8f * 0.25f;
            check(h, Math.abs(p.getHealth() - expected) < 0.01f, "block should absorb 75%: health " + p.getHealth());
            check(h, d.energy() < energy, "blocking should cost energy");

            // Attacks from behind are not blocked
            p.setHealth(p.getMaxHealth());
            p.invulnerableTime = 0;
            Husk behind = target(h, 4.5, 2, 3.0);
            p.hurt(p.damageSources().mobAttack(behind), 8f);
            check(h, Math.abs(p.getHealth() - (p.getMaxHealth() - 8f)) < 0.01f, "hit from behind should not be blocked");
        } finally {
            discard(h, p);
        }
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void rubberBody(GameTestHelper h) {
        ServerPlayer p = mock(h, 4.5, 2, 4.5, 0);
        try {
            OPPlayerData d = data(h, p);
            d.setFruit(DevilFruits.GOMU_GOMU_ID);
            Husk attacker = target(h, 8.5, 2, 8.5);
            p.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.MARTIAL, attacker), 8f);
            check(h, Math.abs(p.getHealth() - (p.getMaxHealth() - 4f)) < 0.01f, "rubber should halve blunt damage: " + p.getHealth());
            p.setHealth(p.getMaxHealth());
            p.invulnerableTime = 0;
            p.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.SLASH, attacker), 8f);
            check(h, Math.abs(p.getHealth() - (p.getMaxHealth() - 8f)) < 0.01f, "blades should cut rubber normally: " + p.getHealth());
            p.setHealth(p.getMaxHealth());
            p.invulnerableTime = 0;
            check(h, !p.hurt(p.damageSources().lightningBolt(), 10f), "rubber must be immune to lightning");
        } finally {
            discard(h, p);
        }
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void seaWaterCancelsPowers(GameTestHelper h) {
        h.setBlock(new BlockPos(3, FLOOR + 1, 3), Blocks.WATER);
        h.setBlock(new BlockPos(3, FLOOR + 2, 3), Blocks.WATER);
        FakePlayer p = fake(h, 3.5, 2, 3.5, 0);
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.GOMU_GOMU_ID);
        d.setMasteryXp(OPPlayerData.totalXpForMastery(30));
        d.setEnergy(d.maxEnergy());
        check(h, AbilityManager.press(p, d, FORMS, 0) == AbilityManager.Result.SUCCESS, "Gear Second should activate on land");
        p.updateFluidHeightAndDoFluidPushing();
        PlayerTicker.tick(p, d);
        check(h, d.seaWeakened(), "submerged fruit user should be weakened");
        check(h, !d.isToggleActive(GomuAbilities.GEAR_SECOND.id()), "sea water should end Gear Second");
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.WEAKENED, "abilities must be unusable in sea water");
        h.succeed();
    }

    // ------------------------------------------------------------------ destruction

    @GameTest(template = ARENA)
    public static void craterRespectsRules(GameTestHelper h) {
        for (BlockPos rel : BlockPos.betweenClosed(5, 2, 5, 9, 6, 9)) h.setBlock(rel, Blocks.STONE);
        h.setBlock(new BlockPos(7, 4, 7), Blocks.CHEST);
        h.setBlock(new BlockPos(7, 4, 6), Blocks.BEDROCK);
        h.setBlock(new BlockPos(6, 4, 7), Blocks.OBSIDIAN);
        h.setBlock(new BlockPos(13, 4, 7), Blocks.STONE);
        ServerLevel level = h.getLevel();
        DestructionManager.crater(level, h.absoluteVec(new Vec3(7.5, 4.5, 7.5)), 2.5f, 1f, 2f, null);
        DestructionManager.flush(level);
        h.assertBlockPresent(Blocks.AIR, new BlockPos(7, 4, 8));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(8, 5, 7));
        h.assertBlockPresent(Blocks.CHEST, new BlockPos(7, 4, 7));
        h.assertBlockPresent(Blocks.BEDROCK, new BlockPos(7, 4, 6));
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(6, 4, 7)); // too hard for power 2
        h.assertBlockPresent(Blocks.STONE, new BlockPos(13, 4, 7));  // well outside the radius
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void destructionIsBudgeted(GameTestHelper h) {
        for (BlockPos rel : BlockPos.betweenClosed(1, 2, 1, 14, 7, 14)) h.setBlock(rel, Blocks.DIRT);
        ServerLevel level = h.getLevel();
        DestructionManager.flush(level);
        DestructionManager.crater(level, h.absoluteVec(new Vec3(7.5, 5, 7.5)), 6f, 1f, 2f, null);
        int before = DestructionManager.pending(level);
        check(h, before > 300, "expected a large job, got " + before);
        DestructionManager.tick(level);
        int after = DestructionManager.pending(level);
        int budget = com.onepiece.ultimate.config.OPConfig.BLOCKS_PER_TICK.get();
        check(h, before - after <= budget, "processed " + (before - after) + " ops in one tick, budget " + budget);
        check(h, after < before, "destruction made no progress");
        DestructionManager.flush(level);
        h.succeed();
    }

    // ------------------------------------------------------------------ haki

    private static final int HAKI = com.onepiece.ultimate.ability.AbilityBars.HAKI;

    private static void setHaki(ServerPlayer p, OPPlayerData d, HakiType type, int level) {
        d.haki().awaken(type);
        d.haki().setXp(type, HakiData.totalXpFor(level));
        d.setHakiGauge(d.maxHaki());
    }

    @GameTest(template = ARENA)
    public static void hakiAwakening(GameTestHelper h) {
        FakePlayer p = fake(h, 3.5, 2, 3.5, 0);
        OPPlayerData d = data(h, p);
        d.haki().setConquerorPotential(false);
        d.progression().setLevel(4);
        HakiManager.checkAwakenings(p, d);
        check(h, !d.haki().anyAwakened(), "nothing should awaken at level 4");
        d.progression().setLevel(5);
        HakiManager.checkAwakenings(p, d);
        check(h, d.hakiLevel(HakiType.ARMAMENT) == 1 && !d.haki().isAwakened(HakiType.OBSERVATION), "armament should awaken at 5");
        d.progression().setLevel(30);
        HakiManager.checkAwakenings(p, d);
        check(h, d.haki().isAwakened(HakiType.OBSERVATION), "observation should awaken at 8");
        check(h, !d.haki().isAwakened(HakiType.CONQUEROR), "conqueror's needs the potential");
        d.haki().setConquerorPotential(true);
        HakiManager.checkAwakenings(p, d);
        check(h, d.haki().isAwakened(HakiType.CONQUEROR), "conqueror's should awaken with potential at 20+");

        // training raises the level
        HakiManager.awardXp(p, d, HakiType.ARMAMENT, HakiData.xpForNext(1) + 1);
        check(h, d.hakiLevel(HakiType.ARMAMENT) == 2, "armament should reach level 2, got " + d.hakiLevel(HakiType.ARMAMENT));
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void hakiGates(GameTestHelper h) {
        FakePlayer p = fake(h, 3.5, 2, 3.5, 0);
        OPPlayerData d = data(h, p);
        check(h, AbilityManager.press(p, d, HAKI, 0) == AbilityManager.Result.LOCKED, "armament must be locked before awakening");
        setHaki(p, d, HakiType.ARMAMENT, 1);
        float energy = d.energy(), gauge = d.hakiGauge();
        check(h, AbilityManager.press(p, d, HAKI, 0) == AbilityManager.Result.SUCCESS, "armament should activate");
        check(h, d.energy() == energy && d.hakiGauge() < gauge, "haki must spend the haki gauge, not energy");
        check(h, AbilityManager.press(p, d, HAKI, 2) == AbilityManager.Result.LOCKED, "emission needs armament level 6");
        setHaki(p, d, HakiType.ARMAMENT, 6);
        check(h, AbilityManager.press(p, d, HAKI, 2) == AbilityManager.Result.SUCCESS, "emission should start charging with armament on");
        AbilityManager.cancelAll(p, d);
        d.cooldowns().clear();
        check(h, AbilityManager.press(p, d, HAKI, 2) == AbilityManager.Result.REFUSED, "emission requires armament to be active");
        check(h, AbilityManager.press(p, d, HAKI, 3) == AbilityManager.Result.LOCKED, "future sight needs observation 5");

        setHaki(p, d, HakiType.CONQUEROR, 5);
        check(h, AbilityManager.press(p, d, HAKI, 5) == AbilityManager.Result.REFUSED, "infusion requires armament");
        check(h, AbilityManager.press(p, d, HAKI, 0) == AbilityManager.Result.SUCCESS, "armament on");
        check(h, AbilityManager.press(p, d, HAKI, 5) == AbilityManager.Result.SUCCESS, "infusion on");
        check(h, AbilityManager.press(p, d, HAKI, 0) == AbilityManager.Result.TOGGLED_OFF, "armament off");
        check(h, !d.isToggleActive(HakiAbilities.CONQUEROR_INFUSION.id()), "infusion must end with armament");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void armamentBeatsRubber(GameTestHelper h) {
        ServerPlayer victim = mock(h, 4.5, 2, 6.5, 180);
        FakePlayer attacker = fake(h, 4.5, 2, 4.5, 0);
        OPPlayerData vd = data(h, victim), ad = data(h, attacker);
        vd.setFruit(DevilFruits.GOMU_GOMU_ID);
        float pvp = com.onepiece.ultimate.config.OPConfig.PVP_DAMAGE_MULTIPLIER.get().floatValue();

        victim.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.MARTIAL, attacker), 8f);
        float plain = victim.getMaxHealth() - victim.getHealth();
        check(h, Math.abs(plain - 8f * pvp * 0.5f) < 0.01f, "rubber should halve an uncoated punch: " + plain);

        victim.setHealth(victim.getMaxHealth());
        victim.invulnerableTime = 0;
        setHaki(attacker, ad, HakiType.ARMAMENT, 1);
        check(h, AbilityManager.press(attacker, ad, HAKI, 0) == AbilityManager.Result.SUCCESS, "armament refused");
        victim.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.MARTIAL, attacker), 8f);
        float coated = victim.getMaxHealth() - victim.getHealth();
        float expected = 8f * com.onepiece.ultimate.combat.DamageCalculator.outgoingMultiplier(ad) * pvp;
        check(h, Math.abs(coated - expected) < 0.01f, "coated punch should ignore rubber and gain the armament bonus: " + coated + " vs " + expected);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void futureSightDodges(GameTestHelper h) {
        ServerPlayer p = mock(h, 4.5, 2, 4.5, 0);
        OPPlayerData d = data(h, p);
        setHaki(p, d, HakiType.OBSERVATION, 5);
        Husk attacker = target(h, 4.5, 2, 6.0);
        check(h, AbilityManager.press(p, d, HAKI, 3) == AbilityManager.Result.REFUSED, "future sight needs observation active");
        check(h, AbilityManager.press(p, d, HAKI, 1) == AbilityManager.Result.SUCCESS, "observation on");
        check(h, AbilityManager.press(p, d, HAKI, 3) == AbilityManager.Result.SUCCESS, "future sight on");
        for (int i = 0; i < 5; i++) {
            p.invulnerableTime = 0;
            check(h, !p.hurt(p.damageSources().mobAttack(attacker), 6f), "future sight must evade attack #" + i);
        }
        check(h, p.getHealth() == p.getMaxHealth(), "no damage should land");
        check(h, d.haki().xp(HakiType.OBSERVATION) > HakiData.totalXpFor(5), "dodging should train observation");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void emissionIgnoresBlock(GameTestHelper h) {
        ServerPlayer victim = mock(h, 4.5, 2, 6.5, 180);
        FakePlayer attacker = fake(h, 4.5, 2, 4.5, 0);
        OPPlayerData vd = data(h, victim);
        float pvp = com.onepiece.ultimate.config.OPConfig.PVP_DAMAGE_MULTIPLIER.get().floatValue();
        CombatManager.startBlocking(victim, vd);
        vd.combat.blockStart = h.getLevel().getGameTime() - 100;
        victim.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.MARTIAL, attacker), 8f);
        float blocked = victim.getMaxHealth() - victim.getHealth();
        victim.setHealth(victim.getMaxHealth());
        victim.invulnerableTime = 0;
        vd.setEnergy(vd.maxEnergy());
        victim.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.INTERNAL, attacker), 8f);
        float internal = victim.getMaxHealth() - victim.getHealth();
        check(h, Math.abs(blocked - 8f * pvp * 0.25f) < 0.01f, "normal hit should be blocked: " + blocked);
        check(h, Math.abs(internal - 8f * pvp) < 0.01f, "internal damage must pass through the block: " + internal);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void conquerorJudgesWillpower(GameTestHelper h) {
        FakePlayer p = fake(h, 7.5, 2, 7.5, 0);
        OPPlayerData d = data(h, p);
        d.progression().setLevel(30);
        setHaki(p, d, HakiType.CONQUEROR, 5);
        var chicken = h.spawnWithNoFreeWill(EntityType.CHICKEN, new Vec3(9.5, 2, 7.5));
        chicken.setNoAi(true);
        var golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(5.5, 2, 7.5));
        golem.setNoAi(true);
        var warden = h.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(7.5, 2, 11.5));
        warden.setNoAi(true);
        float will = ConquerorWaveAbility.willpower(p, d, 1f);
        Vec3 c = p.position();
        check(h, ConquerorWaveAbility.judge(p, will, chicken, c) == ConquerorWaveAbility.Outcome.FAINTED, "a chicken should faint");
        check(h, chicken.hasEffect(ModEffects.STUNNED.get()), "fainted means stunned");
        check(h, ConquerorWaveAbility.judge(p, will, golem, c) == ConquerorWaveAbility.Outcome.INTIMIDATED, "an iron golem should be intimidated (will " + will + ")");
        check(h, golem.hasEffect(ModEffects.INTIMIDATED.get()), "intimidated effect missing");
        check(h, ConquerorWaveAbility.judge(p, will, warden, c) == ConquerorWaveAbility.Outcome.RESISTED, "a warden should resist");

        // Full ability path: charge and release through the ability manager.
        var chicken2 = h.spawnWithNoFreeWill(EntityType.CHICKEN, new Vec3(7.5, 2, 10.5));
        chicken2.setNoAi(true);
        check(h, AbilityManager.press(p, d, HAKI, 4) == AbilityManager.Result.SUCCESS, "conqueror's wave refused");
        for (int i = 0; i < 30; i++) PlayerTicker.tick(p, d);
        check(h, AbilityManager.release(p, d, HAKI, 4) == AbilityManager.Result.RELEASED, "release failed");
        check(h, chicken2.hasEffect(ModEffects.STUNNED.get()), "the released wave should knock out the chicken");
        check(h, d.cooldowns().isCooling(HakiAbilities.CONQUEROR_WAVE.cooldownKey()), "wave should go on cooldown");
        DestructionManager.flush(h.getLevel());
        h.succeed();
    }

    // ------------------------------------------------------------------ transformations (stage 7)

    private static void skipActivation(OPPlayerData d) {
        d.abilities.transformLockUntil = 0;
    }

    @GameTest(template = ARENA)
    public static void gearFourthRules(GameTestHelper h) {
        FakePlayer p = fake(h, 4.5, 2, 4.5, 0);
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.GOMU_GOMU_ID);
        d.setMasteryXp(OPPlayerData.totalXpForMastery(70));
        d.progression().set(PlayerStat.STAMINA, 100);
        d.setEnergy(d.maxEnergy());
        check(h, AbilityManager.press(p, d, FORMS, 1) == AbilityManager.Result.REFUSED, "Boundman needs Armament level 4");
        setHaki(p, d, HakiType.ARMAMENT, 4);

        check(h, AbilityManager.press(p, d, FORMS, 0) == AbilityManager.Result.SUCCESS, "Gear Second");
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.BUSY, "the activation sequence must lock the user");
        skipActivation(d);
        float before = p.getBbHeight();
        check(h, AbilityManager.press(p, d, FORMS, 1) == AbilityManager.Result.SUCCESS, "Boundman");
        check(h, !d.isToggleActive(GomuAbilities.GEAR_SECOND.id()), "gears are exclusive: Gear Second must end");
        check(h, d.isToggleActive(HakiAbilities.ARMAMENT.id()), "Boundman must coat the body with Armament");
        check(h, AbilityManager.abilityInSlot(d, 0) == GomuAbilities.KONG_GUN, "Pistol must become Kong Gun");
        check(h, AbilityManager.abilityInSlot(d, 5) == GomuAbilities.KING_KONG_GUN, "Gigant Pistol must become King Kong Gun");
        check(h, p.getBbHeight() > before + 0.3f, "Boundman must enlarge the hitbox (" + before + " -> " + p.getBbHeight() + ")");
        check(h, com.onepiece.ultimate.combat.DamageCalculator.transformationMultiplier(d) > 1.8f, "Boundman damage bonus missing");

        skipActivation(d);
        check(h, AbilityManager.press(p, d, FORMS, 1) == AbilityManager.Result.TOGGLED_OFF, "Boundman off");
        check(h, Math.abs(p.getBbHeight() - before) < 0.01f, "hitbox must return to normal");
        check(h, !d.isToggleActive(HakiAbilities.ARMAMENT.id()), "Armament must burn out with Gear Fourth");
        d.cooldowns().clear();
        check(h, AbilityManager.press(p, d, HAKI, 0) == AbilityManager.Result.EXHAUSTED, "Haki must be exhausted after Gear Fourth");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void snakemanFistHomes(GameTestHelper h) {
        FakePlayer p = fake(h, 3.5, 2, 1.5, 0); // facing +Z
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.GOMU_GOMU_ID);
        d.setMasteryXp(OPPlayerData.totalXpForMastery(70));
        d.progression().set(PlayerStat.STAMINA, 100);
        setHaki(p, d, HakiType.ARMAMENT, 4);
        d.setEnergy(d.maxEnergy());
        Husk target = target(h, 9.5, 2, 12.5); // ~29° off the aim line
        float start = target.getHealth();
        check(h, AbilityManager.press(p, d, FORMS, 2) == AbilityManager.Result.SUCCESS, "Snakeman");
        skipActivation(d);
        check(h, AbilityManager.abilityInSlot(d, 0) == GomuAbilities.JET_CULVERIN, "Pistol must become Jet Culverin");
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.SUCCESS, "Jet Culverin refused");
        h.onEachTick(() -> PlayerTicker.tick(p, d));
        h.succeedWhen(() -> check(h, target.getHealth() < start, "the homing fist never reached the off-axis target"));
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void gearFifthRubberizes(GameTestHelper h) {
        FakePlayer p = fake(h, 4.5, 2, 2.5, 0);
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.GOMU_GOMU_ID);
        d.setMasteryXp(OPPlayerData.totalXpForMastery(100));
        d.progression().set(PlayerStat.STAMINA, 100);
        d.setEnergy(d.maxEnergy());
        Husk target = target(h, 4.5, 2, 11.5);
        check(h, AbilityManager.press(p, d, FORMS, 3) == AbilityManager.Result.SUCCESS, "Gear Fifth");
        skipActivation(d);
        check(h, AbilityManager.abilityInSlot(d, 0) == GomuAbilities.MOGURA_PISTOL, "Pistol must become Mogura Pistol");
        check(h, AbilityManager.press(p, d, 0) == AbilityManager.Result.SUCCESS, "Mogura Pistol refused");
        h.onEachTick(() -> PlayerTicker.tick(p, d));
        h.succeedWhen(() -> check(h, com.onepiece.ultimate.combat.ToonPhysics.isRubber(target), "the target was not turned to rubber"));
    }

    @GameTest(template = ARENA)
    public static void phoenixRegeneration(GameTestHelper h) {
        ServerPlayer p = mock(h, 4.5, 2, 4.5, 0);
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.TORI_PHOENIX_ID);
        d.setEnergy(d.maxEnergy());
        Husk husk = target(h, 4.5, 2, 6.5);
        p.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.MARTIAL, husk), 8f);
        check(h, p.getHealth() <= p.getMaxHealth() - 7.9f, "the hit should land first");
        for (int i = 0; i < 10; i++) PlayerTicker.tick(p, d);
        check(h, d.energy() < d.maxEnergy() - 1f, "regeneration must cost energy: " + d.energy());
        for (int i = 0; i < 70; i++) PlayerTicker.tick(p, d);
        check(h, p.getHealth() >= p.getMaxHealth() - 1.7f, "blue flames should regenerate most of the damage: " + p.getHealth());

        // Armament Haki suppresses the flames
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        d.abilities.regenPool = 0;
        FakePlayer attacker = fake(h, 4.5, 2, 7.5, 180);
        OPPlayerData ad = data(h, attacker);
        setHaki(attacker, ad, HakiType.ARMAMENT, 1);
        check(h, AbilityManager.press(attacker, ad, HAKI, 0) == AbilityManager.Result.SUCCESS, "armament refused");
        p.hurt(ModDamageTypes.source(h.getLevel(), ModDamageTypes.MARTIAL, attacker), 8f);
        check(h, d.abilities.regenPool == 0f, "Haki-coated damage must not be regenerated");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void phoenixForms(GameTestHelper h) {
        FakePlayer p = fake(h, 4.5, 2, 4.5, 0);
        OPPlayerData d = data(h, p);
        d.setFruit(DevilFruits.TORI_PHOENIX_ID);
        d.setMasteryXp(OPPlayerData.totalXpForMastery(30));
        d.setEnergy(d.maxEnergy());
        check(h, AbilityManager.press(p, d, FORMS, 0) == AbilityManager.Result.SUCCESS, "hybrid form");
        skipActivation(d);
        check(h, AbilityManager.press(p, d, FORMS, 1) == AbilityManager.Result.SUCCESS, "full form");
        check(h, !d.isToggleActive(com.onepiece.ultimate.fruit.phoenix.PhoenixAbilities.HYBRID.id()), "zoan forms are exclusive");
        check(h, p.getAbilities().mayfly && p.getAbilities().flying, "full phoenix must fly");
        check(h, p.getBbHeight() < 1.3f, "full phoenix must have a smaller body: " + p.getBbHeight());
        skipActivation(d);
        check(h, AbilityManager.press(p, d, FORMS, 1) == AbilityManager.Result.TOGGLED_OFF, "full form off");
        check(h, !p.getAbilities().mayfly, "flight must be removed afterwards");
        check(h, p.getBbHeight() > 1.7f, "body size must be restored");
        h.succeed();
    }
}
