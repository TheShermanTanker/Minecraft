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
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatComponentText;

public class GameTestHarnessTestClassArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("techtests", "mobtests");

    @Override
    public String parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        if (GameTestHarnessRegistry.isTestClass(string)) {
            return string;
        } else {
            Message message = new ChatComponentText("No such test class: " + string);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }
    }

    public static GameTestHarnessTestClassArgument testClassName() {
        return new GameTestHarnessTestClassArgument();
    }

    public static String getTestClassName(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(GameTestHarnessRegistry.getAllTestClassNames().stream(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
