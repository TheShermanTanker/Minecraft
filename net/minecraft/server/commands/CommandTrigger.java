package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentScoreboardObjective;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class CommandTrigger {
    private static final SimpleCommandExceptionType ERROR_NOT_PRIMED = new SimpleCommandExceptionType(new ChatMessage("commands.trigger.failed.unprimed"));
    private static final SimpleCommandExceptionType ERROR_INVALID_OBJECTIVE = new SimpleCommandExceptionType(new ChatMessage("commands.trigger.failed.invalid"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("trigger").then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).suggests((context, builder) -> {
            return suggestObjectives(context.getSource(), builder);
        }).executes((context) -> {
            return simpleTrigger(context.getSource(), getScore(context.getSource().getPlayerOrException(), ArgumentScoreboardObjective.getObjective(context, "objective")));
        }).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("value", IntegerArgumentType.integer()).executes((context) -> {
            return addValue(context.getSource(), getScore(context.getSource().getPlayerOrException(), ArgumentScoreboardObjective.getObjective(context, "objective")), IntegerArgumentType.getInteger(context, "value"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.argument("value", IntegerArgumentType.integer()).executes((context) -> {
            return setValue(context.getSource(), getScore(context.getSource().getPlayerOrException(), ArgumentScoreboardObjective.getObjective(context, "objective")), IntegerArgumentType.getInteger(context, "value"));
        })))));
    }

    public static CompletableFuture<Suggestions> suggestObjectives(CommandListenerWrapper source, SuggestionsBuilder builder) {
        Entity entity = source.getEntity();
        List<String> list = Lists.newArrayList();
        if (entity != null) {
            Scoreboard scoreboard = source.getServer().getScoreboard();
            String string = entity.getName();

            for(ScoreboardObjective objective : scoreboard.getObjectives()) {
                if (objective.getCriteria() == IScoreboardCriteria.TRIGGER && scoreboard.hasPlayerScore(string, objective)) {
                    ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, objective);
                    if (!score.isLocked()) {
                        list.add(objective.getName());
                    }
                }
            }
        }

        return ICompletionProvider.suggest(list, builder);
    }

    private static int addValue(CommandListenerWrapper source, ScoreboardScore score, int value) {
        score.addScore(value);
        source.sendMessage(new ChatMessage("commands.trigger.add.success", score.getObjective().getFormattedDisplayName(), value), true);
        return score.getScore();
    }

    private static int setValue(CommandListenerWrapper source, ScoreboardScore score, int value) {
        score.setScore(value);
        source.sendMessage(new ChatMessage("commands.trigger.set.success", score.getObjective().getFormattedDisplayName(), value), true);
        return value;
    }

    private static int simpleTrigger(CommandListenerWrapper source, ScoreboardScore score) {
        score.addScore(1);
        source.sendMessage(new ChatMessage("commands.trigger.simple.success", score.getObjective().getFormattedDisplayName()), true);
        return score.getScore();
    }

    private static ScoreboardScore getScore(EntityPlayer player, ScoreboardObjective objective) throws CommandSyntaxException {
        if (objective.getCriteria() != IScoreboardCriteria.TRIGGER) {
            throw ERROR_INVALID_OBJECTIVE.create();
        } else {
            Scoreboard scoreboard = player.getScoreboard();
            String string = player.getName();
            if (!scoreboard.hasPlayerScore(string, objective)) {
                throw ERROR_NOT_PRIMED.create();
            } else {
                ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, objective);
                if (score.isLocked()) {
                    throw ERROR_NOT_PRIMED.create();
                } else {
                    score.setLocked(true);
                    return score;
                }
            }
        }
    }
}
