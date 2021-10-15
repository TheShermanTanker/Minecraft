package net.minecraft.stats;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public class StatisticWrapper<T> implements Iterable<Statistic<T>> {
    private final IRegistry<T> registry;
    private final Map<T, Statistic<T>> map = new IdentityHashMap<>();
    @Nullable
    private IChatBaseComponent displayName;

    public StatisticWrapper(IRegistry<T> registry) {
        this.registry = registry;
    }

    public boolean contains(T key) {
        return this.map.containsKey(key);
    }

    public Statistic<T> get(T key, Counter formatter) {
        return this.map.computeIfAbsent(key, (object) -> {
            return new Statistic<>(this, object, formatter);
        });
    }

    public IRegistry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public Iterator<Statistic<T>> iterator() {
        return this.map.values().iterator();
    }

    public Statistic<T> get(T key) {
        return this.get(key, Counter.DEFAULT);
    }

    public String getTranslationKey() {
        return "stat_type." + IRegistry.STAT_TYPE.getKey(this).toString().replace(':', '.');
    }

    public IChatBaseComponent getDisplayName() {
        if (this.displayName == null) {
            this.displayName = new ChatMessage(this.getTranslationKey());
        }

        return this.displayName;
    }
}
