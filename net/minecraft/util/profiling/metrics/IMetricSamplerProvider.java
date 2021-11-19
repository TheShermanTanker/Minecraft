package net.minecraft.util.profiling.metrics;

import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.profiling.GameProfilerFillerActive;

public interface IMetricSamplerProvider {
    Set<MetricSampler> samplers(Supplier<GameProfilerFillerActive> profilerSupplier);
}
