package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.NaturalSpawner;

public class CommandDebugMobSpawning {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("debugmobspawning").requires((source) -> {
            return source.hasPermission(2);
        });

        for(EnumCreatureType mobCategory : EnumCreatureType.values()) {
            literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(mobCategory.getName()).then(net.minecraft.commands.CommandDispatcher.argument("at", ArgumentPosition.blockPos()).executes((context) -> {
                return spawnMobs(context.getSource(), mobCategory, ArgumentPosition.getLoadedBlockPos(context, "at"));
            })));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int spawnMobs(CommandListenerWrapper source, EnumCreatureType group, BlockPosition pos) {
        NaturalSpawner.spawnCategoryForPosition(group, source.getWorld(), pos);
        return 1;
    }
}
