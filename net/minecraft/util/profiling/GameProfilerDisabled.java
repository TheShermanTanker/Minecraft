package net.minecraft.util.profiling;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.apache.commons.lang3.tuple.Pair;

public class GameProfilerDisabled implements GameProfilerFillerActive {
    public static final GameProfilerDisabled INSTANCE = new GameProfilerDisabled();

    private GameProfilerDisabled() {
    }

    @Override
    public void startTick() {
    }

    @Override
    public void endTick() {
    }

    @Override
    public void enter(String location) {
    }

    @Override
    public void push(Supplier<String> locationGetter) {
    }

    @Override
    public void markForCharting(MetricCategory type) {
    }

    @Override
    public void exit() {
    }

    @Override
    public void exitEnter(String location) {
    }

    @Override
    public void popPush(Supplier<String> locationGetter) {
    }

    @Override
    public void incrementCounter(String marker) {
    }

    @Override
    public void incrementCounter(Supplier<String> markerGetter) {
    }

    @Override
    public MethodProfilerResults getResults() {
        return MethodProfilerResultsEmpty.EMPTY;
    }

    @Nullable
    @Override
    public MethodProfiler.PathEntry getEntry(String name) {
        return null;
    }

    @Override
    public Set<Pair<String, MetricCategory>> getChartedPaths() {
        return ImmutableSet.of();
    }
}
