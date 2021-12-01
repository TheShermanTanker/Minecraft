package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.util.MathHelper;

public class NBTTagFloat extends NBTNumber {
    private static final int SELF_SIZE_IN_BITS = 96;
    public static final NBTTagFloat ZERO = new NBTTagFloat(0.0F);
    public static final NBTTagType<NBTTagFloat> TYPE = new TagType$StaticSize<NBTTagFloat>() {
        @Override
        public NBTTagFloat load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(96L);
            return NBTTagFloat.valueOf(dataInput.readFloat());
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            return visitor.visit(input.readFloat());
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public String getName() {
            return "FLOAT";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Float";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final float data;

    private NBTTagFloat(float value) {
        this.data = value;
    }

    public static NBTTagFloat valueOf(float value) {
        return value == 0.0F ? ZERO : new NBTTagFloat(value);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeFloat(this.data);
    }

    @Override
    public byte getTypeId() {
        return 5;
    }

    @Override
    public NBTTagType<NBTTagFloat> getType() {
        return TYPE;
    }

    @Override
    public NBTTagFloat copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagFloat && this.data == ((NBTTagFloat)object).data;
        }
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(this.data);
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitFloat(this);
    }

    @Override
    public long asLong() {
        return (long)this.data;
    }

    @Override
    public int asInt() {
        return MathHelper.floor(this.data);
    }

    @Override
    public short asShort() {
        return (short)(MathHelper.floor(this.data) & '\uffff');
    }

    @Override
    public byte asByte() {
        return (byte)(MathHelper.floor(this.data) & 255);
    }

    @Override
    public double asDouble() {
        return (double)this.data;
    }

    @Override
    public float asFloat() {
        return this.data;
    }

    @Override
    public Number getAsNumber() {
        return this.data;
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.data);
    }
}
