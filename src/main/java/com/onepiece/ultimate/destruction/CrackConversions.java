package com.onepiece.ultimate.destruction;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** What a block turns into at the rim of a crater or along a ground crack. */
public final class CrackConversions {
    private static final Map<Block, Block> CRACKED = Map.ofEntries(
            Map.entry(Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT),
            Map.entry(Blocks.DIRT, Blocks.COARSE_DIRT),
            Map.entry(Blocks.PODZOL, Blocks.COARSE_DIRT),
            Map.entry(Blocks.MYCELIUM, Blocks.COARSE_DIRT),
            Map.entry(Blocks.DIRT_PATH, Blocks.COARSE_DIRT),
            Map.entry(Blocks.STONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.COBBLESTONE, Blocks.GRAVEL),
            Map.entry(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
            Map.entry(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE),
            Map.entry(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS),
            Map.entry(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES),
            Map.entry(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS),
            Map.entry(Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS),
            Map.entry(Blocks.SANDSTONE, Blocks.SAND),
            Map.entry(Blocks.RED_SANDSTONE, Blocks.RED_SAND),
            Map.entry(Blocks.ANDESITE, Blocks.GRAVEL),
            Map.entry(Blocks.DIORITE, Blocks.GRAVEL),
            Map.entry(Blocks.GRANITE, Blocks.GRAVEL),
            Map.entry(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW),
            Map.entry(Blocks.ICE, Blocks.WATER),
            Map.entry(Blocks.NETHERRACK, Blocks.MAGMA_BLOCK)
    );

    @Nullable
    public static BlockState cracked(BlockState state) {
        Block b = CRACKED.get(state.getBlock());
        return b == null ? null : b.defaultBlockState();
    }

    private CrackConversions() {}
}
