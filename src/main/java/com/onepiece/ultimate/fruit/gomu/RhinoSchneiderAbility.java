package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.combat.CombatHits;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.combat.HitInfo;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.entity.StretchFistEntity;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModDamageTypes;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Boundman – Rhino Schneider: compress, then launch feet-first like a cannonball. Rams through every enemy on the way
 * (guard break, launch) and smashes a crater into the first wall it meets.
 */
public final class RhinoSchneiderAbility extends Ability {
    private static final int FLIGHT = 12;

    RhinoSchneiderAbility() {
        super("gomu_rhino_schneider", Properties.of(Activation.INSTANT).cost(26).cooldown(90)
                .sharesCooldownWith(OnePieceMod.id("gomu_rocket")).icon("gomu_rhino_schneider").color(0xC03030));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerFx.animation(p, "rhino_schneider");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_INFLATE.get(), 1f, 0.6f);
        Vec3 dir = ctx.look().multiply(1, 0.3, 1).normalize();
        Set<Integer> hit = new HashSet<>();
        boolean[] done = {false};
        for (int t = 3; t <= 3 + FLIGHT; t++) {
            final int tick = t;
            ctx.schedule(t, () -> {
                if (done[0] || !p.isAlive()) return;
                ServerLevel level = ctx.level();
                if (tick == 3) {
                    ServerFx.sound(level, p.position(), ModSounds.GOMU_SNAP.get(), 1.4f, 0.6f);
                    ServerFx.camera(p, CameraEffectPacket.Type.FOV_PUNCH, 0.3f, 14);
                    level.sendParticles(RingParticleOptions.facing(0xFFFFFF, 2.5f, 8, p.getXRot(), p.getYRot()), p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
                }
                p.setDeltaMovement(dir.scale(2.8));
                p.hurtMarked = true;
                p.fallDistance = 0;
                level.sendParticles(ModParticles.STEAM.get(), p.getX(), p.getY() + 0.8, p.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                for (LivingEntity e : HitDetector.arc(p, dir, 2.6f, 360f)) {
                    if (!hit.add(e.getId())) continue;
                    HitInfo info = new HitInfo(p, 24f * ctx.power()).type(ModDamageTypes.MARTIAL).direction(dir)
                            .knockback(3.4f).lift(0.6f).stun(30).launch(2.6f).guardBreak();
                    if (CombatHits.apply(info, e)) {
                        ServerFx.impact(level, e.getBoundingBox().getCenter(), dir, 3f, 0xFFFFFF);
                        ServerFx.shake(level, e.position(), 24, 0.9f, 10);
                        ServerFx.camera(p, CameraEffectPacket.Type.HITSTOP, 1f, 3);
                        ctx.awardMastery(4);
                    }
                }
                if (p.horizontalCollision) {
                    done[0] = true;
                    Vec3 at = p.position().add(dir.scale(1.2)).add(0, 1, 0);
                    DestructionManager.crater(level, at, 3f, 1f, 3.5f, p);
                    DestructionManager.shockwave(level, p.position(), 5f, 6, 10f, 0.6f, 1f, p);
                    ServerFx.impact(level, at, dir, 3.5f, 0xFFFFFF);
                    ServerFx.sound(level, at, ModSounds.HIT_HEAVY.get(), 1.8f, 0.5f);
                    ServerFx.shake(level, at, 30, 1.1f, 14);
                    p.setDeltaMovement(dir.scale(-0.4).add(0, 0.4, 0));
                }
            });
        }
    }
}
