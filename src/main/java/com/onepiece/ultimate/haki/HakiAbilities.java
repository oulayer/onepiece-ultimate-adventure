package com.onepiece.ultimate.haki;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.ability.AbilityRegistry;

import java.util.List;

/** The Haki bar (same for every player). Slot order matches the Haki key bindings. */
public final class HakiAbilities {
    public static final ArmamentAbility ARMAMENT = AbilityRegistry.register(new ArmamentAbility());
    public static final ObservationAbility OBSERVATION = AbilityRegistry.register(new ObservationAbility());
    public static final EmissionAbility EMISSION = AbilityRegistry.register(new EmissionAbility());
    public static final FutureSightAbility FUTURE_SIGHT = AbilityRegistry.register(new FutureSightAbility());
    public static final ConquerorWaveAbility CONQUEROR_WAVE = AbilityRegistry.register(new ConquerorWaveAbility());
    public static final ConquerorInfusionAbility CONQUEROR_INFUSION = AbilityRegistry.register(new ConquerorInfusionAbility());

    private static final List<Ability> ALL = List.of(ARMAMENT, OBSERVATION, EMISSION, FUTURE_SIGHT, CONQUEROR_WAVE, CONQUEROR_INFUSION);

    public static List<Ability> all() {
        return ALL;
    }

    public static void bootstrap() {}

    private HakiAbilities() {}
}
