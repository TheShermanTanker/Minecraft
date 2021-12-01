package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.network.PacketDataSerializer;
import org.apache.commons.lang3.Validate;

public class DataPaletteLinear<T> implements DataPalette<T> {
    private final Registry<T> registry;
    private final T[] values;
    private final DataPaletteExpandable<T> resizeHandler;
    private final int bits;
    private int size;

    private DataPaletteLinear(Registry<T> idList, int bits, DataPaletteExpandable<T> listener, List<T> list) {
        this.registry = idList;
        this.values = (T[])(new Object[1 << bits]);
        this.bits = bits;
        this.resizeHandler = listener;
        Validate.isTrue(list.size() <= this.values.length, "Can't initialize LinearPalette of size %d with %d entries", this.values.length, list.size());

        for(int i = 0; i < list.size(); ++i) {
            this.values[i] = list.get(i);
        }

        this.size = list.size();
    }

    private DataPaletteLinear(Registry<T> idMap, T[] objects, DataPaletteExpandable<T> paletteResize, int i, int j) {
        this.registry = idMap;
        this.values = objects;
        this.resizeHandler = paletteResize;
        this.bits = i;
        this.size = j;
    }

    public static <A> DataPalette<A> create(int bits, Registry<A> idList, DataPaletteExpandable<A> listener, List<A> list) {
        return new DataPaletteLinear<>(idList, bits, listener, list);
    }

    @Override
    public int idFor(T object) {
        for(int i = 0; i < this.size; ++i) {
            if (this.values[i] == object) {
                return i;
            }
        }

        int j = this.size;
        if (j < this.values.length) {
            this.values[j] = object;
            ++this.size;
            return j;
        } else {
            return this.resizeHandler.onResize(this.bits + 1, object);
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        for(int i = 0; i < this.size; ++i) {
            if (predicate.test(this.values[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int id) {
        if (id >= 0 && id < this.size) {
            return this.values[id];
        } else {
            throw new MissingPaletteEntryException(id);
        }
    }

    @Override
    public void read(PacketDataSerializer buf) {
        this.size = buf.readVarInt();

        for(int i = 0; i < this.size; ++i) {
            this.values[i] = this.registry.fromId(buf.readVarInt());
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.size);

        for(int i = 0; i < this.size; ++i) {
            buf.writeVarInt(this.registry.getId(this.values[i]));
        }

    }

    @Override
    public int getSerializedSize() {
        int i = PacketDataSerializer.getVarIntSize(this.getSize());

        for(int j = 0; j < this.getSize(); ++j) {
            i += PacketDataSerializer.getVarIntSize(this.registry.getId(this.values[j]));
        }

        return i;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public DataPalette<T> copy() {
        return new DataPaletteLinear<>(this.registry, (T[])((Object[])this.values.clone()), this.resizeHandler, this.bits, this.size);
    }
}
