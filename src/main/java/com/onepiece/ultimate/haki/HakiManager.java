package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.network.packet.ThreatsPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Haki rules shared by all techniques: awakening, training, the haki gauge, armament imbuement (used by every
 * damage path), observation auto-dodge and threat sensing, and Conqueror's infusion on-hit effects.
 */
public final class HakiManager {
    private static final Vector3f ARMAMENT_DUST = new Vector3f(0.15f, 0.05f, 0.25f);

    // ================================================================== awakening & training

    /** Rolls the Conqueror's potential once per player and awakens Haki types whose level requirement is met. */
    public static void checkAwakenings(ServerPlayer player, OPPlayerData data) {
        HakiData h = data.haki();
        if (!h.potentialRolled()) {
            h.setConquerorPotential(player.getRandom().nextDouble() < OPConfig.CONQUEROR_CHANCE.get());
            data.markFull();
        }
        int level = data.progression().level();
        if (!h.isAwakened(HakiType.ARMAMENT) && level >= OPConfig.ARMAMENT_UNLOCK_LEVEL.get()) awaken(player, data, HakiType.ARMAMENT);
        if (!h.isAwakened(HakiType.OBSERVATION) && level >= OPConfig.OBSERVATION_UNLOCK_LEVEL.get()) awaken(player, data, HakiType.OBSERVATION);
        if (!h.isAwakened(HakiType.CONQUEROR) && h.hasConquerorPotential() && level >= OPConfig.CONQUEROR_UNLOCK_LEVEL.get()) {
            awaken(player, data, HakiType.CONQUEROR);
        }
    }

    public static void awaken(ServerPlayer player, OPPlayerData data, HakiType type) {
        data.haki().awaken(type);
        data.markFull();
        data.markVisual();
        ServerLevel level = player.serverLevel();
        Vec3 p = player.position();
        ServerFx.sound(level, p, ModSounds.HAKI_AWAKEN.get(), 1.2f, 1f);
        ServerFx.animation(player, "haki_awaken");
        level.sendParticles(RingParticleOptions.ground(type.color, 4f, 20), p.x, p.y + 0.1, p.z, 1, 0, 0, 0, 0);
        if (type == HakiType.CONQUEROR) {
            for (int i = 0; i < 10; i++) bolt(level, p.add(0, 1, 0), randomDir(player).scale(3 + player.getRandom().nextFloat() * 3));
            ServerFx.shake(level, p, 40, 0.8f, 20);
        }
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(type.displayName().copy().withStyle(ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.onepiece.haki_awakened")));
        }
    }

    public static void awardXp(ServerPlayer player, OPPlayerData data, HakiType type, int amount) {
        HakiData h = data.haki();
        if (!h.isAwakened(type) || amount <= 0 || h.level(type) >= HakiData.MAX_LEVEL) return;
        int before = h.level(type);
        double mult = OPConfig.HAKI_XP_MULTIPLIER.get() * (1 + 0.02 * data.progression().get(PlayerStat.HAKI));
        h.setXp(type, h.xp(type) + (int) Math.max(1, Math.round(amount * mult)));
        data.markFull();
        int after = h.level(type);
        if (after > before) {
            data.markVisual();
            player.displayClientMessage(Component.translatable("message.onepiece.haki_level", type.displayName(), after)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            ServerFx.sound(player.serverLevel(), player.position(), ModSounds.HAKI_AWAKEN.get(), 0.6f, 1.5f);
            for (var a : HakiAbilities.all()) {
                if (a instanceof HakiAbility ha && ha.type() == type && ha.requiredLevel() > before && ha.requiredLevel() <= after) {
                    player.displayClientMessage(Component.translatable("message.onepiece.unlocked", ha.displayName())
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
                }
            }
        }
    }

    // ================================================================== armament

    /** Is this attacker's force coated with Armament Haki (lets hits connect with Logia bodies, beats rubber…)? */
    public static boolean isImbued(@Nullable Entity attacker) {
        if (!(attacker instanceof Player p)) return false;
        OPPlayerData d = OPCapabilities.getOrNull(p);
        return d != null && d.isToggleActive(HakiAbilities.ARMAMENT.id());
    }

    public static int armamentLevelIfActive(@Nullable Entity e) {
        if (!(e instanceof Player p)) return 0;
        OPPlayerData d = OPCapabilities.getOrNull(p);
        return d != null && d.isToggleActive(HakiAbilities.ARMAMENT.id()) ? d.hakiLevel(HakiType.ARMAMENT) : 0;
    }

    /** Extra melee reach while Conqueror's infusion is active (hits land a breath before contact). */
    public static float reachBonus(OPPlayerData data) {
        return data.isToggleActive(HakiAbilities.CONQUEROR_INFUSION.id()) ? 0.8f : 0f;
    }

    /** Called for every hit a player lands (vanilla attacks, combos, fruit abilities). */
    public static void onDamageDealt(ServerPlayer attacker, OPPlayerData data, LivingEntity victim, float amount) {
        if (data.isToggleActive(HakiAbilities.ARMAMENT.id())) {
            awardXp(attacker, data, HakiType.ARMAMENT, amount >= 6 ? 2 : 1);
            Vec3 at = victim.getBoundingBox().getCenter();
            attacker.serverLevel().sendParticles(new DustParticleOptions(ARMAMENT_DUST, 1.4f), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
        if (data.isToggleActive(HakiAbilities.CONQUEROR_INFUSION.id())) {
            ServerLevel level = attacker.serverLevel();
            Vec3 from = attacker.getEyePosition().add(0, -0.4, 0);
            Vec3 to = victim.getBoundingBox().getCenter();
            bolt(level, from, to.subtract(from));
            for (int i = 0; i < 3; i++) bolt(level, to, randomDir(attacker).scale(1.5 + attacker.getRandom().nextFloat()));
            level.sendParticles(RingParticleOptions.facing(0x200000, 2.2f, 8, attacker.getXRot(), attacker.getYRot()), to.x, to.y, to.z, 1, 0, 0, 0, 0);
            Vec3 push = to.subtract(attacker.position()).multiply(1, 0, 1);
            if (push.lengthSqr() > 1e-4) {
                push = push.normalize().scale(0.6);
                victim.push(push.x, 0.15, push.z);
                victim.hurtMarked = true;
            }
            if (attacker.getRandom().nextInt(3) == 0) ServerFx.sound(level, to, ModSounds.HAKI_LIGHTNING.get(), 0.8f, 1.1f);
            awardXp(attacker, data, HakiType.CONQUEROR, 1);
        }
    }

    // ================================================================== observation

    public static boolean futureSightActive(ServerPlayer player, OPPlayerData data) {
        return player.level().getGameTime() < data.hakiRuntime.futureSightUntil;
    }

    /**
     * Observation reads the attack before it lands. With Observation active there's a level-based chance to
     * sidestep automatically (with a short internal cooldown); Future Sight dodges everything.
     */
    public static boolean tryAutoDodge(ServerPlayer player, OPPlayerData data, Entity source) {
        long now = player.level().getGameTime();
        boolean future = futureSightActive(player, data);
        if (!future) {
            if (!data.isToggleActive(HakiAbilities.OBSERVATION.id()) || now < data.hakiRuntime.kenDodgeReadyAt) return false;
            int lvl = data.hakiLevel(HakiType.OBSERVATION);
            float chance = Math.min(0.75f, 0.25f + 0.05f * lvl + 0.003f * data.progression().get(PlayerStat.HAKI));
            if (player.getRandom().nextFloat() >= chance || !data.consume(com.onepiece.ultimate.ability.AbilityResource.HAKI, 6f)) return false;
            data.hakiRuntime.kenDodgeReadyAt = now + Math.max(10, 40 - 3 * lvl);
        }
        Vec3 incoming = player.position().subtract(source.position()).multiply(1, 0, 1);
        if (incoming.lengthSqr() < 1e-4) incoming = player.getLookAngle().multiply(-1, 0, -1);
        incoming = incoming.normalize();
        int side = player.getRandom().nextBoolean() ? 1 : -1;
        Vec3 step = new Vec3(-incoming.z * side, 0, incoming.x * side).scale(0.75).add(incoming.scale(0.15));
        player.setDeltaMovement(step.x, 0.12, step.z);
        player.hurtMarked = true;
        player.invulnerableTime = Math.max(player.invulnerableTime, 6);
        data.combat.counterUntil = now + 20;

        ServerLevel level = player.serverLevel();
        Vec3 p = player.position();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.3f, 0.85f, 1f), 1.2f), p.x, p.y + 1, p.z, 16, 0.25, 0.6, 0.25, 0.01);
        ServerFx.sound(level, p, ModSounds.HAKI_DODGE.get(), 0.9f, future ? 1.4f : 1.1f);
        ServerFx.animation(player, side > 0 ? "ken_dodge_left" : "ken_dodge_right");
        awardXp(player, data, HakiType.OBSERVATION, 3);
        return true;
    }

    public static float observationRadius(OPPlayerData data) {
        return Math.min(40f, 16f + 2.4f * data.hakiLevel(HakiType.OBSERVATION));
    }

    /** Hostile intent around the player: mobs targeting them and armed players looking at them. */
    private static void senseThreats(ServerPlayer player, OPPlayerData data) {
        float r = observationRadius(data);
        List<Integer> ids = new ArrayList<>();
        for (Entity e : player.level().getEntities(player, new AABB(player.blockPosition()).inflate(r), x -> x instanceof LivingEntity)) {
            if (e.distanceToSqr(player) > r * r) continue;
            boolean threat = e instanceof Mob m && m.getTarget() == player;
            if (!threat && e instanceof Player other && !other.isSpectator()) {
                Vec3 look = other.getLookAngle();
                Vec3 to = player.getEyePosition().subtract(other.getEyePosition()).normalize();
                OPPlayerData od = OPCapabilities.getOrNull(other);
                threat = look.dot(to) > 0.93 && od != null && (od.combatMode() || od.channeling() != null);
            }
            if (threat) ids.add(e.getId());
        }
        int hash = ids.hashCode();
        if (hash != data.hakiRuntime.lastThreatHash) {
            data.hakiRuntime.lastThreatHash = hash;
            OPNetwork.sendTo(player, new ThreatsPacket(ids.stream().mapToInt(Integer::intValue).toArray()));
        }
    }

    // ================================================================== tick

    public static void tick(ServerPlayer player, OPPlayerData data) {
        long now = player.level().getGameTime();
        if (data.futureSight() && now >= data.hakiRuntime.futureSightUntil) data.setFutureSight(false);

        if (data.isToggleActive(HakiAbilities.OBSERVATION.id())) {
            if (++data.hakiRuntime.threatTimer >= 5) {
                data.hakiRuntime.threatTimer = 0;
                senseThreats(player, data);
            }
            if (now % 100 == 0 && data.combat.inCombat(now)) awardXp(player, data, HakiType.OBSERVATION, 1);
        } else if (data.hakiRuntime.lastThreatHash != 0) {
            data.hakiRuntime.lastThreatHash = 0;
            OPNetwork.sendTo(player, new ThreatsPacket(new int[0]));
        }

        if (data.isToggleActive(HakiAbilities.ARMAMENT.id()) && now % 5 == 0
                && com.onepiece.ultimate.combat.CombatManager.isBlade(player.getMainHandItem())) {
            // Weapon coating: dark aura along the blade.
            Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.5, 0);
            player.serverLevel().sendParticles(new DustParticleOptions(ARMAMENT_DUST, 1f), hand.x, hand.y, hand.z, 3, 0.15, 0.25, 0.15, 0);
        }

        boolean draining = false;
        for (var id : data.activeToggles()) {
            var a = com.onepiece.ultimate.ability.AbilityRegistry.get(id);
            if (a instanceof HakiAbility) draining = true;
        }
        if (data.channeling() != null) {
            var a = com.onepiece.ultimate.ability.AbilityRegistry.get(data.channeling());
            if (a instanceof HakiAbility) draining = true;
        }
        if (!draining && !data.seaWeakened() && data.haki().anyAwakened()) {
            float regen = 0.12f + 0.004f * data.progression().get(PlayerStat.HAKI);
            if (!data.combat.inCombat(now)) regen *= 2f;
            data.setHakiGauge(data.hakiGauge() + regen);
        }
    }

    // ================================================================== fx helpers

    /** Spawns one black lightning bolt from {@code from} along {@code offset}. */
    public static void bolt(ServerLevel level, Vec3 from, Vec3 offset) {
        level.sendParticles(ModParticles.BLACK_LIGHTNING.get(), from.x, from.y, from.z, 0, offset.x, offset.y, offset.z, 1.0);
    }

    public static Vec3 randomDir(LivingEntity e) {
        var r = e.getRandom();
        return new Vec3(r.nextGaussian(), r.nextGaussian() * 0.5, r.nextGaussian()).normalize();
    }

    /** Dark screen pulse for every player near a Conqueror's Haki release. */
    public static void conquerorPressure(ServerLevel level, Vec3 pos, double radius, float intensity) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(pos);
            if (d <= radius) {
                OPNetwork.sendTo(p, new CameraEffectPacket(CameraEffectPacket.Type.DARK_PULSE, intensity * (float) (1 - d / radius * 0.6), 30));
            }
        }
    }

    /** Switches a Haki toggle on without cost or checks (used by forms that need the coating, e.g. Gear Fourth). */
    public static void forceToggle(ServerPlayer player, OPPlayerData data, com.onepiece.ultimate.ability.Ability toggle) {
        if (data.isToggleActive(toggle.id()) || !data.haki().isAwakened(HakiType.ARMAMENT)) return;
        data.setToggle(toggle.id(), true);
        data.abilities.toggleTicks.put(toggle.id(), 0);
        toggle.onActivate(com.onepiece.ultimate.ability.AbilityContext.of(player, data));
    }

    /** Haki burns out: active Haki ends and no Haki can be used for {@code ticks}. */
    public static void exhaust(ServerPlayer player, OPPlayerData data, int ticks) {
        data.hakiRuntime.exhaustedUntil = player.level().getGameTime() + ticks;
        for (var a : HakiAbilities.all()) com.onepiece.ultimate.ability.AbilityManager.endToggle(player, data, a, true);
        data.setHakiGauge(0);
        player.displayClientMessage(Component.translatable("message.onepiece.haki_exhausted").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    public static void reset(OPPlayerData data) {
        data.hakiRuntime.reset();
        data.setFutureSight(false);
    }

    private HakiManager() {}
}
