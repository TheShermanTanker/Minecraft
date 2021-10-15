package net.minecraft.util.profiling;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public class GameProfilerSwitcher {
    private final LongSupplier realTime;
    private final IntSupplier tickCount;
    private GameProfilerFillerActive profiler = GameProfilerDisabled.INSTANCE;

    public GameProfilerSwitcher(LongSupplier timeGetter, IntSupplier tickGetter) {
        this.realTime = timeGetter;
        this.tickCount = tickGetter;
    }

    public boolean isEnabled() {
        return this.profiler != GameProfilerDisabled.INSTANCE;
    }

    public void disable() {
        this.profiler = GameProfilerDisabled.INSTANCE;
    }

    public void enable() {
        this.profiler = new MethodProfiler(this.realTime, this.tickCount, true);
    }

    public GameProfilerFiller getFiller() {
        return this.profiler;
    }

    public MethodProfilerResults getResults() {
        return this.profiler.getResults();
    }
}
