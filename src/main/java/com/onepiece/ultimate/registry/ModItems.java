package com.onepiece.ultimate.registry;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.fruit.DevilFruitItem;
import com.onepiece.ultimate.fruit.DevilFruits;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OnePieceMod.MOD_ID);

    public static final RegistryObject<Item> GOMU_GOMU_NO_MI = ITEMS.register("gomu_gomu_no_mi",
            () -> new DevilFruitItem(DevilFruits.GOMU_GOMU_ID, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    private ModItems() {}
}
