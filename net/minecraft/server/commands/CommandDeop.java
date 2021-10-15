package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentProfile;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.players.PlayerList;

public class CommandDeop {
    private static final SimpleCommandExceptionType ERROR_NOT_OP = new SimpleCommandExceptionType(new ChatMessage("commands.deop.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("deop").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentProfile.gameProfile()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(context.getSource().getServer().getPlayerList().getOpNames(), builder);
        }).executes((context) -> {
            return deopPlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"));
        })));
    }

    private static int deopPlayers(CommandListenerWrapper source, Collection<GameProfile> targets) throws CommandSyntaxException {
        PlayerList playerList = source.getServer().getPlayerList();
        int i = 0;

        for(GameProfile gameProfile : targets) {
            if (playerList.isOp(gameProfile)) {
                playerList.removeOp(gameProfile);
                ++i;
                source.sendMessage(new ChatMessage("commands.deop.success", targets.iterator().next().getName()), true);
            }
        }

        if (i == 0) {
            throw ERROR_NOT_OP.create();
        } else {
            source.getServer().kickUnlistedPlayers(source);
            return i;
        }
    }
}
