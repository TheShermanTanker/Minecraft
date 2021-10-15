package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;

public class CommandSay {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("say").requires((commandSourceStack) -> {
            return commandSourceStack.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("message", ArgumentChat.message()).executes((context) -> {
            IChatBaseComponent component = ArgumentChat.getMessage(context, "message");
            IChatBaseComponent component2 = new ChatMessage("chat.type.announcement", context.getSource().getScoreboardDisplayName(), component);
            Entity entity = context.getSource().getEntity();
            if (entity != null) {
                context.getSource().getServer().getPlayerList().sendMessage(component2, ChatMessageType.CHAT, entity.getUniqueID());
            } else {
                context.getSource().getServer().getPlayerList().sendMessage(component2, ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
            }

            return 1;
        })));
    }
}
