package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.coordinates.ArgumentVec2;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec2F;

public class CommandWorldBorder {
    private static final SimpleCommandExceptionType ERROR_SAME_CENTER = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.center.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_SIZE = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.set.failed.nochange"));
    private static final SimpleCommandExceptionType ERROR_TOO_SMALL = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.set.failed.small"));
    private static final SimpleCommandExceptionType ERROR_TOO_BIG = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.set.failed.big", 5.9999968E7D));
    private static final SimpleCommandExceptionType ERROR_TOO_FAR_OUT = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.set.failed.far", 2.9999984E7D));
    private static final SimpleCommandExceptionType ERROR_SAME_WARNING_TIME = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.warning.time.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_WARNING_DISTANCE = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.warning.distance.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_BUFFER = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.damage.buffer.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_AMOUNT = new SimpleCommandExceptionType(new ChatMessage("commands.worldborder.damage.amount.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("worldborder").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("distance", DoubleArgumentType.doubleArg(-5.9999968E7D, 5.9999968E7D)).executes((context) -> {
            return setSize(context.getSource(), context.getSource().getWorld().getWorldBorder().getSize() + DoubleArgumentType.getDouble(context, "distance"), 0L);
        }).then(net.minecraft.commands.CommandDispatcher.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
            return setSize(context.getSource(), context.getSource().getWorld().getWorldBorder().getSize() + DoubleArgumentType.getDouble(context, "distance"), context.getSource().getWorld().getWorldBorder().getLerpRemainingTime() + (long)IntegerArgumentType.getInteger(context, "time") * 1000L);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.argument("distance", DoubleArgumentType.doubleArg(-5.9999968E7D, 5.9999968E7D)).executes((context) -> {
            return setSize(context.getSource(), DoubleArgumentType.getDouble(context, "distance"), 0L);
        }).then(net.minecraft.commands.CommandDispatcher.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
            return setSize(context.getSource(), DoubleArgumentType.getDouble(context, "distance"), (long)IntegerArgumentType.getInteger(context, "time") * 1000L);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("center").then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec2.vec2()).executes((context) -> {
            return setCenter(context.getSource(), ArgumentVec2.getVec2(context, "pos"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("damage").then(net.minecraft.commands.CommandDispatcher.literal("amount").then(net.minecraft.commands.CommandDispatcher.argument("damagePerBlock", FloatArgumentType.floatArg(0.0F)).executes((context) -> {
            return setDamageAmount(context.getSource(), FloatArgumentType.getFloat(context, "damagePerBlock"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("buffer").then(net.minecraft.commands.CommandDispatcher.argument("distance", FloatArgumentType.floatArg(0.0F)).executes((context) -> {
            return setDamageBuffer(context.getSource(), FloatArgumentType.getFloat(context, "distance"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("get").executes((context) -> {
            return getSize(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("warning").then(net.minecraft.commands.CommandDispatcher.literal("distance").then(net.minecraft.commands.CommandDispatcher.argument("distance", IntegerArgumentType.integer(0)).executes((context) -> {
            return setWarningDistance(context.getSource(), IntegerArgumentType.getInteger(context, "distance"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("time").then(net.minecraft.commands.CommandDispatcher.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
            return setWarningTime(context.getSource(), IntegerArgumentType.getInteger(context, "time"));
        })))));
    }

    private static int setDamageBuffer(CommandListenerWrapper source, float distance) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getDamageBuffer() == (double)distance) {
            throw ERROR_SAME_DAMAGE_BUFFER.create();
        } else {
            worldBorder.setDamageBuffer((double)distance);
            source.sendMessage(new ChatMessage("commands.worldborder.damage.buffer.success", String.format(Locale.ROOT, "%.2f", distance)), true);
            return (int)distance;
        }
    }

    private static int setDamageAmount(CommandListenerWrapper source, float damagePerBlock) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getDamageAmount() == (double)damagePerBlock) {
            throw ERROR_SAME_DAMAGE_AMOUNT.create();
        } else {
            worldBorder.setDamageAmount((double)damagePerBlock);
            source.sendMessage(new ChatMessage("commands.worldborder.damage.amount.success", String.format(Locale.ROOT, "%.2f", damagePerBlock)), true);
            return (int)damagePerBlock;
        }
    }

    private static int setWarningTime(CommandListenerWrapper source, int time) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getWarningTime() == time) {
            throw ERROR_SAME_WARNING_TIME.create();
        } else {
            worldBorder.setWarningTime(time);
            source.sendMessage(new ChatMessage("commands.worldborder.warning.time.success", time), true);
            return time;
        }
    }

    private static int setWarningDistance(CommandListenerWrapper source, int distance) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getWarningDistance() == distance) {
            throw ERROR_SAME_WARNING_DISTANCE.create();
        } else {
            worldBorder.setWarningDistance(distance);
            source.sendMessage(new ChatMessage("commands.worldborder.warning.distance.success", distance), true);
            return distance;
        }
    }

    private static int getSize(CommandListenerWrapper source) {
        double d = source.getServer().overworld().getWorldBorder().getSize();
        source.sendMessage(new ChatMessage("commands.worldborder.get", String.format(Locale.ROOT, "%.0f", d)), false);
        return MathHelper.floor(d + 0.5D);
    }

    private static int setCenter(CommandListenerWrapper source, Vec2F pos) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getCenterX() == (double)pos.x && worldBorder.getCenterZ() == (double)pos.y) {
            throw ERROR_SAME_CENTER.create();
        } else if (!((double)Math.abs(pos.x) > 2.9999984E7D) && !((double)Math.abs(pos.y) > 2.9999984E7D)) {
            worldBorder.setCenter((double)pos.x, (double)pos.y);
            source.sendMessage(new ChatMessage("commands.worldborder.center.success", String.format(Locale.ROOT, "%.2f", pos.x), String.format("%.2f", pos.y)), true);
            return 0;
        } else {
            throw ERROR_TOO_FAR_OUT.create();
        }
    }

    private static int setSize(CommandListenerWrapper source, double distance, long time) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        double d = worldBorder.getSize();
        if (d == distance) {
            throw ERROR_SAME_SIZE.create();
        } else if (distance < 1.0D) {
            throw ERROR_TOO_SMALL.create();
        } else if (distance > 5.9999968E7D) {
            throw ERROR_TOO_BIG.create();
        } else {
            if (time > 0L) {
                worldBorder.transitionSizeBetween(d, distance, time);
                if (distance > d) {
                    source.sendMessage(new ChatMessage("commands.worldborder.set.grow", String.format(Locale.ROOT, "%.1f", distance), Long.toString(time / 1000L)), true);
                } else {
                    source.sendMessage(new ChatMessage("commands.worldborder.set.shrink", String.format(Locale.ROOT, "%.1f", distance), Long.toString(time / 1000L)), true);
                }
            } else {
                worldBorder.setSize(distance);
                source.sendMessage(new ChatMessage("commands.worldborder.set.immediate", String.format(Locale.ROOT, "%.1f", distance)), true);
            }

            return (int)(distance - d);
        }
    }
}
