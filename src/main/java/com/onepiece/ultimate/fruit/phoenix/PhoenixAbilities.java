package com.onepiece.ultimate.fruit.phoenix;

import com.onepiece.ultimate.ability.AbilityRegistry;

public final class PhoenixAbilities {
    public static final PhoenixTalonAbility TALON = AbilityRegistry.register(new PhoenixTalonAbility());
    public static final BlueFlameWaveAbility FLAME_WAVE = AbilityRegistry.register(new BlueFlameWaveAbility());
    public static final RegenerationFlamesAbility REGENERATION = AbilityRegistry.register(new RegenerationFlamesAbility());
    public static final PhoenixDiveAbility DIVE = AbilityRegistry.register(new PhoenixDiveAbility());
    public static final PhoenixHybridAbility HYBRID = AbilityRegistry.register(new PhoenixHybridAbility());
    public static final PhoenixFullAbility FULL = AbilityRegistry.register(new PhoenixFullAbility());

    static void bootstrap() {}

    private PhoenixAbilities() {}
}
