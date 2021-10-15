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
import net.minecraft.world.level.World;

public class ArgumentPosition implements ArgumentType<IVectorPosition> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
    public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(new ChatMessage("argument.pos.unloaded"));
    public static final SimpleCommandExceptionType ERROR_OUT_OF_WORLD = new SimpleCommandExceptionType(new ChatMessage("argument.pos.outofworld"));
    public static final SimpleCommandExceptionType ERROR_OUT_OF_BOUNDS = new SimpleCommandExceptionType(new ChatMessage("argument.pos.outofbounds"));

    public static ArgumentPosition blockPos() {
        return new ArgumentPosition();
    }

    public static BlockPosition getLoadedBlockPos(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        BlockPosition blockPos = context.getArgument(name, IVectorPosition.class).getBlockPos(context.getSource());
        if (!context.getSource().getWorld().isLoaded(blockPos)) {
            throw ERROR_NOT_LOADED.create();
        } else if (!context.getSource().getWorld().isValidLocation(blockPos)) {
            throw ERROR_OUT_OF_WORLD.create();
        } else {
            return blockPos;
        }
    }

    public static BlockPosition getSpawnablePos(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        BlockPosition blockPos = context.getArgument(name, IVectorPosition.class).getBlockPos(context.getSource());
        if (!World.isInSpawnableBounds(blockPos)) {
            throw ERROR_OUT_OF_BOUNDS.create();
        } else {
            return blockPos;
        }
    }

    @Override
    public IVectorPosition parse(StringReader stringReader) throws CommandSyntaxException {
        return (IVectorPosition)(stringReader.canRead() && stringReader.peek() == '^' ? ArgumentVectorPosition.parse(stringReader) : VectorPosition.parseInt(stringReader));
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

            return ICompletionProvider.suggestCoordinates(string, collection, suggestionsBuilder, CommandDispatcher.createValidator(this::parse));
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
