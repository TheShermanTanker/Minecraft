package net.minecraft.util.profiling.jfr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nullable;
import net.minecraft.server.DispenserRegistry;
import net.minecraft.util.profiling.jfr.parse.JfrStatsParser;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.Supplier;

public class SummaryReporter {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Runnable onDeregistration;

    protected SummaryReporter(Runnable stopCallback) {
        this.onDeregistration = stopCallback;
    }

    public void recordingStopped(@Nullable Path dumpPath) {
        if (dumpPath != null) {
            this.onDeregistration.run();
            infoWithFallback(() -> {
                return "Dumped flight recorder profiling to " + dumpPath;
            });

            JfrStatsResult jfrStatsResult;
            try {
                jfrStatsResult = JfrStatsParser.parse(dumpPath);
            } catch (Throwable var5) {
                warnWithFallback(() -> {
                    return "Failed to parse JFR recording";
                }, var5);
                return;
            }

            try {
                infoWithFallback(jfrStatsResult::asJson);
                Path path = dumpPath.resolveSibling("jfr-report-" + StringUtils.substringBefore(dumpPath.getFileName().toString(), ".jfr") + ".json");
                Files.writeString(path, jfrStatsResult.asJson(), StandardOpenOption.CREATE);
                infoWithFallback(() -> {
                    return "Dumped recording summary to " + path;
                });
            } catch (Throwable var4) {
                warnWithFallback(() -> {
                    return "Failed to output JFR report";
                }, var4);
            }

        }
    }

    private static void infoWithFallback(Supplier<String> messageSupplier) {
        if (log4jIsActive()) {
            LOGGER.info(messageSupplier);
        } else {
            DispenserRegistry.realStdoutPrintln(messageSupplier.get());
        }

    }

    private static void warnWithFallback(Supplier<String> messageSupplier, Throwable throwable) {
        if (log4jIsActive()) {
            LOGGER.warn(messageSupplier, throwable);
        } else {
            DispenserRegistry.realStdoutPrintln(messageSupplier.get());
            throwable.printStackTrace(DispenserRegistry.STDOUT);
        }

    }

    private static boolean log4jIsActive() {
        LoggerContext loggerContext = LogManager.getContext();
        if (loggerContext instanceof LifeCycle) {
            LifeCycle lifeCycle = (LifeCycle)loggerContext;
            return !lifeCycle.isStopped();
        } else {
            return true;
        }
    }
}
