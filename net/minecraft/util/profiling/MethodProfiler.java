package net.minecraft.util.profiling;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.util.profiling.metrics.EnumMetricCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodProfiler implements GameProfilerFillerActive {
    private static final long WARNING_TIME_NANOS = Duration.ofMillis(100L).toNanos();
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<String> paths = Lists.newArrayList();
    private final LongList startTimes = new LongArrayList();
    private final Map<String, MethodProfiler.PathEntry> entries = Maps.newHashMap();
    private final IntSupplier getTickTime;
    private final LongSupplier getRealTime;
    private final long startTimeNano;
    private final int startTimeTicks;
    private String path = "";
    private boolean started;
    @Nullable
    private MethodProfiler.PathEntry currentEntry;
    private final boolean warn;
    private final Set<Pair<String, EnumMetricCategory>> chartedPaths = new ObjectArraySet<>();

    public MethodProfiler(LongSupplier timeGetter, IntSupplier tickGetter, boolean checkTimeout) {
        this.startTimeNano = timeGetter.getAsLong();
        this.getRealTime = timeGetter;
        this.startTimeTicks = tickGetter.getAsInt();
        this.getTickTime = tickGetter;
        this.warn = checkTimeout;
    }

    @Override
    public void startTick() {
        if (this.started) {
            LOGGER.error("Profiler tick already started - missing endTick()?");
        } else {
            this.started = true;
            this.path = "";
            this.paths.clear();
            this.enter("root");
        }
    }

    @Override
    public void endTick() {
        if (!this.started) {
            LOGGER.error("Profiler tick already ended - missing startTick()?");
        } else {
            this.exit();
            this.started = false;
            if (!this.path.isEmpty()) {
                LOGGER.error("Profiler tick ended before path was fully popped (remainder: '{}'). Mismatched push/pop?", () -> {
                    return MethodProfilerResults.demanglePath(this.path);
                });
            }

        }
    }

    @Override
    public void enter(String location) {
        if (!this.started) {
            LOGGER.error("Cannot push '{}' to profiler if profiler tick hasn't started - missing startTick()?", (Object)location);
        } else {
            if (!this.path.isEmpty()) {
                this.path = this.path + "\u001e";
            }

            this.path = this.path + location;
            this.paths.add(this.path);
            this.startTimes.add(SystemUtils.getMonotonicNanos());
            this.currentEntry = null;
        }
    }

    @Override
    public void push(Supplier<String> locationGetter) {
        this.enter(locationGetter.get());
    }

    @Override
    public void markForCharting(EnumMetricCategory type) {
        this.chartedPaths.add(Pair.of(this.path, type));
    }

    @Override
    public void exit() {
        if (!this.started) {
            LOGGER.error("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?");
        } else if (this.startTimes.isEmpty()) {
            LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
        } else {
            long l = SystemUtils.getMonotonicNanos();
            long m = this.startTimes.removeLong(this.startTimes.size() - 1);
            this.paths.remove(this.paths.size() - 1);
            long n = l - m;
            MethodProfiler.PathEntry pathEntry = this.getCurrentEntry();
            pathEntry.accumulatedDuration += n;
            ++pathEntry.count;
            pathEntry.maxDuration = Math.max(pathEntry.maxDuration, n);
            pathEntry.minDuration = Math.min(pathEntry.minDuration, n);
            if (this.warn && n > WARNING_TIME_NANOS) {
                LOGGER.warn("Something's taking too long! '{}' took aprox {} ms", () -> {
                    return MethodProfilerResults.demanglePath(this.path);
                }, () -> {
                    return (double)n / 1000000.0D;
                });
            }

            this.path = this.paths.isEmpty() ? "" : this.paths.get(this.paths.size() - 1);
            this.currentEntry = null;
        }
    }

    @Override
    public void exitEnter(String location) {
        this.exit();
        this.enter(location);
    }

    @Override
    public void popPush(Supplier<String> locationGetter) {
        this.exit();
        this.push(locationGetter);
    }

    private MethodProfiler.PathEntry getCurrentEntry() {
        if (this.currentEntry == null) {
            this.currentEntry = this.entries.computeIfAbsent(this.path, (k) -> {
                return new MethodProfiler.PathEntry();
            });
        }

        return this.currentEntry;
    }

    @Override
    public void incrementCounter(String marker, int i) {
        this.getCurrentEntry().counters.addTo(marker, (long)i);
    }

    @Override
    public void incrementCounter(Supplier<String> markerGetter, int i) {
        this.getCurrentEntry().counters.addTo(markerGetter.get(), (long)i);
    }

    @Override
    public MethodProfilerResults getResults() {
        return new MethodProfilerResultsFilled(this.entries, this.startTimeNano, this.startTimeTicks, this.getRealTime.getAsLong(), this.getTickTime.getAsInt());
    }

    @Nullable
    @Override
    public MethodProfiler.PathEntry getEntry(String name) {
        return this.entries.get(name);
    }

    @Override
    public Set<Pair<String, EnumMetricCategory>> getChartedPaths() {
        return this.chartedPaths;
    }

    public static class PathEntry implements MethodProfilerResult {
        long maxDuration = Long.MIN_VALUE;
        long minDuration = Long.MAX_VALUE;
        long accumulatedDuration;
        long count;
        final Object2LongOpenHashMap<String> counters = new Object2LongOpenHashMap<>();

        @Override
        public long getDuration() {
            return this.accumulatedDuration;
        }

        @Override
        public long getMaxDuration() {
            return this.maxDuration;
        }

        @Override
        public long getCount() {
            return this.count;
        }

        @Override
        public Object2LongMap<String> getCounters() {
            return Object2LongMaps.unmodifiable(this.counters);
        }
    }
}
