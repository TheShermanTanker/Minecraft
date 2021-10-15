package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;

public class CommandTell {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralCommandNode<CommandListenerWrapper> literalCommandNode = dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("msg").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("message", ArgumentChat.message()).executes((context) -> {
            return sendMessage(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentChat.getMessage(context, "message"));
        }))));
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("tell").redirect(literalCommandNode));
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("w").redirect(literalCommandNode));
    }

    private static int sendMessage(CommandListenerWrapper source, Collection<EntityPlayer> targets, IChatBaseComponent message) {
        UUID uUID = source.getEntity() == null ? SystemUtils.NIL_UUID : source.getEntity().getUniqueID();
        Entity entity = source.getEntity();
        Consumer<IChatBaseComponent> consumer;
        if (entity instanceof EntityPlayer) {
            EntityPlayer serverPlayer = (EntityPlayer)entity;
            consumer = (playerName) -> {
                serverPlayer.sendMessage((new ChatMessage("commands.message.display.outgoing", playerName, message)).withStyle(new EnumChatFormat[]{EnumChatFormat.GRAY, EnumChatFormat.ITALIC}), serverPlayer.getUniqueID());
            };
        } else {
            consumer = (playerName) -> {
                source.sendMessage((new ChatMessage("commands.message.display.outgoing", playerName, message)).withStyle(new EnumChatFormat[]{EnumChatFormat.GRAY, EnumChatFormat.ITALIC}), false);
            };
        }

        for(EntityPlayer serverPlayer2 : targets) {
            consumer.accept(serverPlayer2.getScoreboardDisplayName());
            serverPlayer2.sendMessage((new ChatMessage("commands.message.display.incoming", source.getScoreboardDisplayName(), message)).withStyle(new EnumChatFormat[]{EnumChatFormat.GRAY, EnumChatFormat.ITALIC}), uUID);
        }

        return targets.size();
    }
}
