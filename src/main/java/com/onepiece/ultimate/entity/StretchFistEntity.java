package com.onepiece.ultimate.entity;

import com.onepiece.ultimate.combat.HitDetector;
import com.onepiece.ultimate.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A rubber fist that travels out from its owner's shoulder and snaps back. The arm between shoulder and fist is
 * drawn by the renderer, so the fist really is attached to the player.
 * <p>
 * Hit detection is a swept capsule every tick (no tunnelling at Jet speeds). Behaviour on hit is supplied by the
 * ability that launched it, keeping the entity generic: Pistol, Gatling and Gigant Pistol all reuse it.
 */
public class StretchFistEntity extends Projectile {
    public enum Style { NORMAL, JET, GIGANT, DAWN }

    private static final EntityDataAccessor<Byte> SIDE = SynchedEntityData.defineId(StretchFistEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(StretchFistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(StretchFistEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> STYLE = SynchedEntityData.defineId(StretchFistEntity.class, EntityDataSerializers.BYTE);

    // ---- server configuration
    private double speed = 2.0;
    private double maxRange = 16;
    private int pierce = 0;
    private boolean boreThroughBlocks;
    private int ticks;
    private final Set<Integer> hitIds = new HashSet<>();
    @Nullable private Consumer<LivingEntity> onHitEntity;
    @Nullable private BiConsumer<BlockHitResult, Boolean> onHitBlock; // (hit, isFinal)
    @Nullable private Consumer<Vec3> onTravel;
    @Nullable private LivingEntity homingTarget;
    private double turnRate;

    public StretchFistEntity(EntityType<? extends StretchFistEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static StretchFistEntity create(Level level, LivingEntity owner, Vec3 origin, Vec3 direction, int side) {
        StretchFistEntity e = new StretchFistEntity(ModEntities.STRETCH_FIST.get(), level);
        e.setOwner(owner);
        e.setPos(origin.x, origin.y - e.getBbHeight() / 2, origin.z);
        e.entityData.set(SIDE, (byte) side);
        e.setDeltaMovement(direction.normalize().scale(e.speed));
        e.lookAlong(direction);
        return e;
    }

    // ---- fluent configuration (server side)
    public StretchFistEntity speed(double s) { this.speed = s; setDeltaMovement(getDeltaMovement().normalize().scale(s)); return this; }
    public StretchFistEntity range(double r) { this.maxRange = r; return this; }
    public StretchFistEntity pierce(int n) { this.pierce = n; return this; }
    public StretchFistEntity bore() { this.boreThroughBlocks = true; return this; }
    public StretchFistEntity scale(float s) { entityData.set(SCALE, s); return this; }
    public StretchFistEntity style(Style s) { entityData.set(STYLE, (byte) s.ordinal()); return this; }
    public StretchFistEntity onHitEntity(Consumer<LivingEntity> c) { this.onHitEntity = c; return this; }
    public StretchFistEntity onHitBlock(BiConsumer<BlockHitResult, Boolean> c) { this.onHitBlock = c; return this; }
    public StretchFistEntity onTravel(Consumer<Vec3> c) { this.onTravel = c; return this; }
    /** The fist bends toward {@code target} each tick (0..1 = how sharply it can turn). */
    public StretchFistEntity homing(@Nullable LivingEntity target, double turnRate) { this.homingTarget = target; this.turnRate = turnRate; return this; }

    @Override
    protected void defineSynchedData() {
        entityData.define(SIDE, (byte) 1);
        entityData.define(SCALE, 1f);
        entityData.define(RETURNING, false);
        entityData.define(STYLE, (byte) 0);
    }

    public int side() { return entityData.get(SIDE); }
    public float fistScale() { return entityData.get(SCALE); }
    public boolean isReturning() { return entityData.get(RETURNING); }
    public Style style() {
        int s = entityData.get(STYLE);
        return Style.values()[Math.max(0, Math.min(Style.values().length - 1, s))];
    }

    private void lookAlong(Vec3 d) {
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        setXRot((float) Math.toDegrees(-Math.atan2(d.y, h)));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    /** Shoulder position of the owner on this fist's side. */
    public Vec3 shoulder(LivingEntity owner, float partial) {
        double yaw = Math.toRadians(net.minecraft.util.Mth.lerp(partial, owner.yBodyRotO, owner.yBodyRot));
        int side = side() == 0 ? 1 : side();
        double ox = -Math.cos(yaw) * 0.36 * side;
        double oz = -Math.sin(yaw) * 0.36 * side;
        Vec3 pos = owner.getPosition(partial);
        return pos.add(ox, owner.getBbHeight() * (owner.isCrouching() ? 0.68 : 0.76), oz);
    }

    public void startReturning() {
        entityData.set(RETURNING, true);
    }

    @Override
    public void tick() {
        super.tick();
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != level()) {
            if (!level().isClientSide) discard();
            return;
        }
        if (level().isClientSide) {
            // movement comes from the server, the renderer interpolates; just hide the real arm meanwhile
            FistVisibility.mark(owner.getId(), side(), level().getGameTime());
            return;
        }
        ticks++;

        Vec3 center = position().add(0, getBbHeight() / 2, 0);
        Vec3 shoulder = shoulder(owner, 1f);

        if (isReturning()) {
            Vec3 to = shoulder.subtract(center);
            double dist = to.length();
            if (dist < Math.max(1.2, speed * 1.5) || ticks > 200) {
                discard();
                return;
            }
            Vec3 step = to.scale(Math.min(1.0, speed * 1.5 / dist));
            setDeltaMovement(step);
            setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
            return;
        }

        Vec3 vel = getDeltaMovement();
        if (homingTarget != null && homingTarget.isAlive() && !hitIds.contains(homingTarget.getId())) {
            Vec3 want = homingTarget.getBoundingBox().getCenter().subtract(center);
            if (want.lengthSqr() > 1e-4) {
                Vec3 cur = vel.normalize();
                Vec3 dir = cur.add(want.normalize().subtract(cur).scale(turnRate)).normalize();
                vel = dir.scale(speed);
                setDeltaMovement(vel);
                lookAlong(dir);
            }
        }
        Vec3 next = center.add(vel);
        float radius = 0.35f * fistScale();

        // ---- blocks
        BlockHitResult blockHit = level().clip(new ClipContext(center, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : next;

        // ---- entities along the swept segment
        for (LivingEntity target : HitDetector.sweep(level(), center, end, radius, owner)) {
            if (!HitDetector.isValidTarget(owner, target) || !hitIds.add(target.getId())) continue;
            if (onHitEntity != null) onHitEntity.accept(target);
            if (pierce-- <= 0) {
                setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5 - getBbHeight() / 2, target.getZ());
                startReturning();
                return;
            }
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            if (boreThroughBlocks && onHitBlock != null && center.distanceTo(shoulder) < maxRange) {
                onHitBlock.accept(blockHit, false);
            } else {
                if (onHitBlock != null) onHitBlock.accept(blockHit, true);
                Vec3 p = blockHit.getLocation().subtract(vel.normalize().scale(0.3));
                setPos(p.x, p.y - getBbHeight() / 2, p.z);
                startReturning();
                return;
            }
        }

        setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
        if (onTravel != null) onTravel.accept(position());
        if (position().distanceTo(shoulder) >= maxRange) startReturning();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
