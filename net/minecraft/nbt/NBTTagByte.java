package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagByte extends NBTNumber {
    private static final int SELF_SIZE_IN_BITS = 72;
    public static final NBTTagType<NBTTagByte> TYPE = new NBTTagType<NBTTagByte>() {
        @Override
        public NBTTagByte load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(72L);
            return NBTTagByte.valueOf(dataInput.readByte());
        }

        @Override
        public String getName() {
            return "BYTE";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Byte";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    public static final NBTTagByte ZERO = valueOf((byte)0);
    public static final NBTTagByte ONE = valueOf((byte)1);
    private final byte data;

    NBTTagByte(byte value) {
        this.data = value;
    }

    public static NBTTagByte valueOf(byte value) {
        return NBTTagByte.Cache.cache[128 + value];
    }

    public static NBTTagByte valueOf(boolean value) {
        return value ? ONE : ZERO;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeByte(this.data);
    }

    @Override
    public byte getTypeId() {
        return 1;
    }

    @Override
    public NBTTagType<NBTTagByte> getType() {
        return TYPE;
    }

    @Override
    public NBTTagByte copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagByte && this.data == ((NBTTagByte)object).data;
        }
    }

    @Override
    public int hashCode() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitByte(this);
    }

    @Override
    public long asLong() {
        return (long)this.data;
    }

    @Override
    public int asInt() {
        return this.data;
    }

    @Override
    public short asShort() {
        return (short)this.data;
    }

    @Override
    public byte asByte() {
        return this.data;
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
        static final NBTTagByte[] cache = new NBTTagByte[256];

        private Cache() {
        }

        static {
            for(int i = 0; i < cache.length; ++i) {
                cache[i] = new NBTTagByte((byte)(i - 128));
            }

        }
    }
}
