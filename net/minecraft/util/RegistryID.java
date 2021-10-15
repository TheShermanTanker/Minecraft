package net.minecraft.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;

public class RegistryID<K> implements Registry<K> {
    public static final int NOT_FOUND = -1;
    private static final Object EMPTY_SLOT = null;
    private static final float LOADFACTOR = 0.8F;
    private K[] keys;
    private int[] values;
    private K[] byId;
    private int nextId;
    private int size;

    public RegistryID(int size) {
        size = (int)((float)size / 0.8F);
        this.keys = (K[])(new Object[size]);
        this.values = new int[size];
        this.byId = (K[])(new Object[size]);
    }

    @Override
    public int getId(@Nullable K entry) {
        return this.getValue(this.indexOf(entry, this.hash(entry)));
    }

    @Nullable
    @Override
    public K fromId(int index) {
        return (K)(index >= 0 && index < this.byId.length ? this.byId[index] : null);
    }

    private int getValue(int index) {
        return index == -1 ? -1 : this.values[index];
    }

    public boolean contains(K value) {
        return this.getId(value) != -1;
    }

    public boolean contains(int index) {
        return this.fromId(index) != null;
    }

    public int add(K value) {
        int i = this.nextId();
        this.addMapping(value, i);
        return i;
    }

    private int nextId() {
        while(this.nextId < this.byId.length && this.byId[this.nextId] != null) {
            ++this.nextId;
        }

        return this.nextId;
    }

    private void grow(int newSize) {
        K[] objects = this.keys;
        int[] is = this.values;
        this.keys = (K[])(new Object[newSize]);
        this.values = new int[newSize];
        this.byId = (K[])(new Object[newSize]);
        this.nextId = 0;
        this.size = 0;

        for(int i = 0; i < objects.length; ++i) {
            if (objects[i] != null) {
                this.addMapping(objects[i], is[i]);
            }
        }

    }

    public void addMapping(K value, int id) {
        int i = Math.max(id, this.size + 1);
        if ((float)i >= (float)this.keys.length * 0.8F) {
            int j;
            for(j = this.keys.length << 1; j < id; j <<= 1) {
            }

            this.grow(j);
        }

        int k = this.findEmpty(this.hash(value));
        this.keys[k] = value;
        this.values[k] = id;
        this.byId[id] = value;
        ++this.size;
        if (id == this.nextId) {
            ++this.nextId;
        }

    }

    private int hash(@Nullable K value) {
        return (MathHelper.murmurHash3Mixer(System.identityHashCode(value)) & Integer.MAX_VALUE) % this.keys.length;
    }

    private int indexOf(@Nullable K value, int id) {
        for(int i = id; i < this.keys.length; ++i) {
            if (this.keys[i] == value) {
                return i;
            }

            if (this.keys[i] == EMPTY_SLOT) {
                return -1;
            }
        }

        for(int j = 0; j < id; ++j) {
            if (this.keys[j] == value) {
                return j;
            }

            if (this.keys[j] == EMPTY_SLOT) {
                return -1;
            }
        }

        return -1;
    }

    private int findEmpty(int size) {
        for(int i = size; i < this.keys.length; ++i) {
            if (this.keys[i] == EMPTY_SLOT) {
                return i;
            }
        }

        for(int j = 0; j < size; ++j) {
            if (this.keys[j] == EMPTY_SLOT) {
                return j;
            }
        }

        throw new RuntimeException("Overflowed :(");
    }

    @Override
    public Iterator<K> iterator() {
        return Iterators.filter(Iterators.forArray(this.byId), Predicates.notNull());
    }

    public void clear() {
        Arrays.fill(this.keys, (Object)null);
        Arrays.fill(this.byId, (Object)null);
        this.nextId = 0;
        this.size = 0;
    }

    public int size() {
        return this.size;
    }
}
