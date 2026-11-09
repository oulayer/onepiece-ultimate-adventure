package com.onepiece.ultimate.client;

import com.onepiece.ultimate.client.animation.PlayerAnimations;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Client-only packet handling; referenced through DistExecutor so dedicated servers never load it. */
public final class ClientPacketHandler {

    public static void handleOwnerData(SyncOwnerDataPacket p) {
        Player player = Minecraft.getInstance().player;
        OPCapabilities.get(player).ifPresent(d -> d.loadForOwner(p.tag()));
    }

    public static void handleVisual(SyncVisualPacket p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(p.entityId());
        if (e instanceof Player player) OPCapabilities.get(player).ifPresent(d -> d.loadVisual(p.tag()));
    }

    public static void handleStats(SyncStatsPacket p) {
        Player player = Minecraft.getInstance().player;
        OPCapabilities.get(player).ifPresent(d -> {
            d.setEnergy(p.energy());
            d.setChanneling(d.channeling(), p.channelTicks());
            int before = d.comboHits();
            d.setComboHits(p.comboHits());
            if (p.comboHits() > before) ClientState.onComboHit(p.comboHits());
        });
    }

    public static void handleAnimation(PlayAnimationPacket p) {
        PlayerAnimations.play(p.entityId(), p.animation(), p.speed());
    }

    public static void handleThreats(ThreatsPacket p) {
        ClientState.threats = p.ids();
    }

    public static void handleCamera(CameraEffectPacket p) {
        CameraEffects.handle(p.type(), p.intensity(), p.duration());
    }

    private ClientPacketHandler() {}
}
