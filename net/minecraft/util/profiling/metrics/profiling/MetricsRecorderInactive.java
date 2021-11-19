package net.minecraft.util.profiling.metrics.profiling;

import net.minecraft.util.profiling.GameProfilerDisabled;
import net.minecraft.util.profiling.GameProfilerFiller;

public class MetricsRecorderInactive implements IMetricsRecorder {
    public static final IMetricsRecorder INSTANCE = new MetricsRecorderInactive();

    @Override
    public void end() {
    }

    @Override
    public void startTick() {
    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public GameProfilerFiller getProfiler() {
        return GameProfilerDisabled.INSTANCE;
    }

    @Override
    public void endTick() {
    }
}
