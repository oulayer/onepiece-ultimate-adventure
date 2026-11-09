package com.onepiece.ultimate.client.transform;

import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.fruit.gomu.GomuAbilities;
import com.onepiece.ultimate.fruit.phoenix.PhoenixAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Client registry: transformation id → its look. */
public final class TransformationVisuals {
    private static final Map<ResourceLocation, TransformationVisual> VISUALS = new HashMap<>();

    static {
        VISUALS.put(GomuAbilities.GEAR_SECOND.id(), new GearSecondVisual());
        VISUALS.put(GomuAbilities.BOUNDMAN.id(), new BoundmanVisual());
        VISUALS.put(GomuAbilities.SNAKEMAN.id(), new SnakemanVisual());
        VISUALS.put(GomuAbilities.GEAR_FIFTH.id(), new GearFifthVisual());
        VISUALS.put(PhoenixAbilities.HYBRID.id(), new PhoenixHybridVisual());
        VISUALS.put(PhoenixAbilities.FULL.id(), new PhoenixFullVisual());
    }

    @Nullable
    public static TransformationVisual of(@Nullable Player player) {
        OPPlayerData data = OPCapabilities.getOrNull(player);
        if (data == null) return null;
        TransformationAbility t = TransformationAbility.active(data);
        return t == null ? null : VISUALS.get(t.id());
    }

    public static void tick() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (Player p : level.players()) {
            TransformationVisual v = of(p);
            if (v != null && p instanceof AbstractClientPlayer acp) v.clientTick(acp);
        }
    }

    private TransformationVisuals() {}
}
