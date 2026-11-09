package com.onepiece.ultimate.fruit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum FruitType {
    PARAMECIA(ChatFormatting.LIGHT_PURPLE),
    ZOAN(ChatFormatting.GREEN),
    LOGIA(ChatFormatting.AQUA);

    public final ChatFormatting color;

    FruitType(ChatFormatting color) {
        this.color = color;
    }

    public Component displayName() {
        return Component.translatable("fruit_type.onepiece." + name().toLowerCase()).withStyle(color);
    }
}
