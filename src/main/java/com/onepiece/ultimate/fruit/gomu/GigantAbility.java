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
import com.onepiece.ultimate.core.TransformationEvents;
import com.onepiece.ultimate.data.OPPlayerData;

/**
 * Gear Fifth – Gigant: the rubber body balloons to colossal size for 10 seconds. A real 2.6× hitbox, +3 reach,
 * +50% damage, steps over walls, and every footstep sends out a small shockwave.
 */
public final class GigantAbility extends Ability {
    public static final int DURATION = 200;

    GigantAbility() {
        super("gomu_gigant", Properties.of(Activation.INSTANT).cost(35).cooldown(500)
                .sharesCooldownWith(OnePieceMod.id("gomu_whip")).icon("gomu_gigant").color(0xFFFFFF));
    }

    @Override
    public boolean canActivate(AbilityContext ctx) {
        return !ctx.data().giant();
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        OPPlayerData d = ctx.data();
        ServerFx.animation(p, "gear_fifth_gigant");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_INFLATE.get(), 1.6f, 0.5f);
        ctx.schedule(6, () -> {
            if (!p.isAlive() || !d.isToggleActive(GomuAbilities.GEAR_FIFTH.id())) return;
            d.setGiant(true);
            d.abilities.giantUntil = ctx.now() + DURATION;
            p.refreshDimensions();
            ServerLevel level = ctx.level();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 2, p.getZ(), 80, 2, 2, 2, 0.1);
            level.sendParticles(RingParticleOptions.ground(0xFFFFFF, 8f, 18), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
            ServerFx.sound(level, p.position(), ModSounds.GOMU_BOUNCE.get(), 2f, 0.4f);
            ServerFx.shake(level, p.position(), 40, 1f, 16);
            GearFifthAbility.applyGiantModifiers(p, true);
        });
    }
}
