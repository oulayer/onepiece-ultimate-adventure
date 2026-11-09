package com.onepiece.ultimate.destruction;

import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.entity.DebrisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A queue of block operations produced by one attack. Processed incrementally under the global per-tick
 * budget; operations are ordered centre-out so big craters visibly "expand".
 */
final class BlockJob {
    enum Op { REMOVE, CRACK }

    private record Entry(BlockPos pos, Op op) {}

    private final ServerLevel level;
    private final Vec3 center;
    @Nullable private final Entity source;
    private final float power;
    private final ArrayDeque<Entry> queue = new ArrayDeque<>();
    private final Set<BlockPos> felled = new HashSet<>();
    private int debrisBudget;
    private int removed;
    private final int maxBlocks;
    private final float debrisSpeed;

    BlockJob(ServerLevel level, Vec3 center, @Nullable Entity source, float power, float debrisSpeed) {
        this.level = level;
        this.center = center;
        this.source = source;
        this.power = power;
        this.debrisSpeed = debrisSpeed;
        this.maxBlocks = OPConfig.MAX_BLOCKS_PER_ATTACK.get();
        this.debrisBudget = OPConfig.DEBRIS_ENABLED.get() ? OPConfig.MAX_DEBRIS_PER_ATTACK.get() : 0;
    }

    void add(BlockPos pos, Op op) {
        queue.addLast(new Entry(pos.immutable(), op));
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }

    int size() {
        return queue.size();
    }

    ServerLevel level() {
        return level;
    }

    /** @return operations consumed from the budget. */
    int process(int budget) {
        int used = 0;
        RandomSource random = level.random;
        int restoreSeconds = OPConfig.RESTORE_TERRAIN_SECONDS.get();
        TerrainRestoreData restore = restoreSeconds > 0 ? TerrainRestoreData.get(level) : null;
        long restoreAt = level.getGameTime() + restoreSeconds * 20L;
        double dropChance = OPConfig.BLOCK_DROP_CHANCE.get();

        while (used < budget && !queue.isEmpty()) {
            Entry e = queue.pollFirst();
            used++;
            if (removed >= maxBlocks) {
                queue.clear();
                break;
            }
            BlockPos pos = e.pos;
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!DestructionRules.canDestroy(level, pos, state, source, power)) continue;

            if (e.op == Op.CRACK) {
                BlockState cracked = CrackConversions.cracked(state);
                if (cracked != null) {
                    if (restore != null) restore.add(pos, state, restoreAt);
                    level.setBlock(pos, cracked, 3);
                    BlockState above = level.getBlockState(pos.above());
                    if (above.canBeReplaced() && !above.isAir() && above.getFluidState().isEmpty()) level.removeBlock(pos.above(), false);
                }
                continue;
            }

            if (state.is(BlockTags.LOGS) && felled.add(pos)) fellTree(pos);

            if (restore != null) restore.add(pos, state, restoreAt);
            if (dropChance > 0 && random.nextDouble() < dropChance) Block.dropResources(state, level, pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            removed++;

            if (debrisBudget > 0 && random.nextFloat() < 0.35f && isExposed(pos)) {
                Vec3 p = Vec3.atCenterOf(pos);
                Vec3 out = p.subtract(center);
                out = out.lengthSqr() < 1e-3 ? new Vec3(random.nextGaussian(), 0, random.nextGaussian()) : out;
                out = out.normalize();
                Vec3 v = new Vec3(out.x * debrisSpeed * (0.6 + random.nextFloat() * 0.6),
                        0.35 + random.nextFloat() * 0.45 * debrisSpeed,
                        out.z * debrisSpeed * (0.6 + random.nextFloat() * 0.6));
                if (DebrisEntity.spawn(level, p, v, state)) debrisBudget--;
            }
            if (random.nextInt(4) == 0) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.15);
            }
        }
        return used;
    }

    private boolean isExposed(BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).isAir()) return true;
        }
        return false;
    }

    /** Connected logs and nearby leaves of a hit tree are knocked down as well. */
    private void fellTree(BlockPos start) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        int logs = 0;
        int leaves = 0;
        while (!open.isEmpty() && logs < 96) {
            BlockPos p = open.poll();
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
                if (n.getY() < start.getY() || felled.contains(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.is(BlockTags.LOGS)) {
                    BlockPos imm = n.immutable();
                    felled.add(imm);
                    open.add(imm);
                    queue.addLast(new Entry(imm, Op.REMOVE));
                    logs++;
                } else if (s.is(BlockTags.LEAVES) && leaves < 220) {
                    BlockPos imm = n.immutable();
                    felled.add(imm);
                    queue.addLast(new Entry(imm, Op.REMOVE));
                    leaves++;
                    // leaves around leaves (canopy), one step
                    for (Direction d : Direction.values()) {
                        BlockPos l = imm.relative(d);
                        if (!felled.contains(l) && level.getBlockState(l).is(BlockTags.LEAVES) && leaves < 220) {
                            felled.add(l);
                            queue.addLast(new Entry(l, Op.REMOVE));
                            leaves++;
                        }
                    }
                }
            }
        }
    }
}
