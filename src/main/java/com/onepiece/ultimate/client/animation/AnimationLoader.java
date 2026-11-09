package com.onepiece.ultimate.client.animation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onepiece.ultimate.OnePieceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Loads {@code assets/<namespace>/animations/*.json}. Resource packs can override or add animations.
 * <pre>
 * {
 *   "length": 12, "loop": false, "hold": false, "blend_in": 2, "blend_out": 3, "priority": 10,
 *   "bones": {
 *     "right_arm": { "rotation": [[0, -70, -20, 0], [3, -95, 5, 0, "out_back"]], "additive": false },
 *     "root":      { "position": [[0, 0, 0, 0], [2, 0, 0, 1.5]], "scale": [[0, 1, 1, 1]] }
 *   }
 * }
 * </pre>
 * Keyframes are {@code [tick, x, y, z, easing?]}; rotation in degrees, position in pixels.
 */
public final class AnimationLoader extends SimpleJsonResourceReloadListener {
    // GSON must be initialised before INSTANCE (static initialisation order).
    private static final Gson GSON = new Gson();
    public static final AnimationLoader INSTANCE = new AnimationLoader();

    private Map<ResourceLocation, PoseAnimation> animations = Map.of();

    private AnimationLoader() {
        super(GSON, "animations");
    }

    @Nullable
    public PoseAnimation get(ResourceLocation id) {
        return animations.get(id);
    }

    public int count() {
        return animations.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, PoseAnimation> out = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                out.put(id, parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                OnePieceMod.LOGGER.error("Failed to load animation {}: {}", id, e.getMessage());
            }
        });
        animations = Collections.unmodifiableMap(out);
        OnePieceMod.LOGGER.info("Loaded {} One Piece animations", out.size());
    }

    static PoseAnimation parse(ResourceLocation id, JsonObject o) {
        float length = GsonHelper.getAsFloat(o, "length");
        boolean loop = GsonHelper.getAsBoolean(o, "loop", false);
        boolean hold = GsonHelper.getAsBoolean(o, "hold", false);
        float in = GsonHelper.getAsFloat(o, "blend_in", 2f);
        float out = GsonHelper.getAsFloat(o, "blend_out", 3f);
        int priority = GsonHelper.getAsInt(o, "priority", 10);
        float pivot = GsonHelper.getAsFloat(o, "root_pivot", 0.9f);
        Map<Bone, BoneTrack> tracks = new EnumMap<>(Bone.class);
        JsonObject bones = GsonHelper.getAsJsonObject(o, "bones");
        for (Map.Entry<String, JsonElement> e : bones.entrySet()) {
            Bone bone = Bone.byName(e.getKey());
            if (bone == null) throw new IllegalArgumentException("unknown bone " + e.getKey());
            JsonObject b = e.getValue().getAsJsonObject();
            tracks.put(bone, new BoneTrack(frames(b, "rotation"), frames(b, "position"), frames(b, "scale"),
                    GsonHelper.getAsBoolean(b, "additive", false)));
        }
        return new PoseAnimation(id, length, loop, hold, in, out, priority, pivot, tracks);
    }

    @Nullable
    private static Keyframe[] frames(JsonObject bone, String key) {
        if (!bone.has(key)) return null;
        JsonArray arr = bone.getAsJsonArray(key);
        Keyframe[] frames = new Keyframe[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            JsonArray k = arr.get(i).getAsJsonArray();
            Easing ease = k.size() > 4 ? Easing.parse(k.get(4).getAsString()) : Easing.IN_OUT_QUAD;
            frames[i] = new Keyframe(k.get(0).getAsFloat(), k.get(1).getAsFloat(), k.get(2).getAsFloat(), k.get(3).getAsFloat(), ease);
        }
        Arrays.sort(frames, Comparator.comparingDouble(Keyframe::time));
        return frames;
    }
}
