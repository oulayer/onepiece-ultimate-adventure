package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModEffects;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Melee combat state machine (server authoritative).
 *
 * <ul>
 *   <li><b>Light combo</b> – 4 strings (fists or blade) with input buffering and a timing window.</li>
 *   <li><b>Aerial</b> – light attacks in the air juggle the target; heavy in the air is a ground slam.</li>
 *   <li><b>Heavy</b> – hold to charge, release to strike; breaks guards and launches at high charge.</li>
 *   <li><b>Dash</b> – directional burst with invulnerability frames; dodging a hit in that window is a
 *       <em>perfect dodge</em> that opens a counter window.</li>
 *   <li><b>Block / Parry</b> – hold to block frontal hits; blocking right before a hit parries it,
 *       staggering the attacker and opening a counter window.</li>
 *   <li><b>Counter / Finisher</b> – hits during a counter window deal bonus damage; the last hit of a
 *       string on a low-health target is a cinematic finisher.</li>
 * </ul>
 */
public final class CombatManager {
    public static final ResourceLocation HEAVY_CHANNEL = OnePieceMod.id("heavy_attack");
    private static final UUID BLOCK_SLOW_ID = UUID.fromString("0f4a3c55-27d1-4c1b-b0c3-3f8e2b1e4d10");
    private static final int HEAVY_MAX_CHARGE = 25;
    private static final float HEAVY_COST = 8f;
    private static final float DASH_COST = 10f;
    private static final float BLOCK_COST_PER_DAMAGE = 1.5f;

    // ================================================================== input

    public static void handleInput(ServerPlayer player, CombatAction action, float forward, float strafe) {
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null || !player.isAlive() || player.isSpectator()) return;
        switch (action) {
            case TOGGLE_COMBAT_MODE -> {
                data.setCombatMode(!data.combatMode());
                if (!data.combatMode()) stopBlocking(player, data);
            }
            case LIGHT -> light(player, data);
            case HEAVY_PRESS -> heavyPress(player, data);
            case HEAVY_RELEASE -> heavyRelease(player, data);
            case DASH -> dash(player, data, forward, strafe);
            case BLOCK_PRESS -> startBlocking(player, data);
            case BLOCK_RELEASE -> stopBlocking(player, data);
        }
    }

    private static boolean busy(ServerPlayer player, OPPlayerData data) {
        return player.hasEffect(ModEffects.STUNNED.get()) || data.abilities.channeling != null || player.isUsingItem();
    }

    // ================================================================== light combo

    public static boolean light(ServerPlayer player, OPPlayerData data) {
        if (!data.combatMode() || busy(player, data) || data.blocking() || data.combat.heavyCharging) return false;
        CombatState cs = data.combat;
        long now = player.level().getGameTime();
        if (now < cs.recoveryUntil) {
            cs.bufferedLight = true; // executes as soon as recovery ends
            return false;
        }
        cs.bufferedLight = false;

        boolean airborne = !player.onGround() && !player.isInWater() && !player.onClimbable();
        boolean blade = isBlade(player.getMainHandItem());
        MeleeMove move;
        if (airborne && !blade) {
            move = MeleeMove.AERIAL;
            cs.comboStep = 0;
        } else {
            cs.comboStep = now <= cs.comboExpire ? (cs.comboStep % 4) + 1 : 1;
            move = (blade ? MeleeMove.SWORD_COMBO : MeleeMove.FIST_COMBO)[cs.comboStep - 1];
        }
        boolean finisherSlot = cs.comboStep == 4;
        executeMove(player, data, move, 1f, finisherSlot, blade);
        cs.recoveryUntil = now + move.recovery();
        cs.comboExpire = cs.recoveryUntil + OPConfig.COMBO_WINDOW_TICKS.get();
        return true;
    }

    // ================================================================== heavy

    private static void heavyPress(ServerPlayer player, OPPlayerData data) {
        if (!data.combatMode() || busy(player, data) || data.blocking() || data.combat.heavyCharging) return;
        CombatState cs = data.combat;
        long now = player.level().getGameTime();
        if (now < cs.recoveryUntil) return;

        if (!player.onGround() && heightAboveGround(player) > 2.5) {
            startSlam(player, data);
            return;
        }
        if (!data.consumeEnergy(HEAVY_COST)) return;
        cs.heavyCharging = true;
        cs.heavyStart = now;
        data.setChanneling(HEAVY_CHANNEL, 0);
        ServerFx.sound(player.serverLevel(), player.position(), ModSounds.CHARGE.get(), 0.6f, 1.0f);
    }

    private static void heavyRelease(ServerPlayer player, OPPlayerData data) {
        CombatState cs = data.combat;
        if (!cs.heavyCharging) return;
        cs.heavyCharging = false;
        data.setChanneling(null, 0);
        long now = player.level().getGameTime();
        float charge = Math.min(1f, (now - cs.heavyStart) / (float) HEAVY_MAX_CHARGE);
        MeleeMove move = MeleeMove.HEAVY;
        executeMove(player, data, move, charge, false, isBlade(player.getMainHandItem()));
        cs.recoveryUntil = now + move.recovery();
        cs.comboStep = 0;
    }

    // ================================================================== move execution

    /**
     * Plays the move's animation immediately and lands the hit on the move's impact frame.
     *
     * @param charge 0..1, only meaningful for heavy moves
     */
    private static void executeMove(ServerPlayer player, OPPlayerData data, MeleeMove move, float charge, boolean finisherSlot, boolean blade) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        data.combat.markInCombat(now);
        ServerFx.animation(player, move.animation(), 1f);
        ServerFx.sound(level, player.position(), move.heavy() ? ModSounds.SWING_HEAVY.get() : ModSounds.SWING_LIGHT.get(),
                0.7f, 0.9f + player.getRandom().nextFloat() * 0.25f);
        player.swing(InteractionHand.MAIN_HAND, true);
        player.resetAttackStrengthTicker();

        data.abilities.tasks.schedule(now, move.windup(), () -> landMove(player, data, move, charge, finisherSlot, blade));
    }

    private static void landMove(ServerPlayer player, OPPlayerData data, MeleeMove move, float charge, boolean finisherSlot, boolean blade) {
        if (!player.isAlive() || player.hasEffect(ModEffects.STUNNED.get())) return;
        ServerLevel level = player.serverLevel();
        CombatState cs = data.combat;
        long now = level.getGameTime();
        boolean heavy = move == MeleeMove.HEAVY;

        float reach = move.range() + (heavy ? charge : 0) + com.onepiece.ultimate.haki.HakiManager.reachBonus(data);
        List<LivingEntity> targets = HitDetector.arc(player, reach, move.arc());
        ItemStack weapon = player.getMainHandItem();
        float base = (float) Math.max(2.5, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float dmgMult = move.damage() * (heavy ? 1f + charge * 1.0f : 1f);
        float knock = move.knockback() * (heavy ? 1f + charge * 1.4f : 1f);
        boolean counter = now <= cs.counterUntil;

        boolean anyHit = false;
        for (LivingEntity target : targets) {
            float damage = base * dmgMult + EnchantmentHelper.getDamageBonus(weapon, target.getMobType());
            boolean finisher = finisherSlot && target.getHealth() / target.getMaxHealth() < 0.35f;
            if (counter) damage *= 1.75f;
            if (finisher) damage *= 1.5f;

            Vec3 dir = target.position().subtract(player.position());
            HitInfo hit = new HitInfo(player, damage)
                    .type(blade ? ModDamageTypes.SLASH : ModDamageTypes.MARTIAL)
                    .direction(dir.lengthSqr() < 1e-4 ? player.getLookAngle() : dir)
                    .knockback(finisher ? knock * 1.5f : knock)
                    .lift(move.lift() * (finisher ? 1.4f : 1f))
                    .stun(move.stun());
            if ((move.launch() && (!heavy || charge > 0.5f)) || finisher) hit.launch(heavy ? 1f + charge : 1f);
            if (heavy) hit.guardBreak();

            if (CombatHits.apply(hit, target)) {
                anyHit = true;
                int fire = EnchantmentHelper.getFireAspect(player);
                if (fire > 0) target.setSecondsOnFire(fire * 4);
                Vec3 point = HitDetector.closestPoint(target.getBoundingBox(), player.getEyePosition());
                float size = heavy ? 1.2f + charge * 1.5f : finisher ? 2f : 0.8f;
                ServerFx.impact(level, point, player.getLookAngle(), size, counter ? 0xFFD54A : 0xFFFFFF);
                ServerFx.sound(level, point, move.heavy() ? ModSounds.HIT_HEAVY.get() : ModSounds.HIT_LIGHT.get(),
                        0.9f, 0.85f + player.getRandom().nextFloat() * 0.3f);
                if (blade) {
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                    weapon.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                }
                if (move == MeleeMove.AERIAL) {
                    // Juggle: hang in the air with the target.
                    Vec3 v = player.getDeltaMovement();
                    player.setDeltaMovement(v.x * 0.5, Math.max(v.y, 0.12), v.z * 0.5);
                    player.hurtMarked = true;
                    player.fallDistance = 0;
                }
                if (finisher) {
                    ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, 4);
                    ServerFx.camera(player, CameraEffectPacket.Type.FLASH, 0.35f, 6);
                    ServerFx.shake(level, point, 12, 0.8f, 10);
                    level.sendParticles(RingParticleOptions.facing(0xFFE27A, 3.2f, 10, player.getXRot(), player.getYRot()),
                            point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
            }
        }
        if (anyHit) {
            if (counter) cs.counterUntil = 0;
            if (move.heavy()) {
                ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, heavy ? 3 : 2);
                ServerFx.shake(level, player.position(), 10, heavy ? 0.4f + charge * 0.6f : 0.35f, 6);
            } else {
                ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 0.5f, 1);
            }
        }
        // Heavy blows that hit nothing still crack what they strike.
        if (heavy && charge > 0.6f && targets.isEmpty()) {
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(move.range() + 0.8));
            var hr = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hr.getType() == HitResult.Type.BLOCK) {
                DestructionManager.crater(level, hr.getLocation(), 1.2f + charge, 0.8f, 1.6f, player);
                ServerFx.impact(level, hr.getLocation(), player.getLookAngle(), 1.5f, 0xFFFFFF);
            }
        }
    }

    // ================================================================== aerial slam

    private static void startSlam(ServerPlayer player, OPPlayerData data) {
        CombatState cs = data.combat;
        if (!data.consumeEnergy(HEAVY_COST)) return;
        cs.slamPending = true;
        cs.slamStart = player.level().getGameTime();
        cs.slamStartY = player.getY();
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 0.35, -1.8, look.z * 0.35);
        player.hurtMarked = true;
        ServerFx.animation(player, "aerial_slam");
        ServerFx.sound(player.serverLevel(), player.position(), ModSounds.SWING_HEAVY.get(), 1f, 0.6f);
    }

    private static void landSlam(ServerPlayer player, OPPlayerData data) {
        CombatState cs = data.combat;
        cs.slamPending = false;
        cs.slamLandedAt = player.level().getGameTime();
        ServerLevel level = player.serverLevel();
        double height = Math.max(0, cs.slamStartY - player.getY());
        float radius = (float) Math.max(2.5, Math.min(7.0, 2.5 + height * 0.25));
        float damage = (float) (5 + height * 0.45);
        Vec3 pos = player.position();
        DestructionManager.shockwave(level, pos, radius, 8, damage, 0.55f, 0.6f, player);
        DestructionManager.crater(level, pos.add(0, -0.5, 0), radius * 0.38f, 0.6f, 1.4f, player);
        DestructionManager.cracks(level, pos, 5, (int) (radius * 1.4f), player);
        ServerFx.groundShock(level, pos, radius, 12);
        ServerFx.sound(level, pos, ModSounds.SLAM.get(), 1.4f, 0.8f);
        ServerFx.shake(level, pos, radius * 4, 0.9f, 12);
        ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, 3);
        ServerFx.animation(player, "slam_land");
    }

    // ================================================================== dash / dodge

    public static boolean dash(ServerPlayer player, OPPlayerData data, float forward, float strafe) {
        CombatState cs = data.combat;
        long now = player.level().getGameTime();
        if (now < cs.dashReadyAt || busy(player, data) || player.isPassenger() || player.isFallFlying()) return false;
        boolean airborne = !player.onGround();
        if (airborne && cs.airDashUsed) return false;
        if (!data.consumeEnergy(DASH_COST)) return false;
        if (airborne) cs.airDashUsed = true;

        if (Math.abs(forward) < 0.1f && Math.abs(strafe) < 0.1f) forward = 1f;
        double yaw = Math.toRadians(player.getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        Vec3 dir = new Vec3(strafe * cos - forward * sin, 0, forward * cos + strafe * sin).normalize();

        int agility = data.progression().get(PlayerStat.AGILITY);
        double speed = (1.15 + agility * 0.006) * dashSpeedMultiplier(data);
        player.setDeltaMovement(dir.x * speed, airborne ? 0.12 : 0.18, dir.z * speed);
        player.hurtMarked = true;
        player.fallDistance = 0;

        cs.iframesUntil = now + OPConfig.DODGE_IFRAMES.get();
        cs.perfectDodgeTriggered = false;
        int cooldown = (int) (OPConfig.DASH_COOLDOWN_TICKS.get() * (1.0 - agility * 0.004) / dashSpeedMultiplier(data));
        cs.dashReadyAt = now + Math.max(4, cooldown);

        String anim = Math.abs(forward) >= Math.abs(strafe)
                ? (forward >= 0 ? "dash_forward" : "dash_back")
                : (strafe > 0 ? "dash_left" : "dash_right");
        ServerFx.animation(player, anim);
        ServerLevel level = player.serverLevel();
        ServerFx.sound(level, player.position(), ModSounds.DASH.get(), 0.8f, 1f + player.getRandom().nextFloat() * 0.2f);
        ServerFx.camera(player, CameraEffectPacket.Type.FOV_PUNCH, 0.12f, 6);
        for (int i = 0; i < 4; i++) {
            data.abilities.tasks.schedule(now, i, () -> {
                Vec3 p = player.position();
                level.sendParticles(ModParticles.STEAM.get(), p.x, p.y + 0.2, p.z, 3, 0.2, 0.05, 0.2, 0.01);
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.9, p.z, 1, 0.15, 0.4, 0.15, 0.0);
            });
        }
        return true;
    }

    private static double dashSpeedMultiplier(OPPlayerData data) {
        // Transformations that boost speed register a damage multiplier above 1; reuse as speed scale, capped.
        return Math.min(1.5, Math.max(1.0, DamageCalculator.transformationMultiplier(data)));
    }

    /** Called from the attack event when the player is inside dash i-frames. */
    public static void perfectDodge(ServerPlayer player, OPPlayerData data) {
        CombatState cs = data.combat;
        if (cs.perfectDodgeTriggered) return;
        cs.perfectDodgeTriggered = true;
        cs.counterUntil = player.level().getGameTime() + 25;
        ServerLevel level = player.serverLevel();
        Vec3 p = player.position();
        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 14, 0.4, 0.6, 0.4, 0.02);
        level.sendParticles(RingParticleOptions.ground(0x9FE8FF, 2.2f, 10), p.x, p.y + 0.1, p.z, 1, 0, 0, 0, 0);
        ServerFx.sound(level, p, ModSounds.PARRY.get(), 0.6f, 1.6f);
        ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 0.6f, 3);
    }

    // ================================================================== block / parry

    public static void startBlocking(ServerPlayer player, OPPlayerData data) {
        if (data.blocking() || busy(player, data) || data.combat.heavyCharging) return;
        data.setBlocking(true);
        data.combat.blockStart = player.level().getGameTime();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(BLOCK_SLOW_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(BLOCK_SLOW_ID, "onepiece block", -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public static void stopBlocking(ServerPlayer player, OPPlayerData data) {
        data.setBlocking(false);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(BLOCK_SLOW_ID);
    }

    public static boolean inParryWindow(ServerPlayer player, OPPlayerData data) {
        return data.blocking() && player.level().getGameTime() - data.combat.blockStart <= OPConfig.PARRY_WINDOW_TICKS.get();
    }

    /** True if {@code from} lies in front of the player (within ~145°). */
    public static boolean isFrontal(ServerPlayer player, Vec3 from) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 to = from.subtract(player.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4) return true;
        return look.dot(to.normalize()) > -0.3;
    }

    public static void parry(ServerPlayer player, OPPlayerData data, LivingEntity attacker) {
        ServerLevel level = player.serverLevel();
        data.combat.counterUntil = level.getGameTime() + 25;
        attacker.addEffect(new MobEffectInstance(ModEffects.STUNNED.get(), 30, 0, false, false, true));
        Vec3 dir = attacker.position().subtract(player.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-4) {
            dir = dir.normalize();
            attacker.setDeltaMovement(dir.x * 0.7, 0.25, dir.z * 0.7);
            attacker.hurtMarked = true;
        }
        Vec3 mid = player.getEyePosition().add(player.getLookAngle().scale(0.8));
        ServerFx.impact(level, mid, player.getLookAngle(), 1.4f, 0xFFD54A);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 16, 0.3, 0.3, 0.3, 0.3);
        ServerFx.sound(level, mid, ModSounds.PARRY.get(), 1.2f, 1.0f);
        ServerFx.animation(player, "parry");
        ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, 4);
        if (attacker instanceof ServerPlayer ap) ServerFx.shake(level, ap.position(), 2, 0.6f, 8);
    }

    /**
     * Regular (non-parry) block. Returns the damage that gets through and drains energy; a block that can't be
     * paid for is a guard break.
     */
    public static float block(ServerPlayer player, OPPlayerData data, float amount) {
        float absorbed = amount * OPConfig.BLOCK_REDUCTION.get().floatValue();
        float cost = absorbed * BLOCK_COST_PER_DAMAGE;
        ServerLevel level = player.serverLevel();
        Vec3 mid = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        if (!data.consumeEnergy(cost)) {
            guardBreak(player, data);
            return amount;
        }
        level.sendParticles(ModParticles.IMPACT_SPARK.get(), mid.x, mid.y - 0.3, mid.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y - 0.3, mid.z, 6, 0.2, 0.2, 0.2, 0.2);
        ServerFx.sound(level, mid, ModSounds.BLOCK.get(), 0.9f, 1f);
        ServerFx.animation(player, "block_hit");
        return amount - absorbed;
    }

    public static void guardBreak(ServerPlayer player, OPPlayerData data) {
        stopBlocking(player, data);
        player.addEffect(new MobEffectInstance(ModEffects.STUNNED.get(), 24, 0, false, false, true));
        ServerLevel level = player.serverLevel();
        ServerFx.sound(level, player.position(), ModSounds.HIT_HEAVY.get(), 1f, 1.4f);
        ServerFx.animation(player, "guard_break");
        ServerFx.shake(level, player.position(), 2, 0.6f, 10);
    }

    // ================================================================== tick

    public static void tick(ServerPlayer player, OPPlayerData data) {
        CombatState cs = data.combat;
        long now = player.level().getGameTime();

        if (player.onGround()) cs.airDashUsed = false;
        if (cs.comboStep != 0 && now > cs.comboExpire) cs.comboStep = 0;
        if (cs.bufferedLight && now >= cs.recoveryUntil) light(player, data);

        if (cs.heavyCharging) {
            int t = (int) (now - cs.heavyStart);
            data.setChanneling(HEAVY_CHANNEL, Math.min(t, HEAVY_MAX_CHARGE));
            if (player.hasEffect(ModEffects.STUNNED.get())) {
                cs.heavyCharging = false;
                data.setChanneling(null, 0);
            } else if (t > HEAVY_MAX_CHARGE + 40) {
                heavyRelease(player, data);
            } else if (t == HEAVY_MAX_CHARGE) {
                ServerFx.sound(player.serverLevel(), player.position(), ModSounds.CHARGE.get(), 0.8f, 1.6f);
                Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(-0.4)).add(0, -0.4, 0);
                player.serverLevel().sendParticles(ModParticles.IMPACT_SPARK.get(), hand.x, hand.y, hand.z, 1, 0, 0, 0, 0);
                player.serverLevel().sendParticles(RingParticleOptions.ground(0xFFFFFF, 1.6f, 8), player.getX(), player.getY() + 0.1, player.getZ(), 1, 0, 0, 0, 0);
            }
        }

        if (cs.slamPending) {
            if (player.onGround() || player.isInWater()) landSlam(player, data);
            else if (now - cs.slamStart > 80) cs.slamPending = false;
            else {
                Vec3 v = player.getDeltaMovement();
                if (v.y > -1.2) {
                    player.setDeltaMovement(v.x, -1.6, v.z);
                    player.hurtMarked = true;
                }
            }
        }

        if (data.blocking() && (busy(player, data) || !player.isAlive())) stopBlocking(player, data);

        if (cs.comboHits > 0 && now > cs.comboHitsExpire) {
            cs.comboHits = 0;
            data.setComboHits(0);
        }
    }

    /** Fall damage is cancelled for an active/just-landed slam. */
    public static boolean ignoresFall(OPPlayerData data, long now) {
        return data.combat.slamPending || now - data.combat.slamLandedAt < 5;
    }

    public static void reset(ServerPlayer player, OPPlayerData data) {
        data.combat.reset();
        data.setComboHits(0);
        if (data.channeling() != null && data.channeling().equals(HEAVY_CHANNEL)) data.setChanneling(null, 0);
        stopBlocking(player, data);
    }

    // ================================================================== utils

    public static boolean isBlade(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof TridentItem;
    }

    public static double heightAboveGround(LivingEntity e) {
        Vec3 from = e.position();
        Vec3 to = from.subtract(0, 16, 0);
        var hr = e.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, e));
        return hr.getType() == HitResult.Type.MISS ? 16 : from.y - hr.getLocation().y;
    }

    private CombatManager() {}
}
