package net.minecraft.world.level.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.DataBits;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class DataPaletteBlock<T> implements DataPaletteExpandable<T> {
    private static final int MIN_PALETTE_BITS = 0;
    private final DataPaletteExpandable<T> dummyPaletteResize = (newSize, added) -> {
        return 0;
    };
    public final Registry<T> registry;
    private volatile PalettedContainer$Data<T> data;
    private final PalettedContainer$Strategy strategy;
    private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

    public void acquire() {
        this.threadingDetector.checkAndLock();
    }

    public void release() {
        this.threadingDetector.checkAndUnlock();
    }

    public static <T> Codec<DataPaletteBlock<T>> codec(Registry<T> idList, Codec<T> entryCodec, PalettedContainer$Strategy provider, T object) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(entryCodec.mapResult(ExtraCodecs.orElsePartial(object)).listOf().fieldOf("palette").forGetter(PalettedContainer$DiscData::paletteEntries), Codec.LONG_STREAM.optionalFieldOf("data").forGetter(PalettedContainer$DiscData::storage)).apply(instance, PalettedContainer$DiscData::new);
        }).comapFlatMap((serialized) -> {
            return read(idList, provider, serialized);
        }, (container) -> {
            return container.write(idList, provider);
        });
    }

    public DataPaletteBlock(Registry<T> idList, PalettedContainer$Strategy paletteProvider, PalettedContainer$Configuration<T> dataProvider, DataBits storage, List<T> paletteEntries) {
        this.registry = idList;
        this.strategy = paletteProvider;
        this.data = new PalettedContainer$Data<>(dataProvider, storage, dataProvider.factory().create(dataProvider.bits(), idList, this, paletteEntries));
    }

    private DataPaletteBlock(Registry<T> idList, PalettedContainer$Strategy paletteProvider, PalettedContainer$Data<T> data) {
        this.registry = idList;
        this.strategy = paletteProvider;
        this.data = data;
    }

    public DataPaletteBlock(Registry<T> idList, T object, PalettedContainer$Strategy paletteProvider) {
        this.strategy = paletteProvider;
        this.registry = idList;
        this.data = this.createOrReuseData((PalettedContainer$Data<T>)null, 0);
        this.data.palette.idFor(object);
    }

    private PalettedContainer$Data<T> createOrReuseData(@Nullable PalettedContainer$Data<T> previousData, int bits) {
        PalettedContainer$Configuration<T> configuration = this.strategy.getConfiguration(this.registry, bits);
        return previousData != null && configuration.equals(previousData.configuration()) ? previousData : configuration.createData(this.registry, this, this.strategy.size());
    }

    @Override
    public int onResize(int newBits, T object) {
        PalettedContainer$Data<T> data = this.data;
        PalettedContainer$Data<T> data2 = this.createOrReuseData(data, newBits);
        data2.copyFrom(data.palette, data.storage);
        this.data = data2;
        return data2.palette.idFor(object);
    }

    public T setBlock(int x, int y, int z, T value) {
        this.acquire();

        Object var5;
        try {
            var5 = this.getAndSet(this.strategy.getIndex(x, y, z), value);
        } finally {
            this.release();
        }

        return (T)var5;
    }

    public T getAndSetUnchecked(int x, int y, int z, T value) {
        return this.getAndSet(this.strategy.getIndex(x, y, z), value);
    }

    private T getAndSet(int index, T value) {
        int i = this.data.palette.idFor(value);
        int j = this.data.storage.getAndSet(index, i);
        return this.data.palette.valueFor(j);
    }

    public void set(int x, int y, int z, T value) {
        this.acquire();

        try {
            this.setBlockIndex(this.strategy.getIndex(x, y, z), value);
        } finally {
            this.release();
        }

    }

    private void setBlockIndex(int index, T value) {
        int i = this.data.palette.idFor(value);
        this.data.storage.set(index, i);
    }

    public T get(int x, int y, int z) {
        return this.get(this.strategy.getIndex(x, y, z));
    }

    protected T get(int index) {
        PalettedContainer$Data<T> data = this.data;
        return data.palette.valueFor(data.storage.get(index));
    }

    public void getAll(Consumer<T> consumer) {
        DataPalette<T> palette = this.data.palette();
        IntSet intSet = new IntArraySet();
        this.data.storage.getAll(intSet::add);
        intSet.forEach((id) -> {
            consumer.accept(palette.valueFor(id));
        });
    }

    public void read(PacketDataSerializer buf) {
        this.acquire();

        try {
            int i = buf.readByte();
            PalettedContainer$Data<T> data = this.createOrReuseData(this.data, i);
            data.palette.read(buf);
            buf.readLongArray(data.storage.getRaw());
            this.data = data;
        } finally {
            this.release();
        }

    }

    public void write(PacketDataSerializer buf) {
        this.acquire();

        try {
            this.data.write(buf);
        } finally {
            this.release();
        }

    }

    private static <T> DataResult<DataPaletteBlock<T>> read(Registry<T> idList, PalettedContainer$Strategy provider, PalettedContainer$DiscData<T> serialized) {
        List<T> list = serialized.paletteEntries();
        int i = provider.size();
        int j = provider.calculateBitsForSerialization(idList, list.size());
        PalettedContainer$Configuration<T> configuration = provider.getConfiguration(idList, j);
        DataBits bitStorage;
        if (j == 0) {
            bitStorage = new ZeroBitStorage(i);
        } else {
            Optional<LongStream> optional = serialized.storage();
            if (optional.isEmpty()) {
                return DataResult.error("Missing values for non-zero storage");
            }

            long[] ls = optional.get().toArray();

            try {
                if (configuration.factory() == PalettedContainer$Strategy.GLOBAL_PALETTE_FACTORY) {
                    DataPalette<T> palette = new DataPaletteHash<>(idList, j, (ix, object) -> {
                        return 0;
                    }, list);
                    SimpleBitStorage simpleBitStorage = new SimpleBitStorage(j, i, ls);
                    int[] is = new int[i];
                    simpleBitStorage.unpack(is);
                    swapPalette(is, (ix) -> {
                        return idList.getId(palette.valueFor(ix));
                    });
                    bitStorage = new SimpleBitStorage(configuration.bits(), i, is);
                } else {
                    bitStorage = new SimpleBitStorage(configuration.bits(), i, ls);
                }
            } catch (SimpleBitStorage.InitializationException var13) {
                return DataResult.error("Failed to read PalettedContainer: " + var13.getMessage());
            }
        }

        return DataResult.success(new DataPaletteBlock<>(idList, provider, configuration, bitStorage, list));
    }

    private PalettedContainer$DiscData<T> write(Registry<T> idList, PalettedContainer$Strategy provider) {
        this.acquire();

        PalettedContainer$DiscData var12;
        try {
            DataPaletteHash<T> hashMapPalette = new DataPaletteHash<>(idList, this.data.storage.getBits(), this.dummyPaletteResize);
            int i = provider.size();
            int[] is = new int[i];
            this.data.storage.unpack(is);
            swapPalette(is, (id) -> {
                return hashMapPalette.idFor(this.data.palette.valueFor(id));
            });
            int j = provider.calculateBitsForSerialization(idList, hashMapPalette.getSize());
            Optional<LongStream> optional;
            if (j != 0) {
                SimpleBitStorage simpleBitStorage = new SimpleBitStorage(j, i, is);
                optional = Optional.of(Arrays.stream(simpleBitStorage.getRaw()));
            } else {
                optional = Optional.empty();
            }

            var12 = new PalettedContainer$DiscData<>(hashMapPalette.getEntries(), optional);
        } finally {
            this.release();
        }

        return var12;
    }

    private static <T> void swapPalette(int[] is, IntUnaryOperator intUnaryOperator) {
        int i = -1;
        int j = -1;

        for(int k = 0; k < is.length; ++k) {
            int l = is[k];
            if (l != i) {
                i = l;
                j = intUnaryOperator.applyAsInt(l);
            }

            is[k] = j;
        }

    }

    public int getSerializedSize() {
        return this.data.getSerializedSize();
    }

    public boolean contains(Predicate<T> predicate) {
        return this.data.palette.maybeHas(predicate);
    }

    public DataPaletteBlock<T> copy() {
        return new DataPaletteBlock<>(this.registry, this.strategy, new PalettedContainer$Data<>(this.data.configuration(), this.data.storage().copy(), this.data.palette().copy()));
    }

    public void count(DataPaletteBlock.CountConsumer<T> counter) {
        if (this.data.palette.getSize() == 1) {
            counter.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
        } else {
            Int2IntOpenHashMap int2IntOpenHashMap = new Int2IntOpenHashMap();
            this.data.storage.getAll((key) -> {
                int2IntOpenHashMap.addTo(key, 1);
            });
            int2IntOpenHashMap.int2IntEntrySet().forEach((entry) -> {
                counter.accept(this.data.palette.valueFor(entry.getIntKey()), entry.getIntValue());
            });
        }
    }

    @FunctionalInterface
    public interface CountConsumer<T> {
        void accept(T object, int count);
    }
}
