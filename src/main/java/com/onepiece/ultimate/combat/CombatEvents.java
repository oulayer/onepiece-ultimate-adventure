package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.haki.HakiAbilities;
import com.onepiece.ultimate.haki.HakiManager;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.progression.ProgressionManager;
import com.onepiece.ultimate.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hooks the combat rules into vanilla damage so they apply to <em>every</em> attack in the game –
 * zombies, skeleton arrows, other players and our own abilities alike.
 * <p>
 * Order: {@link LivingAttackEvent} decides whether a hit happens at all (stunned attacker, immunity,
 * dodge i-frames, parry). {@link LivingHurtEvent} then scales the damage (block, stats, transformations,
 * fruit passives, PvP) and awards experience.
 */
@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID)
public final class CombatEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();

        // A staggered entity cannot land melee hits.
        if (attacker instanceof LivingEntity la && direct == attacker && la.hasEffect(ModEffects.STUNNED.get())) {
            event.setCanceled(true);
            return;
        }

        if (!(victim instanceof ServerPlayer player)) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null) return;
        long now = player.level().getGameTime();

        DevilFruit fruit = data.fruit();
        if (fruit != null && !data.seaWeakened() && fruit.isImmuneTo(player, data, source)) {
            event.setCanceled(true);
            return;
        }
        // Logia bodies let attacks pass through unless they're coated with Armament Haki.
        if (fruit != null && !data.seaWeakened() && !HakiManager.isImbued(attacker) && fruit.isIntangibleTo(player, data, source)) {
            event.setCanceled(true);
            return;
        }

        if (attacker == null && direct == null) return; // environmental damage can't be dodged or parried
        if (attacker == player) return;

        if (now < data.combat.iframesUntil) {
            event.setCanceled(true);
            CombatManager.perfectDodge(player, data);
            return;
        }
        // Observation Haki reads the attack and sidesteps it.
        if (HakiManager.tryAutoDodge(player, data, direct != null ? direct : attacker)) {
            event.setCanceled(true);
            return;
        }

        HitInfo hit = CombatHits.current();
        boolean guardBreak = hit != null && hit.guardBreak;
        Vec3 from = direct != null ? direct.position() : attacker.position();
        if (data.blocking() && !guardBreak && !source.is(ModDamageTypes.INTERNAL) && CombatManager.isFrontal(player, from) && CombatManager.inParryWindow(player, data)) {
            event.setCanceled(true);
            if (attacker instanceof LivingEntity la) CombatManager.parry(player, data, la);
            if (direct != null && direct != attacker && direct instanceof net.minecraft.world.entity.projectile.Projectile proj) {
                // Parried projectiles are deflected back.
                proj.setDeltaMovement(proj.getDeltaMovement().scale(-0.6));
                proj.setOwner(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // ---- outgoing (attacker is a player): stats & transformations apply to every damage source they own
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            OPPlayerData ad = OPCapabilities.getOrNull(attacker);
            if (ad != null) {
                amount *= DamageCalculator.outgoingMultiplier(ad);
                if (attacker.hasEffect(ModEffects.INTIMIDATED.get())) amount *= 0.6f;
                if (victim instanceof Player) amount *= OPConfig.PVP_DAMAGE_MULTIPLIER.get().floatValue();
                ad.combat.markInCombat(attacker.level().getGameTime());
            }
        }

        // ---- incoming (victim is a player)
        if (victim instanceof ServerPlayer player) {
            OPPlayerData data = OPCapabilities.getOrNull(player);
            if (data != null) {
                data.combat.markInCombat(player.level().getGameTime());
                Entity src = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
                HitInfo hit = CombatHits.current();
                boolean guardBreak = hit != null && hit.guardBreak;
                if (data.blocking() && src != null && src != player && !source.is(ModDamageTypes.INTERNAL)
                        && CombatManager.isFrontal(player, src.position())) {
                    if (guardBreak) CombatManager.guardBreak(player, data);
                    else amount = CombatManager.block(player, data, amount);
                }
                amount *= DamageCalculator.incomingMultiplier(data);
                // Armament clash: an equal or stronger coating cancels the defender's armament defence.
                if (data.isToggleActive(HakiAbilities.ARMAMENT.id())) {
                    int atk = HakiManager.armamentLevelIfActive(source.getEntity());
                    if (atk > 0 && atk >= data.hakiLevel(HakiType.ARMAMENT)) {
                        amount /= HakiAbilities.ARMAMENT.defenseMultiplier(data);
                        Vec3 at = player.getEyePosition();
                        ((net.minecraft.server.level.ServerLevel) player.level()).sendParticles(
                                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, at.x, at.y - 0.4, at.z, 10, 0.3, 0.3, 0.3, 0.4);
                    }
                }
                DevilFruit fruit = data.fruit();
                if (fruit != null && !data.seaWeakened()) amount = fruit.modifyIncomingDamage(player, data, source, amount);
            }
        }

        event.setAmount(Math.max(0f, amount));
        if (amount <= 0f) {
            event.setCanceled(true);
            return;
        }

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            ProgressionManager.onDamageDealt(attacker, victim, Math.min(amount, victim.getHealth()));
            OPPlayerData ad = OPCapabilities.getOrNull(attacker);
            if (ad != null) HakiManager.onDamageDealt(attacker, ad, victim, amount);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null) return;
        if (CombatManager.ignoresFall(data, player.level().getGameTime())) {
            event.setDistance(0);
            event.setDamageMultiplier(0);
            return;
        }
        DevilFruit fruit = data.fruit();
        if (fruit != null && !data.seaWeakened()) fruit.onFall(player, data, event);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != victim) {
            ProgressionManager.onKill(killer, victim);
        }
    }

    private CombatEvents() {}
}
