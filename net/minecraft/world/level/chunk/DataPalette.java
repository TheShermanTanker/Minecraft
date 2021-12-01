package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.network.PacketDataSerializer;

public interface DataPalette<T> {
    int idFor(T object);

    boolean maybeHas(Predicate<T> predicate);

    T valueFor(int id);

    void read(PacketDataSerializer buf);

    void write(PacketDataSerializer buf);

    int getSerializedSize();

    int getSize();

    DataPalette<T> copy();
}
