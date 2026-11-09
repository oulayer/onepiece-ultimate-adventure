package com.onepiece.ultimate.registry;

import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OnePieceMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.onepiece"))
            .icon(() -> new ItemStack(ModItems.GOMU_GOMU_NO_MI.get()))
            .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(e -> output.accept(e.get())))
            .build());

    private ModCreativeTab() {}
}
