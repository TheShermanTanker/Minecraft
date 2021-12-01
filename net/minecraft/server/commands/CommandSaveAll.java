package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.MinecraftServer;

public class CommandSaveAll {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.save.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("save-all").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            return saveAll(context.getSource(), false);
        }).then(net.minecraft.commands.CommandDispatcher.literal("flush").executes((context) -> {
            return saveAll(context.getSource(), true);
        })));
    }

    private static int saveAll(CommandListenerWrapper source, boolean flush) throws CommandSyntaxException {
        source.sendMessage(new ChatMessage("commands.save.saving"), false);
        MinecraftServer minecraftServer = source.getServer();
        boolean bl = minecraftServer.saveEverything(true, flush, true);
        if (!bl) {
            throw ERROR_FAILED.create();
        } else {
            source.sendMessage(new ChatMessage("commands.save.success"), true);
            return 1;
        }
    }
}
