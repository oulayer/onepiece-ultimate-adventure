package com.onepiece.ultimate.destruction;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.config.OPConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/** Decides whether a block may be destroyed by an attack. Every destruction path goes through here. */
public final class DestructionRules {
    /** Blocks that attacks never destroy (datapack-extensible). */
    public static final TagKey<Block> INDESTRUCTIBLE = TagKey.create(Registries.BLOCK, OnePieceMod.id("indestructible"));

    /** Hardest block (destroy time) an attack of the given power can break. Stone = 1.5, iron block = 5, obsidian = 50. */
    public static float maxHardness(float power) {
        return power * 1.5f;
    }

    /** Checks that apply to the whole attack (cheap, done once). */
    public static boolean attackMayDestroy(ServerLevel level, @Nullable Entity source) {
        if (!OPConfig.DESTRUCTION_ENABLED.get()) return false;
        if (source instanceof ServerPlayer sp) return sp.mayBuild();
        if (OPConfig.RESPECT_MOB_GRIEFING.get()) return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        return true;
    }

    public static boolean canDestroy(ServerLevel level, BlockPos pos, BlockState state, @Nullable Entity source, float power) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > maxHardness(power)) return false;
        if (state.is(INDESTRUCTIBLE)) return false;
        if (OPConfig.PROTECT_BLOCK_ENTITIES.get() && state.hasBlockEntity()) return false;
        if (!level.getWorldBorder().isWithinBounds(pos)) return false;
        if (source instanceof ServerPlayer sp) {
            if (level.getServer().isUnderSpawnProtection(level, pos, sp)) return false;
            if (OPConfig.FIRE_BREAK_EVENTS.get()) {
                BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, sp);
                if (MinecraftForge.EVENT_BUS.post(event)) return false;
            }
        }
        return true;
    }

    private DestructionRules() {}
}
