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
import net.minecraft.world.scores.ScoreboardTeam;

public class ArgumentScoreboardTeam implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "123");
    private static final DynamicCommandExceptionType ERROR_TEAM_NOT_FOUND = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("team.notFound", name);
    });

    public static ArgumentScoreboardTeam team() {
        return new ArgumentScoreboardTeam();
    }

    public static ScoreboardTeam getTeam(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        String string = context.getArgument(name, String.class);
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        ScoreboardTeam playerTeam = scoreboard.getTeam(string);
        if (playerTeam == null) {
            throw ERROR_TEAM_NOT_FOUND.create(string);
        } else {
            return playerTeam;
        }
    }

    public String parse(StringReader stringReader) throws CommandSyntaxException {
        return stringReader.readUnquotedString();
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return commandContext.getSource() instanceof ICompletionProvider ? ICompletionProvider.suggest(((ICompletionProvider)commandContext.getSource()).getAllTeams(), suggestionsBuilder) : Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
