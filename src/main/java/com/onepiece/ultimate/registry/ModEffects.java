package com.onepiece.ultimate.registry;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.effect.StunnedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, OnePieceMod.MOD_ID);

    /** Hit-stun / stagger: the victim cannot move or attack. Applied by combos, parries and heavy hits. */
    public static final RegistryObject<MobEffect> STUNNED = EFFECTS.register("stunned", StunnedEffect::new);
    /** Conqueror's Haki pressure: mobs lose their target, flee from players and hit for less. */
    public static final RegistryObject<MobEffect> INTIMIDATED = EFFECTS.register("intimidated", com.onepiece.ultimate.effect.IntimidatedEffect::new);

    private ModEffects() {}
}
