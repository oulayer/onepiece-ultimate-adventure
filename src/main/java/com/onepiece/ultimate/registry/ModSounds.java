package com.onepiece.ultimate.registry;

import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Every ability/combat action has its own sound event so resource packs can replace them individually. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OnePieceMod.MOD_ID);

    public static final RegistryObject<SoundEvent> SWING_LIGHT = reg("combat.swing_light");
    public static final RegistryObject<SoundEvent> SWING_HEAVY = reg("combat.swing_heavy");
    public static final RegistryObject<SoundEvent> HIT_LIGHT = reg("combat.hit_light");
    public static final RegistryObject<SoundEvent> HIT_HEAVY = reg("combat.hit_heavy");
    public static final RegistryObject<SoundEvent> BLOCK = reg("combat.block");
    public static final RegistryObject<SoundEvent> PARRY = reg("combat.parry");
    public static final RegistryObject<SoundEvent> DASH = reg("combat.dash");
    public static final RegistryObject<SoundEvent> SLAM = reg("combat.slam");
    public static final RegistryObject<SoundEvent> CHARGE = reg("combat.charge");

    public static final RegistryObject<SoundEvent> GOMU_STRETCH = reg("gomu.stretch");
    public static final RegistryObject<SoundEvent> GOMU_SNAP = reg("gomu.snap");
    public static final RegistryObject<SoundEvent> GOMU_INFLATE = reg("gomu.inflate");
    public static final RegistryObject<SoundEvent> GOMU_GEAR_SECOND = reg("gomu.gear_second");
    public static final RegistryObject<SoundEvent> GOMU_BOUNCE = reg("gomu.bounce");
    public static final RegistryObject<SoundEvent> GOMU_GEAR_FOURTH = reg("gomu.gear_fourth");
    public static final RegistryObject<SoundEvent> GOMU_GEAR_FIFTH = reg("gomu.gear_fifth");
    public static final RegistryObject<SoundEvent> PHOENIX_FLAME = reg("phoenix.flame");
    public static final RegistryObject<SoundEvent> PHOENIX_CRY = reg("phoenix.cry");
    public static final RegistryObject<SoundEvent> PHOENIX_WING = reg("phoenix.wing");

    public static final RegistryObject<SoundEvent> RUMBLE = reg("destruction.rumble");

    public static final RegistryObject<SoundEvent> HAKI_AWAKEN = reg("haki.awaken");
    public static final RegistryObject<SoundEvent> HAKI_ARMAMENT = reg("haki.armament");
    public static final RegistryObject<SoundEvent> HAKI_OBSERVATION = reg("haki.observation");
    public static final RegistryObject<SoundEvent> HAKI_DODGE = reg("haki.dodge");
    public static final RegistryObject<SoundEvent> HAKI_EMISSION = reg("haki.emission");
    public static final RegistryObject<SoundEvent> HAKI_CONQUEROR_CHARGE = reg("haki.conqueror_charge");
    public static final RegistryObject<SoundEvent> HAKI_CONQUEROR_WAVE = reg("haki.conqueror_wave");
    public static final RegistryObject<SoundEvent> HAKI_LIGHTNING = reg("haki.lightning");
    public static final RegistryObject<SoundEvent> EAT_FRUIT = reg("fruit.eat");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(OnePieceMod.id(name)));
    }

    private ModSounds() {}
}
