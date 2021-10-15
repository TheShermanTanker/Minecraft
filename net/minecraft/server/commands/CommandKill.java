package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.Entity;

public class CommandKill {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("kill").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return kill(context.getSource(), ImmutableList.of(context.getSource().getEntityOrException()));
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).executes((context) -> {
            return kill(context.getSource(), ArgumentEntity.getEntities(context, "targets"));
        })));
    }

    private static int kill(CommandListenerWrapper source, Collection<? extends Entity> targets) {
        for(Entity entity : targets) {
            entity.killEntity();
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.kill.success.single", targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.kill.success.multiple", targets.size()), true);
        }

        return targets.size();
    }
}
