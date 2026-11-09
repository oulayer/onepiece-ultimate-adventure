package com.onepiece.ultimate.haki;

import net.minecraft.network.chat.Component;

public enum HakiType {
    /** Busoshoku – armament. */
    ARMAMENT(0x7A4BFF),
    /** Kenbunshoku – observation. */
    OBSERVATION(0x4BD8FF),
    /** Haoshoku – conqueror's. */
    CONQUEROR(0xFF3A3A);

    public final int color;

    HakiType(int color) {
        this.color = color;
    }

    public Component displayName() {
        return Component.translatable("haki.onepiece." + name().toLowerCase());
    }
}
