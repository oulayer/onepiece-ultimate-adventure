package com.onepiece.ultimate.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.entity.FistVisibility;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Client registry of animation controllers keyed by entity id. Controllers of entities that left the level are
 * dropped every second and everything is cleared on disconnect, so nothing leaks.
 */
public final class PlayerAnimations {
    private static final Int2ObjectOpenHashMap<AnimationController> CONTROLLERS = new Int2ObjectOpenHashMap<>();
    private static final Int2IntOpenHashMap LAST_HURT = new Int2IntOpenHashMap();
    private static final Pose POSE = new Pose();
    private static int cleanupTimer;

    public static AnimationController controller(int entityId) {
        return CONTROLLERS.computeIfAbsent(entityId, k -> new AnimationController());
    }

    @Nullable
    public static AnimationController existing(int entityId) {
        return CONTROLLERS.get(entityId);
    }

    public static void play(int entityId, ResourceLocation animation, float speed) {
        PoseAnimation anim = AnimationLoader.INSTANCE.get(animation);
        if (anim == null) {
            OnePieceMod.LOGGER.debug("Missing animation {}", animation);
            return;
        }
        controller(entityId).play(anim, speed);
    }

    public static void hitstop(int entityId, int ticks) {
        controller(entityId).hitstop(ticks);
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }
        for (Player p : level.players()) {
            OPCapabilities.get(p).ifPresent(data -> {
                String state = StateAnimations.resolve(p, data);
                PoseAnimation anim = state == null ? null : AnimationLoader.INSTANCE.get(OnePieceMod.id(state));
                AnimationController c = anim == null ? existing(p.getId()) : controller(p.getId());
                if (c != null) c.setState(anim);
            });
            // Flinch when hurt (hurtTime jumps up).
            int prev = LAST_HURT.get(p.getId());
            if (p.hurtTime > prev && p.hurtTime > 0) {
                AnimationController c = controller(p.getId());
                if (!c.isPlaying()) play(p.getId(), OnePieceMod.id("flinch"), 1f);
            }
            LAST_HURT.put(p.getId(), p.hurtTime);
        }
        for (AnimationController c : CONTROLLERS.values()) c.tick();

        if (++cleanupTimer >= 20) {
            cleanupTimer = 0;
            CONTROLLERS.int2ObjectEntrySet().removeIf(e -> {
                Entity ent = level.getEntity(e.getIntKey());
                return ent == null || ent.isRemoved();
            });
            LAST_HURT.keySet().removeIf(id -> level.getEntity(id) == null);
            FistVisibility.cleanup(level.getGameTime());
        }
    }

    /** Called at the end of {@code PlayerModel#setupAnim}. */
    public static void applyToModel(PlayerModel<?> model, Player player, float partial, boolean animate) {
        AnimationController c = CONTROLLERS.get(player.getId());
        if (animate && c != null && !c.isIdle()) {
            POSE.reset();
            POSE.read(Bone.HEAD, model.head);
            POSE.read(Bone.BODY, model.body);
            POSE.read(Bone.RIGHT_ARM, model.rightArm);
            POSE.read(Bone.LEFT_ARM, model.leftArm);
            POSE.read(Bone.RIGHT_LEG, model.rightLeg);
            POSE.read(Bone.LEFT_LEG, model.leftLeg);
            c.apply(POSE, partial);
            POSE.write(Bone.HEAD, model.head);
            POSE.write(Bone.BODY, model.body);
            POSE.write(Bone.RIGHT_ARM, model.rightArm);
            POSE.write(Bone.LEFT_ARM, model.leftArm);
            POSE.write(Bone.RIGHT_LEG, model.rightLeg);
            POSE.write(Bone.LEFT_LEG, model.leftLeg);
        }
        long now = player.level().getGameTime();
        model.rightArm.visible = !FistVisibility.rightHidden(player.getId(), now);
        model.leftArm.visible = !FistVisibility.leftHidden(player.getId(), now);
    }

    /** Whole-body transform (lean, spin, squash), applied in RenderPlayerEvent.Pre. Returns true if pushed. */
    public static boolean applyRoot(Player player, PoseStack stack, float partial) {
        AnimationController c = CONTROLLERS.get(player.getId());
        if (c == null || c.isIdle()) return false;
        c.applyRoot(POSE, partial);
        float rx = POSE.get(Bone.ROOT, 0), ry = POSE.get(Bone.ROOT, 1), rz = POSE.get(Bone.ROOT, 2);
        float px = POSE.get(Bone.ROOT, 3), py = POSE.get(Bone.ROOT, 4), pz = POSE.get(Bone.ROOT, 5);
        float sx = POSE.get(Bone.ROOT, 6), sy = POSE.get(Bone.ROOT, 7), sz = POSE.get(Bone.ROOT, 8);
        if (rx == 0 && ry == 0 && rz == 0 && px == 0 && py == 0 && pz == 0 && sx == 1 && sy == 1 && sz == 1) return false;

        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        float pivot = c.rootPivot();
        stack.pushPose();
        // Work in the body's frame: +X right, +Y up, -Z forward.
        stack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        stack.translate(px / 16f, py / 16f, -pz / 16f);
        stack.translate(0, pivot, 0);
        stack.mulPose(Axis.YP.rotationDegrees(ry));
        stack.mulPose(Axis.XP.rotationDegrees(-rx));
        stack.mulPose(Axis.ZP.rotationDegrees(rz));
        stack.scale(sx, sy, sz);
        stack.translate(0, -pivot, 0);
        stack.mulPose(Axis.YP.rotationDegrees(-(180f - bodyYaw)));
        return true;
    }

    public static void clear() {
        CONTROLLERS.clear();
        LAST_HURT.clear();
        FistVisibility.clear();
    }

    private PlayerAnimations() {}
}
