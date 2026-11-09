package com.onepiece.ultimate.client;

import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.haki.HakiAbilities;
import com.onepiece.ultimate.haki.HakiManager;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Observation Haki "sight": living things within range are outlined through walls for the local player only.
 * Uses the client-side copy of the glowing flag (never sent anywhere), re-applied each tick because server
 * metadata updates overwrite it, and restored when the entity leaves range or the Haki ends.
 */
public final class KenVision {
    private static final IntOpenHashSet MARKED = new IntOpenHashSet();

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            MARKED.clear();
            return;
        }
        OPPlayerData data = OPCapabilities.getOrNull(mc.player);
        boolean active = data != null && data.isToggleActive(HakiAbilities.OBSERVATION.id());
        IntOpenHashSet now = new IntOpenHashSet();
        if (active) {
            double r = HakiManager.observationRadius(data);
            double r2 = r * r;
            for (Entity e : level.entitiesForRendering()) {
                if (e == mc.player || !(e instanceof LivingEntity) || e.distanceToSqr(mc.player) > r2) continue;
                if (!MARKED.contains(e.getId()) && e.isCurrentlyGlowing()) continue; // already glowing for real
                e.setSharedFlag(6, true);
                now.add(e.getId());
            }
        }
        for (int id : MARKED) {
            if (!now.contains(id)) {
                Entity e = level.getEntity(id);
                if (e != null) e.setSharedFlag(6, false);
            }
        }
        MARKED.clear();
        MARKED.addAll(now);
    }

    public static void clear() {
        MARKED.clear();
    }

    private KenVision() {}
}
