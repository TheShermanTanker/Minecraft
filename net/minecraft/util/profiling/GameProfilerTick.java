package net.minecraft.util.profiling;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameProfilerTick {
    private static final Logger LOGGER = LogManager.getLogger();
    private final LongSupplier realTime;
    private final long saveThreshold;
    private int tick;
    private final File location;
    private GameProfilerFillerActive profiler = GameProfilerDisabled.INSTANCE;

    public GameProfilerTick(LongSupplier timeGetter, String filename, long overtime) {
        this.realTime = timeGetter;
        this.location = new File("debug", filename);
        this.saveThreshold = overtime;
    }

    public GameProfilerFiller startTick() {
        this.profiler = new MethodProfiler(this.realTime, () -> {
            return this.tick;
        }, false);
        ++this.tick;
        return this.profiler;
    }

    public void endTick() {
        if (this.profiler != GameProfilerDisabled.INSTANCE) {
            MethodProfilerResults profileResults = this.profiler.getResults();
            this.profiler = GameProfilerDisabled.INSTANCE;
            if (profileResults.getNanoDuration() >= this.saveThreshold) {
                File file = new File(this.location, "tick-results-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
                profileResults.saveResults(file.toPath());
                LOGGER.info("Recorded long tick -- wrote info to: {}", (Object)file.getAbsolutePath());
            }

        }
    }

    @Nullable
    public static GameProfilerTick createTickProfiler(String name) {
        return null;
    }

    public static GameProfilerFiller decorateFiller(GameProfilerFiller profiler, @Nullable GameProfilerTick monitor) {
        return monitor != null ? GameProfilerFiller.tee(monitor.startTick(), profiler) : profiler;
    }
}
