package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.regex.Matcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.players.IpBanList;

public class CommandPardonIP {
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(new ChatMessage("commands.pardonip.invalid"));
    private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(new ChatMessage("commands.pardonip.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("pardon-ip").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("target", StringArgumentType.word()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(context.getSource().getServer().getPlayerList().getIPBans().getEntries(), builder);
        }).executes((context) -> {
            return unban(context.getSource(), StringArgumentType.getString(context, "target"));
        })));
    }

    private static int unban(CommandListenerWrapper source, String target) throws CommandSyntaxException {
        Matcher matcher = CommandBanIp.IP_ADDRESS_PATTERN.matcher(target);
        if (!matcher.matches()) {
            throw ERROR_INVALID.create();
        } else {
            IpBanList ipBanList = source.getServer().getPlayerList().getIPBans();
            if (!ipBanList.isBanned(target)) {
                throw ERROR_NOT_BANNED.create();
            } else {
                ipBanList.remove(target);
                source.sendMessage(new ChatMessage("commands.pardonip.success", target), true);
                return 1;
            }
        }
    }
}
