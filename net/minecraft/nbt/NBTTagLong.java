package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagLong extends NBTNumber {
    private static final int SELF_SIZE_IN_BITS = 128;
    public static final NBTTagType<NBTTagLong> TYPE = new NBTTagType<NBTTagLong>() {
        @Override
        public NBTTagLong load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(128L);
            return NBTTagLong.valueOf(dataInput.readLong());
        }

        @Override
        public String getName() {
            return "LONG";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Long";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final long data;

    NBTTagLong(long l) {
        this.data = l;
    }

    public static NBTTagLong valueOf(long value) {
        return value >= -128L && value <= 1024L ? NBTTagLong.Cache.cache[(int)value - -128] : new NBTTagLong(value);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeLong(this.data);
    }

    @Override
    public byte getTypeId() {
        return 4;
    }

    @Override
    public NBTTagType<NBTTagLong> getType() {
        return TYPE;
    }

    @Override
    public NBTTagLong copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagLong && this.data == ((NBTTagLong)object).data;
        }
    }

    @Override
    public int hashCode() {
        return (int)(this.data ^ this.data >>> 32);
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitLong(this);
    }

    @Override
    public long asLong() {
        return this.data;
    }

    @Override
    public int asInt() {
        return (int)(this.data & -1L);
    }

    @Override
    public short asShort() {
        return (short)((int)(this.data & 65535L));
    }

    @Override
    public byte asByte() {
        return (byte)((int)(this.data & 255L));
    }

    @Override
    public double asDouble() {
        return (double)this.data;
    }

    @Override
    public float asFloat() {
        return (float)this.data;
    }

    @Override
    public Number getAsNumber() {
        return this.data;
    }

    static class Cache {
        private static final int HIGH = 1024;
        private static final int LOW = -128;
        static final NBTTagLong[] cache = new NBTTagLong[1153];

        private Cache() {
        }

        static {
            for(int i = 0; i < cache.length; ++i) {
                cache[i] = new NBTTagLong((long)(-128 + i));
            }

        }
    }
}
