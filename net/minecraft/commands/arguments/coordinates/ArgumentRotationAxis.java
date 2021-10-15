package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;

public class ArgumentRotationAxis implements ArgumentType<EnumSet<EnumDirection.EnumAxis>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("xyz", "x");
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(new ChatMessage("arguments.swizzle.invalid"));

    public static ArgumentRotationAxis swizzle() {
        return new ArgumentRotationAxis();
    }

    public static EnumSet<EnumDirection.EnumAxis> getSwizzle(CommandContext<CommandListenerWrapper> commandContext, String string) {
        return commandContext.getArgument(string, EnumSet.class);
    }

    @Override
    public EnumSet<EnumDirection.EnumAxis> parse(StringReader stringReader) throws CommandSyntaxException {
        EnumSet<EnumDirection.EnumAxis> enumSet = EnumSet.noneOf(EnumDirection.EnumAxis.class);

        while(stringReader.canRead() && stringReader.peek() != ' ') {
            char c = stringReader.read();
            EnumDirection.EnumAxis axis;
            switch(c) {
            case 'x':
                axis = EnumDirection.EnumAxis.X;
                break;
            case 'y':
                axis = EnumDirection.EnumAxis.Y;
                break;
            case 'z':
                axis = EnumDirection.EnumAxis.Z;
                break;
            default:
                throw ERROR_INVALID.create();
            }

            if (enumSet.contains(axis)) {
                throw ERROR_INVALID.create();
            }

            enumSet.add(axis);
        }

        return enumSet;
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
