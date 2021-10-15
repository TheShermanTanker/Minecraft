package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreboardTeam;

public class CommandTeamMsg {
    private static final ChatModifier SUGGEST_STYLE = ChatModifier.EMPTY.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatMessage("chat.type.team.hover"))).setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.SUGGEST_COMMAND, "/teammsg "));
    private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(new ChatMessage("commands.teammsg.failed.noteam"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralCommandNode<CommandListenerWrapper> literalCommandNode = dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("teammsg").then(net.minecraft.commands.CommandDispatcher.argument("message", ArgumentChat.message()).executes((context) -> {
            return sendMessage(context.getSource(), ArgumentChat.getMessage(context, "message"));
        })));
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("tm").redirect(literalCommandNode));
    }

    private static int sendMessage(CommandListenerWrapper source, IChatBaseComponent message) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        ScoreboardTeam playerTeam = (ScoreboardTeam)entity.getScoreboardTeam();
        if (playerTeam == null) {
            throw ERROR_NOT_ON_TEAM.create();
        } else {
            IChatBaseComponent component = playerTeam.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
            List<EntityPlayer> list = source.getServer().getPlayerList().getPlayers();

            for(EntityPlayer serverPlayer : list) {
                if (serverPlayer == entity) {
                    serverPlayer.sendMessage(new ChatMessage("chat.type.team.sent", component, source.getScoreboardDisplayName(), message), entity.getUniqueID());
                } else if (serverPlayer.getScoreboardTeam() == playerTeam) {
                    serverPlayer.sendMessage(new ChatMessage("chat.type.team.text", component, source.getScoreboardDisplayName(), message), entity.getUniqueID());
                }
            }

            return list.size();
        }
    }
}
