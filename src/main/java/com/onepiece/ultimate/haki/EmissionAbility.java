package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.CombatHits;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Advanced Armament – Emission (internal destruction): the haki is released <em>inside</em> what you strike.
 * Deals {@code onepiece:internal} damage that ignores armour, shields and blocking, and the force exits through
 * the far side – blasting the terrain behind the target, or hollowing a wall out from within.
 * Requires Armament to be active.
 */
public final class EmissionAbility extends HakiAbility {
    EmissionAbility() {
        super("haki_emission", HakiType.ARMAMENT, 6, Properties.of(Activation.CHARGE).cost(30).cooldown(240)
                .charge(8, 20).channelAnimation("emission_charge"));
    }

    @Override
    public boolean canActivate(AbilityContext ctx) {
        if (ctx.data().isToggleActive(HakiAbilities.ARMAMENT.id())) return true;
        ctx.player().displayClientMessage(Component.translatable("message.onepiece.requires_active", HakiAbilities.ARMAMENT.displayName())
                .withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.HAKI_ARMAMENT.get(), 0.9f, 0.7f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        if (ticks % 4 == 0) {
            Vec3 hand = ctx.player().getEyePosition().add(ctx.look().scale(-0.3)).add(0, -0.5, 0);
            HakiManager.bolt(ctx.level(), hand, HakiManager.randomDir(ctx.player()).scale(0.7));
        }
        if (ticks == maxCharge()) ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.CHARGE.get(), 0.8f, 0.7f);
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        float charge = chargeTicks / (float) maxCharge();
        ServerFx.animation(ctx.player(), "emission_release");
        ctx.schedule(2, () -> strike(ctx, charge));
    }

    private void strike(AbilityContext ctx, float charge) {
        ServerPlayer player = ctx.player();
        if (!player.isAlive()) return;
        ServerLevel level = ctx.level();
        Vec3 look = ctx.look();
        int lvl = ctx.data().hakiLevel(HakiType.ARMAMENT);
        float damage = (10f + 1.6f * lvl) * (0.6f + 0.4f * charge) * potency(ctx.data());
        float range = 4.5f + charge;

        boolean hit = false;
        for (LivingEntity t : HitDetector.arc(player, range, 55f)) {
            HitInfo info = new HitInfo(player, damage).type(ModDamageTypes.INTERNAL).direction(look)
                    .knockback(1.8f + charge).lift(0.35f).stun(25).launch(1.4f).guardBreak();
            if (!CombatHits.apply(info, t)) continue;
            hit = true;
            Vec3 c = t.getBoundingBox().getCenter();
            Vec3 exit = c.add(look.scale(t.getBbWidth() * 0.5 + 1.2));
            level.sendParticles(RingParticleOptions.facing(0x1A0A2A, 2.4f, 10, player.getXRot(), player.getYRot()), c.x, c.y, c.z, 1, 0, 0, 0, 0);
            level.sendParticles(RingParticleOptions.facing(0x6A3AFF, 1.6f, 8, player.getXRot(), player.getYRot()), exit.x, exit.y, exit.z, 1, 0, 0, 0, 0);
            for (int i = 0; i < 4; i++) HakiManager.bolt(level, c, look.scale(2).add(HakiManager.randomDir(player).scale(0.8)));
            // The force passes through and destroys what's behind.
            DestructionManager.crater(level, exit.add(look.scale(0.8)), 1.3f + charge, 0.9f, 2.6f, player);
            train(ctx, 4);
        }

        Vec3 eye = player.getEyePosition();
        BlockHitResult hr = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (!hit && hr.getType() == HitResult.Type.BLOCK) {
            // Internal destruction: the wall breaks from the inside out, leaving its face cracked.
            Vec3 inside = hr.getLocation().add(look.scale(1.9));
            DestructionManager.crater(level, inside, 1.7f + charge, 1f, 3.0f, player);
            DestructionManager.cracks(level, hr.getLocation(), 3, 4, player);
            level.sendParticles(RingParticleOptions.facing(0x1A0A2A, 2.8f, 10, player.getXRot(), player.getYRot()),
                    hr.getLocation().x, hr.getLocation().y, hr.getLocation().z, 1, 0, 0, 0, 0);
            train(ctx, 1);
        }
        ServerFx.sound(level, eye, ModSounds.HAKI_EMISSION.get(), 1.4f, 0.9f);
        ServerFx.shake(level, eye, 18, 0.7f + 0.4f * charge, 10);
        ServerFx.camera(player, CameraEffectPacket.Type.HITSTOP, 1f, hit ? 4 : 2);
    }
}
