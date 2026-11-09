package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Full data of the local player (fruit, mastery, progression, cooldowns, runtime state). */
public record SyncOwnerDataPacket(CompoundTag tag) {
    public static void encode(SyncOwnerDataPacket p, FriendlyByteBuf buf) {
        buf.writeNbt(p.tag);
    }

    public static SyncOwnerDataPacket decode(FriendlyByteBuf buf) {
        CompoundTag t = buf.readNbt();
        return new SyncOwnerDataPacket(t == null ? new CompoundTag() : t);
    }

    public static void handle(SyncOwnerDataPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOwnerData(p));
        ctx.get().setPacketHandled(true);
    }
}
