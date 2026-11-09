package com.onepiece.ultimate.data;

import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class OPCapabilities {
    public static final ResourceLocation KEY = OnePieceMod.id("player_data");
    public static final Capability<OPPlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    /** Data of a player, or empty if the capability is missing (e.g. invalidated during respawn). */
    public static Optional<OPPlayerData> get(@Nullable Player player) {
        if (player == null) return Optional.empty();
        return player.getCapability(PLAYER_DATA).resolve();
    }

    /** Convenience for code paths where the capability is guaranteed (living players). */
    @Nullable
    public static OPPlayerData getOrNull(@Nullable Player player) {
        return get(player).orElse(null);
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final OPPlayerData data = new OPPlayerData();
        private final LazyOptional<OPPlayerData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }

    private OPCapabilities() {}
}
