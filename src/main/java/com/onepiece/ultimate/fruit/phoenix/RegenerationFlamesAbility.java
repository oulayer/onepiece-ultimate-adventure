package com.onepiece.ultimate.fruit.phoenix;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.CombatManager;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Flames of Regeneration (hold): pour blue flames over yourself and allies within 6 blocks – heals over time,
 * cleanses harmful effects at the start. Costs energy while held.
 */
public final class RegenerationFlamesAbility extends Ability {
    RegenerationFlamesAbility() {
        super("phoenix_regeneration", Properties.of(Activation.HOLD).cost(10).drain(0.5f).maxHold(100).cooldown(240).mastery(10)
                .channelAnimation("phoenix_regen").color(0x6AE0FF));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        p.getActiveEffects().stream().filter(e -> e.getEffect().getCategory() == MobEffectCategory.HARMFUL).map(e -> e.getEffect())
                .toList().forEach(p::removeEffect);
        ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_FLAME.get(), 1f, 1.4f);
    }

    @Override
    public void onHoldTick(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        p.heal(0.3f);
        if (ticks % 2 == 0) PhoenixFx.body(level, p, 3);
        if (ticks % 5 == 0) {
            for (Player ally : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6), a -> a != p && a.isAlive())) {
                ally.heal(1.0f);
                PhoenixFx.body(level, ally, 4);
            }
            level.sendParticles(RingParticleOptions.ground(0x6AE0FF, 6f, 10), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
        }
        if (ticks % 40 == 0) ctx.awardMastery(1);
    }
}
