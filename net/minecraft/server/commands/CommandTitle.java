package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutActionBarText;
import net.minecraft.network.protocol.game.PacketPlayOutClearTitles;
import net.minecraft.network.protocol.game.PacketPlayOutSubtitleText;
import net.minecraft.network.protocol.game.PacketPlayOutTitleAnimations;
import net.minecraft.network.protocol.game.PacketPlayOutTitleText;
import net.minecraft.server.level.EntityPlayer;

public class CommandTitle {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("title").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.literal("clear").executes((context) -> {
            return clearTitle(context.getSource(), ArgumentEntity.getPlayers(context, "targets"));
        })).then(net.minecraft.commands.CommandDispatcher.literal("reset").executes((context) -> {
            return resetTitle(context.getSource(), ArgumentEntity.getPlayers(context, "targets"));
        })).then(net.minecraft.commands.CommandDispatcher.literal("title").then(net.minecraft.commands.CommandDispatcher.argument("title", ArgumentChatComponent.textComponent()).executes((context) -> {
            return showTitle(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentChatComponent.getComponent(context, "title"), "title", PacketPlayOutTitleText::new);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("subtitle").then(net.minecraft.commands.CommandDispatcher.argument("title", ArgumentChatComponent.textComponent()).executes((context) -> {
            return showTitle(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentChatComponent.getComponent(context, "title"), "subtitle", PacketPlayOutSubtitleText::new);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("actionbar").then(net.minecraft.commands.CommandDispatcher.argument("title", ArgumentChatComponent.textComponent()).executes((context) -> {
            return showTitle(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentChatComponent.getComponent(context, "title"), "actionbar", PacketPlayOutActionBarText::new);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("times").then(net.minecraft.commands.CommandDispatcher.argument("fadeIn", IntegerArgumentType.integer(0)).then(net.minecraft.commands.CommandDispatcher.argument("stay", IntegerArgumentType.integer(0)).then(net.minecraft.commands.CommandDispatcher.argument("fadeOut", IntegerArgumentType.integer(0)).executes((context) -> {
            return setTimes(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "fadeIn"), IntegerArgumentType.getInteger(context, "stay"), IntegerArgumentType.getInteger(context, "fadeOut"));
        })))))));
    }

    private static int clearTitle(CommandListenerWrapper source, Collection<EntityPlayer> targets) {
        PacketPlayOutClearTitles clientboundClearTitlesPacket = new PacketPlayOutClearTitles(false);

        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.connection.sendPacket(clientboundClearTitlesPacket);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.title.cleared.single", targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.title.cleared.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int resetTitle(CommandListenerWrapper source, Collection<EntityPlayer> targets) {
        PacketPlayOutClearTitles clientboundClearTitlesPacket = new PacketPlayOutClearTitles(true);

        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.connection.sendPacket(clientboundClearTitlesPacket);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.title.reset.single", targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.title.reset.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int showTitle(CommandListenerWrapper source, Collection<EntityPlayer> targets, IChatBaseComponent title, String titleType, Function<IChatBaseComponent, Packet<?>> constructor) throws CommandSyntaxException {
        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.connection.sendPacket(constructor.apply(ChatComponentUtils.filterForDisplay(source, title, serverPlayer, 0)));
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.title.show." + titleType + ".single", targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.title.show." + titleType + ".multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int setTimes(CommandListenerWrapper source, Collection<EntityPlayer> targets, int fadeIn, int stay, int fadeOut) {
        PacketPlayOutTitleAnimations clientboundSetTitlesAnimationPacket = new PacketPlayOutTitleAnimations(fadeIn, stay, fadeOut);

        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.connection.sendPacket(clientboundSetTitlesAnimationPacket);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.title.times.single", targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.title.times.multiple", targets.size()), true);
        }

        return targets.size();
    }
}
