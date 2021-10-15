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

public class CommandOp {
    private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(new ChatMessage("commands.op.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("op").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentProfile.gameProfile()).suggests((context, builder) -> {
            PlayerList playerList = context.getSource().getServer().getPlayerList();
            return ICompletionProvider.suggest(playerList.getPlayers().stream().filter((player) -> {
                return !playerList.isOp(player.getProfile());
            }).map((player) -> {
                return player.getProfile().getName();
            }), builder);
        }).executes((context) -> {
            return opPlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"));
        })));
    }

    private static int opPlayers(CommandListenerWrapper source, Collection<GameProfile> targets) throws CommandSyntaxException {
        PlayerList playerList = source.getServer().getPlayerList();
        int i = 0;

        for(GameProfile gameProfile : targets) {
            if (!playerList.isOp(gameProfile)) {
                playerList.addOp(gameProfile);
                ++i;
                source.sendMessage(new ChatMessage("commands.op.success", targets.iterator().next().getName()), true);
            }
        }

        if (i == 0) {
            throw ERROR_ALREADY_OP.create();
        } else {
            return i;
        }
    }
}
