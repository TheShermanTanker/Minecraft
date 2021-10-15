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
import net.minecraft.EnumChatFormat;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;

public class ArgumentChatFormat implements ArgumentType<EnumChatFormat> {
    private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");
    public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.color.invalid", object);
    });

    private ArgumentChatFormat() {
    }

    public static ArgumentChatFormat color() {
        return new ArgumentChatFormat();
    }

    public static EnumChatFormat getColor(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, EnumChatFormat.class);
    }

    @Override
    public EnumChatFormat parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        EnumChatFormat chatFormatting = EnumChatFormat.getByName(string);
        if (chatFormatting != null && !chatFormatting.isFormat()) {
            return chatFormatting;
        } else {
            throw ERROR_INVALID_VALUE.create(string);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(EnumChatFormat.getNames(true, false), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
