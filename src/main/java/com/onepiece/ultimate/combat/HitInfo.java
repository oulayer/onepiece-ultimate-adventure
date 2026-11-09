package com.onepiece.ultimate.combat;

import com.onepiece.ultimate.registry.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Description of one hit. Built fluently by moves and abilities, executed by {@link CombatHits#apply}.
 * While the hit is being applied it is exposed through {@link CombatHits#current()} so damage events can
 * honour flags such as {@link #guardBreak}.
 */
public final class HitInfo {
    public final LivingEntity attacker;
    public float damage;
    public ResourceKey<DamageType> type = ModDamageTypes.MARTIAL;
    public Vec3 direction = Vec3.ZERO;
    public float knockback;
    public float lift;
    public int stun;
    public boolean launch;
    public boolean guardBreak;
    /** Wall-slam power used by {@link LaunchTracker} when {@link #launch} is set. */
    public float launchPower = 1f;
    public boolean countsForCombo = true;

    public HitInfo(LivingEntity attacker, float damage) {
        this.attacker = attacker;
        this.damage = damage;
        this.direction = attacker.getLookAngle();
    }

    public HitInfo type(ResourceKey<DamageType> t) { this.type = t; return this; }
    public HitInfo direction(Vec3 d) { this.direction = d.lengthSqr() < 1e-6 ? Vec3.ZERO : d.normalize(); return this; }
    public HitInfo knockback(float k) { this.knockback = k; return this; }
    public HitInfo lift(float l) { this.lift = l; return this; }
    public HitInfo stun(int ticks) { this.stun = ticks; return this; }
    public HitInfo launch(float power) { this.launch = true; this.launchPower = power; return this; }
    public HitInfo guardBreak() { this.guardBreak = true; return this; }
    public HitInfo noCombo() { this.countsForCombo = false; return this; }
}
