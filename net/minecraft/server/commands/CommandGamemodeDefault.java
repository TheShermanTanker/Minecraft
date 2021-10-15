package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.EnumGamemode;

public class CommandGamemodeDefault {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("defaultgamemode").requires((source) -> {
            return source.hasPermission(2);
        });

        for(EnumGamemode gameType : EnumGamemode.values()) {
            literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(gameType.getName()).executes((context) -> {
                return setMode(context.getSource(), gameType);
            }));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int setMode(CommandListenerWrapper source, EnumGamemode defaultGameMode) {
        int i = 0;
        MinecraftServer minecraftServer = source.getServer();
        minecraftServer.setDefaultGameType(defaultGameMode);
        EnumGamemode gameType = minecraftServer.getForcedGameType();
        if (gameType != null) {
            for(EntityPlayer serverPlayer : minecraftServer.getPlayerList().getPlayers()) {
                if (serverPlayer.setGameMode(gameType)) {
                    ++i;
                }
            }
        }

        source.sendMessage(new ChatMessage("commands.defaultgamemode.success", defaultGameMode.getLongDisplayName()), true);
        return i;
    }
}
