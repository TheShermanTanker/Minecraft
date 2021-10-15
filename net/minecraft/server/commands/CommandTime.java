package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentTime;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;

public class CommandTime {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("time").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.literal("day").executes((context) -> {
            return setTime(context.getSource(), 1000);
        })).then(net.minecraft.commands.CommandDispatcher.literal("noon").executes((context) -> {
            return setTime(context.getSource(), 6000);
        })).then(net.minecraft.commands.CommandDispatcher.literal("night").executes((context) -> {
            return setTime(context.getSource(), 13000);
        })).then(net.minecraft.commands.CommandDispatcher.literal("midnight").executes((context) -> {
            return setTime(context.getSource(), 18000);
        })).then(net.minecraft.commands.CommandDispatcher.argument("time", ArgumentTime.time()).executes((context) -> {
            return setTime(context.getSource(), IntegerArgumentType.getInteger(context, "time"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("time", ArgumentTime.time()).executes((context) -> {
            return addTime(context.getSource(), IntegerArgumentType.getInteger(context, "time"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("query").then(net.minecraft.commands.CommandDispatcher.literal("daytime").executes((context) -> {
            return queryTime(context.getSource(), getDayTime(context.getSource().getWorld()));
        })).then(net.minecraft.commands.CommandDispatcher.literal("gametime").executes((context) -> {
            return queryTime(context.getSource(), (int)(context.getSource().getWorld().getTime() % 2147483647L));
        })).then(net.minecraft.commands.CommandDispatcher.literal("day").executes((context) -> {
            return queryTime(context.getSource(), (int)(context.getSource().getWorld().getDayTime() / 24000L % 2147483647L));
        }))));
    }

    private static int getDayTime(WorldServer world) {
        return (int)(world.getDayTime() % 24000L);
    }

    private static int queryTime(CommandListenerWrapper source, int time) {
        source.sendMessage(new ChatMessage("commands.time.query", time), false);
        return time;
    }

    public static int setTime(CommandListenerWrapper source, int time) {
        for(WorldServer serverLevel : source.getServer().getWorlds()) {
            serverLevel.setDayTime((long)time);
        }

        source.sendMessage(new ChatMessage("commands.time.set", time), true);
        return getDayTime(source.getWorld());
    }

    public static int addTime(CommandListenerWrapper source, int time) {
        for(WorldServer serverLevel : source.getServer().getWorlds()) {
            serverLevel.setDayTime(serverLevel.getDayTime() + (long)time);
        }

        int i = getDayTime(source.getWorld());
        source.sendMessage(new ChatMessage("commands.time.set", i), true);
        return i;
    }
}
