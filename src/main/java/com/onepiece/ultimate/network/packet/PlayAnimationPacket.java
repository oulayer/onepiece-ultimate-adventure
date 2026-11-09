package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Plays a one-shot keyframe animation on an entity for every client that sees it. */
public record PlayAnimationPacket(int entityId, ResourceLocation animation, float speed) {
    public static void encode(PlayAnimationPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeResourceLocation(p.animation);
        buf.writeFloat(p.speed);
    }

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readVarInt(), buf.readResourceLocation(), buf.readFloat());
    }

    public static void handle(PlayAnimationPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleAnimation(p));
        ctx.get().setPacketHandled(true);
    }
}
