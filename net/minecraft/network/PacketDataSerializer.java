package net.minecraft.network;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTReadLimiter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class PacketDataSerializer extends ByteBuf {
    private static final int MAX_VARINT_SIZE = 5;
    private static final int MAX_VARLONG_SIZE = 10;
    private static final int DEFAULT_NBT_QUOTA = 2097152;
    private final ByteBuf source;
    public static final short MAX_STRING_LENGTH = 32767;
    public static final int MAX_COMPONENT_STRING_LENGTH = 262144;

    public PacketDataSerializer(ByteBuf parent) {
        this.source = parent;
    }

    public static int getVarIntSize(int value) {
        for(int i = 1; i < 5; ++i) {
            if ((value & -1 << i * 7) == 0) {
                return i;
            }
        }

        return 5;
    }

    public static int getVarLongSize(long value) {
        for(int i = 1; i < 10; ++i) {
            if ((value & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    public <T> T readWithCodec(Codec<T> codec) {
        NBTTagCompound compoundTag = this.readAnySizeNbt();
        DataResult<T> dataResult = codec.parse(DynamicOpsNBT.INSTANCE, compoundTag);
        dataResult.error().ifPresent((partialResult) -> {
            throw new EncoderException("Failed to decode: " + partialResult.message() + " " + compoundTag);
        });
        return dataResult.result().get();
    }

    public <T> void writeWithCodec(Codec<T> codec, T object) {
        DataResult<NBTBase> dataResult = codec.encodeStart(DynamicOpsNBT.INSTANCE, object);
        dataResult.error().ifPresent((partialResult) -> {
            throw new EncoderException("Failed to encode: " + partialResult.message() + " " + object);
        });
        this.writeNbt((NBTTagCompound)dataResult.result().get());
    }

    public static <T> IntFunction<T> limitValue(IntFunction<T> applier, int max) {
        return (value) -> {
            if (value > max) {
                throw new DecoderException("Value " + value + " is larger than limit " + max);
            } else {
                return applier.apply(value);
            }
        };
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, Function<PacketDataSerializer, T> entryParser) {
        int i = this.readVarInt();
        C collection = collectionFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            collection.add(entryParser.apply(this));
        }

        return collection;
    }

    public <T> void writeCollection(Collection<T> collection, BiConsumer<PacketDataSerializer, T> entrySerializer) {
        this.writeVarInt(collection.size());

        for(T object : collection) {
            entrySerializer.accept(this, object);
        }

    }

    public <T> List<T> readList(Function<PacketDataSerializer, T> entryParser) {
        return this.readCollection(Lists::newArrayListWithCapacity, entryParser);
    }

    public IntList readIntIdList() {
        int i = this.readVarInt();
        IntList intList = new IntArrayList();

        for(int j = 0; j < i; ++j) {
            intList.add(this.readVarInt());
        }

        return intList;
    }

    public void writeIntIdList(IntList list) {
        this.writeVarInt(list.size());
        list.forEach(this::writeVarInt);
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> mapFactory, Function<PacketDataSerializer, K> keyParser, Function<PacketDataSerializer, V> valueParser) {
        int i = this.readVarInt();
        M map = mapFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            K object = keyParser.apply(this);
            V object2 = valueParser.apply(this);
            map.put(object, object2);
        }

        return map;
    }

    public <K, V> Map<K, V> readMap(Function<PacketDataSerializer, K> keyParser, Function<PacketDataSerializer, V> valueParser) {
        return this.readMap(Maps::newHashMapWithExpectedSize, keyParser, valueParser);
    }

    public <K, V> void writeMap(Map<K, V> map, BiConsumer<PacketDataSerializer, K> keySerializer, BiConsumer<PacketDataSerializer, V> valueSerializer) {
        this.writeVarInt(map.size());
        map.forEach((key, value) -> {
            keySerializer.accept(this, key);
            valueSerializer.accept(this, value);
        });
    }

    public void readWithCount(Consumer<PacketDataSerializer> consumer) {
        int i = this.readVarInt();

        for(int j = 0; j < i; ++j) {
            consumer.accept(this);
        }

    }

    public <T> void writeOptional(Optional<T> value, BiConsumer<PacketDataSerializer, T> serializer) {
        if (value.isPresent()) {
            this.writeBoolean(true);
            serializer.accept(this, value.get());
        } else {
            this.writeBoolean(false);
        }

    }

    public <T> Optional<T> readOptional(Function<PacketDataSerializer, T> parser) {
        return this.readBoolean() ? Optional.of(parser.apply(this)) : Optional.empty();
    }

    public byte[] readByteArray() {
        return this.readByteArray(this.readableBytes());
    }

    public PacketDataSerializer writeByteArray(byte[] array) {
        this.writeVarInt(array.length);
        this.writeBytes(array);
        return this;
    }

    public byte[] readByteArray(int maxSize) {
        int i = this.readVarInt();
        if (i > maxSize) {
            throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
        } else {
            byte[] bs = new byte[i];
            this.readBytes(bs);
            return bs;
        }
    }

    public PacketDataSerializer writeVarIntArray(int[] array) {
        this.writeVarInt(array.length);

        for(int i : array) {
            this.writeVarInt(i);
        }

        return this;
    }

    public int[] readVarIntArray() {
        return this.readVarIntArray(this.readableBytes());
    }

    public int[] readVarIntArray(int maxSize) {
        int i = this.readVarInt();
        if (i > maxSize) {
            throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxSize);
        } else {
            int[] is = new int[i];

            for(int j = 0; j < is.length; ++j) {
                is[j] = this.readVarInt();
            }

            return is;
        }
    }

    public PacketDataSerializer writeLongArray(long[] array) {
        this.writeVarInt(array.length);

        for(long l : array) {
            this.writeLong(l);
        }

        return this;
    }

    public long[] readLongArray() {
        return this.readLongArray((long[])null);
    }

    public long[] readLongArray(@Nullable long[] toArray) {
        return this.readLongArray(toArray, this.readableBytes() / 8);
    }

    public long[] readLongArray(@Nullable long[] toArray, int maxSize) {
        int i = this.readVarInt();
        if (toArray == null || toArray.length != i) {
            if (i > maxSize) {
                throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + maxSize);
            }

            toArray = new long[i];
        }

        for(int j = 0; j < toArray.length; ++j) {
            toArray[j] = this.readLong();
        }

        return toArray;
    }

    @VisibleForTesting
    public byte[] accessByteBufWithCorrectSize() {
        int i = this.writerIndex();
        byte[] bs = new byte[i];
        this.getBytes(0, bs);
        return bs;
    }

    public BlockPosition readBlockPos() {
        return BlockPosition.fromLong(this.readLong());
    }

    public PacketDataSerializer writeBlockPos(BlockPosition pos) {
        this.writeLong(pos.asLong());
        return this;
    }

    public ChunkCoordIntPair readChunkPos() {
        return new ChunkCoordIntPair(this.readLong());
    }

    public PacketDataSerializer writeChunkPos(ChunkCoordIntPair pos) {
        this.writeLong(pos.pair());
        return this;
    }

    public SectionPosition readSectionPos() {
        return SectionPosition.of(this.readLong());
    }

    public PacketDataSerializer writeSectionPos(SectionPosition pos) {
        this.writeLong(pos.asLong());
        return this;
    }

    public IChatBaseComponent readComponent() {
        return IChatBaseComponent.ChatSerializer.fromJson(this.readUtf(262144));
    }

    public PacketDataSerializer writeComponent(IChatBaseComponent text) {
        return this.writeUtf(IChatBaseComponent.ChatSerializer.toJson(text), 262144);
    }

    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return (enumClass.getEnumConstants())[this.readVarInt()];
    }

    public PacketDataSerializer writeEnum(Enum<?> instance) {
        return this.writeVarInt(instance.ordinal());
    }

    public int readVarInt() {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = this.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b & 128) == 128);

        return i;
    }

    public long readVarLong() {
        long l = 0L;
        int i = 0;

        byte b;
        do {
            b = this.readByte();
            l |= (long)(b & 127) << i++ * 7;
            if (i > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while((b & 128) == 128);

        return l;
    }

    public PacketDataSerializer writeUUID(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public PacketDataSerializer writeVarInt(int value) {
        while((value & -128) != 0) {
            this.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        this.writeByte(value);
        return this;
    }

    public PacketDataSerializer writeVarLong(long value) {
        while((value & -128L) != 0L) {
            this.writeByte((int)(value & 127L) | 128);
            value >>>= 7;
        }

        this.writeByte((int)value);
        return this;
    }

    public PacketDataSerializer writeNbt(@Nullable NBTTagCompound compound) {
        if (compound == null) {
            this.writeByte(0);
        } else {
            try {
                NBTCompressedStreamTools.write(compound, new ByteBufOutputStream(this));
            } catch (IOException var3) {
                throw new EncoderException(var3);
            }
        }

        return this;
    }

    @Nullable
    public NBTTagCompound readNbt() {
        return this.readNbt(new NBTReadLimiter(2097152L));
    }

    @Nullable
    public NBTTagCompound readAnySizeNbt() {
        return this.readNbt(NBTReadLimiter.UNLIMITED);
    }

    @Nullable
    public NBTTagCompound readNbt(NBTReadLimiter sizeTracker) {
        int i = this.readerIndex();
        byte b = this.readByte();
        if (b == 0) {
            return null;
        } else {
            this.readerIndex(i);

            try {
                return NBTCompressedStreamTools.read(new ByteBufInputStream(this), sizeTracker);
            } catch (IOException var5) {
                throw new EncoderException(var5);
            }
        }
    }

    public PacketDataSerializer writeItem(ItemStack stack) {
        if (stack.isEmpty()) {
            this.writeBoolean(false);
        } else {
            this.writeBoolean(true);
            Item item = stack.getItem();
            this.writeVarInt(Item.getId(item));
            this.writeByte(stack.getCount());
            NBTTagCompound compoundTag = null;
            if (item.usesDurability() || item.shouldOverrideMultiplayerNbt()) {
                compoundTag = stack.getTag();
            }

            this.writeNbt(compoundTag);
        }

        return this;
    }

    public ItemStack readItem() {
        if (!this.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            int i = this.readVarInt();
            int j = this.readByte();
            ItemStack itemStack = new ItemStack(Item.getById(i), j);
            itemStack.setTag(this.readNbt());
            return itemStack;
        }
    }

    public String readUtf() {
        return this.readUtf(32767);
    }

    public String readUtf(int maxLength) {
        int i = this.readVarInt();
        if (i > maxLength * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
        } else if (i < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            String string = this.toString(this.readerIndex(), i, StandardCharsets.UTF_8);
            this.readerIndex(this.readerIndex() + i);
            if (string.length() > maxLength) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
            } else {
                return string;
            }
        }
    }

    public PacketDataSerializer writeUtf(String string) {
        return this.writeUtf(string, 32767);
    }

    public PacketDataSerializer writeUtf(String string, int maxLength) {
        byte[] bs = string.getBytes(StandardCharsets.UTF_8);
        if (bs.length > maxLength) {
            throw new EncoderException("String too big (was " + bs.length + " bytes encoded, max " + maxLength + ")");
        } else {
            this.writeVarInt(bs.length);
            this.writeBytes(bs);
            return this;
        }
    }

    public MinecraftKey readResourceLocation() {
        return new MinecraftKey(this.readUtf(32767));
    }

    public PacketDataSerializer writeResourceLocation(MinecraftKey id) {
        this.writeUtf(id.toString());
        return this;
    }

    public Date readDate() {
        return new Date(this.readLong());
    }

    public PacketDataSerializer writeDate(Date date) {
        this.writeLong(date.getTime());
        return this;
    }

    public MovingObjectPositionBlock readBlockHitResult() {
        BlockPosition blockPos = this.readBlockPos();
        EnumDirection direction = this.readEnum(EnumDirection.class);
        float f = this.readFloat();
        float g = this.readFloat();
        float h = this.readFloat();
        boolean bl = this.readBoolean();
        return new MovingObjectPositionBlock(new Vec3D((double)blockPos.getX() + (double)f, (double)blockPos.getY() + (double)g, (double)blockPos.getZ() + (double)h), direction, blockPos, bl);
    }

    public void writeBlockHitResult(MovingObjectPositionBlock hitResult) {
        BlockPosition blockPos = hitResult.getBlockPosition();
        this.writeBlockPos(blockPos);
        this.writeEnum(hitResult.getDirection());
        Vec3D vec3 = hitResult.getPos();
        this.writeFloat((float)(vec3.x - (double)blockPos.getX()));
        this.writeFloat((float)(vec3.y - (double)blockPos.getY()));
        this.writeFloat((float)(vec3.z - (double)blockPos.getZ()));
        this.writeBoolean(hitResult.isInside());
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(this.readLongArray());
    }

    public void writeBitSet(BitSet bitSet) {
        this.writeLongArray(bitSet.toLongArray());
    }

    @Override
    public int capacity() {
        return this.source.capacity();
    }

    @Override
    public ByteBuf capacity(int i) {
        return this.source.capacity(i);
    }

    @Override
    public int maxCapacity() {
        return this.source.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.source.alloc();
    }

    @Override
    public ByteOrder order() {
        return this.source.order();
    }

    @Override
    public ByteBuf order(ByteOrder byteOrder) {
        return this.source.order(byteOrder);
    }

    @Override
    public ByteBuf unwrap() {
        return this.source.unwrap();
    }

    @Override
    public boolean isDirect() {
        return this.source.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.source.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return this.source.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return this.source.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int i) {
        return this.source.readerIndex(i);
    }

    @Override
    public int writerIndex() {
        return this.source.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int i) {
        return this.source.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex(int i, int j) {
        return this.source.setIndex(i, j);
    }

    @Override
    public int readableBytes() {
        return this.source.readableBytes();
    }

    @Override
    public int writableBytes() {
        return this.source.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return this.source.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return this.source.isReadable();
    }

    @Override
    public boolean isReadable(int i) {
        return this.source.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return this.source.isWritable();
    }

    @Override
    public boolean isWritable(int i) {
        return this.source.isWritable(i);
    }

    @Override
    public ByteBuf clear() {
        return this.source.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return this.source.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return this.source.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return this.source.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return this.source.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return this.source.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return this.source.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(int i) {
        return this.source.ensureWritable(i);
    }

    @Override
    public int ensureWritable(int i, boolean bl) {
        return this.source.ensureWritable(i, bl);
    }

    @Override
    public boolean getBoolean(int i) {
        return this.source.getBoolean(i);
    }

    @Override
    public byte getByte(int i) {
        return this.source.getByte(i);
    }

    @Override
    public short getUnsignedByte(int i) {
        return this.source.getUnsignedByte(i);
    }

    @Override
    public short getShort(int i) {
        return this.source.getShort(i);
    }

    @Override
    public short getShortLE(int i) {
        return this.source.getShortLE(i);
    }

    @Override
    public int getUnsignedShort(int i) {
        return this.source.getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return this.source.getUnsignedShortLE(i);
    }

    @Override
    public int getMedium(int i) {
        return this.source.getMedium(i);
    }

    @Override
    public int getMediumLE(int i) {
        return this.source.getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium(int i) {
        return this.source.getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return this.source.getUnsignedMediumLE(i);
    }

    @Override
    public int getInt(int i) {
        return this.source.getInt(i);
    }

    @Override
    public int getIntLE(int i) {
        return this.source.getIntLE(i);
    }

    @Override
    public long getUnsignedInt(int i) {
        return this.source.getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return this.source.getUnsignedIntLE(i);
    }

    @Override
    public long getLong(int i) {
        return this.source.getLong(i);
    }

    @Override
    public long getLongLE(int i) {
        return this.source.getLongLE(i);
    }

    @Override
    public char getChar(int i) {
        return this.source.getChar(i);
    }

    @Override
    public float getFloat(int i) {
        return this.source.getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return this.source.getDouble(i);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf) {
        return this.source.getBytes(i, byteBuf);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int j) {
        return this.source.getBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int j, int k) {
        return this.source.getBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bs) {
        return this.source.getBytes(i, bs);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bs, int j, int k) {
        return this.source.getBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuffer byteBuffer) {
        return this.source.getBytes(i, byteBuffer);
    }

    @Override
    public ByteBuf getBytes(int i, OutputStream outputStream, int j) throws IOException {
        return this.source.getBytes(i, outputStream, j);
    }

    @Override
    public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int j) throws IOException {
        return this.source.getBytes(i, gatheringByteChannel, j);
    }

    @Override
    public int getBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
        return this.source.getBytes(i, fileChannel, l, j);
    }

    @Override
    public CharSequence getCharSequence(int i, int j, Charset charset) {
        return this.source.getCharSequence(i, j, charset);
    }

    @Override
    public ByteBuf setBoolean(int i, boolean bl) {
        return this.source.setBoolean(i, bl);
    }

    @Override
    public ByteBuf setByte(int i, int j) {
        return this.source.setByte(i, j);
    }

    @Override
    public ByteBuf setShort(int i, int j) {
        return this.source.setShort(i, j);
    }

    @Override
    public ByteBuf setShortLE(int i, int j) {
        return this.source.setShortLE(i, j);
    }

    @Override
    public ByteBuf setMedium(int i, int j) {
        return this.source.setMedium(i, j);
    }

    @Override
    public ByteBuf setMediumLE(int i, int j) {
        return this.source.setMediumLE(i, j);
    }

    @Override
    public ByteBuf setInt(int i, int j) {
        return this.source.setInt(i, j);
    }

    @Override
    public ByteBuf setIntLE(int i, int j) {
        return this.source.setIntLE(i, j);
    }

    @Override
    public ByteBuf setLong(int i, long l) {
        return this.source.setLong(i, l);
    }

    @Override
    public ByteBuf setLongLE(int i, long l) {
        return this.source.setLongLE(i, l);
    }

    @Override
    public ByteBuf setChar(int i, int j) {
        return this.source.setChar(i, j);
    }

    @Override
    public ByteBuf setFloat(int i, float f) {
        return this.source.setFloat(i, f);
    }

    @Override
    public ByteBuf setDouble(int i, double d) {
        return this.source.setDouble(i, d);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf) {
        return this.source.setBytes(i, byteBuf);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int j) {
        return this.source.setBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int j, int k) {
        return this.source.setBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bs) {
        return this.source.setBytes(i, bs);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bs, int j, int k) {
        return this.source.setBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuffer byteBuffer) {
        return this.source.setBytes(i, byteBuffer);
    }

    @Override
    public int setBytes(int i, InputStream inputStream, int j) throws IOException {
        return this.source.setBytes(i, inputStream, j);
    }

    @Override
    public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int j) throws IOException {
        return this.source.setBytes(i, scatteringByteChannel, j);
    }

    @Override
    public int setBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
        return this.source.setBytes(i, fileChannel, l, j);
    }

    @Override
    public ByteBuf setZero(int i, int j) {
        return this.source.setZero(i, j);
    }

    @Override
    public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
        return this.source.setCharSequence(i, charSequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return this.source.readBoolean();
    }

    @Override
    public byte readByte() {
        return this.source.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return this.source.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return this.source.readShort();
    }

    @Override
    public short readShortLE() {
        return this.source.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return this.source.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.source.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return this.source.readMedium();
    }

    @Override
    public int readMediumLE() {
        return this.source.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return this.source.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.source.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return this.source.readInt();
    }

    @Override
    public int readIntLE() {
        return this.source.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return this.source.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.source.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return this.source.readLong();
    }

    @Override
    public long readLongLE() {
        return this.source.readLongLE();
    }

    @Override
    public char readChar() {
        return this.source.readChar();
    }

    @Override
    public float readFloat() {
        return this.source.readFloat();
    }

    @Override
    public double readDouble() {
        return this.source.readDouble();
    }

    @Override
    public ByteBuf readBytes(int i) {
        return this.source.readBytes(i);
    }

    @Override
    public ByteBuf readSlice(int i) {
        return this.source.readSlice(i);
    }

    @Override
    public ByteBuf readRetainedSlice(int i) {
        return this.source.readRetainedSlice(i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf) {
        return this.source.readBytes(byteBuf);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i) {
        return this.source.readBytes(byteBuf, i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i, int j) {
        return this.source.readBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf readBytes(byte[] bs) {
        return this.source.readBytes(bs);
    }

    @Override
    public ByteBuf readBytes(byte[] bs, int i, int j) {
        return this.source.readBytes(bs, i, j);
    }

    @Override
    public ByteBuf readBytes(ByteBuffer byteBuffer) {
        return this.source.readBytes(byteBuffer);
    }

    @Override
    public ByteBuf readBytes(OutputStream outputStream, int i) throws IOException {
        return this.source.readBytes(outputStream, i);
    }

    @Override
    public int readBytes(GatheringByteChannel gatheringByteChannel, int i) throws IOException {
        return this.source.readBytes(gatheringByteChannel, i);
    }

    @Override
    public CharSequence readCharSequence(int i, Charset charset) {
        return this.source.readCharSequence(i, charset);
    }

    @Override
    public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return this.source.readBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf skipBytes(int i) {
        return this.source.skipBytes(i);
    }

    @Override
    public ByteBuf writeBoolean(boolean bl) {
        return this.source.writeBoolean(bl);
    }

    @Override
    public ByteBuf writeByte(int i) {
        return this.source.writeByte(i);
    }

    @Override
    public ByteBuf writeShort(int i) {
        return this.source.writeShort(i);
    }

    @Override
    public ByteBuf writeShortLE(int i) {
        return this.source.writeShortLE(i);
    }

    @Override
    public ByteBuf writeMedium(int i) {
        return this.source.writeMedium(i);
    }

    @Override
    public ByteBuf writeMediumLE(int i) {
        return this.source.writeMediumLE(i);
    }

    @Override
    public ByteBuf writeInt(int i) {
        return this.source.writeInt(i);
    }

    @Override
    public ByteBuf writeIntLE(int i) {
        return this.source.writeIntLE(i);
    }

    @Override
    public ByteBuf writeLong(long l) {
        return this.source.writeLong(l);
    }

    @Override
    public ByteBuf writeLongLE(long l) {
        return this.source.writeLongLE(l);
    }

    @Override
    public ByteBuf writeChar(int i) {
        return this.source.writeChar(i);
    }

    @Override
    public ByteBuf writeFloat(float f) {
        return this.source.writeFloat(f);
    }

    @Override
    public ByteBuf writeDouble(double d) {
        return this.source.writeDouble(d);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf) {
        return this.source.writeBytes(byteBuf);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i) {
        return this.source.writeBytes(byteBuf, i);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i, int j) {
        return this.source.writeBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf writeBytes(byte[] bs) {
        return this.source.writeBytes(bs);
    }

    @Override
    public ByteBuf writeBytes(byte[] bs, int i, int j) {
        return this.source.writeBytes(bs, i, j);
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer byteBuffer) {
        return this.source.writeBytes(byteBuffer);
    }

    @Override
    public int writeBytes(InputStream inputStream, int i) throws IOException {
        return this.source.writeBytes(inputStream, i);
    }

    @Override
    public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
        return this.source.writeBytes(scatteringByteChannel, i);
    }

    @Override
    public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return this.source.writeBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf writeZero(int i) {
        return this.source.writeZero(i);
    }

    @Override
    public int writeCharSequence(CharSequence charSequence, Charset charset) {
        return this.source.writeCharSequence(charSequence, charset);
    }

    @Override
    public int indexOf(int i, int j, byte b) {
        return this.source.indexOf(i, j, b);
    }

    @Override
    public int bytesBefore(byte b) {
        return this.source.bytesBefore(b);
    }

    @Override
    public int bytesBefore(int i, byte b) {
        return this.source.bytesBefore(i, b);
    }

    @Override
    public int bytesBefore(int i, int j, byte b) {
        return this.source.bytesBefore(i, j, b);
    }

    @Override
    public int forEachByte(ByteProcessor byteProcessor) {
        return this.source.forEachByte(byteProcessor);
    }

    @Override
    public int forEachByte(int i, int j, ByteProcessor byteProcessor) {
        return this.source.forEachByte(i, j, byteProcessor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor byteProcessor) {
        return this.source.forEachByteDesc(byteProcessor);
    }

    @Override
    public int forEachByteDesc(int i, int j, ByteProcessor byteProcessor) {
        return this.source.forEachByteDesc(i, j, byteProcessor);
    }

    @Override
    public ByteBuf copy() {
        return this.source.copy();
    }

    @Override
    public ByteBuf copy(int i, int j) {
        return this.source.copy(i, j);
    }

    @Override
    public ByteBuf slice() {
        return this.source.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return this.source.retainedSlice();
    }

    @Override
    public ByteBuf slice(int i, int j) {
        return this.source.slice(i, j);
    }

    @Override
    public ByteBuf retainedSlice(int i, int j) {
        return this.source.retainedSlice(i, j);
    }

    @Override
    public ByteBuf duplicate() {
        return this.source.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return this.source.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return this.source.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.source.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int i, int j) {
        return this.source.nioBuffer(i, j);
    }

    @Override
    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.source.internalNioBuffer(i, j);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.source.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.source.nioBuffers(i, j);
    }

    @Override
    public boolean hasArray() {
        return this.source.hasArray();
    }

    @Override
    public byte[] array() {
        return this.source.array();
    }

    @Override
    public int arrayOffset() {
        return this.source.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return this.source.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return this.source.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return this.source.toString(charset);
    }

    @Override
    public String toString(int i, int j, Charset charset) {
        return this.source.toString(i, j, charset);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this.source.equals(object);
    }

    @Override
    public int compareTo(ByteBuf byteBuf) {
        return this.source.compareTo(byteBuf);
    }

    @Override
    public String toString() {
        return this.source.toString();
    }

    @Override
    public ByteBuf retain(int i) {
        return this.source.retain(i);
    }

    @Override
    public ByteBuf retain() {
        return this.source.retain();
    }

    @Override
    public ByteBuf touch() {
        return this.source.touch();
    }

    @Override
    public ByteBuf touch(Object object) {
        return this.source.touch(object);
    }

    @Override
    public int refCnt() {
        return this.source.refCnt();
    }

    @Override
    public boolean release() {
        return this.source.release();
    }

    @Override
    public boolean release(int i) {
        return this.source.release(i);
    }
}
