package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CameraEffectPacket(Type type, float intensity, int duration) {
    public enum Type {
        /** Positional screen shake. */
        SHAKE,
        /** Freezes the local player's animations briefly so heavy hits "connect". */
        HITSTOP,
        /** Quick field of view punch outwards (dash, launches). */
        FOV_PUNCH,
        /** Short white impact flash. */
        FLASH,
        /** Conqueror's Haki pressure: dark red vignette pulse. */
        DARK_PULSE
    }

    public static void encode(CameraEffectPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.type);
        buf.writeFloat(p.intensity);
        buf.writeVarInt(p.duration);
    }

    public static CameraEffectPacket decode(FriendlyByteBuf buf) {
        return new CameraEffectPacket(buf.readEnum(Type.class), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(CameraEffectPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCamera(p));
        ctx.get().setPacketHandled(true);
    }
}
