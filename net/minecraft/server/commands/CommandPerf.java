package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.FileUtils;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FileZipper;
import net.minecraft.util.TimeRange;
import net.minecraft.util.profiling.MethodProfilerResults;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandPerf {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(new ChatMessage("commands.perf.notRunning"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(new ChatMessage("commands.perf.alreadyRunning"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("perf").requires((source) -> {
            return source.hasPermission(4);
        }).then(net.minecraft.commands.CommandDispatcher.literal("start").executes((context) -> {
            return startProfilingDedicatedServer(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("stop").executes((context) -> {
            return stopProfilingDedicatedServer(context.getSource());
        })));
    }

    private static int startProfilingDedicatedServer(CommandListenerWrapper source) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer.isRecordingMetrics()) {
            throw ERROR_ALREADY_RUNNING.create();
        } else {
            Consumer<MethodProfilerResults> consumer = (result) -> {
                whenStopped(source, result);
            };
            Consumer<Path> consumer2 = (dumpDirectory) -> {
                saveResults(source, dumpDirectory, minecraftServer);
            };
            minecraftServer.startRecordingMetrics(consumer, consumer2);
            source.sendMessage(new ChatMessage("commands.perf.started"), false);
            return 0;
        }
    }

    private static int stopProfilingDedicatedServer(CommandListenerWrapper source) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (!minecraftServer.isRecordingMetrics()) {
            throw ERROR_NOT_RUNNING.create();
        } else {
            minecraftServer.finishRecordingMetrics();
            return 0;
        }
    }

    private static void saveResults(CommandListenerWrapper source, Path tempProfilingDirectory, MinecraftServer server) {
        String string = String.format("%s-%s-%s", (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()), server.getSaveData().getName(), SharedConstants.getGameVersion().getId());

        String string2;
        try {
            string2 = FileUtils.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, string, ".zip");
        } catch (IOException var11) {
            source.sendFailureMessage(new ChatMessage("commands.perf.reportFailed"));
            LOGGER.error(var11);
            return;
        }

        FileZipper fileZipper = new FileZipper(MetricsPersister.PROFILING_RESULTS_DIR.resolve(string2));

        try {
            fileZipper.add(Paths.get("system.txt"), server.fillSystemReport(new SystemReport()).toLineSeparatedString());
            fileZipper.add(tempProfilingDirectory);
        } catch (Throwable var10) {
            try {
                fileZipper.close();
            } catch (Throwable var8) {
                var10.addSuppressed(var8);
            }

            throw var10;
        }

        fileZipper.close();

        try {
            org.apache.commons.io.FileUtils.forceDelete(tempProfilingDirectory.toFile());
        } catch (IOException var9) {
            LOGGER.warn("Failed to delete temporary profiling file {}", tempProfilingDirectory, var9);
        }

        source.sendMessage(new ChatMessage("commands.perf.reportSaved", string2), false);
    }

    private static void whenStopped(CommandListenerWrapper source, MethodProfilerResults result) {
        int i = result.getTickDuration();
        double d = (double)result.getNanoDuration() / (double)TimeRange.NANOSECONDS_PER_SECOND;
        source.sendMessage(new ChatMessage("commands.perf.stopped", String.format(Locale.ROOT, "%.2f", d), i, String.format(Locale.ROOT, "%.2f", (double)i / d)), false);
    }
}
