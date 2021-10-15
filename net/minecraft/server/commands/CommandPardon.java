package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentProfile;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.players.GameProfileBanList;

public class CommandPardon {
    private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(new ChatMessage("commands.pardon.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("pardon").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentProfile.gameProfile()).suggests((context, builder) -> {
            return ICompletionProvider.suggest(context.getSource().getServer().getPlayerList().getProfileBans().getEntries(), builder);
        }).executes((context) -> {
            return pardonPlayers(context.getSource(), ArgumentProfile.getGameProfiles(context, "targets"));
        })));
    }

    private static int pardonPlayers(CommandListenerWrapper source, Collection<GameProfile> targets) throws CommandSyntaxException {
        GameProfileBanList userBanList = source.getServer().getPlayerList().getProfileBans();
        int i = 0;

        for(GameProfile gameProfile : targets) {
            if (userBanList.isBanned(gameProfile)) {
                userBanList.remove(gameProfile);
                ++i;
                source.sendMessage(new ChatMessage("commands.pardon.success", ChatComponentUtils.getDisplayName(gameProfile)), true);
            }
        }

        if (i == 0) {
            throw ERROR_NOT_BANNED.create();
        } else {
            return i;
        }
    }
}
