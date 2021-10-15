package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentAngle;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;

public class CommandSetWorldSpawn {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("setworldspawn").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return setSpawn(context.getSource(), new BlockPosition(context.getSource().getPosition()), 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).executes((context) -> {
            return setSpawn(context.getSource(), ArgumentPosition.getSpawnablePos(context, "pos"), 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("angle", ArgumentAngle.angle()).executes((context) -> {
            return setSpawn(context.getSource(), ArgumentPosition.getSpawnablePos(context, "pos"), ArgumentAngle.getAngle(context, "angle"));
        }))));
    }

    private static int setSpawn(CommandListenerWrapper source, BlockPosition pos, float angle) {
        source.getWorld().setDefaultSpawnPos(pos, angle);
        source.sendMessage(new ChatMessage("commands.setworldspawn.success", pos.getX(), pos.getY(), pos.getZ(), angle), true);
        return 1;
    }
}
