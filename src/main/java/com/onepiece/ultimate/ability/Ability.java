package com.onepiece.ultimate.ability;

import com.onepiece.ultimate.OnePieceMod;
import com.onepiece.ultimate.data.OPPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Base class of every active ability.
 * <p>
 * The framework ({@link AbilityManager}) owns the rules shared by all abilities – unlocking, cooldowns,
 * energy, sea-water weakness, stun interruption and the activation life cycle. Subclasses only implement
 * what makes them unique through the hooks below (all server side).
 *
 * <table>
 *   <tr><th>Activation</th><th>Life cycle</th></tr>
 *   <tr><td>INSTANT</td><td>{@link #onActivate}</td></tr>
 *   <tr><td>CHARGE</td><td>{@link #onActivate} → {@link #onChargeTick}… → {@link #onRelease}</td></tr>
 *   <tr><td>HOLD</td><td>{@link #onActivate} → {@link #onHoldTick}… → {@link #onEnd}</td></tr>
 *   <tr><td>TOGGLE</td><td>{@link #onActivate} → {@link #onToggleTick}… → {@link #onEnd}</td></tr>
 * </table>
 */
public abstract class Ability {
    public enum Activation { INSTANT, CHARGE, HOLD, TOGGLE }

    private final ResourceLocation id;
    private final Properties props;

    protected Ability(String name, Properties props) {
        this.id = OnePieceMod.id(name);
        this.props = props;
    }

    // ------------------------------------------------------------------ hooks

    /** Return false to refuse activation for an ability specific reason (target missing, etc.). */
    public boolean canActivate(AbilityContext ctx) {
        return true;
    }

    public abstract void onActivate(AbilityContext ctx);

    public void onChargeTick(AbilityContext ctx, int ticks) {}

    /** CHARGE only. {@code chargeTicks} is clamped to {@link #maxCharge()}. */
    public void onRelease(AbilityContext ctx, int chargeTicks) {}

    public void onHoldTick(AbilityContext ctx, int ticks) {}

    public void onToggleTick(AbilityContext ctx, int ticks) {}

    /** HOLD and TOGGLE end; also called when a CHARGE/HOLD is interrupted. */
    public void onEnd(AbilityContext ctx, boolean interrupted) {}

    /**
     * Abilities can turn into stronger variants under a transformation (e.g. Pistol → Jet Pistol in Gear Second).
     * The variant shares the base ability's slot and cooldown.
     */
    public Ability resolve(OPPlayerData data) {
        return this;
    }

    /** Outgoing damage multiplier while this TOGGLE ability is active. */
    public float damageMultiplier(OPPlayerData data) {
        return 1f;
    }

    /** Incoming damage multiplier while this TOGGLE ability is active. */
    public float defenseMultiplier(OPPlayerData data) {
        return 1f;
    }

    /** Whether the player has unlocked this ability. Fruit abilities unlock with mastery. */
    public boolean isUnlocked(OPPlayerData data) {
        return data.mastery() >= props.mastery;
    }

    /** Shown when trying to use a locked ability. */
    public Component lockedMessage() {
        return Component.translatable("message.onepiece.locked", displayName(), props.mastery);
    }

    /** Short requirement label for the HUD lock overlay ("M45", "B6"). */
    public String requirementLabel() {
        return "M" + props.mastery;
    }

    // ------------------------------------------------------------------ properties

    public ResourceLocation id() { return id; }
    public Activation activation() { return props.activation; }
    public int cooldown() { return props.cooldown; }
    public float energyCost() { return props.cost; }
    public AbilityResource resource() { return props.resource; }
    public float drainPerTick() { return props.drain; }
    /** Drain may depend on the player (e.g. haki gets cheaper with training). */
    public float drainPerTick(OPPlayerData data) { return drainPerTick(); }
    public int requiredMastery() { return props.mastery; }
    public int minCharge() { return props.minCharge; }
    public int maxCharge() { return props.maxCharge; }
    public int maxHold() { return props.maxHold; }
    /** Looping pose while charging/holding (client animation id, or null). */
    @Nullable public String channelAnimation() { return props.channelAnimation; }
    /** Abilities sharing a key share their cooldown. Defaults to the ability id. */
    public ResourceLocation cooldownKey() { return props.cooldownKey != null ? props.cooldownKey : id; }
    public int color() { return props.color; }

    public ResourceLocation icon() {
        return OnePieceMod.id("textures/gui/ability/" + (props.icon != null ? props.icon : id.getPath()) + ".png");
    }

    public Component displayName() {
        return Component.translatable("ability." + id.getNamespace() + "." + id.getPath());
    }

    public Component description() {
        return Component.translatable("ability." + id.getNamespace() + "." + id.getPath() + ".desc");
    }

    @Override
    public String toString() {
        return id.toString();
    }

    // ------------------------------------------------------------------ builder

    public static final class Properties {
        private Activation activation = Activation.INSTANT;
        private int cooldown = 20;
        private float cost;
        private float drain;
        private int mastery;
        private int minCharge = 1;
        private int maxCharge = 20;
        private int maxHold = 100;
        private String channelAnimation;
        private ResourceLocation cooldownKey;
        private String icon;
        private int color = 0xFFFFFF;
        private AbilityResource resource = AbilityResource.ENERGY;

        public static Properties of(Activation a) {
            Properties p = new Properties();
            p.activation = a;
            return p;
        }

        public Properties cooldown(int ticks) { this.cooldown = ticks; return this; }
        public Properties cost(float energy) { this.cost = energy; return this; }
        public Properties drain(float perTick) { this.drain = perTick; return this; }
        public Properties mastery(int required) { this.mastery = required; return this; }
        public Properties charge(int min, int max) { this.minCharge = min; this.maxCharge = max; return this; }
        public Properties maxHold(int ticks) { this.maxHold = ticks; return this; }
        public Properties channelAnimation(String anim) { this.channelAnimation = anim; return this; }
        public Properties sharesCooldownWith(ResourceLocation key) { this.cooldownKey = key; return this; }
        public Properties icon(String icon) { this.icon = icon; return this; }
        public Properties color(int rgb) { this.color = rgb; return this; }
        public Properties resource(AbilityResource r) { this.resource = r; return this; }
    }
}
