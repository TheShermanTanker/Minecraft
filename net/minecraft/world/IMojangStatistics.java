package net.minecraft.world;

public interface IMojangStatistics {
    void populateSnooper(MojangStatisticsGenerator snooper);

    void populateSnooperInitial(MojangStatisticsGenerator snooper);

    boolean isSnooperEnabled();
}
