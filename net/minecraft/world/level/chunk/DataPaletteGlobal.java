package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.network.PacketDataSerializer;

public class DataPaletteGlobal<T> implements DataPalette<T> {
    private final Registry<T> registry;

    public DataPaletteGlobal(Registry<T> idList) {
        this.registry = idList;
    }

    public static <A> DataPalette<A> create(int bits, Registry<A> idList, DataPaletteExpandable<A> listener, List<A> list) {
        return new DataPaletteGlobal<>(idList);
    }

    @Override
    public int idFor(T object) {
        int i = this.registry.getId(object);
        return i == -1 ? 0 : i;
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        return true;
    }

    @Override
    public T valueFor(int id) {
        T object = this.registry.fromId(id);
        if (object == null) {
            throw new MissingPaletteEntryException(id);
        } else {
            return object;
        }
    }

    @Override
    public void read(PacketDataSerializer buf) {
    }

    @Override
    public void write(PacketDataSerializer buf) {
    }

    @Override
    public int getSerializedSize() {
        return PacketDataSerializer.getVarIntSize(0);
    }

    @Override
    public int getSize() {
        return this.registry.size();
    }

    @Override
    public DataPalette<T> copy() {
        return this;
    }
}
