package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.players.ExpirableListEntry;
import net.minecraft.server.players.PlayerList;

public class CommandBanList {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("banlist").requires((source) -> {
            return source.hasPermission(3);
        }).executes((context) -> {
            PlayerList playerList = context.getSource().getServer().getPlayerList();
            return showList(context.getSource(), Lists.newArrayList(Iterables.concat(playerList.getProfileBans().getEntries(), playerList.getIPBans().getEntries())));
        }).then(net.minecraft.commands.CommandDispatcher.literal("ips").executes((context) -> {
            return showList(context.getSource(), context.getSource().getServer().getPlayerList().getIPBans().getEntries());
        })).then(net.minecraft.commands.CommandDispatcher.literal("players").executes((context) -> {
            return showList(context.getSource(), context.getSource().getServer().getPlayerList().getProfileBans().getEntries());
        })));
    }

    private static int showList(CommandListenerWrapper source, Collection<? extends ExpirableListEntry<?>> targets) {
        if (targets.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.banlist.none"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.banlist.list", targets.size()), false);

            for(ExpirableListEntry<?> banListEntry : targets) {
                source.sendMessage(new ChatMessage("commands.banlist.entry", banListEntry.getDisplayName(), banListEntry.getSource(), banListEntry.getReason()), false);
            }
        }

        return targets.size();
    }
}
