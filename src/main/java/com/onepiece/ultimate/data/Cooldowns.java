package com.onepiece.ultimate.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Remaining cooldown ticks keyed by ability id. Ticked on both sides: the server is authoritative and
 * re-syncs whenever a cooldown starts; the client counts down locally so the HUD stays smooth.
 */
public final class Cooldowns {
    private final Map<ResourceLocation, Integer> remaining = new HashMap<>();
    private final Map<ResourceLocation, Integer> total = new HashMap<>();

    public void start(ResourceLocation id, int ticks) {
        if (ticks <= 0) return;
        remaining.put(id, ticks);
        total.put(id, ticks);
    }

    public boolean isCooling(ResourceLocation id) {
        return remaining.getOrDefault(id, 0) > 0;
    }

    public int remaining(ResourceLocation id) {
        return remaining.getOrDefault(id, 0);
    }

    /** 0 = ready, 1 = just started. */
    public float progress(ResourceLocation id) {
        int r = remaining.getOrDefault(id, 0);
        if (r <= 0) return 0f;
        return r / (float) Math.max(1, total.getOrDefault(id, r));
    }

    public void tick() {
        if (remaining.isEmpty()) return;
        var it = remaining.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int v = e.getValue() - 1;
            if (v <= 0) {
                it.remove();
                total.remove(e.getKey());
            } else e.setValue(v);
        }
    }

    public void clear() {
        remaining.clear();
        total.clear();
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        remaining.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("r", v);
            c.putInt("t", total.getOrDefault(k, v));
            t.put(k.toString(), c);
        });
        return t;
    }

    public void load(CompoundTag t) {
        clear();
        for (String key : t.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            CompoundTag c = t.getCompound(key);
            remaining.put(id, c.getInt("r"));
            total.put(id, Math.max(1, c.getInt("t")));
        }
    }
}
