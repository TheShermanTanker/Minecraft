package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.commands.arguments.ArgumentProfile;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.GameProfileBanEntry;
import net.minecraft.server.players.GameProfileBanList;

public class CommandBan {
    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(new ChatMessage("commands.ban.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("ban").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentProfile.gameProfile()).executes((context) -> {
            return banPlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"), (IChatBaseComponent)null);
        }).then(net.minecraft.commands.CommandDispatcher.argument("reason", ArgumentChat.message()).executes((context) -> {
            return banPlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"), ArgumentChat.getMessage(context, "reason"));
        }))));
    }

    private static int banPlayers(CommandListenerWrapper source, Collection<GameProfile> targets, @Nullable IChatBaseComponent reason) throws CommandSyntaxException {
        GameProfileBanList userBanList = source.getServer().getPlayerList().getProfileBans();
        int i = 0;

        for(GameProfile gameProfile : targets) {
            if (!userBanList.isBanned(gameProfile)) {
                GameProfileBanEntry userBanListEntry = new GameProfileBanEntry(gameProfile, (Date)null, source.getName(), (Date)null, reason == null ? null : reason.getString());
                userBanList.add(userBanListEntry);
                ++i;
                source.sendMessage(new ChatMessage("commands.ban.success", ChatComponentUtils.getDisplayName(gameProfile), userBanListEntry.getReason()), true);
                EntityPlayer serverPlayer = source.getServer().getPlayerList().getPlayer(gameProfile.getId());
                if (serverPlayer != null) {
                    serverPlayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.banned"));
                }
            }
        }

        if (i == 0) {
            throw ERROR_ALREADY_BANNED.create();
        } else {
            return i;
        }
    }
}
