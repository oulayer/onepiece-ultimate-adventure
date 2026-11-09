package com.onepiece.ultimate.network;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.network.packet.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class OPNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            OnePieceMod.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;

    public static void register() {
        // client -> server
        CHANNEL.messageBuilder(AbilityInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AbilityInputPacket::encode).decoder(AbilityInputPacket::decode)
                .consumerMainThread(AbilityInputPacket::handle).add();
        CHANNEL.messageBuilder(CombatInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CombatInputPacket::encode).decoder(CombatInputPacket::decode)
                .consumerMainThread(CombatInputPacket::handle).add();
        CHANNEL.messageBuilder(SpendStatPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpendStatPacket::encode).decoder(SpendStatPacket::decode)
                .consumerMainThread(SpendStatPacket::handle).add();

        // server -> client
        CHANNEL.messageBuilder(SyncOwnerDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncOwnerDataPacket::encode).decoder(SyncOwnerDataPacket::decode)
                .consumerMainThread(SyncOwnerDataPacket::handle).add();
        CHANNEL.messageBuilder(SyncVisualPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncVisualPacket::encode).decoder(SyncVisualPacket::decode)
                .consumerMainThread(SyncVisualPacket::handle).add();
        CHANNEL.messageBuilder(SyncStatsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStatsPacket::encode).decoder(SyncStatsPacket::decode)
                .consumerMainThread(SyncStatsPacket::handle).add();
        CHANNEL.messageBuilder(PlayAnimationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayAnimationPacket::encode).decoder(PlayAnimationPacket::decode)
                .consumerMainThread(PlayAnimationPacket::handle).add();
        CHANNEL.messageBuilder(ThreatsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ThreatsPacket::encode).decoder(ThreatsPacket::decode)
                .consumerMainThread(ThreatsPacket::handle).add();
        CHANNEL.messageBuilder(CameraEffectPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CameraEffectPacket::encode).decoder(CameraEffectPacket::decode)
                .consumerMainThread(CameraEffectPacket::handle).add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        if (player instanceof FakePlayer || player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /** Everyone tracking {@code entity} plus the entity itself if it is a player. */
    public static void sendToTrackingAndSelf(Entity entity, Object msg) {
        if (entity instanceof FakePlayer || entity.level().isClientSide) return;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    public static void sendToTracking(Entity entity, Object msg) {
        if (entity instanceof FakePlayer || entity.level().isClientSide) return;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    private OPNetwork() {}
}
