package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentTime;
import net.minecraft.commands.arguments.item.ArgumentTag;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.timers.CustomFunctionCallback;
import net.minecraft.world.level.timers.CustomFunctionCallbackTag;
import net.minecraft.world.level.timers.CustomFunctionCallbackTimerQueue;

public class CommandSchedule {
    private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType(new ChatMessage("commands.schedule.same_tick"));
    private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType((eventName) -> {
        return new ChatMessage("commands.schedule.cleared.failure", eventName);
    });
    private static final SuggestionProvider<CommandListenerWrapper> SUGGEST_SCHEDULE = (context, builder) -> {
        return ICompletionProvider.suggest(context.getSource().getServer().getSaveData().overworldData().getScheduledEvents().getEventsIds(), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("schedule").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("function").then(net.minecraft.commands.CommandDispatcher.argument("function", ArgumentTag.functions()).suggests(CommandFunction.SUGGEST_FUNCTION).then(net.minecraft.commands.CommandDispatcher.argument("time", ArgumentTime.time()).executes((context) -> {
            return schedule(context.getSource(), ArgumentTag.getFunctionOrTag(context, "function"), IntegerArgumentType.getInteger(context, "time"), true);
        }).then(net.minecraft.commands.CommandDispatcher.literal("append").executes((context) -> {
            return schedule(context.getSource(), ArgumentTag.getFunctionOrTag(context, "function"), IntegerArgumentType.getInteger(context, "time"), false);
        })).then(net.minecraft.commands.CommandDispatcher.literal("replace").executes((context) -> {
            return schedule(context.getSource(), ArgumentTag.getFunctionOrTag(context, "function"), IntegerArgumentType.getInteger(context, "time"), true);
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("clear").then(net.minecraft.commands.CommandDispatcher.argument("function", StringArgumentType.greedyString()).suggests(SUGGEST_SCHEDULE).executes((context) -> {
            return remove(context.getSource(), StringArgumentType.getString(context, "function"));
        }))));
    }

    private static int schedule(CommandListenerWrapper source, Pair<MinecraftKey, Either<CustomFunction, Tag<CustomFunction>>> function, int time, boolean replace) throws CommandSyntaxException {
        if (time == 0) {
            throw ERROR_SAME_TICK.create();
        } else {
            long l = source.getWorld().getTime() + (long)time;
            MinecraftKey resourceLocation = function.getFirst();
            CustomFunctionCallbackTimerQueue<MinecraftServer> timerQueue = source.getServer().getSaveData().overworldData().getScheduledEvents();
            function.getSecond().ifLeft((functionx) -> {
                String string = resourceLocation.toString();
                if (replace) {
                    timerQueue.remove(string);
                }

                timerQueue.schedule(string, l, new CustomFunctionCallback(resourceLocation));
                source.sendMessage(new ChatMessage("commands.schedule.created.function", resourceLocation, time, l), true);
            }).ifRight((tag) -> {
                String string = "#" + resourceLocation;
                if (replace) {
                    timerQueue.remove(string);
                }

                timerQueue.schedule(string, l, new CustomFunctionCallbackTag(resourceLocation));
                source.sendMessage(new ChatMessage("commands.schedule.created.tag", resourceLocation, time, l), true);
            });
            return Math.floorMod(l, Integer.MAX_VALUE);
        }
    }

    private static int remove(CommandListenerWrapper source, String eventName) throws CommandSyntaxException {
        int i = source.getServer().getSaveData().overworldData().getScheduledEvents().remove(eventName);
        if (i == 0) {
            throw ERROR_CANT_REMOVE.create(eventName);
        } else {
            source.sendMessage(new ChatMessage("commands.schedule.cleared.success", i, eventName), true);
            return i;
        }
    }
}
