package com.onepiece.ultimate.data;

import com.onepiece.ultimate.ability.AbilityResource;
import com.onepiece.ultimate.ability.AbilityRuntime;
import com.onepiece.ultimate.haki.HakiData;
import com.onepiece.ultimate.haki.HakiType;
import com.onepiece.ultimate.combat.CombatState;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.fruit.DevilFruits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * All One Piece state attached to a player.
 * <p>
 * Three tiers of data:
 * <ol>
 *   <li><b>Persistent</b> – fruit, mastery, progression, cooldowns, combat-mode preference (saved to NBT).</li>
 *   <li><b>Synced runtime</b> – energy, active toggles, blocking/charging flags (sent to client(s), not saved
 *       except energy).</li>
 *   <li><b>Server runtime</b> – {@link CombatState} and {@link AbilityRuntime}; never leave the server.</li>
 * </ol>
 * Dirty flags tell {@code DataSync} which packets to send at the end of the tick.
 */
public final class OPPlayerData {
    public static final int MAX_MASTERY = 100;

    // ---- persistent ----
    @Nullable private ResourceLocation fruitId;
    private int masteryXp;
    private final Progression progression = new Progression();
    private final Cooldowns cooldowns = new Cooldowns();
    private boolean combatMode;
    private float energy = 100f;
    private final HakiData haki = new HakiData();
    private float hakiGauge = 100f;

    // ---- synced runtime ----
    private final Set<ResourceLocation> activeToggles = new LinkedHashSet<>();
    private boolean blocking;
    @Nullable private ResourceLocation channeling; // ability being charged or held
    private int channelTicks;
    private boolean seaWeakened;
    private int comboHits;
    private boolean futureSight;
    private boolean giant;
    /** Armament level of this player as seen by other clients (coverage of the coating). */
    private int visualArmamentLevel;

    // ---- server runtime ----
    public final CombatState combat = new CombatState();
    public final AbilityRuntime abilities = new AbilityRuntime();
    public final com.onepiece.ultimate.haki.HakiRuntime hakiRuntime = new com.onepiece.ultimate.haki.HakiRuntime();

    // ---- dirty flags ----
    private boolean fullDirty = true, visualDirty = true, statsDirty = true;

    // ===================================================================== fruit & mastery

    @Nullable
    public DevilFruit fruit() {
        return fruitId == null ? null : DevilFruits.get(fruitId);
    }

    @Nullable
    public ResourceLocation fruitId() {
        return fruitId;
    }

    public boolean hasFruit() {
        return fruit() != null;
    }

    public void setFruit(@Nullable ResourceLocation id) {
        this.fruitId = id;
        this.masteryXp = 0;
        this.activeToggles.clear();
        this.channeling = null;
        markFull();
        markVisual();
    }

    public static int masteryXpForNext(int level) {
        return 30 + level * 9;
    }

    public int masteryXp() {
        return masteryXp;
    }

    public void setMasteryXp(int xp) {
        this.masteryXp = Math.max(0, xp);
        markFull();
    }

    /** Mastery level 0..100 derived from accumulated xp. */
    public int mastery() {
        int lvl = 0;
        long remaining = masteryXp;
        while (lvl < MAX_MASTERY && remaining >= masteryXpForNext(lvl)) {
            remaining -= masteryXpForNext(lvl);
            lvl++;
        }
        return lvl;
    }

    /** Progress (0..1) inside the current mastery level. */
    public float masteryProgress() {
        int lvl = 0;
        long remaining = masteryXp;
        while (lvl < MAX_MASTERY && remaining >= masteryXpForNext(lvl)) {
            remaining -= masteryXpForNext(lvl);
            lvl++;
        }
        return lvl >= MAX_MASTERY ? 1f : remaining / (float) masteryXpForNext(lvl);
    }

    public static int totalXpForMastery(int level) {
        int sum = 0;
        for (int i = 0; i < Math.min(level, MAX_MASTERY); i++) sum += masteryXpForNext(i);
        return sum;
    }

    // ===================================================================== progression & energy

    public Progression progression() {
        return progression;
    }

    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public float maxEnergy() {
        return 100f + 5f * progression.get(PlayerStat.STAMINA);
    }

    public float energy() {
        return energy;
    }

    public void setEnergy(float value) {
        float clamped = Math.max(0f, Math.min(maxEnergy(), value));
        if (clamped != energy) {
            energy = clamped;
            statsDirty = true;
        }
    }

    public boolean consumeEnergy(float amount) {
        if (energy < amount) return false;
        setEnergy(energy - amount);
        return true;
    }

    // ===================================================================== haki

    public HakiData haki() {
        return haki;
    }

    public int hakiLevel(HakiType type) {
        return haki.level(type);
    }

    public float maxHaki() {
        return 80f + 6f * progression.get(PlayerStat.HAKI) + 4f * haki.totalLevels();
    }

    public float hakiGauge() {
        return hakiGauge;
    }

    public void setHakiGauge(float value) {
        float clamped = Math.max(0f, Math.min(maxHaki(), value));
        if (clamped != hakiGauge) {
            hakiGauge = clamped;
            statsDirty = true;
        }
    }

    public float resource(AbilityResource r) {
        return r == AbilityResource.HAKI ? hakiGauge : energy;
    }

    public boolean consume(AbilityResource r, float amount) {
        if (amount <= 0) return true;
        if (r == AbilityResource.HAKI) {
            if (hakiGauge < amount) return false;
            setHakiGauge(hakiGauge - amount);
            return true;
        }
        return consumeEnergy(amount);
    }

    /** Gear Fifth "Gigant": the body is temporarily colossal (hitbox and model). */
    public boolean giant() {
        return giant;
    }

    public void setGiant(boolean on) {
        if (giant != on) {
            giant = on;
            markVisual();
        }
    }

    public boolean futureSight() {
        return futureSight;
    }

    public void setFutureSight(boolean on) {
        if (futureSight != on) {
            futureSight = on;
            markVisual();
        }
    }

    /** Armament level for rendering (own level on the server/owner, synced level for other players). */
    public int armamentLevelForRender() {
        return Math.max(visualArmamentLevel, haki.level(HakiType.ARMAMENT));
    }

    // ===================================================================== runtime state

    public boolean combatMode() {
        return combatMode;
    }

    public void setCombatMode(boolean on) {
        if (combatMode != on) {
            combatMode = on;
            markFull();
            markVisual();
        }
    }

    public Set<ResourceLocation> activeToggles() {
        return Collections.unmodifiableSet(activeToggles);
    }

    public boolean isToggleActive(ResourceLocation id) {
        return activeToggles.contains(id);
    }

    public void setToggle(ResourceLocation id, boolean active) {
        boolean changed = active ? activeToggles.add(id) : activeToggles.remove(id);
        if (changed) {
            markVisual();
            markFull();
        }
    }

    public boolean blocking() {
        return blocking;
    }

    public void setBlocking(boolean b) {
        if (blocking != b) {
            blocking = b;
            markVisual();
        }
    }

    @Nullable
    public ResourceLocation channeling() {
        return channeling;
    }

    public int channelTicks() {
        return channelTicks;
    }

    public void setChanneling(@Nullable ResourceLocation id, int ticks) {
        boolean changedId = (id == null) != (channeling == null) || (id != null && !id.equals(channeling));
        channeling = id;
        if (channelTicks != ticks) statsDirty = true;
        channelTicks = ticks;
        if (changedId) markVisual();
    }

    public boolean seaWeakened() {
        return seaWeakened;
    }

    public void setSeaWeakened(boolean w) {
        if (seaWeakened != w) {
            seaWeakened = w;
            markVisual();
        }
    }

    public int comboHits() {
        return comboHits;
    }

    public void setComboHits(int hits) {
        if (comboHits != hits) {
            comboHits = hits;
            statsDirty = true;
        }
    }

    // ===================================================================== dirty tracking

    public void markFull() { fullDirty = true; }
    public void markVisual() { visualDirty = true; }
    public void markStats() { statsDirty = true; }

    public boolean consumeFullDirty() { boolean d = fullDirty; fullDirty = false; return d; }
    public boolean consumeVisualDirty() { boolean d = visualDirty; visualDirty = false; return d; }
    public boolean consumeStatsDirty() { boolean d = statsDirty; statsDirty = false; return d; }

    // ===================================================================== serialization

    /** Persistent data (world save). */
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (fruitId != null) t.putString("fruit", fruitId.toString());
        t.putInt("masteryXp", masteryXp);
        t.put("progression", progression.save());
        t.put("cooldowns", cooldowns.save());
        t.putBoolean("combatMode", combatMode);
        t.putFloat("energy", energy);
        t.put("haki", haki.save());
        t.putFloat("hakiGauge", hakiGauge);
        return t;
    }

    public void load(CompoundTag t) {
        fruitId = t.contains("fruit") ? ResourceLocation.tryParse(t.getString("fruit")) : null;
        if (fruitId != null && DevilFruits.get(fruitId) == null) fruitId = null; // fruit removed from the mod
        masteryXp = t.getInt("masteryXp");
        progression.load(t.getCompound("progression"));
        cooldowns.load(t.getCompound("cooldowns"));
        combatMode = t.getBoolean("combatMode");
        haki.load(t.getCompound("haki"));
        energy = t.contains("energy") ? t.getFloat("energy") : maxEnergy();
        hakiGauge = t.contains("hakiGauge") ? t.getFloat("hakiGauge") : maxHaki();
        markFull();
        markVisual();
    }

    /** Everything the owning client needs: persistent data plus synced runtime. */
    public CompoundTag saveForOwner() {
        CompoundTag t = save();
        t.put("visual", saveVisual());
        return t;
    }

    public void loadForOwner(CompoundTag t) {
        load(t);
        loadVisual(t.getCompound("visual"));
    }

    /** What other players need to render this player (transformations, poses). */
    public CompoundTag saveVisual() {
        CompoundTag t = new CompoundTag();
        if (fruitId != null) t.putString("fruit", fruitId.toString());
        ListTag toggles = new ListTag();
        activeToggles.forEach(id -> toggles.add(StringTag.valueOf(id.toString())));
        t.put("toggles", toggles);
        t.putBoolean("blocking", blocking);
        t.putBoolean("combatMode", combatMode);
        t.putBoolean("weak", seaWeakened);
        if (channeling != null) t.putString("channel", channeling.toString());
        t.putBoolean("futureSight", futureSight);
        t.putBoolean("giant", giant);
        t.putByte("buso", (byte) haki.level(HakiType.ARMAMENT));
        return t;
    }

    public void loadVisual(CompoundTag t) {
        fruitId = t.contains("fruit") ? ResourceLocation.tryParse(t.getString("fruit")) : null;
        activeToggles.clear();
        for (Tag tag : t.getList("toggles", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null) activeToggles.add(id);
        }
        blocking = t.getBoolean("blocking");
        combatMode = t.getBoolean("combatMode");
        seaWeakened = t.getBoolean("weak");
        channeling = t.contains("channel") ? ResourceLocation.tryParse(t.getString("channel")) : null;
        futureSight = t.getBoolean("futureSight");
        giant = t.getBoolean("giant");
        visualArmamentLevel = t.getByte("buso");
    }

    /** Copy persistent progress when the player respawns. */
    public void copyOnDeath(OPPlayerData old, boolean keepFruit) {
        this.progression.copyFrom(old.progression);
        this.haki.load(old.haki.save()); // haki is trained willpower: never lost on death
        this.hakiGauge = maxHaki();
        if (keepFruit) {
            this.fruitId = old.fruitId;
            this.masteryXp = old.masteryXp;
        }
        this.combatMode = old.combatMode;
        this.energy = maxEnergy();
        markFull();
        markVisual();
    }
}
