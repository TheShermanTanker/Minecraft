package net.minecraft.world.level.chunk;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.DataBits;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ThreadingDetector;

public class DataPaletteBlock<T> implements DataPaletteExpandable<T> {
    private static final int SIZE = 4096;
    public static final int GLOBAL_PALETTE_BITS = 9;
    public static final int MIN_PALETTE_SIZE = 4;
    private final DataPalette<T> globalPalette;
    private final DataPaletteExpandable<T> dummyPaletteResize = (newSize, added) -> {
        return 0;
    };
    private final RegistryBlockID<T> registry;
    private final Function<NBTTagCompound, T> reader;
    private final Function<T, NBTTagCompound> writer;
    private final T defaultValue;
    protected DataBits storage;
    private DataPalette<T> palette;
    private int bits;
    private final Semaphore lock = new Semaphore(1);
    @Nullable
    private final DebugBuffer<Pair<Thread, StackTraceElement[]>> traces = null;

    public void acquire() {
        if (this.traces != null) {
            Thread thread = Thread.currentThread();
            this.traces.push(Pair.of(thread, thread.getStackTrace()));
        }

        ThreadingDetector.checkAndLock(this.lock, this.traces, "PalettedContainer");
    }

    public void release() {
        this.lock.release();
    }

    public DataPaletteBlock(DataPalette<T> fallbackPalette, RegistryBlockID<T> idList, Function<NBTTagCompound, T> elementDeserializer, Function<T, NBTTagCompound> elementSerializer, T defaultElement) {
        this.globalPalette = fallbackPalette;
        this.registry = idList;
        this.reader = elementDeserializer;
        this.writer = elementSerializer;
        this.defaultValue = defaultElement;
        this.setBits(4);
    }

    private static int getIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private void setBits(int size) {
        if (size != this.bits) {
            this.bits = size;
            if (this.bits <= 4) {
                this.bits = 4;
                this.palette = new DataPaletteLinear<>(this.registry, this.bits, this, this.reader);
            } else if (this.bits < 9) {
                this.palette = new DataPaletteHash<>(this.registry, this.bits, this, this.reader, this.writer);
            } else {
                this.palette = this.globalPalette;
                this.bits = MathHelper.ceillog2(this.registry.size());
            }

            this.palette.idFor(this.defaultValue);
            this.storage = new DataBits(this.bits, 4096);
        }
    }

    @Override
    public int onResize(int newSize, T objectAdded) {
        DataBits bitStorage = this.storage;
        DataPalette<T> palette = this.palette;
        this.setBits(newSize);

        for(int i = 0; i < bitStorage.getSize(); ++i) {
            T object = palette.valueFor(bitStorage.get(i));
            if (object != null) {
                this.setBlockIndex(i, object);
            }
        }

        return this.palette.idFor(objectAdded);
    }

    public T setBlock(int x, int y, int z, T value) {
        Object var6;
        try {
            this.acquire();
            T object = this.getAndSet(getIndex(x, y, z), value);
            var6 = object;
        } finally {
            this.release();
        }

        return (T)var6;
    }

    public T getAndSetUnchecked(int x, int y, int z, T value) {
        return this.getAndSet(getIndex(x, y, z), value);
    }

    private T getAndSet(int index, T value) {
        int i = this.palette.idFor(value);
        int j = this.storage.getAndSet(index, i);
        T object = this.palette.valueFor(j);
        return (T)(object == null ? this.defaultValue : object);
    }

    public void set(int i, int j, int k, T object) {
        try {
            this.acquire();
            this.setBlockIndex(getIndex(i, j, k), object);
        } finally {
            this.release();
        }

    }

    private void setBlockIndex(int index, T object) {
        int i = this.palette.idFor(object);
        this.storage.set(index, i);
    }

    public T get(int x, int y, int z) {
        return this.get(getIndex(x, y, z));
    }

    protected T get(int index) {
        T object = this.palette.valueFor(this.storage.get(index));
        return (T)(object == null ? this.defaultValue : object);
    }

    public void read(PacketDataSerializer buf) {
        try {
            this.acquire();
            int i = buf.readByte();
            if (this.bits != i) {
                this.setBits(i);
            }

            this.palette.read(buf);
            buf.readLongArray(this.storage.getRaw());
        } finally {
            this.release();
        }

    }

    public void write(PacketDataSerializer buf) {
        try {
            this.acquire();
            buf.writeByte(this.bits);
            this.palette.write(buf);
            buf.writeLongArray(this.storage.getRaw());
        } finally {
            this.release();
        }

    }

    public void read(NBTTagList paletteNbt, long[] data) {
        try {
            this.acquire();
            int i = Math.max(4, MathHelper.ceillog2(paletteNbt.size()));
            if (i != this.bits) {
                this.setBits(i);
            }

            this.palette.read(paletteNbt);
            int j = data.length * 64 / 4096;
            if (this.palette == this.globalPalette) {
                DataPalette<T> palette = new DataPaletteHash<>(this.registry, i, this.dummyPaletteResize, this.reader, this.writer);
                palette.read(paletteNbt);
                DataBits bitStorage = new DataBits(i, 4096, data);

                for(int k = 0; k < 4096; ++k) {
                    this.storage.set(k, this.globalPalette.idFor(palette.valueFor(bitStorage.get(k))));
                }
            } else if (j == this.bits) {
                System.arraycopy(data, 0, this.storage.getRaw(), 0, data.length);
            } else {
                DataBits bitStorage2 = new DataBits(j, 4096, data);

                for(int l = 0; l < 4096; ++l) {
                    this.storage.set(l, bitStorage2.get(l));
                }
            }
        } finally {
            this.release();
        }

    }

    public void write(NBTTagCompound nbt, String paletteKey, String dataKey) {
        try {
            this.acquire();
            DataPaletteHash<T> hashMapPalette = new DataPaletteHash<>(this.registry, this.bits, this.dummyPaletteResize, this.reader, this.writer);
            T object = this.defaultValue;
            int i = hashMapPalette.idFor(this.defaultValue);
            int[] is = new int[4096];

            for(int j = 0; j < 4096; ++j) {
                T object2 = this.get(j);
                if (object2 != object) {
                    object = object2;
                    i = hashMapPalette.idFor(object2);
                }

                is[j] = i;
            }

            NBTTagList listTag = new NBTTagList();
            hashMapPalette.write(listTag);
            nbt.set(paletteKey, listTag);
            int k = Math.max(4, MathHelper.ceillog2(listTag.size()));
            DataBits bitStorage = new DataBits(k, 4096);

            for(int l = 0; l < is.length; ++l) {
                bitStorage.set(l, is[l]);
            }

            nbt.putLongArray(dataKey, bitStorage.getRaw());
        } finally {
            this.release();
        }

    }

    public int getSerializedSize() {
        return 1 + this.palette.getSerializedSize() + PacketDataSerializer.getVarIntSize(this.storage.getSize()) + this.storage.getRaw().length * 8;
    }

    public boolean contains(Predicate<T> predicate) {
        return this.palette.maybeHas(predicate);
    }

    public void count(DataPaletteBlock.CountConsumer<T> consumer) {
        Int2IntMap int2IntMap = new Int2IntOpenHashMap();
        this.storage.getAll((i) -> {
            int2IntMap.put(i, int2IntMap.get(i) + 1);
        });
        int2IntMap.int2IntEntrySet().forEach((entry) -> {
            consumer.accept(this.palette.valueFor(entry.getIntKey()), entry.getIntValue());
        });
    }

    @FunctionalInterface
    public interface CountConsumer<T> {
        void accept(T object, int count);
    }
}
