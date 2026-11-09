package com.onepiece.ultimate.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Real stagger: removes almost all movement, stops pathing every tick and (via {@code CombatEvents})
 * cancels any attack the stunned entity tries to make.
 */
public class StunnedEffect extends MobEffect {
    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2D64B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "6d8b1f0e-3c51-4b2a-9e47-0b5e7c2f9a11", -0.9, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED, "a1f3c2d4-5b6e-4f70-8a9b-1c2d3e4f5a6b", -0.9, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
