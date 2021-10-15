package net.minecraft.world.level.chunk;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;

public class DataPaletteLinear<T> implements DataPalette<T> {
    private final RegistryBlockID<T> registry;
    private final T[] values;
    private final DataPaletteExpandable<T> resizeHandler;
    private final Function<NBTTagCompound, T> reader;
    private final int bits;
    private int size;

    public DataPaletteLinear(RegistryBlockID<T> idList, int integer, DataPaletteExpandable<T> resizeListener, Function<NBTTagCompound, T> valueDeserializer) {
        this.registry = idList;
        this.values = (T[])(new Object[1 << integer]);
        this.bits = integer;
        this.resizeHandler = resizeListener;
        this.reader = valueDeserializer;
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

    @Nullable
    @Override
    public T valueFor(int index) {
        return (T)(index >= 0 && index < this.size ? this.values[index] : null);
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
    public void read(NBTTagList nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            this.values[i] = this.reader.apply(nbt.getCompound(i));
        }

        this.size = nbt.size();
    }
}
