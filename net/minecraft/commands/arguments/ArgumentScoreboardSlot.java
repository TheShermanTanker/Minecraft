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

public class ArgumentScoreboardSlot implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("sidebar", "foo.bar");
    public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.scoreboardDisplaySlot.invalid", object);
    });

    private ArgumentScoreboardSlot() {
    }

    public static ArgumentScoreboardSlot displaySlot() {
        return new ArgumentScoreboardSlot();
    }

    public static int getDisplaySlot(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public Integer parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        int i = Scoreboard.getSlotForName(string);
        if (i == -1) {
            throw ERROR_INVALID_VALUE.create(string);
        } else {
            return i;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(Scoreboard.getDisplaySlotNames(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
