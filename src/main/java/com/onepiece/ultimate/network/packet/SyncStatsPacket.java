package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Small high-frequency packet for values that change almost every tick. */
public record SyncStatsPacket(float energy, int channelTicks, int comboHits) {
    public static void encode(SyncStatsPacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.energy);
        buf.writeVarInt(p.channelTicks);
        buf.writeVarInt(p.comboHits);
    }

    public static SyncStatsPacket decode(FriendlyByteBuf buf) {
        return new SyncStatsPacket(buf.readFloat(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SyncStatsPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStats(p));
        ctx.get().setPacketHandled(true);
    }
}
