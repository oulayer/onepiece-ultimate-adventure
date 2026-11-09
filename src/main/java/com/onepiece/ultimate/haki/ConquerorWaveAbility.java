package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.AbilityContext;
import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.particle.RingParticleOptions;
import com.onepiece.ultimate.registry.ModEffects;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Haoshoku Haki – Conqueror's wave. Charge to gather willpower (black lightning crackles, the ground trembles),
 * release to unleash a pressure wave. Every creature in range is judged by willpower:
 * <ul>
 *   <li>far weaker → <b>faints</b> (long stun, loses its target);</li>
 *   <li>weaker → <b>intimidated</b> (drops its target, flees, deals 40% less damage);</li>
 *   <li>comparable or stronger → only pushed back.</li>
 * </ul>
 * Players are judged by level and Haki. The pressure cracks the ground and shatters glass and foliage.
 */
public final class ConquerorWaveAbility extends HakiAbility {
    public enum Outcome { FAINTED, INTIMIDATED, RESISTED }

    ConquerorWaveAbility() {
        super("haki_conqueror_wave", HakiType.CONQUEROR, 1, Properties.of(Activation.CHARGE).cost(40).cooldown(500)
                .charge(5, 30).channelAnimation("conqueror_charge"));
    }

    @Override
    public void onActivate(AbilityContext ctx) {
        ServerFx.sound(ctx.level(), ctx.player().position(), ModSounds.HAKI_CONQUEROR_CHARGE.get(), 1.2f, 1f);
    }

    @Override
    public void onChargeTick(AbilityContext ctx, int ticks) {
        ServerPlayer p = ctx.player();
        if (ticks % 3 == 0) {
            Vec3 c = p.position().add(0, 1.0, 0);
            HakiManager.bolt(ctx.level(), c, HakiManager.randomDir(p).scale(1.5 + ticks * 0.08));
        }
        if (ticks % 8 == 0) ServerFx.shake(ctx.level(), p.position(), 12 + ticks * 0.4, 0.15f + ticks * 0.01f, 8);
    }

    public static float willpower(ServerPlayer player, OPPlayerData data, float charge) {
        return (data.progression().level() + 6f * data.hakiLevel(HakiType.CONQUEROR) + 0.5f * data.progression().get(PlayerStat.HAKI))
                * (0.8f + 0.4f * charge);
    }

    public static float willpower(LivingEntity e) {
        if (e instanceof Player p) {
            OPPlayerData d = OPCapabilities.getOrNull(p);
            if (d == null) return 10f;
            return d.progression().level() + 6f * d.hakiLevel(HakiType.CONQUEROR) + 2f * d.hakiLevel(HakiType.ARMAMENT);
        }
        float w = e.getMaxHealth() * 0.7f + (float) e.getAttributeValue(Attributes.ARMOR) * 2f;
        if (e.getType().is(Tags.EntityTypes.BOSSES)) w *= 3f;
        return w;
    }

    /** Judges one target and applies the effect. Public for tests. */
    public static Outcome judge(ServerPlayer user, float will, LivingEntity t, Vec3 center) {
        float ratio = will / Math.max(1f, willpower(t));
        boolean isPlayer = t instanceof Player;
        Vec3 out = t.position().subtract(center).multiply(1, 0, 1);
        out = out.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : out.normalize();
        if (ratio >= 1.6f) {
            t.addEffect(new MobEffectInstance(ModEffects.STUNNED.get(), isPlayer ? 60 : 200, 0, false, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 1, false, false, true));
            if (t instanceof Mob m) m.setTarget(null);
            if (isPlayer) t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
            ((ServerLevel) t.level()).sendParticles(ParticleTypes.SMOKE, t.getX(), t.getY() + t.getBbHeight(), t.getZ(), 8, 0.2, 0.1, 0.2, 0.01);
            return Outcome.FAINTED;
        }
        if (ratio >= 1.0f) {
            t.addEffect(new MobEffectInstance(ModEffects.INTIMIDATED.get(), 160, 0, false, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, true));
            t.push(out.x * 0.6, 0.2, out.z * 0.6);
            t.hurtMarked = true;
            return Outcome.INTIMIDATED;
        }
        t.push(out.x * 0.25, 0.05, out.z * 0.25);
        t.hurtMarked = true;
        return Outcome.RESISTED;
    }

    @Override
    public void onRelease(AbilityContext ctx, int chargeTicks) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        OPPlayerData data = ctx.data();
        float charge = chargeTicks / (float) maxCharge();
        int lvl = data.hakiLevel(HakiType.CONQUEROR);
        float radius = (8f + 1.6f * lvl) * (0.6f + 0.4f * charge);
        float will = willpower(player, data, charge);
        Vec3 c = player.position();

        int affected = 0;
        for (Entity e : level.getEntities(player, new AABB(c, c).inflate(radius), x -> x instanceof LivingEntity)) {
            LivingEntity t = (LivingEntity) e;
            if (t.distanceToSqr(c) > radius * radius || !HitDetector.isValidTarget(player, t)) continue;
            if (t instanceof Player && !OPConfig.HAOSHOKU_AFFECTS_PLAYERS.get()) continue;
            Outcome o = judge(player, will, t, c);
            if (o != Outcome.RESISTED) affected += o == Outcome.FAINTED ? 3 : 2;
        }

        // ---- presentation
        ServerFx.animation(player, "conqueror_release");
        ServerFx.sound(level, c, ModSounds.HAKI_CONQUEROR_WAVE.get(), 2f, 0.9f);
        ServerFx.sound(level, c, ModSounds.HAKI_LIGHTNING.get(), 1.5f, 0.8f);
        level.sendParticles(RingParticleOptions.ground(0x120000, radius, 22), c.x, c.y + 0.15, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(RingParticleOptions.ground(0xC01818, radius * 0.75f, 18), c.x, c.y + 0.2, c.z, 1, 0, 0, 0, 0);
        int bolts = 8 + lvl;
        for (int i = 0; i < bolts; i++) {
            double a = Math.PI * 2 * i / bolts + player.getRandom().nextFloat() * 0.3;
            double len = radius * (0.3 + player.getRandom().nextFloat() * 0.45);
            Vec3 dir = new Vec3(Math.cos(a) * len, (player.getRandom().nextFloat() - 0.2) * 3, Math.sin(a) * len);
            HakiManager.bolt(level, c.add(0, 1.0, 0), dir);
        }
        ServerFx.shake(level, c, radius * 3, 0.9f + 0.5f * charge, 22);
        HakiManager.conquerorPressure(level, c, radius * 1.6, 0.9f);

        // ---- the pressure acts on the environment
        DestructionManager.cracks(level, c, 4 + lvl / 2, (int) (radius * 0.7f), player);
        DestructionManager.crater(level, c.add(0, 1, 0), radius * 0.5f, 1f, 0.2f, player); // power 0.2: only glass, leaves, plants

        train(ctx, Math.max(2, affected));
    }
}
