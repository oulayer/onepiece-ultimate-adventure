package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.haki.HakiAbilities;
import com.onepiece.ultimate.haki.HakiManager;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.network.packet.CameraEffectPacket;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Shared rules of Gear Fourth: the muscles are inflated with air and coated with Armament Haki.
 * Needs Armament level 4 (switched on automatically and required for the whole duration). When it ends the user
 * is spent: Haki is exhausted for 30 s and the body is weakened.
 */
public abstract class GearFourthAbility extends TransformationAbility {
    public static final int EXHAUSTION_TICKS = 600;

    protected GearFourthAbility(String name, int activationTicks, Properties props) {
        super(name, "gomu_gear", activationTicks, props);
    }

    @Override
    public boolean canActivate(AbilityContext ctx) {
        if (ctx.data().hakiLevel(HakiType.ARMAMENT) >= 4) return true;
        ctx.player().displayClientMessage(Component.translatable("message.onepiece.haki_locked", displayName(),
                HakiType.ARMAMENT.displayName(), 4).withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    @Override
    protected void activate(AbilityContext ctx) {
        HakiManager.forceToggle(ctx.player(), ctx.data(), HakiAbilities.ARMAMENT);
        ServerFx.animation(ctx.player(), activationAnimation());
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.GOMU_INFLATE.get(), 1.2f, 0.8f);
    }

    protected String activationAnimation() {
        return "gear_fourth_activate";
    }

    @Override
    protected void tickActivation(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        if (ticks % 5 == 0) ServerFx.sound(level, p.position(), ModSounds.GOMU_INFLATE.get(), 0.8f, 0.7f + ticks * 0.015f);
        if (ticks % 3 == 0) {
            Vec3 c = p.position().add(0, 1.4, 0);
            level.sendParticles(ModParticles.STEAM.get(), c.x, c.y, c.z, 4, 0.4, 0.3, 0.4, 0.03);
        }
        if (ticks == activationTicks()) {
            Vec3 c = p.position();
            level.sendParticles(RingParticleOptions.ground(0x2A1040, 5f, 16), c.x, c.y + 0.1, c.z, 1, 0, 0, 0, 0);
            level.sendParticles(ModParticles.STEAM.get(), c.x, c.y + 1.2, c.z, 40, 0.8, 0.8, 0.8, 0.06);
            ServerFx.sound(level, c, ModSounds.GOMU_GEAR_FOURTH.get(), 1.6f, 1f);
            ServerFx.shake(level, c, 24, 0.9f, 16);
            ServerFx.camera(p, CameraEffectPacket.Type.FOV_PUNCH, 0.2f, 14);
            com.onepiece.ultimate.destruction.DestructionManager.cracks(level, c, 4, 5, p);
        }
    }

    @Override
    protected void tickActive(AbilityContext ctx, int ticks) {
        OPPlayerData data = ctx.data();
        if (!data.isToggleActive(HakiAbilities.ARMAMENT.id())) { // the coating gave out
            AbilityManager.endToggle(ctx.player(), data, this, true);
            return;
        }
        if (ctx.player().onGround()) data.abilities.airJumps = 0;
        if (ticks % 3 == 0) {
            ServerPlayer p = ctx.player();
            double yaw = Math.toRadians(p.yBodyRot);
            for (int side : new int[]{1, -1}) {
                double x = p.getX() - Math.cos(yaw) * 0.6 * side, z = p.getZ() - Math.sin(yaw) * 0.6 * side;
                ctx.level().sendParticles(ModParticles.STEAM.get(), x, p.getY() + p.getBbHeight() * 0.85, z, 1, 0.08, 0.05, 0.08, 0.02);
            }
        }
        if (ticks % 200 == 0) ctx.awardMastery(2);
    }

    @Override
    protected void deactivate(AbilityContext ctx, boolean interrupted) {
        ServerPlayer p = ctx.player();
        HakiManager.exhaust(p, ctx.data(), EXHAUSTION_TICKS);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EXHAUSTION_TICKS, 1, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EXHAUSTION_TICKS, 1, false, true, true));
        ServerFx.animation(p, "gear_fourth_deflate");
        GomuFx.steamBurst(ctx.level(), p, 40);
        ServerFx.sound(ctx.level(), p.position(), ModSounds.GOMU_INFLATE.get(), 1f, 0.5f);
    }

    @Override
    public boolean ignoresFall() {
        return true;
    }
}
