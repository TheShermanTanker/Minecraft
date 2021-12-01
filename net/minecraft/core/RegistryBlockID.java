package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;

public class RegistryBlockID<T> implements Registry<T> {
    private int nextId;
    private final Object2IntMap<T> tToId;
    private final List<T> idToT;

    public RegistryBlockID() {
        this(512);
    }

    public RegistryBlockID(int initialSize) {
        this.idToT = Lists.newArrayListWithExpectedSize(initialSize);
        this.tToId = new Object2IntOpenCustomHashMap<>(initialSize, SystemUtils.identityStrategy());
        this.tToId.defaultReturnValue(-1);
    }

    public void addMapping(T value, int id) {
        this.tToId.put(value, id);

        while(this.idToT.size() <= id) {
            this.idToT.add((T)null);
        }

        this.idToT.set(id, value);
        if (this.nextId <= id) {
            this.nextId = id + 1;
        }

    }

    public void add(T value) {
        this.addMapping(value, this.nextId);
    }

    @Override
    public int getId(T entry) {
        return this.tToId.getInt(entry);
    }

    @Nullable
    @Override
    public final T fromId(int index) {
        return (T)(index >= 0 && index < this.idToT.size() ? this.idToT.get(index) : null);
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.filter(this.idToT.iterator(), Objects::nonNull);
    }

    public boolean contains(int index) {
        return this.fromId(index) != null;
    }

    @Override
    public int size() {
        return this.tToId.size();
    }
}
