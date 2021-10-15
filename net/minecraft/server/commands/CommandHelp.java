package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;

public class CommandHelp {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.help.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("help").executes((context) -> {
            Map<CommandNode<CommandListenerWrapper>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot(), context.getSource());

            for(String string : map.values()) {
                context.getSource().sendMessage(new ChatComponentText("/" + string), false);
            }

            return map.size();
        }).then(net.minecraft.commands.CommandDispatcher.argument("command", StringArgumentType.greedyString()).executes((context) -> {
            ParseResults<CommandListenerWrapper> parseResults = dispatcher.parse(StringArgumentType.getString(context, "command"), context.getSource());
            if (parseResults.getContext().getNodes().isEmpty()) {
                throw ERROR_FAILED.create();
            } else {
                Map<CommandNode<CommandListenerWrapper>, String> map = dispatcher.getSmartUsage(Iterables.getLast(parseResults.getContext().getNodes()).getNode(), context.getSource());

                for(String string : map.values()) {
                    context.getSource().sendMessage(new ChatComponentText("/" + parseResults.getReader().getString() + " " + string), false);
                }

                return map.size();
            }
        })));
    }
}
