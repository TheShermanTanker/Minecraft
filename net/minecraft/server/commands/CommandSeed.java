package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.EnumChatFormat;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public class CommandSeed {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher, boolean dedicated) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("seed").requires((source) -> {
            return !dedicated || source.hasPermission(2);
        }).executes((context) -> {
            long l = context.getSource().getWorld().getSeed();
            IChatBaseComponent component = ChatComponentUtils.wrapInSquareBrackets((new ChatComponentText(String.valueOf(l))).format((style) -> {
                return style.setColor(EnumChatFormat.GREEN).setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.COPY_TO_CLIPBOARD, String.valueOf(l))).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatMessage("chat.copy.click"))).setInsertion(String.valueOf(l));
            }));
            context.getSource().sendMessage(new ChatMessage("commands.seed.success", component), false);
            return (int)l;
        }));
    }
}
