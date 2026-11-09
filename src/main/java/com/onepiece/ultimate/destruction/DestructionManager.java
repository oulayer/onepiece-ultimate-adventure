package com.onepiece.ultimate.destruction;

import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.fx.ServerFx;
import com.onepiece.ultimate.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Public API for environmental destruction. All shapes are converted into {@link BlockJob}s which are processed
 * under a global per-dimension budget ({@code blocksPerTick}), protected by {@link DestructionRules} and capped
 * per attack ({@code maxBlocksPerAttack}).
 */
public final class DestructionManager {

    private static final class LevelQueue {
        final ArrayDeque<BlockJob> jobs = new ArrayDeque<>();
        final List<ShockwaveJob> waves = new ArrayList<>();
    }

    private static final Map<ServerLevel, LevelQueue> QUEUES = new WeakHashMap<>();

    private static LevelQueue queue(ServerLevel level) {
        return QUEUES.computeIfAbsent(level, l -> new LevelQueue());
    }

    // ================================================================== shapes

    /**
     * Bowl-shaped crater with a noisy edge and a cracked rim.
     *
     * @param depthScale vertical radius relative to the horizontal one (0.5 = shallow bowl)
     * @param power      destruction strength; see {@link DestructionRules#maxHardness}
     */
    public static void crater(ServerLevel level, Vec3 center, float radius, float depthScale, float power, @Nullable Entity source) {
        if (radius < 0.5f || !DestructionRules.attackMayDestroy(level, source)) return;
        BlockJob job = new BlockJob(level, center, source, power, Math.min(1.2f, 0.3f + radius * 0.12f));
        int r = Mth.ceil(radius * 1.3f);
        float ry = Math.max(0.6f, radius * depthScale);
        BlockPos c = BlockPos.containing(center);
        List<long[]> inner = new ArrayList<>();
        List<BlockPos> rim = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -Mth.ceil(ry * 1.3f); dy <= Mth.ceil(radius * 0.8f); dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double yScale = dy < 0 ? ry : radius * 0.8; // above the impact point the blast reaches less high
                    double d = Math.sqrt((dx * dx + dz * dz) / (double) (radius * radius) + (dy * dy) / (yScale * yScale));
                    double noise = 0.18 * noise(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockPos p = c.offset(dx, dy, dz);
                    if (d <= 1.0 + noise) inner.add(new long[]{p.asLong(), (long) (d * 1000)});
                    else if (d <= 1.3 + noise && dy <= 0) rim.add(p);
                }
            }
        }
        inner.sort(Comparator.comparingLong(a -> a[1]));
        for (long[] e : inner) job.add(BlockPos.of(e[0]), BlockJob.Op.REMOVE);
        for (BlockPos p : rim) {
            if (level.getBlockState(p.above()).isAir() || level.random.nextInt(3) == 0) job.add(p, BlockJob.Op.CRACK);
        }
        enqueue(level, job);
        if (radius >= 2.5f) ServerFx.sound(level, center, ModSounds.RUMBLE.get(), Math.min(3f, radius * 0.4f), 0.8f);
    }

    /** Straight bore through terrain, e.g. a giant fist travelling through a hill. */
    public static void tunnel(ServerLevel level, Vec3 from, Vec3 to, float radius, float power, @Nullable Entity source) {
        if (!DestructionRules.attackMayDestroy(level, source)) return;
        BlockJob job = new BlockJob(level, from, source, power, 0.6f);
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 1e-3) return;
        dir = dir.scale(1 / len);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int ri = Mth.ceil(radius);
        for (double t = 0; t <= len; t += 0.5) {
            Vec3 p = from.add(dir.scale(t));
            BlockPos bp = BlockPos.containing(p);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dy = -ri; dy <= ri; dy++)
                    for (int dz = -ri; dz <= ri; dz++) {
                        BlockPos q = bp.offset(dx, dy, dz);
                        if (Vec3.atCenterOf(q).distanceToSqr(p) <= radius * radius && seen.add(q.asLong())) job.add(q, BlockJob.Op.REMOVE);
                    }
        }
        enqueue(level, job);
    }

    /** Fissures that run outward along the ground surface from {@code center}. */
    public static void cracks(ServerLevel level, Vec3 center, int count, int length, @Nullable Entity source) {
        if (!DestructionRules.attackMayDestroy(level, source)) return;
        BlockJob job = new BlockJob(level, center, source, 2.0f, 0.4f);
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double x = center.x, z = center.z;
            int y = Mth.floor(center.y);
            for (int s = 0; s < length; s++) {
                angle += (random.nextDouble() - 0.5) * 0.6;
                x += Math.cos(angle);
                z += Math.sin(angle);
                BlockPos surface = findSurface(level, BlockPos.containing(x, y, z));
                if (surface == null) break;
                y = surface.getY();
                job.add(surface, BlockJob.Op.CRACK);
                if (random.nextFloat() < 0.3f) job.add(surface, BlockJob.Op.REMOVE); // open gap
                if (s < length / 2) { // wider near the epicentre
                    BlockPos side = surface.offset((int) Math.round(-Math.sin(angle)), 0, (int) Math.round(Math.cos(angle)));
                    job.add(side, BlockJob.Op.CRACK);
                }
            }
        }
        enqueue(level, job);
    }

    /** Expanding ground ring that damages, lifts and pushes entities as it passes. */
    public static void shockwave(ServerLevel level, Vec3 center, float maxRadius, int durationTicks, float damage, float lift, float knockback,
                                 @Nullable LivingEntity source) {
        queue(level).waves.add(new ShockwaveJob(level, center, maxRadius, durationTicks, damage, lift, knockback, source));
    }

    // ================================================================== processing

    private static void enqueue(ServerLevel level, BlockJob job) {
        if (!job.isEmpty()) queue(level).jobs.addLast(job);
    }

    /** End of each server level tick. */
    public static void tick(ServerLevel level) {
        LevelQueue q = QUEUES.get(level);
        if (q != null) {
            if (!q.waves.isEmpty()) q.waves.removeIf(ShockwaveJob::tick);
            int budget = OPConfig.BLOCKS_PER_TICK.get();
            // Round-robin so one huge crater can't starve the others.
            int jobs = q.jobs.size();
            while (budget > 0 && !q.jobs.isEmpty() && jobs-- > 0) {
                BlockJob job = q.jobs.pollFirst();
                int share = Math.max(16, budget / Math.max(1, q.jobs.size() + 1));
                budget -= job.process(Math.min(budget, share));
                if (!job.isEmpty()) q.jobs.addLast(job);
            }
        }
        if (OPConfig.RESTORE_TERRAIN_SECONDS.get() > 0 || level.getGameTime() % 100 == 0) {
            TerrainRestoreData.get(level).tick(level);
        }
    }

    /** Pending block operations in a level (tests / debug command). */
    public static int pending(ServerLevel level) {
        LevelQueue q = QUEUES.get(level);
        if (q == null) return 0;
        int n = 0;
        for (BlockJob j : q.jobs) n += j.size();
        return n + q.waves.size();
    }

    /** Process everything immediately (used by tests). */
    public static void flush(ServerLevel level) {
        LevelQueue q = QUEUES.get(level);
        if (q == null) return;
        while (!q.waves.isEmpty()) q.waves.removeIf(ShockwaveJob::tick);
        for (Iterator<BlockJob> it = q.jobs.iterator(); it.hasNext(); ) {
            BlockJob j = it.next();
            while (!j.isEmpty()) j.process(Integer.MAX_VALUE);
            it.remove();
        }
    }

    public static void clear() {
        QUEUES.clear();
    }

    // ================================================================== helpers

    @Nullable
    private static BlockPos findSurface(ServerLevel level, BlockPos around) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos p = around.offset(0, dy, 0);
            if (!level.getBlockState(p).isAir() && level.getBlockState(p).isSolid() && !level.getBlockState(p.above()).isSolid()) return p;
        }
        return null;
    }

    /** Deterministic value noise in [-1, 1]. */
    private static double noise(int x, int y, int z) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        h ^= h >>> 15;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    private DestructionManager() {}
}
