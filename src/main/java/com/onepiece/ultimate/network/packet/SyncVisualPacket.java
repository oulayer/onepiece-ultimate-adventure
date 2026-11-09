package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Render-relevant state of any player (transformations, block/charge poses) for everyone tracking them. */
public record SyncVisualPacket(int entityId, CompoundTag tag) {
    public static void encode(SyncVisualPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeNbt(p.tag);
    }

    public static SyncVisualPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        CompoundTag t = buf.readNbt();
        return new SyncVisualPacket(id, t == null ? new CompoundTag() : t);
    }

    public static void handle(SyncVisualPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleVisual(p));
        ctx.get().setPacketHandled(true);
    }
}
