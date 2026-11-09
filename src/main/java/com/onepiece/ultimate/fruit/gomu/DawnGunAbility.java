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

/** Gear Fifth – Dawn Gun: a giant cartoon fist. Whatever it hits turns to rubber and bounces; terrain wobbles instead of breaking. */
public final class DawnGunAbility extends Ability {
    DawnGunAbility() {
        super("gomu_dawn_gun", Properties.of(Activation.INSTANT).cost(18).cooldown(36)
                .sharesCooldownWith(OnePieceMod.id("gomu_pistol")).icon("gomu_dawn_gun").color(0xF4F4F4));
    }

    static Consumer<LivingEntity> toonHit(AbilityContext ctx, Vec3 dir, float damage, float knock, float lift, float impact) {
        Consumer<LivingEntity> base = GomuFx.fistHit(ctx, dir, damage, knock, lift, 10, 1.4f, impact, 0xFFFFFF, 3, true);
        return t -> {
            base.accept(t);
            ToonPhysics.rubberize(t, 120);
        };
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        Vec3 dir = ctx.look();
        ServerFx.animation(p, "gomu_pistol");
        ctx.schedule(3, () -> {
            level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(1), dir, 1)
                    .speed(3.0).range(22).scale(3.0f).style(StretchFistEntity.Style.DAWN)
                    .onHitEntity(toonHit(ctx, dir, 20f, 2.2f, 1.4f, 2.2f))
                    .onHitBlock((hit, last) -> {
                        // rubber terrain: no destruction, a bouncy shockwave instead
                        DestructionManager.shockwave(level, hit.getLocation(), 5f, 6, 4f, 1.1f, 0.6f, p);
                        level.sendParticles(RingParticleOptions.ground(0xFFFFFF, 5f, 12), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 1, 0, 0, 0, 0);
                        ServerFx.sound(level, hit.getLocation(), ModSounds.GOMU_BOUNCE.get(), 1.4f, 0.7f);
                    }));
            GomuFx.stretch(level, p.position(), 1.2f);
        });
    }
}
