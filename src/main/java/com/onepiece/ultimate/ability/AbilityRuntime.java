package com.onepiece.ultimate.ability;

import com.onepiece.ultimate.core.DelayedTasks;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Server-only execution state of a player's abilities. */
public final class AbilityRuntime {
    /** Ability currently being charged or held (only one at a time). */
    @Nullable public Ability channeling;
    /** The slot's base ability – cooldown is applied to its key. */
    @Nullable public Ability channelingBase;
    public int channelSlot = -1;
    public int channelBar = -1;
    public int channelTicks;

    /** Ticks each active toggle has been running. */
    public final Map<ResourceLocation, Integer> toggleTicks = new HashMap<>();

    public final DelayedTasks tasks = new DelayedTasks();

    public void clearChannel() {
        channeling = null;
        channelingBase = null;
        channelSlot = -1;
        channelBar = -1;
        channelTicks = 0;
    }

    public void reset() {
        clearChannel();
        toggleTicks.clear();
        tasks.clear();
    }
}
