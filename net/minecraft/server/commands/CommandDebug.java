package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.commands.ICommandListener;
import net.minecraft.commands.arguments.item.ArgumentTag;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.CustomFunctionData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeRange;
import net.minecraft.util.profiling.MethodProfilerResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandDebug {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(new ChatMessage("commands.debug.notRunning"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(new ChatMessage("commands.debug.alreadyRunning"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("debug").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.literal("start").executes((context) -> {
            return start(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("stop").executes((context) -> {
            return stop(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("function").requires((commandSourceStack) -> {
            return commandSourceStack.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.argument("name", ArgumentTag.functions()).suggests(CommandFunction.SUGGEST_FUNCTION).executes((context) -> {
            return traceFunction(context.getSource(), ArgumentTag.getFunctions(context, "name"));
        }))));
    }

    private static int start(CommandListenerWrapper source) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer.isTimeProfilerRunning()) {
            throw ERROR_ALREADY_RUNNING.create();
        } else {
            minecraftServer.startTimeProfiler();
            source.sendMessage(new ChatMessage("commands.debug.started"), true);
            return 0;
        }
    }

    private static int stop(CommandListenerWrapper source) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (!minecraftServer.isTimeProfilerRunning()) {
            throw ERROR_NOT_RUNNING.create();
        } else {
            MethodProfilerResults profileResults = minecraftServer.stopTimeProfiler();
            double d = (double)profileResults.getNanoDuration() / (double)TimeRange.NANOSECONDS_PER_SECOND;
            double e = (double)profileResults.getTickDuration() / d;
            source.sendMessage(new ChatMessage("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d), profileResults.getTickDuration(), String.format("%.2f", e)), true);
            return (int)e;
        }
    }

    private static int traceFunction(CommandListenerWrapper source, Collection<CustomFunction> functions) {
        int i = 0;
        MinecraftServer minecraftServer = source.getServer();
        String string = "debug-trace-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt";

        try {
            Path path = minecraftServer.getFile("debug").toPath();
            Files.createDirectories(path);
            Writer writer = Files.newBufferedWriter(path.resolve(string), StandardCharsets.UTF_8);

            try {
                PrintWriter printWriter = new PrintWriter(writer);

                for(CustomFunction commandFunction : functions) {
                    printWriter.println((Object)commandFunction.getId());
                    CommandDebug.Tracer tracer = new CommandDebug.Tracer(printWriter);
                    i += source.getServer().getFunctionData().execute(commandFunction, source.withSource(tracer).withMaximumPermission(2), tracer);
                }
            } catch (Throwable var12) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                    }
                }

                throw var12;
            }

            if (writer != null) {
                writer.close();
            }
        } catch (IOException | UncheckedIOException var13) {
            LOGGER.warn("Tracing failed", (Throwable)var13);
            source.sendFailureMessage(new ChatMessage("commands.debug.function.traceFailed"));
        }

        if (functions.size() == 1) {
            source.sendMessage(new ChatMessage("commands.debug.function.success.single", i, functions.iterator().next().getId(), string), true);
        } else {
            source.sendMessage(new ChatMessage("commands.debug.function.success.multiple", i, functions.size(), string), true);
        }

        return i;
    }

    static class Tracer implements ICommandListener, CustomFunctionData.TraceCallbacks {
        public static final int INDENT_OFFSET = 1;
        private final PrintWriter output;
        private int lastIndent;
        private boolean waitingForResult;

        Tracer(PrintWriter writer) {
            this.output = writer;
        }

        private void indentAndSave(int width) {
            this.printIndent(width);
            this.lastIndent = width;
        }

        private void printIndent(int width) {
            for(int i = 0; i < width + 1; ++i) {
                this.output.write("    ");
            }

        }

        private void newLine() {
            if (this.waitingForResult) {
                this.output.println();
                this.waitingForResult = false;
            }

        }

        @Override
        public void onCommand(int depth, String command) {
            this.newLine();
            this.indentAndSave(depth);
            this.output.print("[C] ");
            this.output.print(command);
            this.waitingForResult = true;
        }

        @Override
        public void onReturn(int depth, String command, int result) {
            if (this.waitingForResult) {
                this.output.print(" -> ");
                this.output.println(result);
                this.waitingForResult = false;
            } else {
                this.indentAndSave(depth);
                this.output.print("[R = ");
                this.output.print(result);
                this.output.print("] ");
                this.output.println(command);
            }

        }

        @Override
        public void onCall(int depth, MinecraftKey function, int size) {
            this.newLine();
            this.indentAndSave(depth);
            this.output.print("[F] ");
            this.output.print((Object)function);
            this.output.print(" size=");
            this.output.println(size);
        }

        @Override
        public void onError(int depth, String message) {
            this.newLine();
            this.indentAndSave(depth + 1);
            this.output.print("[E] ");
            this.output.print(message);
        }

        @Override
        public void sendMessage(IChatBaseComponent message, UUID sender) {
            this.newLine();
            this.printIndent(this.lastIndent + 1);
            this.output.print("[M] ");
            if (sender != SystemUtils.NIL_UUID) {
                this.output.print((Object)sender);
                this.output.print(": ");
            }

            this.output.println(message.getString());
        }

        @Override
        public boolean shouldSendSuccess() {
            return true;
        }

        @Override
        public boolean shouldSendFailure() {
            return true;
        }

        @Override
        public boolean shouldBroadcastCommands() {
            return false;
        }

        @Override
        public boolean alwaysAccepts() {
            return true;
        }
    }
}
