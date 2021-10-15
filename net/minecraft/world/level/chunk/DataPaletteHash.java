package net.minecraft.world.level.chunk;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.RegistryID;

public class DataPaletteHash<T> implements DataPalette<T> {
    private final RegistryBlockID<T> registry;
    private final RegistryID<T> values;
    private final DataPaletteExpandable<T> resizeHandler;
    private final Function<NBTTagCompound, T> reader;
    private final Function<T, NBTTagCompound> writer;
    private final int bits;

    public DataPaletteHash(RegistryBlockID<T> idList, int indexBits, DataPaletteExpandable<T> resizeHandler, Function<NBTTagCompound, T> elementDeserializer, Function<T, NBTTagCompound> elementSerializer) {
        this.registry = idList;
        this.bits = indexBits;
        this.resizeHandler = resizeHandler;
        this.reader = elementDeserializer;
        this.writer = elementSerializer;
        this.values = new RegistryID<>(1 << indexBits);
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

    @Nullable
    @Override
    public T valueFor(int index) {
        return this.values.fromId(index);
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

    @Override
    public int getSize() {
        return this.values.size();
    }

    @Override
    public void read(NBTTagList nbt) {
        this.values.clear();

        for(int i = 0; i < nbt.size(); ++i) {
            this.values.add(this.reader.apply(nbt.getCompound(i)));
        }

    }

    public void write(NBTTagList nbt) {
        for(int i = 0; i < this.getSize(); ++i) {
            nbt.add(this.writer.apply(this.values.fromId(i)));
        }

    }
}
