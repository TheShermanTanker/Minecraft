package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;

public class NBTTagCompound implements NBTBase {
    public static final Codec<NBTTagCompound> CODEC = Codec.PASSTHROUGH.comapFlatMap((dynamic) -> {
        NBTBase tag = dynamic.convert(DynamicOpsNBT.INSTANCE).getValue();
        return tag instanceof NBTTagCompound ? DataResult.success((NBTTagCompound)tag) : DataResult.error("Not a compound tag: " + tag);
    }, (nbt) -> {
        return new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt);
    });
    private static final int SELF_SIZE_IN_BITS = 384;
    private static final int MAP_ENTRY_SIZE_IN_BITS = 256;
    public static final NBTTagType<NBTTagCompound> TYPE = new NBTTagType<NBTTagCompound>() {
        @Override
        public NBTTagCompound load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(384L);
            if (i > 512) {
                throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
            } else {
                Map<String, NBTBase> map = Maps.newHashMap();

                byte b;
                while((b = NBTTagCompound.readNamedTagType(dataInput, nbtAccounter)) != 0) {
                    String string = NBTTagCompound.readNamedTagName(dataInput, nbtAccounter);
                    nbtAccounter.accountBits((long)(224 + 16 * string.length()));
                    NBTBase tag = NBTTagCompound.readNamedTagData(NBTTagTypes.getType(b), string, dataInput, i + 1, nbtAccounter);
                    if (map.put(string, tag) != null) {
                        nbtAccounter.accountBits(288L);
                    }
                }

                return new NBTTagCompound(map);
            }
        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, NBTBase> tags;

    protected NBTTagCompound(Map<String, NBTBase> entries) {
        this.tags = entries;
    }

    public NBTTagCompound() {
        this(Maps.newHashMap());
    }

    @Override
    public void write(DataOutput output) throws IOException {
        for(String string : this.tags.keySet()) {
            NBTBase tag = this.tags.get(string);
            writeNamedTag(string, tag, output);
        }

        output.writeByte(0);
    }

    public Set<String> getKeys() {
        return this.tags.keySet();
    }

    @Override
    public byte getTypeId() {
        return 10;
    }

    @Override
    public NBTTagType<NBTTagCompound> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    @Nullable
    public NBTBase set(String key, NBTBase element) {
        return this.tags.put(key, element);
    }

    public void setByte(String key, byte value) {
        this.tags.put(key, NBTTagByte.valueOf(value));
    }

    public void setShort(String key, short value) {
        this.tags.put(key, NBTTagShort.valueOf(value));
    }

    public void setInt(String key, int value) {
        this.tags.put(key, NBTTagInt.valueOf(value));
    }

    public void setLong(String key, long value) {
        this.tags.put(key, NBTTagLong.valueOf(value));
    }

    public void putUUID(String key, UUID value) {
        this.tags.put(key, GameProfileSerializer.createUUID(value));
    }

    public UUID getUUID(String key) {
        return GameProfileSerializer.loadUUID(this.get(key));
    }

    public boolean hasUUID(String key) {
        NBTBase tag = this.get(key);
        return tag != null && tag.getType() == NBTTagIntArray.TYPE && ((NBTTagIntArray)tag).getInts().length == 4;
    }

    public void setFloat(String key, float value) {
        this.tags.put(key, NBTTagFloat.valueOf(value));
    }

    public void setDouble(String key, double value) {
        this.tags.put(key, NBTTagDouble.valueOf(value));
    }

    public void setString(String key, String value) {
        this.tags.put(key, NBTTagString.valueOf(value));
    }

    public void setByteArray(String key, byte[] value) {
        this.tags.put(key, new NBTTagByteArray(value));
    }

    public void putByteArray(String key, List<Byte> value) {
        this.tags.put(key, new NBTTagByteArray(value));
    }

    public void setIntArray(String key, int[] value) {
        this.tags.put(key, new NBTTagIntArray(value));
    }

    public void putIntArray(String key, List<Integer> value) {
        this.tags.put(key, new NBTTagIntArray(value));
    }

    public void putLongArray(String key, long[] value) {
        this.tags.put(key, new NBTTagLongArray(value));
    }

    public void putLongArray(String key, List<Long> value) {
        this.tags.put(key, new NBTTagLongArray(value));
    }

    public void setBoolean(String key, boolean value) {
        this.tags.put(key, NBTTagByte.valueOf(value));
    }

    @Nullable
    public NBTBase get(String key) {
        return this.tags.get(key);
    }

    public byte getTagType(String key) {
        NBTBase tag = this.tags.get(key);
        return tag == null ? 0 : tag.getTypeId();
    }

    public boolean hasKey(String key) {
        return this.tags.containsKey(key);
    }

    public boolean hasKeyOfType(String key, int type) {
        int i = this.getTagType(key);
        if (i == type) {
            return true;
        } else if (type != 99) {
            return false;
        } else {
            return i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6;
        }
    }

    public byte getByte(String key) {
        try {
            if (this.hasKeyOfType(key, 99)) {
                return ((NBTNumber)this.tags.get(key)).asByte();
            }
        } catch (ClassCastException var3) {
        }

        return 0;
    }

    public short getShort(String key) {
        try {
            if (this.hasKeyOfType(key, 99)) {
                return ((NBTNumber)this.tags.get(key)).asShort();
            }
        } catch (ClassCastException var3) {
        }

        return 0;
    }

    public int getInt(String key) {
        try {
            if (this.hasKeyOfType(key, 99)) {
                return ((NBTNumber)this.tags.get(key)).asInt();
            }
        } catch (ClassCastException var3) {
        }

        return 0;
    }

    public long getLong(String key) {
        try {
            if (this.hasKeyOfType(key, 99)) {
                return ((NBTNumber)this.tags.get(key)).asLong();
            }
        } catch (ClassCastException var3) {
        }

        return 0L;
    }

    public float getFloat(String key) {
        try {
            if (this.hasKeyOfType(key, 99)) {
                return ((NBTNumber)this.tags.get(key)).asFloat();
            }
        } catch (ClassCastException var3) {
        }

        return 0.0F;
    }

    public double getDouble(String key) {
        try {
            if (this.hasKeyOfType(key, 99)) {
                return ((NBTNumber)this.tags.get(key)).asDouble();
            }
        } catch (ClassCastException var3) {
        }

        return 0.0D;
    }

    public String getString(String key) {
        try {
            if (this.hasKeyOfType(key, 8)) {
                return this.tags.get(key).asString();
            }
        } catch (ClassCastException var3) {
        }

        return "";
    }

    public byte[] getByteArray(String key) {
        try {
            if (this.hasKeyOfType(key, 7)) {
                return ((NBTTagByteArray)this.tags.get(key)).getBytes();
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, NBTTagByteArray.TYPE, var3));
        }

        return new byte[0];
    }

    public int[] getIntArray(String key) {
        try {
            if (this.hasKeyOfType(key, 11)) {
                return ((NBTTagIntArray)this.tags.get(key)).getInts();
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, NBTTagIntArray.TYPE, var3));
        }

        return new int[0];
    }

    public long[] getLongArray(String key) {
        try {
            if (this.hasKeyOfType(key, 12)) {
                return ((NBTTagLongArray)this.tags.get(key)).getLongs();
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, NBTTagLongArray.TYPE, var3));
        }

        return new long[0];
    }

    public NBTTagCompound getCompound(String key) {
        try {
            if (this.hasKeyOfType(key, 10)) {
                return (NBTTagCompound)this.tags.get(key);
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, TYPE, var3));
        }

        return new NBTTagCompound();
    }

    public NBTTagList getList(String key, int type) {
        try {
            if (this.getTagType(key) == 9) {
                NBTTagList listTag = (NBTTagList)this.tags.get(key);
                if (!listTag.isEmpty() && listTag.getElementType() != type) {
                    return new NBTTagList();
                }

                return listTag;
            }
        } catch (ClassCastException var4) {
            throw new ReportedException(this.createReport(key, NBTTagList.TYPE, var4));
        }

        return new NBTTagList();
    }

    public boolean getBoolean(String key) {
        return this.getByte(key) != 0;
    }

    public void remove(String key) {
        this.tags.remove(key);
    }

    @Override
    public String toString() {
        return this.asString();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    private CrashReport createReport(String key, NBTTagType<?> reader, ClassCastException classCastException) {
        CrashReport crashReport = CrashReport.forThrowable(classCastException, "Reading NBT data");
        CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Corrupt NBT tag", 1);
        crashReportCategory.setDetail("Tag type found", () -> {
            return this.tags.get(key).getType().getName();
        });
        crashReportCategory.setDetail("Tag type expected", reader::getName);
        crashReportCategory.setDetail("Tag name", key);
        return crashReport;
    }

    @Override
    public NBTTagCompound c() {
        Map<String, NBTBase> map = Maps.newHashMap(Maps.transformValues(this.tags, NBTBase::clone));
        return new NBTTagCompound(map);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagCompound && Objects.equals(this.tags, ((NBTTagCompound)object).tags);
        }
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String key, NBTBase element, DataOutput output) throws IOException {
        output.writeByte(element.getTypeId());
        if (element.getTypeId() != 0) {
            output.writeUTF(key);
            element.write(output);
        }
    }

    static byte readNamedTagType(DataInput input, NBTReadLimiter tracker) throws IOException {
        return input.readByte();
    }

    static String readNamedTagName(DataInput input, NBTReadLimiter tracker) throws IOException {
        return input.readUTF();
    }

    static NBTBase readNamedTagData(NBTTagType<?> reader, String key, DataInput input, int depth, NBTReadLimiter tracker) {
        try {
            return reader.load(input, depth, tracker);
        } catch (IOException var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Loading NBT data");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("NBT Tag");
            crashReportCategory.setDetail("Tag name", key);
            crashReportCategory.setDetail("Tag type", reader.getName());
            throw new ReportedException(crashReport);
        }
    }

    public NBTTagCompound merge(NBTTagCompound source) {
        for(String string : source.tags.keySet()) {
            NBTBase tag = source.tags.get(string);
            if (tag.getTypeId() == 10) {
                if (this.hasKeyOfType(string, 10)) {
                    NBTTagCompound compoundTag = this.getCompound(string);
                    compoundTag.merge((NBTTagCompound)tag);
                } else {
                    this.set(string, tag.clone());
                }
            } else {
                this.set(string, tag.clone());
            }
        }

        return this;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitCompound(this);
    }

    protected Map<String, NBTBase> entries() {
        return Collections.unmodifiableMap(this.tags);
    }
}
