package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.server.level.EntityPlayer;

public class CommandTellRaw {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("tellraw").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("message", ArgumentChatComponent.textComponent()).executes((context) -> {
            int i = 0;

            for(EntityPlayer serverPlayer : ArgumentEntity.getPlayers(context, "targets")) {
                serverPlayer.sendMessage(ChatComponentUtils.filterForDisplay(context.getSource(), ArgumentChatComponent.getComponent(context, "message"), serverPlayer, 0), SystemUtils.NIL_UUID);
                ++i;
            }

            return i;
        }))));
    }
}
