package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.coordinates.ArgumentParserPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;

public class ArgumentAngle implements ArgumentType<ArgumentAngle.SingleAngle> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0", "~", "~-5");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new ChatMessage("argument.angle.incomplete"));
    public static final SimpleCommandExceptionType ERROR_INVALID_ANGLE = new SimpleCommandExceptionType(new ChatMessage("argument.angle.invalid"));

    public static ArgumentAngle angle() {
        return new ArgumentAngle();
    }

    public static float getAngle(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, ArgumentAngle.SingleAngle.class).getAngle(context.getSource());
    }

    @Override
    public ArgumentAngle.SingleAngle parse(StringReader stringReader) throws CommandSyntaxException {
        if (!stringReader.canRead()) {
            throw ERROR_NOT_COMPLETE.createWithContext(stringReader);
        } else {
            boolean bl = ArgumentParserPosition.isRelative(stringReader);
            float f = stringReader.canRead() && stringReader.peek() != ' ' ? stringReader.readFloat() : 0.0F;
            if (!Float.isNaN(f) && !Float.isInfinite(f)) {
                return new ArgumentAngle.SingleAngle(f, bl);
            } else {
                throw ERROR_INVALID_ANGLE.createWithContext(stringReader);
            }
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static final class SingleAngle {
        private final float angle;
        private final boolean isRelative;

        SingleAngle(float f, boolean bl) {
            this.angle = f;
            this.isRelative = bl;
        }

        public float getAngle(CommandListenerWrapper source) {
            return MathHelper.wrapDegrees(this.isRelative ? this.angle + source.getRotation().y : this.angle);
        }
    }
}
