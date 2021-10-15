package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.item.ArgumentTag;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.CustomFunctionData;

public class CommandFunction {
    public static final SuggestionProvider<CommandListenerWrapper> SUGGEST_FUNCTION = (context, builder) -> {
        CustomFunctionData serverFunctionManager = context.getSource().getServer().getFunctionData();
        ICompletionProvider.suggestResource(serverFunctionManager.getTagNames(), builder, "#");
        return ICompletionProvider.suggestResource(serverFunctionManager.getFunctionNames(), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("function").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("name", ArgumentTag.functions()).suggests(SUGGEST_FUNCTION).executes((context) -> {
            return runFunction(context.getSource(), ArgumentTag.getFunctions(context, "name"));
        })));
    }

    private static int runFunction(CommandListenerWrapper source, Collection<CustomFunction> functions) {
        int i = 0;

        for(CustomFunction commandFunction : functions) {
            i += source.getServer().getFunctionData().execute(commandFunction, source.withSuppressedOutput().withMaximumPermission(2));
        }

        if (functions.size() == 1) {
            source.sendMessage(new ChatMessage("commands.function.success.single", i, functions.iterator().next().getId()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.function.success.multiple", i, functions.size()), true);
        }

        return i;
    }
}
