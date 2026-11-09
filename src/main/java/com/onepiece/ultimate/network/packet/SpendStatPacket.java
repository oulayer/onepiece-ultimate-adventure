package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.progression.ProgressionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SpendStatPacket(PlayerStat stat) {
    public static void encode(SpendStatPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.stat);
    }

    public static SpendStatPacket decode(FriendlyByteBuf buf) {
        return new SpendStatPacket(buf.readEnum(PlayerStat.class));
    }

    public static void handle(SpendStatPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) ProgressionManager.spendPoint(player, p.stat);
        ctx.get().setPacketHandled(true);
    }
}
