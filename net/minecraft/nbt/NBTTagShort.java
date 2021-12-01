package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagShort extends NBTNumber {
    private static final int SELF_SIZE_IN_BITS = 80;
    public static final NBTTagType<NBTTagShort> TYPE = new TagType$StaticSize<NBTTagShort>() {
        @Override
        public NBTTagShort load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(80L);
            return NBTTagShort.valueOf(dataInput.readShort());
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            return visitor.visit(input.readShort());
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public String getName() {
            return "SHORT";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Short";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final short data;

    NBTTagShort(short value) {
        this.data = value;
    }

    public static NBTTagShort valueOf(short value) {
        return value >= -128 && value <= 1024 ? NBTTagShort.Cache.cache[value - -128] : new NBTTagShort(value);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeShort(this.data);
    }

    @Override
    public byte getTypeId() {
        return 2;
    }

    @Override
    public NBTTagType<NBTTagShort> getType() {
        return TYPE;
    }

    @Override
    public NBTTagShort copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagShort && this.data == ((NBTTagShort)object).data;
        }
    }

    @Override
    public int hashCode() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitShort(this);
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
        return this.data;
    }

    @Override
    public byte asByte() {
        return (byte)(this.data & 255);
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

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.data);
    }

    static class Cache {
        private static final int HIGH = 1024;
        private static final int LOW = -128;
        static final NBTTagShort[] cache = new NBTTagShort[1153];

        private Cache() {
        }

        static {
            for(int i = 0; i < cache.length; ++i) {
                cache[i] = new NBTTagShort((short)(-128 + i));
            }

        }
    }
}
