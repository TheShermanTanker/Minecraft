package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.IpBanEntry;
import net.minecraft.server.players.IpBanList;

public class CommandBanIp {
    public static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final SimpleCommandExceptionType ERROR_INVALID_IP = new SimpleCommandExceptionType(new ChatMessage("commands.banip.invalid"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(new ChatMessage("commands.banip.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("ban-ip").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("target", StringArgumentType.word()).executes((context) -> {
            return banIpOrName(context.getSource(), StringArgumentType.getString(context, "target"), (IChatBaseComponent)null);
        }).then(net.minecraft.commands.CommandDispatcher.argument("reason", ArgumentChat.message()).executes((context) -> {
            return banIpOrName(context.getSource(), StringArgumentType.getString(context, "target"), ArgumentChat.getMessage(context, "reason"));
        }))));
    }

    private static int banIpOrName(CommandListenerWrapper source, String target, @Nullable IChatBaseComponent reason) throws CommandSyntaxException {
        Matcher matcher = IP_ADDRESS_PATTERN.matcher(target);
        if (matcher.matches()) {
            return banIp(source, target, reason);
        } else {
            EntityPlayer serverPlayer = source.getServer().getPlayerList().getPlayer(target);
            if (serverPlayer != null) {
                return banIp(source, serverPlayer.getIpAddress(), reason);
            } else {
                throw ERROR_INVALID_IP.create();
            }
        }
    }

    private static int banIp(CommandListenerWrapper source, String targetIp, @Nullable IChatBaseComponent reason) throws CommandSyntaxException {
        IpBanList ipBanList = source.getServer().getPlayerList().getIPBans();
        if (ipBanList.isBanned(targetIp)) {
            throw ERROR_ALREADY_BANNED.create();
        } else {
            List<EntityPlayer> list = source.getServer().getPlayerList().getPlayersWithAddress(targetIp);
            IpBanEntry ipBanListEntry = new IpBanEntry(targetIp, (Date)null, source.getName(), (Date)null, reason == null ? null : reason.getString());
            ipBanList.add(ipBanListEntry);
            source.sendMessage(new ChatMessage("commands.banip.success", targetIp, ipBanListEntry.getReason()), true);
            if (!list.isEmpty()) {
                source.sendMessage(new ChatMessage("commands.banip.info", list.size(), EntitySelector.joinNames(list)), true);
            }

            for(EntityPlayer serverPlayer : list) {
                serverPlayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.ip_banned"));
            }

            return list.size();
        }
    }
}
