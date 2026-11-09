package com.onepiece.ultimate.ability;

import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative ability execution: validates inputs, runs the activation life cycle,
 * drains energy, applies cooldowns and interrupts channels when the player is stunned or in sea water.
 */
public final class AbilityManager {

    public enum Result { SUCCESS, NO_ABILITY, LOCKED, COOLDOWN, NO_ENERGY, WEAKENED, STUNNED, BUSY, REFUSED, RELEASED, TOGGLED_OFF }

    // ================================================================== input

    public static void handleInput(ServerPlayer player, int bar, int slot, boolean pressed) {
        OPCapabilities.get(player).ifPresent(data -> {
            Result r = pressed ? press(player, data, bar, slot) : release(player, data, bar, slot);
            feedback(player, r, bar, slot, data);
        });
    }

    /** Resolves the fruit ability currently shown in {@code slot} (taking transformations into account). */
    @Nullable
    public static Ability abilityInSlot(OPPlayerData data, int slot) {
        return abilityInSlot(data, AbilityBars.FRUIT, slot);
    }

    @Nullable
    public static Ability abilityInSlot(OPPlayerData data, int bar, int slot) {
        Ability base = AbilityBars.base(data, bar, slot);
        return base == null ? null : base.resolve(data);
    }

    /** Fruit bar shortcut. */
    public static Result press(ServerPlayer player, OPPlayerData data, int slot) {
        return press(player, data, AbilityBars.FRUIT, slot);
    }

    public static Result press(ServerPlayer player, OPPlayerData data, int bar, int slot) {
        Ability base = AbilityBars.base(data, bar, slot);
        if (base == null) return Result.NO_ABILITY;
        Ability ability = base.resolve(data);
        AbilityRuntime rt = data.abilities;

        // Toggles switch off with the same key, regardless of anything else.
        if (base.activation() == Ability.Activation.TOGGLE && data.isToggleActive(base.id())) {
            endToggle(player, data, base, false);
            return Result.TOGGLED_OFF;
        }
        if (rt.channeling != null) return Result.BUSY;

        Result check = canUse(player, data, base, ability);
        if (check != Result.SUCCESS) return check;

        AbilityContext ctx = AbilityContext.of(player, data);
        if (!ability.canActivate(ctx)) return Result.REFUSED;
        data.consume(ability.resource(), ability.energyCost());
        data.combat.markInCombat(ctx.now());

        switch (ability.activation()) {
            case INSTANT -> {
                ability.onActivate(ctx);
                data.cooldowns().start(base.cooldownKey(), ability.cooldown());
                data.markFull();
            }
            case CHARGE, HOLD -> {
                rt.channeling = ability;
                rt.channelingBase = base;
                rt.channelSlot = slot;
                rt.channelBar = bar;
                rt.channelTicks = 0;
                data.setChanneling(ability.id(), 0);
                ability.onActivate(ctx);
            }
            case TOGGLE -> {
                data.setToggle(base.id(), true);
                rt.toggleTicks.put(base.id(), 0);
                ability.onActivate(ctx);
            }
        }
        return Result.SUCCESS;
    }

    public static Result release(ServerPlayer player, OPPlayerData data, int slot) {
        return release(player, data, AbilityBars.FRUIT, slot);
    }

    public static Result release(ServerPlayer player, OPPlayerData data, int bar, int slot) {
        AbilityRuntime rt = data.abilities;
        if (rt.channeling == null || rt.channelSlot != slot || rt.channelBar != bar) return Result.NO_ABILITY;
        finishChannel(player, data, false);
        return Result.RELEASED;
    }

    private static Result canUse(ServerPlayer player, OPPlayerData data, Ability base, Ability ability) {
        if (!ability.isUnlocked(data)) return Result.LOCKED;
        if (data.seaWeakened()) return Result.WEAKENED;
        if (player.hasEffect(ModEffects.STUNNED.get())) return Result.STUNNED;
        if (data.cooldowns().isCooling(base.cooldownKey())) return Result.COOLDOWN;
        if (data.resource(ability.resource()) < ability.energyCost()) return Result.NO_ENERGY;
        return Result.SUCCESS;
    }

    // ================================================================== tick

    public static void tick(ServerPlayer player, OPPlayerData data) {
        AbilityRuntime rt = data.abilities;
        AbilityContext ctx = AbilityContext.of(player, data);
        rt.tasks.tick(ctx.now());

        boolean interrupted = data.seaWeakened() || player.hasEffect(ModEffects.STUNNED.get()) || !player.isAlive();

        // ---- channel (charge / hold)
        if (rt.channeling != null) {
            Ability a = rt.channeling;
            if (interrupted) {
                finishChannel(player, data, true);
            } else {
                rt.channelTicks++;
                if (a.activation() == Ability.Activation.CHARGE) {
                    int t = Math.min(rt.channelTicks, a.maxCharge());
                    data.setChanneling(a.id(), t);
                    a.onChargeTick(ctx, t);
                    // Holding a full charge for too long fires it automatically.
                    if (rt.channelTicks >= a.maxCharge() + 40) finishChannel(player, data, false);
                } else {
                    data.setChanneling(a.id(), rt.channelTicks);
                    if (!data.consume(a.resource(), a.drainPerTick(data)) || rt.channelTicks >= a.maxHold()) {
                        finishChannel(player, data, false);
                    } else {
                        a.onHoldTick(ctx, rt.channelTicks);
                    }
                }
            }
        }

        // ---- toggles (copy: ending a toggle mutates the set)
        if (!data.activeToggles().isEmpty()) {
            List<ResourceLocation> active = new ArrayList<>(data.activeToggles());
            for (ResourceLocation id : active) {
                Ability a = AbilityRegistry.get(id);
                if (a == null) {
                    data.setToggle(id, false);
                    continue;
                }
                if (data.seaWeakened() || !player.isAlive() || !data.consume(a.resource(), a.drainPerTick(data))) {
                    endToggle(player, data, a, true);
                    continue;
                }
                int t = rt.toggleTicks.merge(id, 1, Integer::sum);
                a.onToggleTick(ctx, t);
            }
        }
    }

    private static void finishChannel(ServerPlayer player, OPPlayerData data, boolean interrupted) {
        AbilityRuntime rt = data.abilities;
        Ability a = rt.channeling;
        Ability base = rt.channelingBase;
        int ticks = rt.channelTicks;
        rt.clearChannel();
        data.setChanneling(null, 0);
        if (a == null || base == null) return;
        AbilityContext ctx = AbilityContext.of(player, data);

        if (a.activation() == Ability.Activation.CHARGE) {
            if (interrupted || ticks < a.minCharge()) {
                a.onEnd(ctx, true);
                data.cooldowns().start(base.cooldownKey(), Math.max(10, a.cooldown() / 4));
            } else {
                a.onRelease(ctx, Math.min(ticks, a.maxCharge()));
                data.cooldowns().start(base.cooldownKey(), a.cooldown());
            }
        } else {
            a.onEnd(ctx, interrupted);
            data.cooldowns().start(base.cooldownKey(), a.cooldown());
        }
        data.markFull();
    }

    public static void endToggle(ServerPlayer player, OPPlayerData data, Ability toggle, boolean forced) {
        if (!data.isToggleActive(toggle.id())) return;
        data.setToggle(toggle.id(), false);
        data.abilities.toggleTicks.remove(toggle.id());
        toggle.onEnd(AbilityContext.of(player, data), forced);
        data.cooldowns().start(toggle.cooldownKey(), toggle.cooldown());
        data.markFull();
    }

    /** Stops everything (death, fruit change, sea water). */
    public static void cancelAll(ServerPlayer player, OPPlayerData data) {
        if (data.abilities.channeling != null) finishChannel(player, data, true);
        for (ResourceLocation id : new ArrayList<>(data.activeToggles())) {
            Ability a = AbilityRegistry.get(id);
            if (a != null) endToggle(player, data, a, true);
            else data.setToggle(id, false);
        }
        data.abilities.reset();
    }

    // ================================================================== feedback

    private static void feedback(ServerPlayer player, Result r, int bar, int slot, OPPlayerData data) {
        Component msg = switch (r) {
            case LOCKED -> {
                Ability a = abilityInSlot(data, bar, slot);
                yield a == null ? null : a.lockedMessage().copy().withStyle(ChatFormatting.GRAY);
            }
            case NO_ENERGY -> {
                Ability a = abilityInSlot(data, bar, slot);
                yield Component.translatable(a != null && a.resource() == AbilityResource.HAKI ? "message.onepiece.no_haki" : "message.onepiece.no_energy")
                        .withStyle(ChatFormatting.GOLD);
            }
            case WEAKENED -> Component.translatable("message.onepiece.weakened").withStyle(ChatFormatting.AQUA);
            default -> null;
        };
        if (msg != null) player.displayClientMessage(msg, true);
    }

    private AbilityManager() {}
}
