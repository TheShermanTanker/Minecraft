package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.EnumChatFormat;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;

public class JfrCommand {
    private static final SimpleCommandExceptionType START_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.jfr.start.failed"));
    private static final DynamicCommandExceptionType DUMP_FAILED = new DynamicCommandExceptionType((message) -> {
        return new ChatMessage("commands.jfr.dump.failed", message);
    });

    private JfrCommand() {
    }

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("jfr").requires((source) -> {
            return source.hasPermission(4);
        }).then(net.minecraft.commands.CommandDispatcher.literal("start").executes((context) -> {
            return startJfr(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("stop").executes((context) -> {
            return stopJfr(context.getSource());
        })));
    }

    private static int startJfr(CommandListenerWrapper source) throws CommandSyntaxException {
        Environment environment = Environment.from(source.getServer());
        if (!JvmProfiler.INSTANCE.start(environment)) {
            throw START_FAILED.create();
        } else {
            source.sendMessage(new ChatMessage("commands.jfr.started"), false);
            return 1;
        }
    }

    private static int stopJfr(CommandListenerWrapper source) throws CommandSyntaxException {
        try {
            Path path = Paths.get(".").relativize(JvmProfiler.INSTANCE.stop().normalize());
            Path path2 = source.getServer().isPublished() && !SharedConstants.IS_RUNNING_IN_IDE ? path : path.toAbsolutePath();
            IChatBaseComponent component = (new ChatComponentText(path.toString())).withStyle(EnumChatFormat.UNDERLINE).format((style) -> {
                return style.setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.COPY_TO_CLIPBOARD, path2.toString())).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatMessage("chat.copy.click")));
            });
            source.sendMessage(new ChatMessage("commands.jfr.stopped", component), false);
            return 1;
        } catch (Throwable var4) {
            throw DUMP_FAILED.create(var4.getMessage());
        }
    }
}
