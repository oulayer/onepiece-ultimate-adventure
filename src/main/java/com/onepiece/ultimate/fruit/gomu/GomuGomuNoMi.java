package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.fruit.FruitRarity;
import com.onepiece.ultimate.fruit.FruitType;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import java.util.List;

/**
 * Gomu Gomu no Mi – the rubber body.
 * <p>
 * Passives: immune to lightning, takes half damage from blunt force (fists, shockwaves, impacts) unless it's a
 * blade, shrugs off part of projectile damage, never takes fall damage and bounces off the ground from high falls.
 * <p>
 * Moves (slot → mastery): Pistol 0, Whip 5, Gatling 10, Rocket 15, Bazooka 22, Gear Second 30, Gigant Pistol 45.
 */
public final class GomuGomuNoMi extends DevilFruit {

    public GomuGomuNoMi(ResourceLocation id) {
        super(id, FruitType.PARAMECIA, FruitRarity.MYTHICAL, 0x8E3FD1);
    }

    @Override
    protected List<Ability> createAbilities() {
        GomuAbilities.bootstrap();
        return List.of(
                GomuAbilities.PISTOL,
                GomuAbilities.WHIP,
                GomuAbilities.GATLING,
                GomuAbilities.ROCKET,
                GomuAbilities.BAZOOKA,
                GomuAbilities.GIGANT_PISTOL
        );
    }

    @Override
    protected List<Ability> createTransformations() {
        return List.of(GomuAbilities.GEAR_SECOND, GomuAbilities.BOUNDMAN, GomuAbilities.SNAKEMAN, GomuAbilities.GEAR_FIFTH);
    }

    @Override
    public boolean isImmuneTo(ServerPlayer player, OPPlayerData data, DamageSource source) {
        return source.is(DamageTypes.LIGHTNING_BOLT);
    }

    @Override
    public float modifyIncomingDamage(ServerPlayer player, OPPlayerData data, DamageSource source, float amount) {
        if (source.is(ModDamageTypes.SLASH) || source.is(ModDamageTypes.INTERNAL)) return amount;
        // Armament Haki hardens the blow – rubber can't absorb it.
        if (com.onepiece.ultimate.haki.HakiManager.isImbued(source.getEntity())) return amount;
        if (source.is(ModDamageTypes.MARTIAL) || source.is(ModDamageTypes.IMPACT)) return amount * 0.5f;
        if (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK)) {
            boolean blade = source.getEntity() instanceof LivingEntity le && CombatManager.isBlade(le.getMainHandItem());
            return blade ? amount : amount * 0.5f;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof AbstractArrow) return amount * 0.6f;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount * 0.8f;
        return amount;
    }

    @Override
    public void onFall(ServerPlayer player, OPPlayerData data, LivingFallEvent event) {
        float distance = event.getDistance();
        event.setDamageMultiplier(0f);
        if (distance > 5f && !player.isShiftKeyDown()) {
            Vec3 v = player.getDeltaMovement();
            double bounce = Math.min(1.3, 0.25 + distance * 0.045);
            player.setDeltaMovement(v.x * 1.1, bounce, v.z * 1.1);
            player.hurtMarked = true;
            ServerFx.sound(player.serverLevel(), player.position(), ModSounds.GOMU_BOUNCE.get(), 1f, (float) (1.3 - bounce * 0.4));
            ServerFx.animation(player, "gomu_bounce");
        }
        event.setDistance(0);
    }
}
