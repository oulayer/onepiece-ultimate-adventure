package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Observation Haki: ids of entities with hostile intent toward the receiving player. */
public record ThreatsPacket(int[] ids) {
    public static void encode(ThreatsPacket p, FriendlyByteBuf buf) {
        buf.writeVarIntArray(p.ids);
    }

    public static ThreatsPacket decode(FriendlyByteBuf buf) {
        return new ThreatsPacket(buf.readVarIntArray(256));
    }

    public static void handle(ThreatsPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleThreats(p));
        ctx.get().setPacketHandled(true);
    }
}
