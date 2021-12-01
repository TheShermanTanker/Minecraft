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
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class ArgumentVec2 implements ArgumentType<IVectorPosition> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "0.1 -0.5", "~1 ~-2");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new ChatMessage("argument.pos2d.incomplete"));
    private final boolean centerCorrect;

    public ArgumentVec2(boolean centerIntegers) {
        this.centerCorrect = centerIntegers;
    }

    public static ArgumentVec2 vec2() {
        return new ArgumentVec2(true);
    }

    public static ArgumentVec2 vec2(boolean centerIntegers) {
        return new ArgumentVec2(centerIntegers);
    }

    public static Vec2F getVec2(CommandContext<CommandListenerWrapper> context, String name) {
        Vec3D vec3 = context.getArgument(name, IVectorPosition.class).getPosition(context.getSource());
        return new Vec2F((float)vec3.x, (float)vec3.z);
    }

    public IVectorPosition parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        if (!stringReader.canRead()) {
            throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
        } else {
            ArgumentParserPosition worldCoordinate = ArgumentParserPosition.parseDouble(stringReader, this.centerCorrect);
            if (stringReader.canRead() && stringReader.peek() == ' ') {
                stringReader.skip();
                ArgumentParserPosition worldCoordinate2 = ArgumentParserPosition.parseDouble(stringReader, this.centerCorrect);
                return new VectorPosition(worldCoordinate, new ArgumentParserPosition(true, 0.0D), worldCoordinate2);
            } else {
                stringReader.setCursor(i);
                throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
            }
        }
    }

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

            return ICompletionProvider.suggest2DCoordinates(string, collection, suggestionsBuilder, CommandDispatcher.createValidator(this::parse));
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
