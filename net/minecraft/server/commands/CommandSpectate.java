package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EnumGamemode;

public class CommandSpectate {
    private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(new ChatMessage("commands.spectate.self"));
    private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType((playerName) -> {
        return new ChatMessage("commands.spectate.not_spectator", playerName);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("spectate").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return spectate(context.getSource(), (Entity)null, context.getSource().getPlayerOrException());
        }).then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentEntity.entity()).executes((context) -> {
            return spectate(context.getSource(), ArgumentEntity.getEntity(context, "target"), context.getSource().getPlayerOrException());
        }).then(net.minecraft.commands.CommandDispatcher.argument("player", ArgumentEntity.player()).executes((context) -> {
            return spectate(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentEntity.getPlayer(context, "player"));
        }))));
    }

    private static int spectate(CommandListenerWrapper source, @Nullable Entity entity, EntityPlayer player) throws CommandSyntaxException {
        if (player == entity) {
            throw ERROR_SELF.create();
        } else if (player.gameMode.getGameMode() != EnumGamemode.SPECTATOR) {
            throw ERROR_NOT_SPECTATOR.create(player.getScoreboardDisplayName());
        } else {
            player.setSpectatorTarget(entity);
            if (entity != null) {
                source.sendMessage(new ChatMessage("commands.spectate.success.started", entity.getScoreboardDisplayName()), false);
            } else {
                source.sendMessage(new ChatMessage("commands.spectate.success.stopped"), false);
            }

            return 1;
        }
    }
}
