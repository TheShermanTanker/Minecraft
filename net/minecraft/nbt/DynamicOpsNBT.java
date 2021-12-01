package net.minecraft.nbt;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class DynamicOpsNBT implements DynamicOps<NBTBase> {
    public static final DynamicOpsNBT INSTANCE = new DynamicOpsNBT();

    protected DynamicOpsNBT() {
    }

    public NBTBase empty() {
        return NBTTagEnd.INSTANCE;
    }

    public <U> U convertTo(DynamicOps<U> dynamicOps, NBTBase tag) {
        switch(tag.getTypeId()) {
        case 0:
            return dynamicOps.empty();
        case 1:
            return dynamicOps.createByte(((NBTNumber)tag).asByte());
        case 2:
            return dynamicOps.createShort(((NBTNumber)tag).asShort());
        case 3:
            return dynamicOps.createInt(((NBTNumber)tag).asInt());
        case 4:
            return dynamicOps.createLong(((NBTNumber)tag).asLong());
        case 5:
            return dynamicOps.createFloat(((NBTNumber)tag).asFloat());
        case 6:
            return dynamicOps.createDouble(((NBTNumber)tag).asDouble());
        case 7:
            return dynamicOps.createByteList(ByteBuffer.wrap(((NBTTagByteArray)tag).getBytes()));
        case 8:
            return dynamicOps.createString(tag.asString());
        case 9:
            return this.convertList(dynamicOps, tag);
        case 10:
            return this.convertMap(dynamicOps, tag);
        case 11:
            return dynamicOps.createIntList(Arrays.stream(((NBTTagIntArray)tag).getInts()));
        case 12:
            return dynamicOps.createLongList(Arrays.stream(((NBTTagLongArray)tag).getLongs()));
        default:
            throw new IllegalStateException("Unknown tag type: " + tag);
        }
    }

    public DataResult<Number> getNumberValue(NBTBase tag) {
        return tag instanceof NBTNumber ? DataResult.success(((NBTNumber)tag).getAsNumber()) : DataResult.error("Not a number");
    }

    public NBTBase createNumeric(Number number) {
        return NBTTagDouble.valueOf(number.doubleValue());
    }

    public NBTBase createByte(byte b) {
        return NBTTagByte.valueOf(b);
    }

    public NBTBase createShort(short s) {
        return NBTTagShort.valueOf(s);
    }

    public NBTBase createInt(int i) {
        return NBTTagInt.valueOf(i);
    }

    public NBTBase createLong(long l) {
        return NBTTagLong.valueOf(l);
    }

    public NBTBase createFloat(float f) {
        return NBTTagFloat.valueOf(f);
    }

    public NBTBase createDouble(double d) {
        return NBTTagDouble.valueOf(d);
    }

    public NBTBase createBoolean(boolean bl) {
        return NBTTagByte.valueOf(bl);
    }

    public DataResult<String> getStringValue(NBTBase tag) {
        return tag instanceof NBTTagString ? DataResult.success(tag.asString()) : DataResult.error("Not a string");
    }

    public NBTBase createString(String string) {
        return NBTTagString.valueOf(string);
    }

    private static NBTList<?> createGenericList(byte knownType, byte valueType) {
        if (typesMatch(knownType, valueType, (byte)4)) {
            return new NBTTagLongArray(new long[0]);
        } else if (typesMatch(knownType, valueType, (byte)1)) {
            return new NBTTagByteArray(new byte[0]);
        } else {
            return (NBTList<?>)(typesMatch(knownType, valueType, (byte)3) ? new NBTTagIntArray(new int[0]) : new NBTTagList());
        }
    }

    private static boolean typesMatch(byte knownType, byte valueType, byte expectedType) {
        return knownType == expectedType && (valueType == expectedType || valueType == 0);
    }

    private static <T extends NBTBase> void fillOne(NBTList<T> destination, NBTBase source, NBTBase additionalValue) {
        if (source instanceof NBTList) {
            NBTList<?> collectionTag = (NBTList)source;
            collectionTag.forEach((nbt) -> {
                destination.add(nbt);
            });
        }

        destination.add(additionalValue);
    }

    private static <T extends NBTBase> void fillMany(NBTList<T> destination, NBTBase source, List<NBTBase> additionalValues) {
        if (source instanceof NBTList) {
            NBTList<?> collectionTag = (NBTList)source;
            collectionTag.forEach((nbt) -> {
                destination.add(nbt);
            });
        }

        additionalValues.forEach((nbt) -> {
            destination.add(nbt);
        });
    }

    public DataResult<NBTBase> mergeToList(NBTBase tag, NBTBase tag2) {
        if (!(tag instanceof NBTList) && !(tag instanceof NBTTagEnd)) {
            return DataResult.error("mergeToList called with not a list: " + tag, tag);
        } else {
            NBTList<?> collectionTag = createGenericList(tag instanceof NBTList ? ((NBTList)tag).getElementType() : 0, tag2.getTypeId());
            fillOne(collectionTag, tag, tag2);
            return DataResult.success(collectionTag);
        }
    }

    public DataResult<NBTBase> mergeToList(NBTBase tag, List<NBTBase> list) {
        if (!(tag instanceof NBTList) && !(tag instanceof NBTTagEnd)) {
            return DataResult.error("mergeToList called with not a list: " + tag, tag);
        } else {
            NBTList<?> collectionTag = createGenericList(tag instanceof NBTList ? ((NBTList)tag).getElementType() : 0, list.stream().findFirst().map(NBTBase::getTypeId).orElse((byte)0));
            fillMany(collectionTag, tag, list);
            return DataResult.success(collectionTag);
        }
    }

    public DataResult<NBTBase> mergeToMap(NBTBase tag, NBTBase tag2, NBTBase tag3) {
        if (!(tag instanceof NBTTagCompound) && !(tag instanceof NBTTagEnd)) {
            return DataResult.error("mergeToMap called with not a map: " + tag, tag);
        } else if (!(tag2 instanceof NBTTagString)) {
            return DataResult.error("key is not a string: " + tag2, tag);
        } else {
            NBTTagCompound compoundTag = new NBTTagCompound();
            if (tag instanceof NBTTagCompound) {
                NBTTagCompound compoundTag2 = (NBTTagCompound)tag;
                compoundTag2.getKeys().forEach((key) -> {
                    compoundTag.set(key, compoundTag2.get(key));
                });
            }

            compoundTag.set(tag2.asString(), tag3);
            return DataResult.success(compoundTag);
        }
    }

    public DataResult<NBTBase> mergeToMap(NBTBase tag, MapLike<NBTBase> mapLike) {
        if (!(tag instanceof NBTTagCompound) && !(tag instanceof NBTTagEnd)) {
            return DataResult.error("mergeToMap called with not a map: " + tag, tag);
        } else {
            NBTTagCompound compoundTag = new NBTTagCompound();
            if (tag instanceof NBTTagCompound) {
                NBTTagCompound compoundTag2 = (NBTTagCompound)tag;
                compoundTag2.getKeys().forEach((string) -> {
                    compoundTag.set(string, compoundTag2.get(string));
                });
            }

            List<NBTBase> list = Lists.newArrayList();
            mapLike.entries().forEach((pair) -> {
                NBTBase tag = pair.getFirst();
                if (!(tag instanceof NBTTagString)) {
                    list.add(tag);
                } else {
                    compoundTag.set(tag.asString(), pair.getSecond());
                }
            });
            return !list.isEmpty() ? DataResult.error("some keys are not strings: " + list, compoundTag) : DataResult.success(compoundTag);
        }
    }

    public DataResult<Stream<Pair<NBTBase, NBTBase>>> getMapValues(NBTBase tag) {
        if (!(tag instanceof NBTTagCompound)) {
            return DataResult.error("Not a map: " + tag);
        } else {
            NBTTagCompound compoundTag = (NBTTagCompound)tag;
            return DataResult.success(compoundTag.getKeys().stream().map((key) -> {
                return Pair.of(this.createString(key), compoundTag.get(key));
            }));
        }
    }

    public DataResult<Consumer<BiConsumer<NBTBase, NBTBase>>> getMapEntries(NBTBase tag) {
        if (!(tag instanceof NBTTagCompound)) {
            return DataResult.error("Not a map: " + tag);
        } else {
            NBTTagCompound compoundTag = (NBTTagCompound)tag;
            return DataResult.success((entryConsumer) -> {
                compoundTag.getKeys().forEach((key) -> {
                    entryConsumer.accept(this.createString(key), compoundTag.get(key));
                });
            });
        }
    }

    public DataResult<MapLike<NBTBase>> getMap(NBTBase tag) {
        if (!(tag instanceof NBTTagCompound)) {
            return DataResult.error("Not a map: " + tag);
        } else {
            final NBTTagCompound compoundTag = (NBTTagCompound)tag;
            return DataResult.success(new MapLike<NBTBase>() {
                @Nullable
                public NBTBase get(NBTBase tag) {
                    return compoundTag.get(tag.asString());
                }

                @Nullable
                public NBTBase get(String string) {
                    return compoundTag.get(string);
                }

                public Stream<Pair<NBTBase, NBTBase>> entries() {
                    return compoundTag.getKeys().stream().map((key) -> {
                        return Pair.of(DynamicOpsNBT.this.createString(key), compoundTag.get(key));
                    });
                }

                @Override
                public String toString() {
                    return "MapLike[" + compoundTag + "]";
                }
            });
        }
    }

    public NBTBase createMap(Stream<Pair<NBTBase, NBTBase>> stream) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        stream.forEach((entry) -> {
            compoundTag.set(entry.getFirst().asString(), entry.getSecond());
        });
        return compoundTag;
    }

    public DataResult<Stream<NBTBase>> getStream(NBTBase tag) {
        return tag instanceof NBTList ? DataResult.success(((NBTList)tag).stream().map((nbt) -> {
            return nbt;
        })) : DataResult.error("Not a list");
    }

    public DataResult<Consumer<Consumer<NBTBase>>> getList(NBTBase tag) {
        if (tag instanceof NBTList) {
            NBTList<?> collectionTag = (NBTList)tag;
            return DataResult.success(collectionTag::forEach);
        } else {
            return DataResult.error("Not a list: " + tag);
        }
    }

    public DataResult<ByteBuffer> getByteBuffer(NBTBase tag) {
        return tag instanceof NBTTagByteArray ? DataResult.success(ByteBuffer.wrap(((NBTTagByteArray)tag).getBytes())) : DynamicOps.super.getByteBuffer(tag);
    }

    public NBTBase createByteList(ByteBuffer byteBuffer) {
        return new NBTTagByteArray(DataFixUtils.toArray(byteBuffer));
    }

    public DataResult<IntStream> getIntStream(NBTBase tag) {
        return tag instanceof NBTTagIntArray ? DataResult.success(Arrays.stream(((NBTTagIntArray)tag).getInts())) : DynamicOps.super.getIntStream(tag);
    }

    public NBTBase createIntList(IntStream intStream) {
        return new NBTTagIntArray(intStream.toArray());
    }

    public DataResult<LongStream> getLongStream(NBTBase tag) {
        return tag instanceof NBTTagLongArray ? DataResult.success(Arrays.stream(((NBTTagLongArray)tag).getLongs())) : DynamicOps.super.getLongStream(tag);
    }

    public NBTBase createLongList(LongStream longStream) {
        return new NBTTagLongArray(longStream.toArray());
    }

    public NBTBase createList(Stream<NBTBase> stream) {
        PeekingIterator<NBTBase> peekingIterator = Iterators.peekingIterator(stream.iterator());
        if (!peekingIterator.hasNext()) {
            return new NBTTagList();
        } else {
            NBTBase tag = peekingIterator.peek();
            if (tag instanceof NBTTagByte) {
                List<Byte> list = Lists.newArrayList(Iterators.transform(peekingIterator, (nbt) -> {
                    return ((NBTTagByte)nbt).asByte();
                }));
                return new NBTTagByteArray(list);
            } else if (tag instanceof NBTTagInt) {
                List<Integer> list2 = Lists.newArrayList(Iterators.transform(peekingIterator, (nbt) -> {
                    return ((NBTTagInt)nbt).asInt();
                }));
                return new NBTTagIntArray(list2);
            } else if (tag instanceof NBTTagLong) {
                List<Long> list3 = Lists.newArrayList(Iterators.transform(peekingIterator, (nbt) -> {
                    return ((NBTTagLong)nbt).asLong();
                }));
                return new NBTTagLongArray(list3);
            } else {
                NBTTagList listTag = new NBTTagList();

                while(peekingIterator.hasNext()) {
                    NBTBase tag2 = peekingIterator.next();
                    if (!(tag2 instanceof NBTTagEnd)) {
                        listTag.add(tag2);
                    }
                }

                return listTag;
            }
        }
    }

    public NBTBase remove(NBTBase tag, String string) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compoundTag = (NBTTagCompound)tag;
            NBTTagCompound compoundTag2 = new NBTTagCompound();
            compoundTag.getKeys().stream().filter((k) -> {
                return !Objects.equals(k, string);
            }).forEach((k) -> {
                compoundTag2.set(k, compoundTag.get(k));
            });
            return compoundTag2;
        } else {
            return tag;
        }
    }

    @Override
    public String toString() {
        return "NBT";
    }

    public RecordBuilder<NBTBase> mapBuilder() {
        return new DynamicOpsNBT.NBTRecordBuilder();
    }

    class NBTRecordBuilder extends AbstractStringBuilder<NBTBase, NBTTagCompound> {
        protected NBTRecordBuilder() {
            super(DynamicOpsNBT.this);
        }

        protected NBTTagCompound initBuilder() {
            return new NBTTagCompound();
        }

        protected NBTTagCompound append(String string, NBTBase tag, NBTTagCompound compoundTag) {
            compoundTag.set(string, tag);
            return compoundTag;
        }

        protected DataResult<NBTBase> build(NBTTagCompound compoundTag, NBTBase tag) {
            if (tag != null && tag != NBTTagEnd.INSTANCE) {
                if (!(tag instanceof NBTTagCompound)) {
                    return DataResult.error("mergeToMap called with not a map: " + tag, tag);
                } else {
                    NBTTagCompound compoundTag2 = new NBTTagCompound(Maps.newHashMap(((NBTTagCompound)tag).entries()));

                    for(Entry<String, NBTBase> entry : compoundTag.entries().entrySet()) {
                        compoundTag2.set(entry.getKey(), entry.getValue());
                    }

                    return DataResult.success(compoundTag2);
                }
            } else {
                return DataResult.success(compoundTag);
            }
        }
    }
}
