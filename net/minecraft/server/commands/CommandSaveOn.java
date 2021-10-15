package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;

public class CommandSaveOn {
    private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(new ChatMessage("commands.save.alreadyOn"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("save-on").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            CommandListenerWrapper commandSourceStack = context.getSource();
            boolean bl = false;

            for(WorldServer serverLevel : commandSourceStack.getServer().getWorlds()) {
                if (serverLevel != null && serverLevel.noSave) {
                    serverLevel.noSave = false;
                    bl = true;
                }
            }

            if (!bl) {
                throw ERROR_ALREADY_ON.create();
            } else {
                commandSourceStack.sendMessage(new ChatMessage("commands.save.enabled"), true);
                return 1;
            }
        }));
    }
}
