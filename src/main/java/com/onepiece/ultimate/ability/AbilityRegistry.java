package com.onepiece.ultimate.ability;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Static registry of ability singletons. Populated during mod construction, then frozen. */
public final class AbilityRegistry {
    private static final Map<ResourceLocation, Ability> ABILITIES = new LinkedHashMap<>();
    private static boolean frozen;

    public static <T extends Ability> T register(T ability) {
        if (frozen) throw new IllegalStateException("Ability registry is frozen: " + ability.id());
        if (ABILITIES.putIfAbsent(ability.id(), ability) != null)
            throw new IllegalStateException("Duplicate ability id " + ability.id());
        return ability;
    }

    @Nullable
    public static Ability get(@Nullable ResourceLocation id) {
        return id == null ? null : ABILITIES.get(id);
    }

    public static Collection<Ability> all() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    public static void freeze() {
        frozen = true;
    }

    private AbilityRegistry() {}
}
