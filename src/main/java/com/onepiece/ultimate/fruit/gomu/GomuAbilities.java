package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityRegistry;

/** Ability singletons of the Gomu Gomu no Mi. Base abilities occupy slots; Jet variants replace them in Gear Second. */
public final class GomuAbilities {
    public static final GearSecondAbility GEAR_SECOND = AbilityRegistry.register(new GearSecondAbility());

    public static final PistolAbility JET_PISTOL = AbilityRegistry.register(new PistolAbility(true, null));
    public static final PistolAbility PISTOL = AbilityRegistry.register(new PistolAbility(false, JET_PISTOL));

    public static final WhipAbility WHIP = AbilityRegistry.register(new WhipAbility());

    public static final GatlingAbility JET_GATLING = AbilityRegistry.register(new GatlingAbility(true, null));
    public static final GatlingAbility GATLING = AbilityRegistry.register(new GatlingAbility(false, JET_GATLING));

    public static final RocketAbility ROCKET = AbilityRegistry.register(new RocketAbility());

    public static final BazookaAbility JET_BAZOOKA = AbilityRegistry.register(new BazookaAbility(true, null));
    public static final BazookaAbility BAZOOKA = AbilityRegistry.register(new BazookaAbility(false, JET_BAZOOKA));

    public static final GigantPistolAbility GIGANT_PISTOL = AbilityRegistry.register(new GigantPistolAbility());

    /** Forces class initialisation (registration) at a controlled moment. */
    static void bootstrap() {}

    private GomuAbilities() {}
}
