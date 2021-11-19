package net.minecraft.util.profiling.metrics;

import java.util.List;

public interface IProfilerMeasured {
    List<MetricSampler> profiledMetrics();
}
