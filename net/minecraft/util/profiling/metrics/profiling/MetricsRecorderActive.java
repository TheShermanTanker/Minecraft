package net.minecraft.util.profiling.metrics.profiling;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.GameProfilerDisabled;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.profiling.GameProfilerFillerActive;
import net.minecraft.util.profiling.GameProfilerSwitcher;
import net.minecraft.util.profiling.MethodProfiler;
import net.minecraft.util.profiling.MethodProfilerResults;
import net.minecraft.util.profiling.metrics.IMetricSamplerProvider;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.profiling.metrics.storage.RecordedDeviation;

public class MetricsRecorderActive implements IMetricsRecorder {
    public static final int PROFILING_MAX_DURATION_SECONDS = 10;
    @Nullable
    private static Consumer<Path> globalOnReportFinished = null;
    private final Map<MetricSampler, List<RecordedDeviation>> deviationsBySampler = new Object2ObjectOpenHashMap<>();
    private final GameProfilerSwitcher taskProfiler;
    private final Executor ioExecutor;
    private final MetricsPersister metricsPersister;
    private final Consumer<MethodProfilerResults> onProfilingEnd;
    private final Consumer<Path> onReportFinished;
    private final IMetricSamplerProvider metricsSamplerProvider;
    private final LongSupplier wallTimeSource;
    private final long deadlineNano;
    private int currentTick;
    private GameProfilerFillerActive singleTickProfiler;
    private volatile boolean killSwitch;
    private Set<MetricSampler> thisTickSamplers = ImmutableSet.of();

    private MetricsRecorderActive(IMetricSamplerProvider samplerSource, LongSupplier timeGetter, Executor dumpExecutor, MetricsPersister dumper, Consumer<MethodProfilerResults> resultConsumer, Consumer<Path> dumpConsumer) {
        this.metricsSamplerProvider = samplerSource;
        this.wallTimeSource = timeGetter;
        this.taskProfiler = new GameProfilerSwitcher(timeGetter, () -> {
            return this.currentTick;
        });
        this.ioExecutor = dumpExecutor;
        this.metricsPersister = dumper;
        this.onProfilingEnd = resultConsumer;
        this.onReportFinished = globalOnReportFinished == null ? dumpConsumer : dumpConsumer.andThen(globalOnReportFinished);
        this.deadlineNano = timeGetter.getAsLong() + TimeUnit.NANOSECONDS.convert(10L, TimeUnit.SECONDS);
        this.singleTickProfiler = new MethodProfiler(this.wallTimeSource, () -> {
            return this.currentTick;
        }, false);
        this.taskProfiler.enable();
    }

    public static MetricsRecorderActive createStarted(IMetricSamplerProvider source, LongSupplier timeGetter, Executor dumpExecutor, MetricsPersister dumper, Consumer<MethodProfilerResults> resultConsumer, Consumer<Path> dumpConsumer) {
        return new MetricsRecorderActive(source, timeGetter, dumpExecutor, dumper, resultConsumer, dumpConsumer);
    }

    @Override
    public synchronized void end() {
        if (this.isRecording()) {
            this.killSwitch = true;
        }
    }

    @Override
    public void startTick() {
        this.verifyStarted();
        this.thisTickSamplers = this.metricsSamplerProvider.samplers(() -> {
            return this.singleTickProfiler;
        });

        for(MetricSampler metricSampler : this.thisTickSamplers) {
            metricSampler.onStartTick();
        }

        ++this.currentTick;
    }

    @Override
    public void endTick() {
        this.verifyStarted();
        if (this.currentTick != 0) {
            for(MetricSampler metricSampler : this.thisTickSamplers) {
                metricSampler.onEndTick(this.currentTick);
                if (metricSampler.triggersThreshold()) {
                    RecordedDeviation recordedDeviation = new RecordedDeviation(Instant.now(), this.currentTick, this.singleTickProfiler.getResults());
                    this.deviationsBySampler.computeIfAbsent(metricSampler, (s) -> {
                        return Lists.newArrayList();
                    }).add(recordedDeviation);
                }
            }

            if (!this.killSwitch && this.wallTimeSource.getAsLong() <= this.deadlineNano) {
                this.singleTickProfiler = new MethodProfiler(this.wallTimeSource, () -> {
                    return this.currentTick;
                }, false);
            } else {
                this.killSwitch = false;
                this.singleTickProfiler = GameProfilerDisabled.INSTANCE;
                MethodProfilerResults profileResults = this.taskProfiler.getResults();
                this.onProfilingEnd.accept(profileResults);
                this.scheduleSaveResults(profileResults);
            }
        }
    }

    @Override
    public boolean isRecording() {
        return this.taskProfiler.isEnabled();
    }

    @Override
    public GameProfilerFiller getProfiler() {
        return GameProfilerFiller.tee(this.taskProfiler.getFiller(), this.singleTickProfiler);
    }

    private void verifyStarted() {
        if (!this.isRecording()) {
            throw new IllegalStateException("Not started!");
        }
    }

    private void scheduleSaveResults(MethodProfilerResults result) {
        HashSet<MetricSampler> hashSet = new HashSet<>(this.thisTickSamplers);
        this.ioExecutor.execute(() -> {
            Path path = this.metricsPersister.saveReports(hashSet, this.deviationsBySampler, result);

            for(MetricSampler metricSampler : hashSet) {
                metricSampler.onFinished();
            }

            this.deviationsBySampler.clear();
            this.taskProfiler.disable();
            this.onReportFinished.accept(path);
        });
    }

    public static void registerGlobalCompletionCallback(Consumer<Path> consumer) {
        globalOnReportFinished = consumer;
    }
}
