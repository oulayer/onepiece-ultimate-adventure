package com.onepiece.ultimate.core;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.ability.TransformationAbility;
import com.onepiece.ultimate.data.OPCapabilities;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hitbox of transformed players (both sides – clients refresh when the visual state arrives). */
@Mod.EventBusSubscriber(modid = OnePieceMod.MOD_ID)
public final class TransformationEvents {
    public static final float GIANT_SCALE = 2.6f;

    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getPose() == Pose.SLEEPING || event.getPose() == Pose.SWIMMING || event.getPose() == Pose.FALL_FLYING) return;
        // capabilities may not be attached yet while the entity is being constructed
        var data = OPCapabilities.getOrNull(player);
        if (data == null) return;
        TransformationAbility form = TransformationAbility.active(data);
        if (form == null) return;
        EntityDimensions dims = form.dimensions();
        if (data.giant()) dims = (dims != null ? dims : EntityDimensions.scalable(0.6f, 1.8f)).scale(GIANT_SCALE);
        if (dims == null) return;
        if (event.getPose() == Pose.CROUCHING) dims = dims.scale(1f, 0.85f);
        event.setNewSize(dims, false);
        event.setNewEyeHeight(dims.height * 0.85f);
    }

    private TransformationEvents() {}
}
