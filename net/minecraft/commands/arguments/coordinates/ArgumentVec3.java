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
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.phys.Vec3D;

public class ArgumentVec3 implements ArgumentType<IVectorPosition> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new ChatMessage("argument.pos3d.incomplete"));
    public static final SimpleCommandExceptionType ERROR_MIXED_TYPE = new SimpleCommandExceptionType(new ChatMessage("argument.pos.mixed"));
    private final boolean centerCorrect;

    public ArgumentVec3(boolean centerIntegers) {
        this.centerCorrect = centerIntegers;
    }

    public static ArgumentVec3 vec3() {
        return new ArgumentVec3(true);
    }

    public static ArgumentVec3 vec3(boolean centerIntegers) {
        return new ArgumentVec3(centerIntegers);
    }

    public static Vec3D getVec3(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, IVectorPosition.class).getPosition(context.getSource());
    }

    public static IVectorPosition getCoordinates(CommandContext<CommandListenerWrapper> commandContext, String string) {
        return commandContext.getArgument(string, IVectorPosition.class);
    }

    @Override
    public IVectorPosition parse(StringReader stringReader) throws CommandSyntaxException {
        return (IVectorPosition)(stringReader.canRead() && stringReader.peek() == '^' ? ArgumentVectorPosition.parse(stringReader) : VectorPosition.parseDouble(stringReader, this.centerCorrect));
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
                collection = ((ICompletionProvider)commandContext.getSource()).getAbsoluteCoordinates();
            }

            return ICompletionProvider.suggestCoordinates(string, collection, suggestionsBuilder, CommandDispatcher.createValidator(this::parse));
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
