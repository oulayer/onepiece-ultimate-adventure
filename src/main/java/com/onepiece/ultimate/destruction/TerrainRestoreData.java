package com.onepiece.ultimate.destruction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;

/**
 * Optional terrain regeneration ("restoreTerrainSeconds"). Destroyed blocks are remembered per dimension and
 * put back after the delay – only into empty space, so it never overwrites a player's build.
 * Persisted with the world so a restart doesn't make destruction permanent.
 */
public final class TerrainRestoreData extends SavedData {
    private static final String NAME = "onepiece_terrain_restore";
    private static final int MAX_ENTRIES = 200_000;
    private static final int RESTORES_PER_TICK = 64;

    private record Entry(BlockPos pos, BlockState state, long at) {}

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public static TerrainRestoreData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(tag -> load(level, tag), TerrainRestoreData::new, NAME);
    }

    public void add(BlockPos pos, BlockState original, long restoreAt) {
        if (entries.size() >= MAX_ENTRIES) return;
        entries.addLast(new Entry(pos.immutable(), original, restoreAt));
        setDirty();
    }

    public int size() {
        return entries.size();
    }

    public void tick(ServerLevel level) {
        if (entries.isEmpty()) return;
        long now = level.getGameTime();
        int budget = RESTORES_PER_TICK;
        while (budget-- > 0 && !entries.isEmpty() && entries.peekFirst().at <= now) {
            Entry e = entries.pollFirst();
            if (!level.isLoaded(e.pos)) {
                entries.addLast(new Entry(e.pos, e.state, now + 200)); // try again later
                continue;
            }
            if (level.getBlockState(e.pos).isAir()) level.setBlock(e.pos, e.state, 3);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag c = new CompoundTag();
            c.putLong("p", e.pos.asLong());
            c.put("s", NbtUtils.writeBlockState(e.state));
            c.putLong("t", e.at);
            list.add(c);
        }
        tag.put("entries", list);
        return tag;
    }

    private static TerrainRestoreData load(ServerLevel level, CompoundTag tag) {
        TerrainRestoreData data = new TerrainRestoreData();
        var blocks = level.holderLookup(Registries.BLOCK);
        for (Tag t : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            data.entries.addLast(new Entry(BlockPos.of(c.getLong("p")), NbtUtils.readBlockState(blocks, c.getCompound("s")), c.getLong("t")));
        }
        return data;
    }
}
