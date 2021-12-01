package net.minecraft.gametest.framework;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatComponentText;

public class GameTestHarnessTestFunctionArgument implements ArgumentType<GameTestHarnessTestFunction> {
    private static final Collection<String> EXAMPLES = Arrays.asList("techtests.piston", "techtests");

    public GameTestHarnessTestFunction parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        Optional<GameTestHarnessTestFunction> optional = GameTestHarnessRegistry.findTestFunction(string);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            Message message = new ChatComponentText("No such test: " + string);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }
    }

    public static GameTestHarnessTestFunctionArgument testFunctionArgument() {
        return new GameTestHarnessTestFunctionArgument();
    }

    public static GameTestHarnessTestFunction getTestFunction(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, GameTestHarnessTestFunction.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        Stream<String> stream = GameTestHarnessRegistry.getAllTestFunctions().stream().map(GameTestHarnessTestFunction::getTestName);
        return ICompletionProvider.suggest(stream, suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
