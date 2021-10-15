package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.BlockPosition2D;

public class ArgumentVec2I implements ArgumentType<IVectorPosition> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~1 ~-2", "^ ^", "^-1 ^0");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new ChatMessage("argument.pos2d.incomplete"));

    public static ArgumentVec2I columnPos() {
        return new ArgumentVec2I();
    }

    public static BlockPosition2D getColumnPos(CommandContext<CommandListenerWrapper> context, String name) {
        BlockPosition blockPos = context.getArgument(name, IVectorPosition.class).getBlockPos(context.getSource());
        return new BlockPosition2D(blockPos.getX(), blockPos.getZ());
    }

    @Override
    public IVectorPosition parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        if (!stringReader.canRead()) {
            throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
        } else {
            ArgumentParserPosition worldCoordinate = ArgumentParserPosition.parseInt(stringReader);
            if (stringReader.canRead() && stringReader.peek() == ' ') {
                stringReader.skip();
                ArgumentParserPosition worldCoordinate2 = ArgumentParserPosition.parseInt(stringReader);
                return new VectorPosition(worldCoordinate, new ArgumentParserPosition(true, 0.0D), worldCoordinate2);
            } else {
                stringReader.setCursor(i);
                throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
            }
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        if (!(commandContext.getSource() instanceof ICompletionProvider)) {
            return Suggestions.empty();
        } else {
            String string = suggestionsBuilder.getRemaining();
            Collection<ICompletionProvider.TextCoordinates> collection;
            if (!string.isEmpty() && string.charAt(0) == '^') {
                collection = Collections.singleton(ICompletionProvider.TextCoordinates.DEFAULT_LOCAL);
            } else {
                collection = ((ICompletionProvider)commandContext.getSource()).getRelevantCoordinates();
            }

            return ICompletionProvider.suggest2DCoordinates(string, collection, suggestionsBuilder, CommandDispatcher.createValidator(this::parse));
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
