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

/** Gear Fifth – Dawn Rocket: the body itself becomes a cartoon rocket, flying straight along the gaze and ramming everything. */
public final class DawnRocketAbility extends Ability {
    private static final int FLIGHT = 30;

    DawnRocketAbility() {
        super("gomu_dawn_rocket", Properties.of(Activation.INSTANT).cost(22).cooldown(80)
                .sharesCooldownWith(OnePieceMod.id("gomu_rocket")).icon("gomu_dawn_rocket").color(0xF4F4F4));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        Vec3 dir = ctx.look();
        ServerFx.animation(p, "gomu_rocket");
        ServerFx.camera(p, CameraEffectPacket.Type.FOV_PUNCH, 0.35f, 30);
        ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_SNAP.get(), 1.2f, 0.8f);
        Set<Integer> hit = new HashSet<>();
        for (int t = 1; t <= FLIGHT; t++) {
            ctx.schedule(t, () -> {
                if (!p.isAlive()) return;
                // keep overriding velocity (gravity included) instead of toggling no-gravity, which would persist on logout
                p.setDeltaMovement(dir.scale(2.2));
                p.hurtMarked = true;
                p.fallDistance = 0;
                ServerLevel level = ctx.level();
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 0.8, p.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
                for (LivingEntity e : HitDetector.arc(p, dir, 2.4f, 360f)) {
                    if (!hit.add(e.getId())) continue;
                    HitInfo info = new HitInfo(p, 16f * ctx.power()).type(ModDamageTypes.MARTIAL).direction(dir).knockback(2f).lift(0.9f).stun(20).launch(1.6f);
                    if (CombatHits.apply(info, e)) {
                        ToonPhysics.rubberize(e, 120);
                        ServerFx.impact(level, e.getBoundingBox().getCenter(), dir, 2f, 0xFFFFFF);
                        ctx.awardMastery(3);
                    }
                }
            });
        }
    }
}
