package com.onepiece.ultimate.fruit.gomu;

import com.onepiece.ultimate.ability.AbilityRegistry;

/**
 * Ability singletons of the Gomu Gomu no Mi. Base abilities occupy slots; transformations (Gears) replace them with
 * their own versions while active.
 */
public final class GomuAbilities {
    // ---- base moveset
    public static final PistolAbility PISTOL = AbilityRegistry.register(new PistolAbility(false));
    public static final WhipAbility WHIP = AbilityRegistry.register(new WhipAbility());
    public static final GatlingAbility GATLING = AbilityRegistry.register(new GatlingAbility(false));
    public static final RocketAbility ROCKET = AbilityRegistry.register(new RocketAbility());
    public static final BazookaAbility BAZOOKA = AbilityRegistry.register(new BazookaAbility(false));
    public static final GigantPistolAbility GIGANT_PISTOL = AbilityRegistry.register(new GigantPistolAbility());

    // ---- Gear Second
    public static final PistolAbility JET_PISTOL = AbilityRegistry.register(new PistolAbility(true));
    public static final GatlingAbility JET_GATLING = AbilityRegistry.register(new GatlingAbility(true));
    public static final BazookaAbility JET_BAZOOKA = AbilityRegistry.register(new BazookaAbility(true));
    public static final GearSecondAbility GEAR_SECOND = AbilityRegistry.register(new GearSecondAbility());

    // ---- Gear Fourth: Boundman
    public static final KongGunAbility KONG_GUN = AbilityRegistry.register(new KongGunAbility());
    public static final KongOrganAbility KONG_ORGAN = AbilityRegistry.register(new KongOrganAbility());
    public static final LeoBazookaAbility LEO_BAZOOKA = AbilityRegistry.register(new LeoBazookaAbility());
    public static final KingKongGunAbility KING_KONG_GUN = AbilityRegistry.register(new KingKongGunAbility());
    public static final RhinoSchneiderAbility RHINO_SCHNEIDER = AbilityRegistry.register(new RhinoSchneiderAbility());
    public static final CulverinAbility CULVERIN = AbilityRegistry.register(new CulverinAbility());
    public static final BoundmanAbility BOUNDMAN = AbilityRegistry.register(new BoundmanAbility());

    // ---- Gear Fourth: Snakeman
    public static final JetCulverinAbility JET_CULVERIN = AbilityRegistry.register(new JetCulverinAbility());
    public static final BlackMambaAbility BLACK_MAMBA = AbilityRegistry.register(new BlackMambaAbility());
    public static final KingCobraAbility KING_COBRA = AbilityRegistry.register(new KingCobraAbility());
    public static final HydraAbility HYDRA = AbilityRegistry.register(new HydraAbility());
    public static final SnakemanAbility SNAKEMAN = AbilityRegistry.register(new SnakemanAbility());

    // ---- Gear Fifth
    public static final MoguraPistolAbility MOGURA_PISTOL = AbilityRegistry.register(new MoguraPistolAbility());
    public static final GigantAbility GIGANT = AbilityRegistry.register(new GigantAbility());
    public static final DawnGatlingAbility DAWN_GATLING = AbilityRegistry.register(new DawnGatlingAbility());
    public static final KaminariAbility KAMINARI = AbilityRegistry.register(new KaminariAbility());
    public static final DawnRocketAbility DAWN_ROCKET = AbilityRegistry.register(new DawnRocketAbility());
    public static final BajrangGunAbility BAJRANG_GUN = AbilityRegistry.register(new BajrangGunAbility());
    public static final GearFifthAbility GEAR_FIFTH = AbilityRegistry.register(new GearFifthAbility());

    static {
        GEAR_SECOND.variant(PISTOL, JET_PISTOL).variant(GATLING, JET_GATLING).variant(BAZOOKA, JET_BAZOOKA);
        BOUNDMAN.variant(PISTOL, KONG_GUN).variant(WHIP, CULVERIN).variant(GATLING, KONG_ORGAN).variant(ROCKET, RHINO_SCHNEIDER)
                .variant(BAZOOKA, LEO_BAZOOKA).variant(GIGANT_PISTOL, KING_KONG_GUN);
        SNAKEMAN.variant(PISTOL, JET_CULVERIN).variant(WHIP, HYDRA).variant(GATLING, BLACK_MAMBA).variant(BAZOOKA, KING_COBRA);
        GEAR_FIFTH.variant(PISTOL, MOGURA_PISTOL).variant(WHIP, GIGANT).variant(GATLING, DAWN_GATLING).variant(ROCKET, DAWN_ROCKET)
                .variant(BAZOOKA, KAMINARI).variant(GIGANT_PISTOL, BAJRANG_GUN);
    }

    /** Forces class initialisation (registration) at a controlled moment. */
    static void bootstrap() {}

    private GomuAbilities() {}
}
