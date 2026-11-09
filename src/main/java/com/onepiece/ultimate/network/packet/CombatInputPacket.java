package com.onepiece.ultimate.network.packet;

import com.onepiece.ultimate.combat.CombatAction;
import com.onepiece.ultimate.combat.CombatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Combat input with the client's movement intent (used for dash direction). */
public record CombatInputPacket(CombatAction action, float forward, float strafe) {
    public static void encode(CombatInputPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.action);
        buf.writeFloat(p.forward);
        buf.writeFloat(p.strafe);
    }

    public static CombatInputPacket decode(FriendlyByteBuf buf) {
        return new CombatInputPacket(buf.readEnum(CombatAction.class),
                Mth.clamp(buf.readFloat(), -1f, 1f), Mth.clamp(buf.readFloat(), -1f, 1f));
    }

    public static void handle(CombatInputPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) CombatManager.handleInput(player, p.action, p.forward, p.strafe);
        ctx.get().setPacketHandled(true);
    }
}
