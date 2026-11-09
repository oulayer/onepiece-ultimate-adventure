package com.onepiece.ultimate.fruit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Rarity drives world spawn weights (loot) and the item name colour. */
public enum FruitRarity {
    RARE(ChatFormatting.BLUE, 30),
    EPIC(ChatFormatting.DARK_PURPLE, 12),
    LEGENDARY(ChatFormatting.GOLD, 4),
    MYTHICAL(ChatFormatting.RED, 1);

    public final ChatFormatting color;
    public final int weight;

    FruitRarity(ChatFormatting color, int weight) {
        this.color = color;
        this.weight = weight;
    }

    public Component displayName() {
        return Component.translatable("rarity.onepiece." + name().toLowerCase()).withStyle(color);
    }
}
