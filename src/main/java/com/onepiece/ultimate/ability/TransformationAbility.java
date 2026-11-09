package com.onepiece.ultimate.ability;

import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A transformation: a TOGGLE ability that changes what the player <em>is</em> while active.
 * <ul>
 *   <li><b>Exclusive group</b> – activating one ends the others of the same group (one Gear at a time).</li>
 *   <li><b>Activation</b> – a cinematic window during which the user can't act but can't be hurt either.</li>
 *   <li><b>Variants</b> – slot abilities are replaced by stronger/different versions (Pistol → Kong Gun).</li>
 *   <li><b>Body</b> – attribute modifiers, hitbox size, air jumps / flight, incoming-damage changes.</li>
 *   <li><b>Aftermath</b> – {@link #deactivate} applies the cost of the form (exhaustion etc.).</li>
 * </ul>
 * Rendering is attached on the client by transformation id (TransformationVisuals).
 */
public abstract class TransformationAbility extends Ability {
    private record AttrMod(Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {}

    private static final List<TransformationAbility> ALL = new ArrayList<>();

    private final String group;
    private final int activationTicks;
    private final Map<ResourceLocation, Ability> variants = new HashMap<>();
    private final List<AttrMod> modifiers = new ArrayList<>();

    protected TransformationAbility(String name, String group, int activationTicks, Properties props) {
        super(name, props);
        this.group = group;
        this.activationTicks = activationTicks;
        ALL.add(this);
    }

    // ------------------------------------------------------------------ configuration

    /** While active, {@code base} (a slot ability) is replaced by {@code variant}. */
    public TransformationAbility variant(Ability base, Ability variant) {
        variants.put(base.id(), variant);
        return this;
    }

    protected void modifier(Attribute attribute, String uuid, double amount, AttributeModifier.Operation op) {
        modifiers.add(new AttrMod(attribute, UUID.fromString(uuid), "onepiece " + id().getPath(), amount, op));
    }

    @Nullable
    public Ability variantOf(Ability base) {
        return variants.get(base.id());
    }

    public String group() {
        return group;
    }

    public int activationTicks() {
        return activationTicks;
    }

    // ------------------------------------------------------------------ hooks for subclasses

    protected void activate(AbilityContext ctx) {}

    /** Ticks of the activation sequence (1..activationTicks). */
    protected void tickActivation(AbilityContext ctx, int ticks) {}

    /** Ticks after the activation sequence. */
    protected void tickActive(AbilityContext ctx, int ticks) {}

    protected void deactivate(AbilityContext ctx, boolean interrupted) {}

    /** Jump pressed in mid-air. Return true if handled (bounce, wing flap…). */
    public boolean onAirJump(AbilityContext ctx) {
        return false;
    }

    /** Hitbox while transformed, or null to keep the normal one. */
    @Nullable
    public EntityDimensions dimensions() {
        return null;
    }

    /** Whether the form flies (creative-style flight is granted while active). */
    public boolean grantsFlight() {
        return false;
    }

    /** Whether fall damage is ignored while transformed. */
    public boolean ignoresFall() {
        return false;
    }

    // ------------------------------------------------------------------ life cycle (final: shared rules)

    @Override
    public final void onActivate(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        OPPlayerData data = ctx.data();
        for (ResourceLocation id : new ArrayList<>(data.activeToggles())) {
            if (AbilityRegistry.get(id) instanceof TransformationAbility other && other != this && other.group.equals(group)) {
                AbilityManager.endToggle(player, data, other, false);
            }
        }
        data.abilities.transformLockUntil = ctx.now() + activationTicks;
        for (AttrMod m : modifiers) {
            AttributeInstance inst = player.getAttribute(m.attribute);
            if (inst != null && inst.getModifier(m.id) == null) inst.addTransientModifier(new AttributeModifier(m.id, m.name, m.amount, m.op));
        }
        if (grantsFlight()) setFlight(player, true);
        player.refreshDimensions();
        activate(ctx);
    }

    @Override
    public final void onToggleTick(AbilityContext ctx, int ticks) {
        if (ticks <= activationTicks) tickActivation(ctx, ticks);
        else tickActive(ctx, ticks - activationTicks);
    }

    @Override
    public final void onEnd(AbilityContext ctx, boolean interrupted) {
        ServerPlayer player = ctx.player();
        removeModifiers(player);
        if (grantsFlight()) setFlight(player, false);
        player.refreshDimensions();
        ctx.data().abilities.transformLockUntil = 0;
        deactivate(ctx, interrupted);
    }

    private void removeModifiers(ServerPlayer player) {
        for (AttrMod m : modifiers) {
            AttributeInstance inst = player.getAttribute(m.attribute);
            if (inst != null) inst.removeModifier(m.id);
        }
    }

    private static void setFlight(ServerPlayer player, boolean on) {
        if (player.isCreative() || player.isSpectator()) return;
        player.getAbilities().mayfly = on;
        player.getAbilities().flying = on;
        if (!on) player.getAbilities().setFlyingSpeed(0.05f);
        player.onUpdateAbilities();
    }

    /** Removes every transformation modifier/flight (login, respawn – toggles are not persisted). */
    public static void cleanup(ServerPlayer player) {
        for (TransformationAbility t : ALL) {
            t.removeModifiers(player);
            if (t.grantsFlight()) setFlight(player, false);
        }
        player.refreshDimensions();
    }

    /** The active transformation of the player, if any. */
    @Nullable
    public static TransformationAbility active(OPPlayerData data) {
        for (ResourceLocation id : data.activeToggles()) {
            if (AbilityRegistry.get(id) instanceof TransformationAbility t) return t;
        }
        return null;
    }
}
