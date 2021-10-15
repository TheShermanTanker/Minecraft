package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;

public class CommandStop {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("stop").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            context.getSource().sendMessage(new ChatMessage("commands.stop.stopping"), true);
            context.getSource().getServer().safeShutdown(false);
            return 1;
        }));
    }
}
