package com.onepiece.ultimate.registry;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.entity.DebrisEntity;
import com.onepiece.ultimate.entity.StretchFistEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OnePieceMod.MOD_ID);

    public static final RegistryObject<EntityType<StretchFistEntity>> STRETCH_FIST = ENTITIES.register("stretch_fist",
            () -> EntityType.Builder.<StretchFistEntity>of(StretchFistEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f).clientTrackingRange(8).updateInterval(1).noSave()
                    .build(OnePieceMod.id("stretch_fist").toString()));

    public static final RegistryObject<EntityType<DebrisEntity>> DEBRIS = ENTITIES.register("debris",
            () -> EntityType.Builder.<DebrisEntity>of(DebrisEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(6).updateInterval(2).noSave()
                    .build(OnePieceMod.id("debris").toString()));

    private ModEntities() {}
}
