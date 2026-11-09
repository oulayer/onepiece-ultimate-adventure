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

/** Snakeman – Black Mamba: a storm of homing fists striking the targets from every angle. */
public final class BlackMambaAbility extends Ability {
    BlackMambaAbility() {
        super("gomu_black_mamba", Properties.of(Activation.HOLD).cost(18).drain(0.9f).maxHold(50).cooldown(200)
                .channelAnimation("gomu_jet_gatling").sharesCooldownWith(OnePieceMod.id("gomu_gatling")).icon("gomu_black_mamba").color(0x8A2040));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        GomuFx.stretch(ctx.level(), ctx.player().position(), 1.4f);
    }

    @Override
    public void onHoldTick(AbilityContext ctx, int ticks) {
        if (ticks < 2) return;
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        var r = p.getRandom();
        var targets = HitDetector.arc(p, 16f, 80f);
        LivingEntity target = targets.isEmpty() ? null : targets.get(r.nextInt(Math.min(4, targets.size())));
        int side = r.nextBoolean() ? 1 : -1;
        Vec3 look = ctx.look();
        // launch outward to the side, the homing bends it back in – hits come from every direction
        Vec3 sideways = new Vec3(-look.z, 0, look.x).scale(side * (0.6 + r.nextFloat() * 0.6));
        Vec3 dir = look.add(sideways).add(0, (r.nextFloat() - 0.3) * 0.6, 0).normalize();
        level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(side), dir, side)
                .speed(4.0).range(18).homing(target, 0.35).serpentine(0.7 + r.nextFloat() * 0.6, 0.9 + r.nextFloat() * 0.6, r.nextFloat() * 6.28)
                .style(StretchFistEntity.Style.JET)
                .onHitEntity(GomuFx.fistHit(ctx, look, 5.5f, 0.3f, 0.05f, 7, 0f, 0.8f, 0xFF5070, ticks % 5 == 0 ? 1 : 0, false)));
        if (ticks % 3 == 0) ServerFx.sound(level, p.position(), ModSounds.SWING_LIGHT.get(), 0.6f, 1.4f + r.nextFloat() * 0.3f);
    }
}
