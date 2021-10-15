package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.HttpUtilities;
import net.minecraft.world.level.EnumGamemode;

public class CommandPublish {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.publish.failed"));
    private static final DynamicCommandExceptionType ERROR_ALREADY_PUBLISHED = new DynamicCommandExceptionType((port) -> {
        return new ChatMessage("commands.publish.alreadyPublished", port);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("publish").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            return publish(context.getSource(), HttpUtilities.getAvailablePort());
        }).then(net.minecraft.commands.CommandDispatcher.argument("port", IntegerArgumentType.integer(0, 65535)).executes((context) -> {
            return publish(context.getSource(), IntegerArgumentType.getInteger(context, "port"));
        })));
    }

    private static int publish(CommandListenerWrapper source, int port) throws CommandSyntaxException {
        if (source.getServer().isPublished()) {
            throw ERROR_ALREADY_PUBLISHED.create(source.getServer().getPort());
        } else if (!source.getServer().publishServer((EnumGamemode)null, false, port)) {
            throw ERROR_FAILED.create();
        } else {
            source.sendMessage(new ChatMessage("commands.publish.success", port), true);
            return port;
        }
    }
}
