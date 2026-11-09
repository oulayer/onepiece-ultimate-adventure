package com.onepiece.ultimate.registry;

import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Data-driven damage types (see data/onepiece/damage_type). Distinct types let passives react correctly:
 * rubber shrugs off {@link #MARTIAL} blunt force, but not {@link #SLASH}.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> MARTIAL = key("martial");
    public static final ResourceKey<DamageType> SLASH = key("slash");
    public static final ResourceKey<DamageType> FRUIT = key("fruit");
    public static final ResourceKey<DamageType> IMPACT = key("impact");
    /** Armament emission: force released inside the target. Bypasses armour, shields and blocking. */
    public static final ResourceKey<DamageType> INTERNAL = key("internal");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, OnePieceMod.id(name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    private ModDamageTypes() {}
}
