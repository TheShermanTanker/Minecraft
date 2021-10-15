package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentAngle;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.World;

public class CommandSpawnpoint {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("spawnpoint").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return setSpawn(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException()), new BlockPosition(context.getSource().getPosition()), 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).executes((context) -> {
            return setSpawn(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), new BlockPosition(context.getSource().getPosition()), 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).executes((context) -> {
            return setSpawn(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentPosition.getSpawnablePos(context, "pos"), 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("angle", ArgumentAngle.angle()).executes((context) -> {
            return setSpawn(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentPosition.getSpawnablePos(context, "pos"), ArgumentAngle.getAngle(context, "angle"));
        })))));
    }

    private static int setSpawn(CommandListenerWrapper source, Collection<EntityPlayer> targets, BlockPosition pos, float angle) {
        ResourceKey<World> resourceKey = source.getWorld().getDimensionKey();

        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.setRespawnPosition(resourceKey, pos, angle, true, false);
        }

        String string = resourceKey.location().toString();
        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.spawnpoint.success.single", pos.getX(), pos.getY(), pos.getZ(), angle, string, targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.spawnpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), angle, string, targets.size()), true);
        }

        return targets.size();
    }
}
