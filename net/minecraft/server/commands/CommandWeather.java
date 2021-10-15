package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;

public class CommandWeather {
    private static final int DEFAULT_TIME = 6000;

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("weather").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("clear").executes((context) -> {
            return setClear(context.getSource(), 6000);
        }).then(net.minecraft.commands.CommandDispatcher.argument("duration", IntegerArgumentType.integer(0, 1000000)).executes((context) -> {
            return setClear(context.getSource(), IntegerArgumentType.getInteger(context, "duration") * 20);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("rain").executes((context) -> {
            return setRain(context.getSource(), 6000);
        }).then(net.minecraft.commands.CommandDispatcher.argument("duration", IntegerArgumentType.integer(0, 1000000)).executes((context) -> {
            return setRain(context.getSource(), IntegerArgumentType.getInteger(context, "duration") * 20);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("thunder").executes((context) -> {
            return setThunder(context.getSource(), 6000);
        }).then(net.minecraft.commands.CommandDispatcher.argument("duration", IntegerArgumentType.integer(0, 1000000)).executes((context) -> {
            return setThunder(context.getSource(), IntegerArgumentType.getInteger(context, "duration") * 20);
        }))));
    }

    private static int setClear(CommandListenerWrapper source, int duration) {
        source.getWorld().setWeatherParameters(duration, 0, false, false);
        source.sendMessage(new ChatMessage("commands.weather.set.clear"), true);
        return duration;
    }

    private static int setRain(CommandListenerWrapper source, int duration) {
        source.getWorld().setWeatherParameters(0, duration, true, false);
        source.sendMessage(new ChatMessage("commands.weather.set.rain"), true);
        return duration;
    }

    private static int setThunder(CommandListenerWrapper source, int duration) {
        source.getWorld().setWeatherParameters(0, duration, true, true);
        source.sendMessage(new ChatMessage("commands.weather.set.thunder"), true);
        return duration;
    }
}
