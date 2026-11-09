package com.onepiece.ultimate.fruit;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.fruit.gomu.GomuGomuNoMi;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of implemented Devil Fruits. Only fully implemented fruits are registered. */
public final class DevilFruits {
    public static final ResourceLocation GOMU_GOMU_ID = OnePieceMod.id("gomu_gomu");
    public static final ResourceLocation TORI_PHOENIX_ID = OnePieceMod.id("tori_tori_phoenix");

    private static final Map<ResourceLocation, DevilFruit> FRUITS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        register(new GomuGomuNoMi(GOMU_GOMU_ID));
        register(new com.onepiece.ultimate.fruit.phoenix.PhoenixFruit(TORI_PHOENIX_ID));
    }

    private static void register(DevilFruit fruit) {
        fruit.init();
        FRUITS.put(fruit.id(), fruit);
    }

    @Nullable
    public static DevilFruit get(@Nullable ResourceLocation id) {
        return id == null ? null : FRUITS.get(id);
    }

    public static Collection<DevilFruit> all() {
        return Collections.unmodifiableCollection(FRUITS.values());
    }

    private DevilFruits() {}
}
