package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;

public class CommandGamemode {
    public static final int PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("gamemode").requires((source) -> {
            return source.hasPermission(2);
        });

        for(EnumGamemode gameType : EnumGamemode.values()) {
            literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(gameType.getName()).executes((context) -> {
                return setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), gameType);
            }).then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentEntity.players()).executes((context) -> {
                return setMode(context, ArgumentEntity.getPlayers(context, "target"), gameType);
            })));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static void logGamemodeChange(CommandListenerWrapper source, EntityPlayer player, EnumGamemode gameMode) {
        IChatBaseComponent component = new ChatMessage("gameMode." + gameMode.getName());
        if (source.getEntity() == player) {
            source.sendMessage(new ChatMessage("commands.gamemode.success.self", component), true);
        } else {
            if (source.getWorld().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                player.sendMessage(new ChatMessage("gameMode.changed", component), SystemUtils.NIL_UUID);
            }

            source.sendMessage(new ChatMessage("commands.gamemode.success.other", player.getScoreboardDisplayName(), component), true);
        }

    }

    private static int setMode(CommandContext<CommandListenerWrapper> context, Collection<EntityPlayer> targets, EnumGamemode gameMode) {
        int i = 0;

        for(EntityPlayer serverPlayer : targets) {
            if (serverPlayer.setGameMode(gameMode)) {
                logGamemodeChange(context.getSource(), serverPlayer, gameMode);
                ++i;
            }
        }

        return i;
    }
}
