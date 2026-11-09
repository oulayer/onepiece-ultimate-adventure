package com.onepiece.ultimate.progression;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.data.Progression;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;

import java.util.UUID;

/**
 * Level, experience, attribute points and Devil Fruit mastery.
 * <ul>
 *     <li>Experience comes from damage dealt and kills (scaled by the victim's max health; hostile mobs and
 *         players give more).</li>
 *     <li>Each level grants attribute points spent in {@link PlayerStat}s, whose effects are applied here as
 *         real attribute modifiers or read by {@code DamageCalculator}.</li>
 *     <li>Mastery is earned by landing fruit abilities and unlocks the fruit's moves gradually.</li>
 * </ul>
 */
public final class ProgressionManager {
    private static final UUID HEALTH_ID = UUID.fromString("3b1f8c7a-90e2-4d55-8a61-7c3e2f4b9d01");
    private static final UUID SPEED_ID = UUID.fromString("8e2c6d4f-1a3b-4c5d-9e7f-0a1b2c3d4e5f");

    // ================================================================== experience

    public static void onDamageDealt(ServerPlayer player, LivingEntity victim, float damage) {
        if (damage <= 0 || victim instanceof net.minecraft.world.entity.decoration.ArmorStand) return;
        double factor = victim instanceof Enemy || victim instanceof Player ? 1.0 : 0.3;
        awardXp(player, Math.max(1, Math.round(damage * 0.6 * factor)));
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        double factor = victim instanceof Enemy ? 1.0 : victim instanceof Player ? 1.5 : 0.25;
        awardXp(player, Math.max(1, Math.round(victim.getMaxHealth() * 1.2 * factor)));
    }

    public static void awardXp(ServerPlayer player, long amount) {
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null || amount <= 0) return;
        Progression p = data.progression();
        int maxLevel = OPConfig.MAX_LEVEL.get();
        if (p.level() >= maxLevel) return;
        long xp = p.xp() + Math.round(amount * OPConfig.XP_MULTIPLIER.get());
        int levelsGained = 0;
        while (p.level() < maxLevel && xp >= Progression.xpForNext(p.level())) {
            xp -= Progression.xpForNext(p.level());
            p.setLevel(p.level() + 1);
            p.setUnspentPoints(p.unspentPoints() + OPConfig.POINTS_PER_LEVEL.get());
            levelsGained++;
        }
        p.setXp(p.level() >= maxLevel ? 0 : xp);
        data.markFull();
        if (levelsGained > 0) {
            onLevelUp(player, p.level());
            com.onepiece.ultimate.haki.HakiManager.checkAwakenings(player, data);
        }
    }

    private static void onLevelUp(ServerPlayer player, int level) {
        var sl = player.serverLevel();
        player.displayClientMessage(Component.translatable("message.onepiece.level_up", level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, player.getSoundSource(), 0.8f, 1.2f);
        sl.sendParticles(RingParticleOptions.ground(0xFFD54A, 2.5f, 16), player.getX(), player.getY() + 0.1, player.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.3);
    }

    // ================================================================== mastery

    public static void awardMastery(ServerPlayer player, OPPlayerData data, int amount) {
        if (!data.hasFruit() || amount <= 0) return;
        int before = data.mastery();
        if (before >= OPPlayerData.MAX_MASTERY) return;
        data.setMasteryXp(data.masteryXp() + amount);
        int after = data.mastery();
        if (after > before) {
            player.displayClientMessage(Component.translatable("message.onepiece.mastery_up", after).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            DevilFruit fruit = data.fruit();
            if (fruit != null) {
                for (Ability a : fruit.abilities()) {
                    if (a.requiredMastery() > before && a.requiredMastery() <= after) {
                        player.displayClientMessage(Component.translatable("message.onepiece.unlocked", a.displayName())
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
                        ServerFx.sound(player.serverLevel(), player.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
                    }
                }
            }
        }
    }

    // ================================================================== attributes

    public static boolean spendPoint(ServerPlayer player, PlayerStat stat) {
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null) return false;
        Progression p = data.progression();
        if (p.unspentPoints() <= 0 || p.get(stat) >= PlayerStat.MAX_POINTS) return false;
        p.setUnspentPoints(p.unspentPoints() - 1);
        p.set(stat, p.get(stat) + 1);
        applyAttributes(player, data);
        data.markFull();
        return true;
    }

    /** Re-applies stat-driven attribute modifiers (idempotent). */
    public static void applyAttributes(ServerPlayer player, OPPlayerData data) {
        Progression p = data.progression();
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "onepiece durability",
                p.get(PlayerStat.DURABILITY) * 0.5, AttributeModifier.Operation.ADDITION);
        setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, "onepiece agility",
                p.get(PlayerStat.AGILITY) * 0.004, AttributeModifier.Operation.MULTIPLY_BASE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static void setModifier(AttributeInstance inst, UUID id, String name, double value, AttributeModifier.Operation op) {
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && existing.getAmount() == value) return;
        inst.removeModifier(id);
        if (value != 0) inst.addPermanentModifier(new AttributeModifier(id, name, value, op));
    }

    /** Resets the character (admin command). */
    public static void reset(ServerPlayer player, OPPlayerData data) {
        Progression p = data.progression();
        p.setLevel(1);
        p.setXp(0);
        p.setUnspentPoints(0);
        for (PlayerStat s : PlayerStat.values()) p.set(s, 0);
        applyAttributes(player, data);
        data.markFull();
    }

    private ProgressionManager() {}
}
