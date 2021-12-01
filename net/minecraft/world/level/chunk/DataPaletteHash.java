package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.RegistryID;

public class DataPaletteHash<T> implements DataPalette<T> {
    private final Registry<T> registry;
    private final RegistryID<T> values;
    private final DataPaletteExpandable<T> resizeHandler;
    private final int bits;

    public DataPaletteHash(Registry<T> idList, int bits, DataPaletteExpandable<T> listener, List<T> entries) {
        this(idList, bits, listener);
        entries.forEach(this.values::add);
    }

    public DataPaletteHash(Registry<T> idList, int indexBits, DataPaletteExpandable<T> listener) {
        this(idList, indexBits, listener, RegistryID.create(1 << indexBits));
    }

    private DataPaletteHash(Registry<T> idMap, int i, DataPaletteExpandable<T> paletteResize, RegistryID<T> crudeIncrementalIntIdentityHashBiMap) {
        this.registry = idMap;
        this.bits = i;
        this.resizeHandler = paletteResize;
        this.values = crudeIncrementalIntIdentityHashBiMap;
    }

    public static <A> DataPalette<A> create(int bits, Registry<A> idList, DataPaletteExpandable<A> listener, List<A> entries) {
        return new DataPaletteHash<>(idList, bits, listener, entries);
    }

    @Override
    public int idFor(T object) {
        int i = this.values.getId(object);
        if (i == -1) {
            i = this.values.add(object);
            if (i >= 1 << this.bits) {
                i = this.resizeHandler.onResize(this.bits + 1, object);
            }
        }

        return i;
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        for(int i = 0; i < this.getSize(); ++i) {
            if (predicate.test(this.values.fromId(i))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int id) {
        T object = this.values.fromId(id);
        if (object == null) {
            throw new MissingPaletteEntryException(id);
        } else {
            return object;
        }
    }

    @Override
    public void read(PacketDataSerializer buf) {
        this.values.clear();
        int i = buf.readVarInt();

        for(int j = 0; j < i; ++j) {
            this.values.add(this.registry.fromId(buf.readVarInt()));
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        int i = this.getSize();
        buf.writeVarInt(i);

        for(int j = 0; j < i; ++j) {
            buf.writeVarInt(this.registry.getId(this.values.fromId(j)));
        }

    }

    @Override
    public int getSerializedSize() {
        int i = PacketDataSerializer.getVarIntSize(this.getSize());

        for(int j = 0; j < this.getSize(); ++j) {
            i += PacketDataSerializer.getVarIntSize(this.registry.getId(this.values.fromId(j)));
        }

        return i;
    }

    public List<T> getEntries() {
        ArrayList<T> arrayList = new ArrayList<>();
        this.values.iterator().forEachRemaining(arrayList::add);
        return arrayList;
    }

    @Override
    public int getSize() {
        return this.values.size();
    }

    @Override
    public DataPalette<T> copy() {
        return new DataPaletteHash<>(this.registry, this.bits, this.resizeHandler, this.values.copy());
    }
}
