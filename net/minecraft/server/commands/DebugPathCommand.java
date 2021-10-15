package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.level.pathfinder.PathEntity;

public class DebugPathCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_MOB = new SimpleCommandExceptionType(new ChatComponentText("Source is not a mob"));
    private static final SimpleCommandExceptionType ERROR_NO_PATH = new SimpleCommandExceptionType(new ChatComponentText("Path not found"));
    private static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new ChatComponentText("Target not reached"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("debugpath").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("to", ArgumentPosition.blockPos()).executes((context) -> {
            return fillBlocks(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "to"));
        })));
    }

    private static int fillBlocks(CommandListenerWrapper source, BlockPosition pos) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        if (!(entity instanceof EntityInsentient)) {
            throw ERROR_NOT_MOB.create();
        } else {
            EntityInsentient mob = (EntityInsentient)entity;
            NavigationAbstract pathNavigation = new Navigation(mob, source.getWorld());
            PathEntity path = pathNavigation.createPath(pos, 0);
            PacketDebug.sendPathFindingPacket(source.getWorld(), mob, path, pathNavigation.getMaxDistanceToWaypoint());
            if (path == null) {
                throw ERROR_NO_PATH.create();
            } else if (!path.canReach()) {
                throw ERROR_NOT_COMPLETE.create();
            } else {
                source.sendMessage(new ChatComponentText("Made path"), true);
                return 1;
            }
        }
    }
}
