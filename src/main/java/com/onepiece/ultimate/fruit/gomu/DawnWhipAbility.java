package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.combat.ToonPhysics;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Gear Fifth – Dawn Whip: a full-circle leg sweep 14 blocks wide that rubberises and catapults everyone into the sky. */
public final class DawnWhipAbility extends Ability {
    DawnWhipAbility() {
        super("gomu_dawn_whip", Properties.of(Activation.INSTANT).cost(26).cooldown(120)
                .sharesCooldownWith(OnePieceMod.id("gomu_whip")).icon("gomu_dawn_whip").color(0xF4F4F4));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "gomu_whip");
        GomuFx.stretch(ctx.level(), p.position(), 0.6f);
        Set<Integer> hit = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            final int step = i;
            ctx.schedule(2 + i, () -> {
                if (!p.isAlive()) return;
                ServerLevel level = ctx.level();
                double a0 = Math.toRadians(p.getYRot() + 90 - step * 60);
                Vec3 dir = new Vec3(-Math.sin(a0), 0, Math.cos(a0));
                for (LivingEntity t : HitDetector.arc(p, dir, 14f, 70f)) {
                    if (!hit.add(t.getId())) continue;
                    HitInfo info = new HitInfo(p, 14f * ctx.power()).type(ModDamageTypes.MARTIAL).direction(dir).knockback(1.2f).lift(1.6f).stun(20).launch(1.2f);
                    if (CombatHits.apply(info, t)) {
                        ToonPhysics.rubberize(t, 140);
                        ServerFx.impact(level, t.getBoundingBox().getCenter(), dir, 1.5f, 0xFFFFFF);
                        ctx.awardMastery(2);
                    }
                }
                for (float d = 2; d < 14; d += 2) {
                    Vec3 pt = p.position().add(dir.scale(d));
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, pt.x, pt.y + 0.6, pt.z, 1, 0.1, 0.1, 0.1, 0.01);
                }
                if (step == 0) ServerFx.sound(level, p.position(), ModSounds.SWING_HEAVY.get(), 1.4f, 0.6f);
            });
        }
    }
}
