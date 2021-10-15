package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;

public class CommandIdleTimeout {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("setidletimeout").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("minutes", IntegerArgumentType.integer(0)).executes((context) -> {
            return setIdleTimeout(context.getSource(), IntegerArgumentType.getInteger(context, "minutes"));
        })));
    }

    private static int setIdleTimeout(CommandListenerWrapper source, int minutes) {
        source.getServer().setIdleTimeout(minutes);
        source.sendMessage(new ChatMessage("commands.setidletimeout.success", minutes), true);
        return minutes;
    }
}
