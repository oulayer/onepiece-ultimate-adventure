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

/**
 * Gear Fifth – Bajrang Gun: the fist swells to the size of a mountain and is driven forward. Unstoppable: hits
 * everything, rubberises survivors, bores through hills and ends in the biggest crater of the moveset.
 */
public final class BajrangGunAbility extends Ability {
    BajrangGunAbility() {
        super("gomu_bajrang_gun", Properties.of(Activation.CHARGE).cost(50).cooldown(1600).charge(20, 40)
                .channelAnimation("gomu_gigant_charge").sharesCooldownWith(OnePieceMod.id("gomu_gigant_pistol")).icon("gomu_bajrang_gun").color(0xFFFFFF));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_GEAR_FIFTH.get(), 1.4f, 0.8f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        if (ticks % 10 == 0) {
            ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_GEAR_FIFTH.get(), 1f, 0.9f + ticks * 0.01f);
            ServerFx.shake(ctx.level(), ctx.player().position(), 30, 0.3f + ticks * 0.015f, 10);
        }
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        float c = chargeTicks / (float) maxCharge();
        Vec3 dir = ctx.look();
        ServerFx.animation(p, "gomu_gigant_release");
        ServerFx.camera(p, CameraEffectPacket.Type.FLASH, 0.8f, 14);
        ServerFx.shake(level, p.position(), 70, 1.5f, 30);
        level.addFreshEntity(StretchFistEntity.create(level, p, ctx.shoulder(1).add(dir.scale(3)), dir, 1)
                .speed(2.4).range(42).scale(13f).pierce(999).bore().style(StretchFistEntity.Style.DAWN)
                .onHitEntity(ToonFx.toonHit(ctx, dir, 70f * (0.7f + 0.3f * c), 4.5f, 1.2f, 7f))
                .onHitBlock((hit, last) -> {
                    Vec3 at = hit.getLocation();
                    if (last) {
                        DestructionManager.crater(level, at, 9f, 0.8f, 6f, p);
                        DestructionManager.cracks(level, at, 8, 16, p);
                        DestructionManager.shockwave(level, at, 14f, 14, 10f, 1.2f, 1.4f, p);
                        ServerFx.impact(level, at, dir, 9f, 0xFFFFFF);
                        ServerFx.shake(level, at, 90, 1.6f, 34);
                    } else {
                        DestructionManager.tunnel(level, at, at.add(dir.scale(3)), 4.6f, 6f, p);
                    }
                    ServerFx.sound(level, at, ModSounds.RUMBLE.get(), 3f, 0.5f);
                }));
    }
}
