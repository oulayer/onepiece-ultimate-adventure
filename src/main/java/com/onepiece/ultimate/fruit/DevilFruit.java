package com.onepiece.ultimate.fruit;

import com.onepiece.ultimate.ability.Ability;
import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Definition of a Devil Fruit: its ability loadout (one ability per hotkey slot) and its passive traits.
 * Every fruit is its own subclass in its own package so mechanics never get shared by accident.
 */
public abstract class DevilFruit {
    private final ResourceLocation id;
    private final FruitType type;
    private final FruitRarity rarity;
    private final int color;
    private List<Ability> abilities = List.of();
    private List<Ability> transformations = List.of();

    protected DevilFruit(ResourceLocation id, FruitType type, FruitRarity rarity, int color) {
        this.id = id;
        this.type = type;
        this.rarity = rarity;
        this.color = color;
    }

    /** Called once at bootstrap; abilities must be registered in AbilityRegistry by the implementation. */
    protected abstract List<Ability> createAbilities();

    /** Forms selectable on the transformation wheel. */
    protected List<Ability> createTransformations() {
        return List.of();
    }

    final void init() {
        this.abilities = List.copyOf(createAbilities());
        this.transformations = List.copyOf(createTransformations());
    }

    public List<Ability> transformations() {
        return transformations;
    }

    // ------------------------------------------------------------------ passive hooks (server)

    /** Modify (or cancel by returning 0) incoming damage. Called from LivingHurtEvent. */
    public float modifyIncomingDamage(ServerPlayer player, OPPlayerData data, DamageSource source, float amount) {
        return amount;
    }

    /** Return true if this damage should be ignored completely (LivingAttackEvent – no hurt animation). */
    public boolean isImmuneTo(ServerPlayer player, OPPlayerData data, DamageSource source) {
        return false;
    }

    public void onFall(ServerPlayer player, OPPlayerData data, LivingFallEvent event) {}

    /**
     * Logia hook: return true if this body lets the attack pass through. Attacks coated with Armament Haki always
     * connect (checked by CombatEvents before calling this).
     */
    public boolean isIntangibleTo(ServerPlayer player, OPPlayerData data, DamageSource source) {
        return false;
    }

    public void passiveTick(ServerPlayer player, OPPlayerData data) {}

    // ------------------------------------------------------------------ accessors

    public ResourceLocation id() { return id; }
    public FruitType type() { return type; }
    public FruitRarity rarity() { return rarity; }
    public int color() { return color; }
    public List<Ability> abilities() { return abilities; }

    @Nullable
    public Ability abilityAt(int slot) {
        return slot >= 0 && slot < abilities.size() ? abilities.get(slot) : null;
    }

    public int slotOf(Ability ability) {
        return abilities.indexOf(ability);
    }

    public Component displayName() {
        return Component.translatable("fruit." + id.getNamespace() + "." + id.getPath());
    }

    public Component description() {
        return Component.translatable("fruit." + id.getNamespace() + "." + id.getPath() + ".desc");
    }
}
