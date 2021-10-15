package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;

public class DataPaletteGlobal<T> implements DataPalette<T> {
    private final RegistryBlockID<T> registry;
    private final T defaultValue;

    public DataPaletteGlobal(RegistryBlockID<T> idList, T defaultValue) {
        this.registry = idList;
        this.defaultValue = defaultValue;
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
    public T valueFor(int index) {
        T object = this.registry.fromId(index);
        return (T)(object == null ? this.defaultValue : object);
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
    public void read(NBTTagList nbt) {
    }
}
