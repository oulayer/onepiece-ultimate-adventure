package com.onepiece.ultimate;

import com.mojang.logging.LogUtils;
import com.onepiece.ultimate.ability.AbilityRegistry;
import com.onepiece.ultimate.config.OPClientConfig;
import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.fruit.DevilFruits;
import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.registry.ModCreativeTab;
import com.onepiece.ultimate.registry.ModEffects;
import com.onepiece.ultimate.registry.ModEntities;
import com.onepiece.ultimate.registry.ModItems;
import com.onepiece.ultimate.registry.ModParticles;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point of One Piece: Ultimate Adventure.
 * <p>
 * Each system lives in its own package and owns its own registration; this class only wires them together:
 * <ul>
 *   <li>{@code registry}    – vanilla registry objects (items, entities, sounds, particles, effects)</li>
 *   <li>{@code data}        – persistent per-player state (capability) and client sync</li>
 *   <li>{@code ability}     – generic ability framework (activation types, cooldowns, energy)</li>
 *   <li>{@code fruit}       – Devil Fruit definitions; each fruit in its own sub package</li>
 *   <li>{@code combat}      – melee combos, dash/dodge, block/parry, hit detection, stun &amp; launches</li>
 *   <li>{@code destruction} – budgeted, protected, optionally self-restoring terrain destruction</li>
 *   <li>{@code progression} – levels, attributes and mastery</li>
 *   <li>{@code client}      – input, HUD, keyframe animation engine, renderers, camera effects</li>
 * </ul>
 */
@Mod(OnePieceMod.MOD_ID)
public final class OnePieceMod {
    public static final String MOD_ID = "onepiece";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OnePieceMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModCreativeTab.TABS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, OPConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OPClientConfig.SPEC);

        // Content definitions are plain Java registries (immutable after construction).
        DevilFruits.bootstrap();
        com.onepiece.ultimate.haki.HakiAbilities.bootstrap();
        AbilityRegistry.freeze();

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(OPNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
