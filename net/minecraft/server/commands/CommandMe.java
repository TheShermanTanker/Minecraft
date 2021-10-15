package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;

public class CommandMe {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("me").then(net.minecraft.commands.CommandDispatcher.argument("action", StringArgumentType.greedyString()).executes((context) -> {
            String string = StringArgumentType.getString(context, "action");
            Entity entity = context.getSource().getEntity();
            MinecraftServer minecraftServer = context.getSource().getServer();
            if (entity != null) {
                if (entity instanceof EntityPlayer) {
                    EntityPlayer serverPlayer = (EntityPlayer)entity;
                    serverPlayer.getTextFilter().processStreamMessage(string).thenAcceptAsync((message) -> {
                        String string = message.getFiltered();
                        IChatBaseComponent component = string.isEmpty() ? null : createMessage(context, string);
                        IChatBaseComponent component2 = createMessage(context, message.getRaw());
                        minecraftServer.getPlayerList().broadcastMessage(component2, (player) -> {
                            return serverPlayer.shouldFilterMessageTo(player) ? component : component2;
                        }, ChatMessageType.CHAT, entity.getUniqueID());
                    }, minecraftServer);
                    return 1;
                }

                minecraftServer.getPlayerList().sendMessage(createMessage(context, string), ChatMessageType.CHAT, entity.getUniqueID());
            } else {
                minecraftServer.getPlayerList().sendMessage(createMessage(context, string), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
            }

            return 1;
        })));
    }

    private static IChatBaseComponent createMessage(CommandContext<CommandListenerWrapper> context, String arg) {
        return new ChatMessage("chat.type.emote", context.getSource().getScoreboardDisplayName(), arg);
    }
}
