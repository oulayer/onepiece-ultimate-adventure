package com.onepiece.ultimate.data;

import com.onepiece.ultimate.network.OPNetwork;
import com.onepiece.ultimate.network.packet.SyncOwnerDataPacket;
import com.onepiece.ultimate.network.packet.SyncStatsPacket;
import com.onepiece.ultimate.network.packet.SyncVisualPacket;
import net.minecraft.server.level.ServerPlayer;

/** Sends only what changed, at most once per tick per category. */
public final class DataSync {
    public static void flush(ServerPlayer player, OPPlayerData data) {
        boolean full = data.consumeFullDirty();
        boolean visual = data.consumeVisualDirty();
        boolean stats = data.consumeStatsDirty();
        if (full) {
            OPNetwork.sendTo(player, new SyncOwnerDataPacket(data.saveForOwner()));
        }
        if (visual) {
            SyncVisualPacket packet = new SyncVisualPacket(player.getId(), data.saveVisual());
            if (full) OPNetwork.sendToTracking(player, packet);
            else OPNetwork.sendToTrackingAndSelf(player, packet);
        }
        if (stats || full) {
            OPNetwork.sendTo(player, new SyncStatsPacket(data.energy(), data.channelTicks(), data.comboHits()));
        }
    }

    public static void sendVisualTo(ServerPlayer target, ServerPlayer receiver) {
        OPCapabilities.get(target).ifPresent(d ->
                OPNetwork.sendTo(receiver, new SyncVisualPacket(target.getId(), d.saveVisual())));
    }

    private DataSync() {}
}
