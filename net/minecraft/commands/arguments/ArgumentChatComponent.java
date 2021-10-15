package net.minecraft.commands.arguments;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public class ArgumentChatComponent implements ArgumentType<IChatBaseComponent> {
    private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
    public static final DynamicCommandExceptionType ERROR_INVALID_JSON = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.component.invalid", object);
    });

    private ArgumentChatComponent() {
    }

    public static IChatBaseComponent getComponent(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, IChatBaseComponent.class);
    }

    public static ArgumentChatComponent textComponent() {
        return new ArgumentChatComponent();
    }

    @Override
    public IChatBaseComponent parse(StringReader stringReader) throws CommandSyntaxException {
        try {
            IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(stringReader);
            if (component == null) {
                throw ERROR_INVALID_JSON.createWithContext(stringReader, "empty");
            } else {
                return component;
            }
        } catch (JsonParseException var4) {
            String string = var4.getCause() != null ? var4.getCause().getMessage() : var4.getMessage();
            throw ERROR_INVALID_JSON.createWithContext(stringReader, string);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
