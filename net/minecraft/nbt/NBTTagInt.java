package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagInt extends NBTNumber {
    private static final int SELF_SIZE_IN_BITS = 96;
    public static final NBTTagType<NBTTagInt> TYPE = new TagType$StaticSize<NBTTagInt>() {
        @Override
        public NBTTagInt load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(96L);
            return NBTTagInt.valueOf(dataInput.readInt());
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            return visitor.visit(input.readInt());
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public String getName() {
            return "INT";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Int";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final int data;

    NBTTagInt(int value) {
        this.data = value;
    }

    public static NBTTagInt valueOf(int value) {
        return value >= -128 && value <= 1024 ? NBTTagInt.Cache.cache[value - -128] : new NBTTagInt(value);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data);
    }

    @Override
    public byte getTypeId() {
        return 3;
    }

    @Override
    public NBTTagType<NBTTagInt> getType() {
        return TYPE;
    }

    @Override
    public NBTTagInt copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagInt && this.data == ((NBTTagInt)object).data;
        }
    }

    @Override
    public int hashCode() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitInt(this);
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
        return (short)(this.data & '\uffff');
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
        static final NBTTagInt[] cache = new NBTTagInt[1153];

        private Cache() {
        }

        static {
            for(int i = 0; i < cache.length; ++i) {
                cache[i] = new NBTTagInt(-128 + i);
            }

        }
    }
}
