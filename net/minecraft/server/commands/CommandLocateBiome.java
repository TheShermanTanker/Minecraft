package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.biome.BiomeBase;

public class CommandLocateBiome {
    public static final DynamicCommandExceptionType ERROR_INVALID_BIOME = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("commands.locatebiome.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("commands.locatebiome.notFound", id);
    });
    private static final int MAX_SEARCH_RADIUS = 6400;
    private static final int SEARCH_STEP = 8;

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("locatebiome").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("biome", ArgumentMinecraftKeyRegistered.id()).suggests(CompletionProviders.AVAILABLE_BIOMES).executes((context) -> {
            return locateBiome(context.getSource(), context.getArgument("biome", MinecraftKey.class));
        })));
    }

    private static int locateBiome(CommandListenerWrapper source, MinecraftKey id) throws CommandSyntaxException {
        BiomeBase biome = source.getServer().getCustomRegistry().registryOrThrow(IRegistry.BIOME_REGISTRY).getOptional(id).orElseThrow(() -> {
            return ERROR_INVALID_BIOME.create(id);
        });
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        BlockPosition blockPos2 = source.getWorld().findNearestBiome(biome, blockPos, 6400, 8);
        String string = id.toString();
        if (blockPos2 == null) {
            throw ERROR_BIOME_NOT_FOUND.create(string);
        } else {
            return CommandLocate.showLocateResult(source, string, blockPos, blockPos2, "commands.locatebiome.success");
        }
    }
}
