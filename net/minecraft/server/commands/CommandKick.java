package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;

public class CommandKick {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("kick").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).executes((context) -> {
            return kickPlayers(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), new ChatMessage("multiplayer.disconnect.kicked"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("reason", ArgumentChat.message()).executes((context) -> {
            return kickPlayers(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentChat.getMessage(context, "reason"));
        }))));
    }

    private static int kickPlayers(CommandListenerWrapper source, Collection<EntityPlayer> targets, IChatBaseComponent reason) {
        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.connection.disconnect(reason);
            source.sendMessage(new ChatMessage("commands.kick.success", serverPlayer.getScoreboardDisplayName(), reason), true);
        }

        return targets.size();
    }
}
