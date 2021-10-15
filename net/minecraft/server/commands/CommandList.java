package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.EntityHuman;

public class CommandList {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listPlayers(context.getSource());
        }).then(net.minecraft.commands.CommandDispatcher.literal("uuids").executes((context) -> {
            return listPlayersWithUuids(context.getSource());
        })));
    }

    private static int listPlayers(CommandListenerWrapper source) {
        return format(source, EntityHuman::getScoreboardDisplayName);
    }

    private static int listPlayersWithUuids(CommandListenerWrapper source) {
        return format(source, (player) -> {
            return new ChatMessage("commands.list.nameAndId", player.getDisplayName(), player.getProfile().getId());
        });
    }

    private static int format(CommandListenerWrapper source, Function<EntityPlayer, IChatBaseComponent> nameProvider) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<EntityPlayer> list = playerList.getPlayers();
        IChatBaseComponent component = ChatComponentUtils.formatList(list, nameProvider);
        source.sendMessage(new ChatMessage("commands.list.players", list.size(), playerList.getMaxPlayers(), component), false);
        return list.size();
    }
}
