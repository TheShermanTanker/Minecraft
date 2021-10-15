package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;

public class CommandDifficulty {
    private static final DynamicCommandExceptionType ERROR_ALREADY_DIFFICULT = new DynamicCommandExceptionType((difficulty) -> {
        return new ChatMessage("commands.difficulty.failure", difficulty);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("difficulty");

        for(EnumDifficulty difficulty : EnumDifficulty.values()) {
            literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(difficulty.getKey()).executes((context) -> {
                return setDifficulty(context.getSource(), difficulty);
            }));
        }

        dispatcher.register(literalArgumentBuilder.requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            EnumDifficulty difficulty = context.getSource().getWorld().getDifficulty();
            context.getSource().sendMessage(new ChatMessage("commands.difficulty.query", difficulty.getDisplayName()), false);
            return difficulty.getId();
        }));
    }

    public static int setDifficulty(CommandListenerWrapper source, EnumDifficulty difficulty) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer.getSaveData().getDifficulty() == difficulty) {
            throw ERROR_ALREADY_DIFFICULT.create(difficulty.getKey());
        } else {
            minecraftServer.setDifficulty(difficulty, true);
            source.sendMessage(new ChatMessage("commands.difficulty.success", difficulty.getDisplayName()), true);
            return 0;
        }
    }
}
