package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Map.Entry;
import net.minecraft.EnumChatFormat;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;

public class CommandLocate {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.locate.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("locate").requires((source) -> {
            return source.hasPermission(2);
        });

        for(Entry<String, StructureGenerator<?>> entry : StructureGenerator.STRUCTURES_REGISTRY.entrySet()) {
            literalArgumentBuilder = literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(entry.getKey()).executes((context) -> {
                return locate(context.getSource(), entry.getValue());
            }));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int locate(CommandListenerWrapper source, StructureGenerator<?> structure) throws CommandSyntaxException {
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        BlockPosition blockPos2 = source.getWorld().findNearestMapFeature(structure, blockPos, 100, false);
        if (blockPos2 == null) {
            throw ERROR_FAILED.create();
        } else {
            return showLocateResult(source, structure.getFeatureName(), blockPos, blockPos2, "commands.locate.success");
        }
    }

    public static int showLocateResult(CommandListenerWrapper source, String structure, BlockPosition sourcePos, BlockPosition structurePos, String successMessage) {
        int i = MathHelper.floor(dist(sourcePos.getX(), sourcePos.getZ(), structurePos.getX(), structurePos.getZ()));
        IChatBaseComponent component = ChatComponentUtils.wrapInSquareBrackets(new ChatMessage("chat.coordinates", structurePos.getX(), "~", structurePos.getZ())).format((style) -> {
            return style.setColor(EnumChatFormat.GREEN).setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.SUGGEST_COMMAND, "/tp @s " + structurePos.getX() + " ~ " + structurePos.getZ())).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatMessage("chat.coordinates.tooltip")));
        });
        source.sendMessage(new ChatMessage(successMessage, structure, component, i), false);
        return i;
    }

    private static float dist(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return MathHelper.sqrt((float)(i * i + j * j));
    }
}
