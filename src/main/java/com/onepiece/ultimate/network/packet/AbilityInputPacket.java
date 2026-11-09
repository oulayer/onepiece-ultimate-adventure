package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.ability.AbilityManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Ability key pressed or released by the client. The server validates everything. */
public record AbilityInputPacket(int bar, int slot, boolean pressed) {
    public AbilityInputPacket(int slot, boolean pressed) {
        this(com.onepiece.ultimate.ability.AbilityBars.FRUIT, slot, pressed);
    }

    public static void encode(AbilityInputPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.bar);
        buf.writeByte(p.slot);
        buf.writeBoolean(p.pressed);
    }

    public static AbilityInputPacket decode(FriendlyByteBuf buf) {
        return new AbilityInputPacket(buf.readByte(), buf.readByte(), buf.readBoolean());
    }

    public static void handle(AbilityInputPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) AbilityManager.handleInput(player, p.bar, p.slot, p.pressed);
        ctx.get().setPacketHandled(true);
    }
}
