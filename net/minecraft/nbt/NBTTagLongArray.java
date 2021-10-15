package net.minecraft.nbt;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class NBTTagLongArray extends NBTList<NBTTagLong> {
    private static final int SELF_SIZE_IN_BITS = 192;
    public static final NBTTagType<NBTTagLongArray> TYPE = new NBTTagType<NBTTagLongArray>() {
        @Override
        public NBTTagLongArray load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(192L);
            int j = dataInput.readInt();
            nbtAccounter.accountBits(64L * (long)j);
            long[] ls = new long[j];

            for(int k = 0; k < j; ++k) {
                ls[k] = dataInput.readLong();
            }

            return new NBTTagLongArray(ls);
        }

        @Override
        public String getName() {
            return "LONG[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Long_Array";
        }
    };
    private long[] data;

    public NBTTagLongArray(long[] value) {
        this.data = value;
    }

    public NBTTagLongArray(LongSet value) {
        this.data = value.toLongArray();
    }

    public NBTTagLongArray(List<Long> value) {
        this(toArray(value));
    }

    private static long[] toArray(List<Long> list) {
        long[] ls = new long[list.size()];

        for(int i = 0; i < list.size(); ++i) {
            Long long_ = list.get(i);
            ls[i] = long_ == null ? 0L : long_;
        }

        return ls;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data.length);

        for(long l : this.data) {
            output.writeLong(l);
        }

    }

    @Override
    public byte getTypeId() {
        return 12;
    }

    @Override
    public NBTTagType<NBTTagLongArray> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public NBTTagLongArray copy() {
        long[] ls = new long[this.data.length];
        System.arraycopy(this.data, 0, ls, 0, this.data.length);
        return new NBTTagLongArray(ls);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagLongArray && Arrays.equals(this.data, ((NBTTagLongArray)object).data);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitLongArray(this);
    }

    public long[] getLongs() {
        return this.data;
    }

    @Override
    public int size() {
        return this.data.length;
    }

    @Override
    public NBTTagLong get(int i) {
        return NBTTagLong.valueOf(this.data[i]);
    }

    @Override
    public NBTTagLong set(int i, NBTTagLong longTag) {
        long l = this.data[i];
        this.data[i] = longTag.asLong();
        return NBTTagLong.valueOf(l);
    }

    @Override
    public void add(int i, NBTTagLong longTag) {
        this.data = ArrayUtils.add(this.data, i, longTag.asLong());
    }

    @Override
    public boolean setTag(int index, NBTBase element) {
        if (element instanceof NBTNumber) {
            this.data[index] = ((NBTNumber)element).asLong();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, NBTBase element) {
        if (element instanceof NBTNumber) {
            this.data = ArrayUtils.add(this.data, index, ((NBTNumber)element).asLong());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public NBTTagLong remove(int i) {
        long l = this.data[i];
        this.data = ArrayUtils.remove(this.data, i);
        return NBTTagLong.valueOf(l);
    }

    @Override
    public byte getElementType() {
        return 4;
    }

    @Override
    public void clear() {
        this.data = new long[0];
    }
}
