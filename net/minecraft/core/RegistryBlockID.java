package net.minecraft.core;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

public class RegistryBlockID<T> implements Registry<T> {
    public static final int DEFAULT = -1;
    private int nextId;
    private final IdentityHashMap<T, Integer> tToId;
    private final List<T> idToT;

    public RegistryBlockID() {
        this(512);
    }

    public RegistryBlockID(int initialSize) {
        this.idToT = Lists.newArrayListWithExpectedSize(initialSize);
        this.tToId = new IdentityHashMap<>(initialSize);
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
        Integer integer = this.tToId.get(entry);
        return integer == null ? -1 : integer;
    }

    @Nullable
    @Override
    public final T fromId(int index) {
        return (T)(index >= 0 && index < this.idToT.size() ? this.idToT.get(index) : null);
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.filter(this.idToT.iterator(), Predicates.notNull());
    }

    public boolean contains(int index) {
        return this.fromId(index) != null;
    }

    public int size() {
        return this.tToId.size();
    }
}
