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

/** Blue Flame Wave: a beat of burning wings that blasts a cone in front – pushes, burns and lights the air. */
public final class BlueFlameWaveAbility extends Ability {
    BlueFlameWaveAbility() {
        super("phoenix_flame_wave", Properties.of(Activation.INSTANT).cost(20).cooldown(80).mastery(5).color(0x3EC7FF));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        Vec3 look = ctx.look();
        ServerFx.animation(p, "phoenix_wing_wave");
        ServerFx.sound(level, p.position(), ModSounds.PHOENIX_WING.get(), 1.4f, 0.7f);
        ctx.schedule(3, () -> {
            for (LivingEntity e : HitDetector.arc(p, 9f, 75f)) {
                HitInfo info = new HitInfo(p, 7f * ctx.power()).type(ModDamageTypes.FRUIT).direction(look).knockback(1.8f).lift(0.35f).stun(10);
                if (CombatHits.apply(info, e)) {
                    e.setSecondsOnFire(5);
                    ctx.awardMastery(2);
                }
            }
            for (int i = 1; i <= 8; i++) {
                Vec3 at = p.getEyePosition().add(look.scale(i * 1.1));
                PhoenixFx.flames(level, at, 6 + i, 0.15 + i * 0.12);
            }
            level.sendParticles(RingParticleOptions.facing(0x3EC7FF, 4f, 12, p.getXRot(), p.getYRot()),
                    p.getEyePosition().add(look.scale(2)).x, p.getEyePosition().add(look.scale(2)).y, p.getEyePosition().add(look.scale(2)).z, 1, 0, 0, 0, 0);
            ServerFx.sound(level, p.position(), ModSounds.PHOENIX_FLAME.get(), 1.2f, 0.8f);
        });
    }
}
