package net.minecraft.util.profiling.metrics.profiling;

import net.minecraft.util.profiling.GameProfilerFiller;

public interface IMetricsRecorder {
    void end();

    void startTick();

    boolean isRecording();

    GameProfilerFiller getProfiler();

    void endTick();
}
