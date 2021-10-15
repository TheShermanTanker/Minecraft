package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;

public class CommandSaveOff {
    private static final SimpleCommandExceptionType ERROR_ALREADY_OFF = new SimpleCommandExceptionType(new ChatMessage("commands.save.alreadyOff"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("save-off").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            CommandListenerWrapper commandSourceStack = context.getSource();
            boolean bl = false;

            for(WorldServer serverLevel : commandSourceStack.getServer().getWorlds()) {
                if (serverLevel != null && !serverLevel.noSave) {
                    serverLevel.noSave = true;
                    bl = true;
                }
            }

            if (!bl) {
                throw ERROR_ALREADY_OFF.create();
            } else {
                commandSourceStack.sendMessage(new ChatMessage("commands.save.disabled"), true);
                return 1;
            }
        }));
    }
}
