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

/** Phoenix Talon: a leaping kick wreathed in blue flame. Sets targets on fire. */
public final class PhoenixTalonAbility extends Ability {
    PhoenixTalonAbility() {
        super("phoenix_talon", Properties.of(Activation.INSTANT).cost(14).cooldown(36).mastery(0).color(0x3EC7FF));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        Vec3 look = ctx.look();
        p.setDeltaMovement(look.x * 1.1, Math.max(0.2, look.y * 0.6 + 0.2), look.z * 1.1);
        p.hurtMarked = true;
        ServerFx.animation(p, "kick_finisher");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_FLAME.get(), 1f, 1.3f);
        Set<Integer> hit = new HashSet<>();
        for (int t = 3; t <= 7; t++) {
            ctx.schedule(t, () -> {
                ServerLevel level = ctx.level();
                PhoenixFx.flames(level, p.position().add(0, 0.5, 0), 6, 0.3);
                for (LivingEntity e : HitDetector.arc(p, 3.6f, 110f)) {
                    if (!hit.add(e.getId())) continue;
                    HitInfo info = new HitInfo(p, 10f * ctx.power()).type(ModDamageTypes.FRUIT).direction(look).knockback(1.4f).lift(0.45f).stun(14).launch(1.1f);
                    if (CombatHits.apply(info, e)) {
                        e.setSecondsOnFire(4);
                        PhoenixFx.flames(level, e.getBoundingBox().getCenter(), 20, 0.4);
                        ServerFx.impact(level, e.getBoundingBox().getCenter(), look, 1.4f, 0x3EC7FF);
                        ctx.awardMastery(2);
                    }
                }
            });
        }
    }
}
