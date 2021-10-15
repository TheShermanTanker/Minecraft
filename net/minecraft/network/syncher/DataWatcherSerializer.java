package net.minecraft.network.syncher;

import net.minecraft.network.PacketDataSerializer;

public interface DataWatcherSerializer<T> {
    void write(PacketDataSerializer buf, T value);

    T read(PacketDataSerializer buf);

    default DataWatcherObject<T> createAccessor(int i) {
        return new DataWatcherObject<>(i, this);
    }

    T copy(T value);
}
