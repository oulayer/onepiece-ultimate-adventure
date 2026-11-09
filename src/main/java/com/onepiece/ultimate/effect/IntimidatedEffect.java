package com.onepiece.ultimate.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Overwhelmed by Conqueror's Haki: the creature drops its target and backs away from the nearest player.
 * Its damage is reduced in {@code CombatEvents}.
 */
public class IntimidatedEffect extends MobEffect {
    public IntimidatedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A0A0A);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, "2b7c9e51-4f8a-4d63-9a1e-7c5b3d2f1e09", -0.4, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Mob mob) || mob.level().isClientSide) return;
        mob.setTarget(null);
        if (mob.tickCount % 10 != 0) return;
        Player p = mob.level().getNearestPlayer(mob, 24);
        if (p == null) return;
        Vec3 away = mob.position().subtract(p.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-3) away = new Vec3(1, 0, 0);
        Vec3 dest = mob.position().add(away.normalize().scale(8));
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.3);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
