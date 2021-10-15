package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentProfile;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.WhiteList;
import net.minecraft.server.players.WhiteListEntry;

public class CommandWhitelist {
    private static final SimpleCommandExceptionType ERROR_ALREADY_ENABLED = new SimpleCommandExceptionType(new ChatMessage("commands.whitelist.alreadyOn"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_DISABLED = new SimpleCommandExceptionType(new ChatMessage("commands.whitelist.alreadyOff"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_WHITELISTED = new SimpleCommandExceptionType(new ChatMessage("commands.whitelist.add.failed"));
    private static final SimpleCommandExceptionType ERROR_NOT_WHITELISTED = new SimpleCommandExceptionType(new ChatMessage("commands.whitelist.remove.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("whitelist").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.literal("on").executes((context) -> {
            return enableWhitelist(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("off").executes((context) -> {
            return disableWhitelist(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return showList(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentProfile.gameProfile()).suggests((context, builder) -> {
            PlayerList playerList = context.getSource().getServer().getPlayerList();
            return ICompletionProvider.suggest(playerList.getPlayers().stream().filter((player) -> {
                return !playerList.getWhitelist().isWhitelisted(player.getProfile());
            }).map((player) -> {
                return player.getProfile().getName();
            }), builder);
        }).executes((context) -> {
            return addPlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentProfile.gameProfile()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(context.getSource().getServer().getPlayerList().getWhitelisted(), builder);
        }).executes((context) -> {
            return removePlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("reload").executes((context) -> {
            return reload(context.getSource());
        })));
    }

    private static int reload(CommandListenerWrapper source) {
        source.getServer().getPlayerList().reloadWhitelist();
        source.sendMessage(new ChatMessage("commands.whitelist.reloaded"), true);
        source.getServer().kickUnlistedPlayers(source);
        return 1;
    }

    private static int addPlayers(CommandListenerWrapper source, Collection<GameProfile> targets) throws CommandSyntaxException {
        WhiteList userWhiteList = source.getServer().getPlayerList().getWhitelist();
        int i = 0;

        for(GameProfile gameProfile : targets) {
            if (!userWhiteList.isWhitelisted(gameProfile)) {
                WhiteListEntry userWhiteListEntry = new WhiteListEntry(gameProfile);
                userWhiteList.add(userWhiteListEntry);
                source.sendMessage(new ChatMessage("commands.whitelist.add.success", ChatComponentUtils.getDisplayName(gameProfile)), true);
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_ALREADY_WHITELISTED.create();
        } else {
            return i;
        }
    }

    private static int removePlayers(CommandListenerWrapper source, Collection<GameProfile> targets) throws CommandSyntaxException {
        WhiteList userWhiteList = source.getServer().getPlayerList().getWhitelist();
        int i = 0;

        for(GameProfile gameProfile : targets) {
            if (userWhiteList.isWhitelisted(gameProfile)) {
                WhiteListEntry userWhiteListEntry = new WhiteListEntry(gameProfile);
                userWhiteList.remove(userWhiteListEntry);
                source.sendMessage(new ChatMessage("commands.whitelist.remove.success", ChatComponentUtils.getDisplayName(gameProfile)), true);
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_NOT_WHITELISTED.create();
        } else {
            source.getServer().kickUnlistedPlayers(source);
            return i;
        }
    }

    private static int enableWhitelist(CommandListenerWrapper source) throws CommandSyntaxException {
        PlayerList playerList = source.getServer().getPlayerList();
        if (playerList.getHasWhitelist()) {
            throw ERROR_ALREADY_ENABLED.create();
        } else {
            playerList.setHasWhitelist(true);
            source.sendMessage(new ChatMessage("commands.whitelist.enabled"), true);
            source.getServer().kickUnlistedPlayers(source);
            return 1;
        }
    }

    private static int disableWhitelist(CommandListenerWrapper source) throws CommandSyntaxException {
        PlayerList playerList = source.getServer().getPlayerList();
        if (!playerList.getHasWhitelist()) {
            throw ERROR_ALREADY_DISABLED.create();
        } else {
            playerList.setHasWhitelist(false);
            source.sendMessage(new ChatMessage("commands.whitelist.disabled"), true);
            return 1;
        }
    }

    private static int showList(CommandListenerWrapper source) {
        String[] strings = source.getServer().getPlayerList().getWhitelisted();
        if (strings.length == 0) {
            source.sendMessage(new ChatMessage("commands.whitelist.none"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.whitelist.list", strings.length, String.join(", ", strings)), false);
        }

        return strings.length;
    }
}
