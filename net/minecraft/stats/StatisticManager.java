package net.minecraft.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.entity.player.EntityHuman;

public class StatisticManager {
    protected final Object2IntMap<Statistic<?>> stats = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());

    public StatisticManager() {
        this.stats.defaultReturnValue(0);
    }

    public void increment(EntityHuman player, Statistic<?> stat, int value) {
        int i = (int)Math.min((long)this.getStatisticValue(stat) + (long)value, 2147483647L);
        this.setStatistic(player, stat, i);
    }

    public void setStatistic(EntityHuman player, Statistic<?> stat, int value) {
        this.stats.put(stat, value);
    }

    public <T> int getValue(StatisticWrapper<T> type, T stat) {
        return type.contains(stat) ? this.getStatisticValue(type.get(stat)) : 0;
    }

    public int getStatisticValue(Statistic<?> stat) {
        return this.stats.getInt(stat);
    }
}
