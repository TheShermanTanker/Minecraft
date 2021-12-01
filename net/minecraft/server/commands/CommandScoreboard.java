package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentMathOperation;
import net.minecraft.commands.arguments.ArgumentScoreboardCriteria;
import net.minecraft.commands.arguments.ArgumentScoreboardObjective;
import net.minecraft.commands.arguments.ArgumentScoreboardSlot;
import net.minecraft.commands.arguments.ArgumentScoreholder;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class CommandScoreboard {
    private static final SimpleCommandExceptionType ERROR_OBJECTIVE_ALREADY_EXISTS = new SimpleCommandExceptionType(new ChatMessage("commands.scoreboard.objectives.add.duplicate"));
    private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_EMPTY = new SimpleCommandExceptionType(new ChatMessage("commands.scoreboard.objectives.display.alreadyEmpty"));
    private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_SET = new SimpleCommandExceptionType(new ChatMessage("commands.scoreboard.objectives.display.alreadySet"));
    private static final SimpleCommandExceptionType ERROR_TRIGGER_ALREADY_ENABLED = new SimpleCommandExceptionType(new ChatMessage("commands.scoreboard.players.enable.failed"));
    private static final SimpleCommandExceptionType ERROR_NOT_TRIGGER = new SimpleCommandExceptionType(new ChatMessage("commands.scoreboard.players.enable.invalid"));
    private static final Dynamic2CommandExceptionType ERROR_NO_VALUE = new Dynamic2CommandExceptionType((objective, target) -> {
        return new ChatMessage("commands.scoreboard.players.get.null", objective, target);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("scoreboard").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("objectives").then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listObjectives(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("objective", StringArgumentType.word()).then(net.minecraft.commands.CommandDispatcher.argument("criteria", ArgumentScoreboardCriteria.criteria()).executes((context) -> {
            return addObjective(context.getSource(), StringArgumentType.getString(context, "objective"), ArgumentScoreboardCriteria.getCriteria(context, "criteria"), new ChatComponentText(StringArgumentType.getString(context, "objective")));
        }).then(net.minecraft.commands.CommandDispatcher.argument("displayName", ArgumentChatComponent.textComponent()).executes((context) -> {
            return addObjective(context.getSource(), StringArgumentType.getString(context, "objective"), ArgumentScoreboardCriteria.getCriteria(context, "criteria"), ArgumentChatComponent.getComponent(context, "displayName"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("modify").then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).then(net.minecraft.commands.CommandDispatcher.literal("displayname").then(net.minecraft.commands.CommandDispatcher.argument("displayName", ArgumentChatComponent.textComponent()).executes((context) -> {
            return setDisplayName(context.getSource(), ArgumentScoreboardObjective.getObjective(context, "objective"), ArgumentChatComponent.getComponent(context, "displayName"));
        }))).then(createRenderTypeModify()))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).executes((context) -> {
            return removeObjective(context.getSource(), ArgumentScoreboardObjective.getObjective(context, "objective"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("setdisplay").then(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentScoreboardSlot.displaySlot()).executes((context) -> {
            return clearDisplaySlot(context.getSource(), ArgumentScoreboardSlot.getDisplaySlot(context, "slot"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).executes((context) -> {
            return setDisplaySlot(context.getSource(), ArgumentScoreboardSlot.getDisplaySlot(context, "slot"), ArgumentScoreboardObjective.getObjective(context, "objective"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("players").then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listTrackedPlayers(context.getSource());
        }).then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return listTrackedPlayerScores(context.getSource(), ArgumentScoreholder.getName(context, "target"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).then(net.minecraft.commands.CommandDispatcher.argument("score", IntegerArgumentType.integer()).executes((context) -> {
            return setScore(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("get").then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).executes((context) -> {
            return getScore(context.getSource(), ArgumentScoreholder.getName(context, "target"), ArgumentScoreboardObjective.getObjective(context, "objective"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).then(net.minecraft.commands.CommandDispatcher.argument("score", IntegerArgumentType.integer(0)).executes((context) -> {
            return addScore(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).then(net.minecraft.commands.CommandDispatcher.argument("score", IntegerArgumentType.integer(0)).executes((context) -> {
            return removeScore(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("reset").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return resetScores(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).executes((context) -> {
            return resetScore(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getObjective(context, "objective"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("enable").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).suggests((context, builder) -> {
            return suggestTriggers(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), builder);
        }).executes((context) -> {
            return enableTrigger(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getObjective(context, "objective"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("operation").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("targetObjective", ArgumentScoreboardObjective.objective()).then(net.minecraft.commands.CommandDispatcher.argument("operation", ArgumentMathOperation.operation()).then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("sourceObjective", ArgumentScoreboardObjective.objective()).executes((context) -> {
            return performOperation(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getWritableObjective(context, "targetObjective"), ArgumentMathOperation.getOperation(context, "operation"), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "source"), ArgumentScoreboardObjective.getObjective(context, "sourceObjective"));
        })))))))));
    }

    private static LiteralArgumentBuilder<CommandListenerWrapper> createRenderTypeModify() {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("rendertype");

        for(IScoreboardCriteria.EnumScoreboardHealthDisplay renderType : IScoreboardCriteria.EnumScoreboardHealthDisplay.values()) {
            literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(renderType.getId()).executes((context) -> {
                return setRenderType(context.getSource(), ArgumentScoreboardObjective.getObjective(context, "objective"), renderType);
            }));
        }

        return literalArgumentBuilder;
    }

    private static CompletableFuture<Suggestions> suggestTriggers(CommandListenerWrapper source, Collection<String> targets, SuggestionsBuilder builder) {
        List<String> list = Lists.newArrayList();
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(ScoreboardObjective objective : scoreboard.getObjectives()) {
            if (objective.getCriteria() == IScoreboardCriteria.TRIGGER) {
                boolean bl = false;

                for(String string : targets) {
                    if (!scoreboard.hasPlayerScore(string, objective) || scoreboard.getPlayerScoreForObjective(string, objective).isLocked()) {
                        bl = true;
                        break;
                    }
                }

                if (bl) {
                    list.add(objective.getName());
                }
            }
        }

        return ICompletionProvider.suggest(list, builder);
    }

    private static int getScore(CommandListenerWrapper source, String target, ScoreboardObjective objective) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (!scoreboard.hasPlayerScore(target, objective)) {
            throw ERROR_NO_VALUE.create(objective.getName(), target);
        } else {
            ScoreboardScore score = scoreboard.getPlayerScoreForObjective(target, objective);
            source.sendMessage(new ChatMessage("commands.scoreboard.players.get.success", target, score.getScore(), objective.getFormattedDisplayName()), false);
            return score.getScore();
        }
    }

    private static int performOperation(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective targetObjective, ArgumentMathOperation.Operation operation, Collection<String> sources, ScoreboardObjective sourceObjectives) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int i = 0;

        for(String string : targets) {
            ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, targetObjective);

            for(String string2 : sources) {
                ScoreboardScore score2 = scoreboard.getPlayerScoreForObjective(string2, sourceObjectives);
                operation.apply(score, score2);
            }

            i += score.getScore();
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.operation.success.single", targetObjective.getFormattedDisplayName(), targets.iterator().next(), i), true);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.operation.success.multiple", targetObjective.getFormattedDisplayName(), targets.size()), true);
        }

        return i;
    }

    private static int enableTrigger(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective objective) throws CommandSyntaxException {
        if (objective.getCriteria() != IScoreboardCriteria.TRIGGER) {
            throw ERROR_NOT_TRIGGER.create();
        } else {
            Scoreboard scoreboard = source.getServer().getScoreboard();
            int i = 0;

            for(String string : targets) {
                ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, objective);
                if (score.isLocked()) {
                    score.setLocked(false);
                    ++i;
                }
            }

            if (i == 0) {
                throw ERROR_TRIGGER_ALREADY_ENABLED.create();
            } else {
                if (targets.size() == 1) {
                    source.sendMessage(new ChatMessage("commands.scoreboard.players.enable.success.single", objective.getFormattedDisplayName(), targets.iterator().next()), true);
                } else {
                    source.sendMessage(new ChatMessage("commands.scoreboard.players.enable.success.multiple", objective.getFormattedDisplayName(), targets.size()), true);
                }

                return i;
            }
        }
    }

    private static int resetScores(CommandListenerWrapper source, Collection<String> targets) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : targets) {
            scoreboard.resetPlayerScores(string, (ScoreboardObjective)null);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.reset.all.single", targets.iterator().next()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.reset.all.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int resetScore(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective objective) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : targets) {
            scoreboard.resetPlayerScores(string, objective);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.reset.specific.single", objective.getFormattedDisplayName(), targets.iterator().next()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.reset.specific.multiple", objective.getFormattedDisplayName(), targets.size()), true);
        }

        return targets.size();
    }

    private static int setScore(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective objective, int score) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : targets) {
            ScoreboardScore score2 = scoreboard.getPlayerScoreForObjective(string, objective);
            score2.setScore(score);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.set.success.single", objective.getFormattedDisplayName(), targets.iterator().next(), score), true);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.set.success.multiple", objective.getFormattedDisplayName(), targets.size(), score), true);
        }

        return score * targets.size();
    }

    private static int addScore(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective objective, int score) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int i = 0;

        for(String string : targets) {
            ScoreboardScore score2 = scoreboard.getPlayerScoreForObjective(string, objective);
            score2.setScore(score2.getScore() + score);
            i += score2.getScore();
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.add.success.single", score, objective.getFormattedDisplayName(), targets.iterator().next(), i), true);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.add.success.multiple", score, objective.getFormattedDisplayName(), targets.size()), true);
        }

        return i;
    }

    private static int removeScore(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective objective, int score) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int i = 0;

        for(String string : targets) {
            ScoreboardScore score2 = scoreboard.getPlayerScoreForObjective(string, objective);
            score2.setScore(score2.getScore() - score);
            i += score2.getScore();
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.remove.success.single", score, objective.getFormattedDisplayName(), targets.iterator().next(), i), true);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.remove.success.multiple", score, objective.getFormattedDisplayName(), targets.size()), true);
        }

        return i;
    }

    private static int listTrackedPlayers(CommandListenerWrapper source) {
        Collection<String> collection = source.getServer().getScoreboard().getPlayers();
        if (collection.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.list.empty"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.list.success", collection.size(), ChatComponentUtils.formatList(collection)), false);
        }

        return collection.size();
    }

    private static int listTrackedPlayerScores(CommandListenerWrapper source, String target) {
        Map<ScoreboardObjective, ScoreboardScore> map = source.getServer().getScoreboard().getPlayerObjectives(target);
        if (map.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.list.entity.empty", target), false);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.players.list.entity.success", target, map.size()), false);

            for(Entry<ScoreboardObjective, ScoreboardScore> entry : map.entrySet()) {
                source.sendMessage(new ChatMessage("commands.scoreboard.players.list.entity.entry", entry.getKey().getFormattedDisplayName(), entry.getValue().getScore()), false);
            }
        }

        return map.size();
    }

    private static int clearDisplaySlot(CommandListenerWrapper source, int slot) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getObjectiveForSlot(slot) == null) {
            throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
        } else {
            scoreboard.setDisplaySlot(slot, (ScoreboardObjective)null);
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.display.cleared", Scoreboard.getDisplaySlotNames()[slot]), true);
            return 0;
        }
    }

    private static int setDisplaySlot(CommandListenerWrapper source, int slot, ScoreboardObjective objective) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getObjectiveForSlot(slot) == objective) {
            throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
        } else {
            scoreboard.setDisplaySlot(slot, objective);
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.display.set", Scoreboard.getDisplaySlotNames()[slot], objective.getDisplayName()), true);
            return 0;
        }
    }

    private static int setDisplayName(CommandListenerWrapper source, ScoreboardObjective objective, IChatBaseComponent displayName) {
        if (!objective.getDisplayName().equals(displayName)) {
            objective.setDisplayName(displayName);
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.modify.displayname", objective.getName(), objective.getFormattedDisplayName()), true);
        }

        return 0;
    }

    private static int setRenderType(CommandListenerWrapper source, ScoreboardObjective objective, IScoreboardCriteria.EnumScoreboardHealthDisplay type) {
        if (objective.getRenderType() != type) {
            objective.setRenderType(type);
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.modify.rendertype", objective.getFormattedDisplayName()), true);
        }

        return 0;
    }

    private static int removeObjective(CommandListenerWrapper source, ScoreboardObjective objective) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        scoreboard.unregisterObjective(objective);
        source.sendMessage(new ChatMessage("commands.scoreboard.objectives.remove.success", objective.getFormattedDisplayName()), true);
        return scoreboard.getObjectives().size();
    }

    private static int addObjective(CommandListenerWrapper source, String objective, IScoreboardCriteria criteria, IChatBaseComponent displayName) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getObjective(objective) != null) {
            throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
        } else {
            scoreboard.registerObjective(objective, criteria, displayName, criteria.getDefaultRenderType());
            ScoreboardObjective objective2 = scoreboard.getObjective(objective);
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.add.success", objective2.getFormattedDisplayName()), true);
            return scoreboard.getObjectives().size();
        }
    }

    private static int listObjectives(CommandListenerWrapper source) {
        Collection<ScoreboardObjective> collection = source.getServer().getScoreboard().getObjectives();
        if (collection.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.list.empty"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.scoreboard.objectives.list.success", collection.size(), ChatComponentUtils.formatList(collection, ScoreboardObjective::getFormattedDisplayName)), false);
        }

        return collection.size();
    }
}
