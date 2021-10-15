package net.minecraft.stats;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class Statistic<T> extends IScoreboardCriteria {
    private final Counter formatter;
    private final T value;
    private final StatisticWrapper<T> type;

    protected Statistic(StatisticWrapper<T> type, T value, Counter formatter) {
        super(buildName(type, value));
        this.type = type;
        this.formatter = formatter;
        this.value = value;
    }

    public static <T> String buildName(StatisticWrapper<T> type, T value) {
        return locationToKey(IRegistry.STAT_TYPE.getKey(type)) + ":" + locationToKey(type.getRegistry().getKey(value));
    }

    private static <T> String locationToKey(@Nullable MinecraftKey id) {
        return id.toString().replace(':', '.');
    }

    public StatisticWrapper<T> getWrapper() {
        return this.type;
    }

    public T getValue() {
        return this.value;
    }

    public String format(int i) {
        return this.formatter.format(i);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Statistic && Objects.equals(this.getName(), ((Statistic)object).getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String toString() {
        return "Stat{name=" + this.getName() + ", formatter=" + this.formatter + "}";
    }
}
