package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;

public interface DataPalette<T> {
    int idFor(T object);

    boolean maybeHas(Predicate<T> predicate);

    @Nullable
    T valueFor(int index);

    void read(PacketDataSerializer buf);

    void write(PacketDataSerializer buf);

    int getSerializedSize();

    int getSize();

    void read(NBTTagList nbt);
}
