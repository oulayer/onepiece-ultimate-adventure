package com.onepiece.ultimate.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.onepiece.ultimate.haki.HakiData;
import com.onepiece.ultimate.haki.HakiManager;
import com.onepiece.ultimate.haki.HakiType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.onepiece.ultimate.ability.AbilityManager;
import com.onepiece.ultimate.data.OPCapabilities;
import com.onepiece.ultimate.data.OPPlayerData;
import com.onepiece.ultimate.data.PlayerStat;
import com.onepiece.ultimate.data.Progression;
import com.onepiece.ultimate.destruction.DestructionManager;
import com.onepiece.ultimate.fruit.DevilFruit;
import com.onepiece.ultimate.fruit.DevilFruits;
import com.onepiece.ultimate.progression.ProgressionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * {@code /onepiece} – administration and testing.
 * <pre>
 * /onepiece fruit set &lt;fruit&gt; [players]     /onepiece fruit remove [players]
 * /onepiece mastery set &lt;0-100&gt; [players]   /onepiece level set &lt;n&gt; [players]
 * /onepiece xp add &lt;n&gt; [players]            /onepiece points add &lt;n&gt; [players]
 * /onepiece info [player]                    /onepiece reset [players]
 * /onepiece destruction pending
 * </pre>
 */
public final class OnePieceCommand {
    private static final SimpleCommandExceptionType NO_DATA = new SimpleCommandExceptionType(Component.literal("Player has no One Piece data"));
    private static final SimpleCommandExceptionType UNKNOWN_FRUIT = new SimpleCommandExceptionType(Component.translatable("command.onepiece.unknown_fruit"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("onepiece").requires(s -> s.hasPermission(2))
                .then(Commands.literal("fruit")
                        .then(Commands.literal("set")
                                .then(Commands.argument("fruit", ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(DevilFruits.all().stream().map(DevilFruit::id), b))
                                        .executes(c -> setFruit(c, self(c)))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(c -> setFruit(c, EntityArgument.getPlayers(c, "targets"))))))
                        .then(Commands.literal("remove")
                                .executes(c -> removeFruit(c, self(c)))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> removeFruit(c, EntityArgument.getPlayers(c, "targets"))))))
                .then(Commands.literal("mastery").then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, OPPlayerData.MAX_MASTERY))
                                .executes(c -> setMastery(c, self(c)))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> setMastery(c, EntityArgument.getPlayers(c, "targets")))))))
                .then(Commands.literal("level").then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 1000))
                                .executes(c -> setLevel(c, self(c)))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> setLevel(c, EntityArgument.getPlayers(c, "targets")))))))
                .then(Commands.literal("xp").then(Commands.literal("add")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                .executes(c -> addXp(c, self(c)))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> addXp(c, EntityArgument.getPlayers(c, "targets")))))))
                .then(Commands.literal("points").then(Commands.literal("add")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10000))
                                .executes(c -> addPoints(c, self(c)))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> addPoints(c, EntityArgument.getPlayers(c, "targets")))))))
                .then(Commands.literal("haki")
                        .then(Commands.literal("level").then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(HakiType.values()).map(t -> t.name().toLowerCase()), b))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, HakiData.MAX_LEVEL))
                                        .executes(c -> hakiLevel(c, self(c)))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(c -> hakiLevel(c, EntityArgument.getPlayers(c, "targets")))))))
                        .then(Commands.literal("potential").then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(c -> hakiPotential(c, self(c)))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> hakiPotential(c, EntityArgument.getPlayers(c, "targets"))))))
                        .then(Commands.literal("refill")
                                .executes(c -> {
                                    for (ServerPlayer p : self(c)) { OPPlayerData d = data(p); d.setHakiGauge(d.maxHaki()); d.setEnergy(d.maxEnergy()); }
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(c -> reset(c, self(c)))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> reset(c, EntityArgument.getPlayers(c, "targets")))))
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "target")))))
                .then(Commands.literal("destruction").then(Commands.literal("pending").executes(c -> {
                    int n = DestructionManager.pending(c.getSource().getLevel());
                    c.getSource().sendSuccess(() -> Component.literal("Pending destruction operations: " + n), false);
                    return n;
                }))));
    }

    private static Collection<ServerPlayer> self(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return List.of(c.getSource().getPlayerOrException());
    }

    private static OPPlayerData data(ServerPlayer p) throws CommandSyntaxException {
        OPPlayerData d = OPCapabilities.getOrNull(p);
        if (d == null) throw NO_DATA.create();
        return d;
    }

    private static int setFruit(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(c, "fruit");
        DevilFruit fruit = DevilFruits.get(id);
        if (fruit == null) throw UNKNOWN_FRUIT.create();
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            AbilityManager.cancelAll(p, d);
            d.setFruit(id);
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.onepiece.fruit_set", fruit.displayName(), players.size()), true);
        return players.size();
    }

    private static int removeFruit(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            AbilityManager.cancelAll(p, d);
            d.setFruit(null);
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.onepiece.fruit_removed", players.size()), true);
        return players.size();
    }

    private static int setMastery(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        int value = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) data(p).setMasteryXp(OPPlayerData.totalXpForMastery(value));
        c.getSource().sendSuccess(() -> Component.translatable("command.onepiece.mastery_set", value, players.size()), true);
        return players.size();
    }

    private static int setLevel(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        int value = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            Progression pr = d.progression();
            int gained = Math.max(0, value - pr.level());
            pr.setLevel(value);
            pr.setXp(0);
            pr.setUnspentPoints(pr.unspentPoints() + gained * com.onepiece.ultimate.config.OPConfig.POINTS_PER_LEVEL.get());
            d.markFull();
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.onepiece.level_set", value, players.size()), true);
        return players.size();
    }

    private static int addXp(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) {
        int value = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) ProgressionManager.awardXp(p, value);
        c.getSource().sendSuccess(() -> Component.literal("+" + value + " XP"), true);
        return players.size();
    }

    private static int addPoints(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        int value = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            d.progression().setUnspentPoints(d.progression().unspentPoints() + value);
            d.markFull();
        }
        c.getSource().sendSuccess(() -> Component.literal("+" + value + " points"), true);
        return players.size();
    }

    private static HakiType hakiType(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = StringArgumentType.getString(c, "type");
        for (HakiType t : HakiType.values()) if (t.name().equalsIgnoreCase(name)) return t;
        throw new SimpleCommandExceptionType(Component.literal("Unknown haki type: " + name)).create();
    }

    private static int hakiLevel(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        HakiType type = hakiType(c);
        int value = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            if (value <= 0) {
                d.haki().setAwakened(type, false);
                for (var a : com.onepiece.ultimate.haki.HakiAbilities.all())
                    if (a instanceof com.onepiece.ultimate.haki.HakiAbility ha && ha.type() == type) AbilityManager.endToggle(p, d, a, true);
            } else {
                if (!d.haki().isAwakened(type)) HakiManager.awaken(p, d, type);
                d.haki().setXp(type, HakiData.totalXpFor(value));
            }
            d.markFull();
            d.markVisual();
        }
        c.getSource().sendSuccess(() -> Component.literal(type.name().toLowerCase() + " haki level " + value + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int hakiPotential(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        boolean v = BoolArgumentType.getBool(c, "value");
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            d.haki().setConquerorPotential(v);
            d.markFull();
            HakiManager.checkAwakenings(p, d);
        }
        c.getSource().sendSuccess(() -> Component.literal("Conqueror's potential = " + v + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
        for (ServerPlayer p : players) {
            OPPlayerData d = data(p);
            AbilityManager.cancelAll(p, d);
            d.setFruit(null);
            d.cooldowns().clear();
            for (HakiType t : HakiType.values()) d.haki().setAwakened(t, false);
            ProgressionManager.reset(p, d);
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.onepiece.reset", players.size()), true);
        return players.size();
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        OPPlayerData d = data(p);
        Progression pr = d.progression();
        DevilFruit fruit = d.fruit();
        StringBuilder sb = new StringBuilder();
        sb.append(p.getName().getString()).append(": Lv ").append(pr.level())
                .append(" (").append(pr.xp()).append('/').append(Progression.xpForNext(pr.level())).append(" xp), points ").append(pr.unspentPoints());
        for (PlayerStat s : PlayerStat.values()) sb.append(", ").append(s.name()).append(' ').append(pr.get(s));
        sb.append(" | fruit ").append(fruit == null ? "none" : fruit.id().toString()).append(" mastery ").append(d.mastery())
                .append(" | energy ").append(Math.round(d.energy())).append('/').append(Math.round(d.maxEnergy()));
        sb.append(" | haki");
        for (HakiType t : HakiType.values()) sb.append(' ').append(t.name().toLowerCase()).append(' ').append(d.hakiLevel(t));
        sb.append(" potential ").append(d.haki().hasConquerorPotential()).append(" gauge ").append(Math.round(d.hakiGauge()));
        String text = sb.toString();
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private OnePieceCommand() {}
}
