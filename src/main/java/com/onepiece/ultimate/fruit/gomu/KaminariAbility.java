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
import com.onepiece.ultimate.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Gear Fifth – Kaminari: grab lightning from the sky (rubber is immune to it) and hurl it down on the point you aim at:
 * a storm of strikes, an electric shockwave that damages and paralyses, and a scorched crater.
 */
public final class KaminariAbility extends Ability {
    KaminariAbility() {
        super("gomu_kaminari", Properties.of(Activation.CHARGE).cost(40).cooldown(400).charge(10, 30)
                .channelAnimation("kaminari_charge").sharesCooldownWith(OnePieceMod.id("gomu_bazooka")).icon("gomu_kaminari").color(0xFFF07A));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.HAKI_LIGHTNING.get(), 1f, 1.3f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        if (ticks % 3 == 0) {
            Vec3 hand = p.position().add(0, p.getBbHeight() + 0.6, 0);
            Vec3 sky = new Vec3((p.getRandom().nextFloat() - 0.5) * 3, 6 + p.getRandom().nextFloat() * 4, (p.getRandom().nextFloat() - 0.5) * 3);
            ctx.level().sendParticles(com.onepiece.ultimate.registry.ModParticles.WHITE_LIGHTNING.get(), hand.x + sky.x, hand.y + sky.y, hand.z + sky.z,
                    0, -sky.x, -sky.y, -sky.z, 1.0);
        }
        if (ticks % 8 == 0) ServerFx.sound(ctx.level(), p.position(), ModSounds.HAKI_LIGHTNING.get(), 0.8f, 1.5f);
        if (ticks == maxCharge()) ServerFx.shake(ctx.level(), p.position(), 20, 0.4f, 8);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        float c = chargeTicks / (float) maxCharge();
        ServerFx.animation(p, "kaminari_throw");
        Vec3 eye = p.getEyePosition();
        BlockHitResult hr = level.clip(new ClipContext(eye, eye.add(ctx.look().scale(40)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 at = hr.getType() == HitResult.Type.BLOCK ? hr.getLocation() : eye.add(ctx.look().scale(24));
        int strikes = 2 + (int) (c * 3);
        for (int i = 0; i < strikes; i++) {
            final int n = i;
            ctx.schedule(2 + i * 2, () -> {
                Vec3 pt = at.add((p.getRandom().nextFloat() - 0.5) * 3 * n, 0, (p.getRandom().nextFloat() - 0.5) * 3 * n);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(pt.x, pt.y, pt.z);
                    bolt.setVisualOnly(true); // damage and fire are handled below, never by vanilla lightning
                    level.addFreshEntity(bolt);
                }
            });
        }
        ctx.schedule(3, () -> {
            float radius = 4f + 3f * c;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                    x -> HitDetector.isValidTarget(p, x) && x.distanceToSqr(at) <= radius * radius)) {
                HitInfo info = new HitInfo(p, (14f + 14f * c) * ctx.power()).type(ModDamageTypes.FRUIT)
                        .direction(e.position().subtract(at)).knockback(1.2f).lift(0.6f).stun(40).guardBreak();
                if (CombatHits.apply(info, e)) {
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED.get(), 50, 0, false, false, true));
                    ctx.awardMastery(3);
                }
            }
            DestructionManager.shockwave(level, at, radius + 2, 8, 6f, 0.8f, 0.8f, p);
            DestructionManager.crater(level, at, 1.5f + c * 1.5f, 0.6f, 2f, p);
            DestructionManager.cracks(level, at, 5, 7, p);
            for (int k = 0; k < 10; k++) {
                Vec3 dir = new Vec3(p.getRandom().nextGaussian(), 0.2, p.getRandom().nextGaussian()).normalize().scale(radius);
                level.sendParticles(com.onepiece.ultimate.registry.ModParticles.WHITE_LIGHTNING.get(), at.x, at.y + 0.3, at.z, 0, dir.x, dir.y, dir.z, 1.0);
            }
            level.sendParticles(RingParticleOptions.ground(0xFFF07A, radius + 2, 16), at.x, at.y + 0.1, at.z, 1, 0, 0, 0, 0);
            ServerFx.sound(level, at, ModSounds.HAKI_CONQUEROR_WAVE.get(), 1.8f, 1.4f);
            ServerFx.shake(level, at, 40, 1.2f, 16);
            ServerFx.camera(p, CameraEffectPacket.Type.FLASH, 0.5f, 8);
        });
    }
}
