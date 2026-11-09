package com.onepiece.ultimate.entity;

import com.onepiece.ultimate.config.OPConfig;
import com.onepiece.ultimate.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * A chunk of terrain thrown by an impact. Pure physics (gravity, collision, bounce) and short lived;
 * never places blocks. The number alive per dimension is capped by config.
 */
public class DebrisEntity extends Entity {
    private static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final Map<ResourceKey<Level>, Integer> ALIVE = new HashMap<>();

    private int life;
    private int groundTicks;
    private boolean counted;
    public final float spinX, spinZ;

    public DebrisEntity(EntityType<? extends DebrisEntity> type, Level level) {
        super(type, level);
        this.spinX = (level.random.nextFloat() - 0.5f) * 40f;
        this.spinZ = (level.random.nextFloat() - 0.5f) * 40f;
    }

    /** @return false if the cap is reached or the block can't be rendered. */
    public static boolean spawn(ServerLevel level, Vec3 pos, Vec3 velocity, BlockState state) {
        if (state.getRenderShape() != RenderShape.MODEL) return false;
        if (ALIVE.getOrDefault(level.dimension(), 0) >= OPConfig.MAX_DEBRIS_PER_LEVEL.get()) return false;
        DebrisEntity e = new DebrisEntity(ModEntities.DEBRIS.get(), level);
        e.setBlockState(state);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(velocity);
        return level.addFreshEntity(e);
    }

    public static void clearCounts() {
        ALIVE.clear();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(STATE, Blocks.STONE.defaultBlockState());
    }

    public BlockState blockState() {
        return entityData.get(STATE);
    }

    public void setBlockState(BlockState s) {
        entityData.set(STATE, s);
    }

    public int life() {
        return life;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        if (!isNoGravity()) v = v.add(0, -0.045, 0);
        boolean wasOnGround = onGround();
        move(MoverType.SELF, v);
        Vec3 after = getDeltaMovement();
        if (onGround() && !wasOnGround && v.y < -0.2) {
            after = new Vec3(v.x * 0.5, -v.y * 0.3, v.z * 0.5); // bounce
        } else if (onGround()) {
            after = after.multiply(0.7, 1, 0.7);
        }
        setDeltaMovement(after.scale(0.98));
        life++;
        if (onGround()) groundTicks++;
        if (!level().isClientSide && (life > 100 || groundTicks > 30 || isInWater())) discard();
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            ALIVE.merge(level().dimension(), 1, Integer::sum);
            counted = true;
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (counted) {
            ALIVE.computeIfPresent(level().dimension(), (k, n) -> n > 1 ? n - 1 : null);
            counted = false;
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
