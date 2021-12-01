package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;

public class ArgumentRotation implements ArgumentType<IVectorPosition> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~-5 ~5");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new ChatMessage("argument.rotation.incomplete"));

    public static ArgumentRotation rotation() {
        return new ArgumentRotation();
    }

    public static IVectorPosition getRotation(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, IVectorPosition.class);
    }

    public IVectorPosition parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        if (!stringReader.canRead()) {
            throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
        } else {
            ArgumentParserPosition worldCoordinate = ArgumentParserPosition.parseDouble(stringReader, false);
            if (stringReader.canRead() && stringReader.peek() == ' ') {
                stringReader.skip();
                ArgumentParserPosition worldCoordinate2 = ArgumentParserPosition.parseDouble(stringReader, false);
                return new VectorPosition(worldCoordinate2, worldCoordinate, new ArgumentParserPosition(true, 0.0D));
            } else {
                stringReader.setCursor(i);
                throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
            }
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
