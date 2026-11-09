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

/** Gear Fifth – Dawn Gatling: a cartoon storm of huge white fists, one every tick, each rubberising what it hits. */
public final class DawnGatlingAbility extends Ability {
    DawnGatlingAbility() {
        super("gomu_dawn_gatling", Properties.of(Activation.HOLD).cost(20).drain(0.8f).maxHold(50).cooldown(160)
                .channelAnimation("gomu_jet_gatling").sharesCooldownWith(OnePieceMod.id("gomu_gatling")).icon("gomu_dawn_gatling").color(0xFFFFFF));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        GomuFx.stretch(ctx.level(), ctx.player().position(), 1.6f);
    }

    @Override
    public void onHoldTick(AbilityContext ctx, int ticks) {
        if (ticks < 2) return;
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        var r = p.getRandom();
        int side = ticks % 2 == 0 ? 1 : -1;
        Vec3 look = ctx.look();
        Vec3 dir = look.add((r.nextFloat() - 0.5) * 0.45, (r.nextFloat() - 0.5) * 0.35, (r.nextFloat() - 0.5) * 0.45).normalize();
        level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(side), dir, side)
                .speed(3.6).range(14).scale(1.6f).grow(2.6f, 3).style(StretchFistEntity.Style.DAWN)
                .onHitEntity(ToonFx.toonHit(ctx, look, 6f, 0.4f, 0.25f, 1f)));
        if (ticks % 3 == 0) {
            ServerFx.sound(level, p.position(), ModSounds.SWING_LIGHT.get(), 0.7f, 1.2f + r.nextFloat() * 0.4f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 1.4, p.getZ(), 2, 0.4, 0.3, 0.4, 0.02);
        }
    }
}
