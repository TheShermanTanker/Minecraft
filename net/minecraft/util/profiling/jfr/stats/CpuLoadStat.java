package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record CpuLoadStat(double jvm, double userJvm, double system) {
    public CpuLoadStat(double d, double e, double f) {
        this.jvm = d;
        this.userJvm = e;
        this.system = f;
    }

    public static CpuLoadStat from(RecordedEvent event) {
        return new CpuLoadStat((double)event.getFloat("jvmSystem"), (double)event.getFloat("jvmUser"), (double)event.getFloat("machineTotal"));
    }

    public double jvm() {
        return this.jvm;
    }

    public double userJvm() {
        return this.userJvm;
    }

    public double system() {
        return this.system;
    }
}
