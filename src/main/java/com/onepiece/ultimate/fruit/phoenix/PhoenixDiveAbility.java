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

/**
 * Phoenix Brand (dive): from the air, plunge along your gaze as a comet of blue fire. The impact explodes in flames:
 * damage, launch, burning and a scorched crater. Must be at least 4 blocks above the ground.
 */
public final class PhoenixDiveAbility extends Ability {
    PhoenixDiveAbility() {
        super("phoenix_dive", Properties.of(Activation.INSTANT).cost(25).cooldown(140).mastery(20).color(0x3EC7FF));
    }

    @Override
    public boolean canActivate(AbilityContext ctx) {
        if (CombatManager.heightAboveGround(ctx.player()) >= 4) return true;
        ctx.player().displayClientMessage(Component.translatable("message.onepiece.needs_height").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerPlayer p = ctx.player();
        Vec3 dir = ctx.look();
        if (dir.y > -0.35) dir = new Vec3(dir.x, -0.35, dir.z).normalize();
        final Vec3 d = dir;
        ServerFx.animation(p, "phoenix_dive");
        ServerFx.sound(ctx.level(), p.position(), ModSounds.PHOENIX_CRY.get(), 1.4f, 1.1f);
        boolean[] done = {false};
        for (int t = 1; t <= 30; t++) {
            ctx.schedule(t, () -> {
                if (done[0] || !p.isAlive()) return;
                ServerLevel level = ctx.level();
                boolean impact = p.onGround() || p.horizontalCollision || !HitDetector.arc(p, d, 1.8f, 360f).isEmpty();
                if (impact) {
                    done[0] = true;
                    explode(ctx, level, p);
                    return;
                }
                p.setDeltaMovement(d.scale(2.4));
                p.hurtMarked = true;
                p.fallDistance = 0;
                PhoenixFx.flames(level, p.position().add(0, 0.6, 0), 8, 0.35);
            });
        }
    }

    private void explode(AbilityContext ctx, ServerLevel level, ServerPlayer p) {
        Vec3 c = p.position();
        DestructionManager.shockwave(level, c, 6f, 8, 14f * ctx.power(), 0.7f, 1.1f, p);
        DestructionManager.crater(level, c.add(0, -0.4, 0), 2.2f, 0.6f, 1.5f, p);
        for (LivingEntity e : HitDetector.arc(p, 5f, 360f)) e.setSecondsOnFire(5);
        PhoenixFx.flames(level, c.add(0, 0.5, 0), 120, 2.5);
        level.sendParticles(RingParticleOptions.ground(0x3EC7FF, 6f, 16), c.x, c.y + 0.1, c.z, 1, 0, 0, 0, 0);
        ServerFx.sound(level, c, ModSounds.SLAM.get(), 1.6f, 0.8f);
        ServerFx.sound(level, c, ModSounds.PHOENIX_FLAME.get(), 1.6f, 0.6f);
        ServerFx.shake(level, c, 30, 1f, 14);
        ServerFx.camera(p, CameraEffectPacket.Type.HITSTOP, 1f, 4);
        ServerFx.animation(p, "slam_land");
        ctx.awardMastery(4);
    }
}
