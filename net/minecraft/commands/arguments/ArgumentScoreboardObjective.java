package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;

public class ArgumentScoreboardObjective implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "*", "012");
    private static final DynamicCommandExceptionType ERROR_OBJECTIVE_NOT_FOUND = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("arguments.objective.notFound", name);
    });
    private static final DynamicCommandExceptionType ERROR_OBJECTIVE_READ_ONLY = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("arguments.objective.readonly", name);
    });

    public static ArgumentScoreboardObjective objective() {
        return new ArgumentScoreboardObjective();
    }

    public static ScoreboardObjective getObjective(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        String string = context.getArgument(name, String.class);
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjective(string);
        if (objective == null) {
            throw ERROR_OBJECTIVE_NOT_FOUND.create(string);
        } else {
            return objective;
        }
    }

    public static ScoreboardObjective getWritableObjective(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        ScoreboardObjective objective = getObjective(context, name);
        if (objective.getCriteria().isReadOnly()) {
            throw ERROR_OBJECTIVE_READ_ONLY.create(objective.getName());
        } else {
            return objective;
        }
    }

    public String parse(StringReader stringReader) throws CommandSyntaxException {
        return stringReader.readUnquotedString();
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        if (commandContext.getSource() instanceof CommandListenerWrapper) {
            return ICompletionProvider.suggest(((CommandListenerWrapper)commandContext.getSource()).getServer().getScoreboard().getObjectiveNames(), suggestionsBuilder);
        } else if (commandContext.getSource() instanceof ICompletionProvider) {
            ICompletionProvider sharedSuggestionProvider = (ICompletionProvider)commandContext.getSource();
            return sharedSuggestionProvider.customSuggestion(commandContext, suggestionsBuilder);
        } else {
            return Suggestions.empty();
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
