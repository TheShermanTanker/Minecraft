package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;

public class ArgumentTag implements ArgumentType<ArgumentTag.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("arguments.function.tag.unknown", object);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("arguments.function.unknown", object);
    });

    public static ArgumentTag functions() {
        return new ArgumentTag();
    }

    @Override
    public ArgumentTag.Result parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            stringReader.skip();
            final MinecraftKey resourceLocation = MinecraftKey.read(stringReader);
            return new ArgumentTag.Result() {
                @Override
                public Collection<CustomFunction> create(CommandContext<CommandListenerWrapper> commandContext) throws CommandSyntaxException {
                    Tag<CustomFunction> tag = ArgumentTag.getFunctionTag(commandContext, resourceLocation);
                    return tag.getTagged();
                }

                @Override
                public Pair<MinecraftKey, Either<CustomFunction, Tag<CustomFunction>>> unwrap(CommandContext<CommandListenerWrapper> commandContext) throws CommandSyntaxException {
                    return Pair.of(resourceLocation, Either.right(ArgumentTag.getFunctionTag(commandContext, resourceLocation)));
                }
            };
        } else {
            final MinecraftKey resourceLocation2 = MinecraftKey.read(stringReader);
            return new ArgumentTag.Result() {
                @Override
                public Collection<CustomFunction> create(CommandContext<CommandListenerWrapper> commandContext) throws CommandSyntaxException {
                    return Collections.singleton(ArgumentTag.getFunction(commandContext, resourceLocation2));
                }

                @Override
                public Pair<MinecraftKey, Either<CustomFunction, Tag<CustomFunction>>> unwrap(CommandContext<CommandListenerWrapper> commandContext) throws CommandSyntaxException {
                    return Pair.of(resourceLocation2, Either.left(ArgumentTag.getFunction(commandContext, resourceLocation2)));
                }
            };
        }
    }

    static CustomFunction getFunction(CommandContext<CommandListenerWrapper> context, MinecraftKey id) throws CommandSyntaxException {
        return context.getSource().getServer().getFunctionData().get(id).orElseThrow(() -> {
            return ERROR_UNKNOWN_FUNCTION.create(id.toString());
        });
    }

    static Tag<CustomFunction> getFunctionTag(CommandContext<CommandListenerWrapper> context, MinecraftKey id) throws CommandSyntaxException {
        Tag<CustomFunction> tag = context.getSource().getServer().getFunctionData().getTag(id);
        if (tag == null) {
            throw ERROR_UNKNOWN_TAG.create(id.toString());
        } else {
            return tag;
        }
    }

    public static Collection<CustomFunction> getFunctions(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ArgumentTag.Result.class).create(context);
    }

    public static Pair<MinecraftKey, Either<CustomFunction, Tag<CustomFunction>>> getFunctionOrTag(CommandContext<CommandListenerWrapper> commandContext, String string) throws CommandSyntaxException {
        return commandContext.getArgument(string, ArgumentTag.Result.class).unwrap(commandContext);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public interface Result {
        Collection<CustomFunction> create(CommandContext<CommandListenerWrapper> commandContext) throws CommandSyntaxException;

        Pair<MinecraftKey, Either<CustomFunction, Tag<CustomFunction>>> unwrap(CommandContext<CommandListenerWrapper> commandContext) throws CommandSyntaxException;
    }
}
