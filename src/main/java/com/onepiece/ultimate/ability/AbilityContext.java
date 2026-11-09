package com.onepiece.ultimate.ability;

import com.onepiece.ultimate.combat.DamageCalculator;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.progression.ProgressionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Everything an ability needs while running, plus helpers shared by all abilities. */
public record AbilityContext(ServerPlayer player, ServerLevel level, OPPlayerData data) {

    public static AbilityContext of(ServerPlayer player, OPPlayerData data) {
        return new AbilityContext(player, player.serverLevel(), data);
    }

    public long now() {
        return level.getGameTime();
    }

    /** Outgoing damage multiplier from strength, transformations and fruit mastery. */
    public float power() {
        return DamageCalculator.fruitMultiplier(player, data);
    }

    /** 0..1 mastery fraction – abilities scale range/size with it. */
    public float masteryFraction() {
        return data.mastery() / (float) OPPlayerData.MAX_MASTERY;
    }

    public void awardMastery(int amount) {
        ProgressionManager.awardMastery(player, data, (int) Math.max(1, Math.round(amount * OPConfig.MASTERY_MULTIPLIER.get())));
    }

    public Vec3 look() {
        return player.getLookAngle();
    }

    /** World position of the player's shoulder on the given side (+1 right, -1 left). */
    public Vec3 shoulder(int side) {
        double yaw = Math.toRadians(player.yBodyRot);
        double ox = -Math.cos(yaw) * 0.38 * side;
        double oz = -Math.sin(yaw) * 0.38 * side;
        return player.position().add(ox, player.getBbHeight() * 0.78, oz);
    }

    public void schedule(int delay, Runnable action) {
        data.abilities.tasks.schedule(now(), delay, action);
    }
}
