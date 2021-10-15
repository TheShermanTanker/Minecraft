package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.util.MathHelper;

public class NBTTagDouble extends NBTNumber {
    private static final int SELF_SIZE_IN_BITS = 128;
    public static final NBTTagDouble ZERO = new NBTTagDouble(0.0D);
    public static final NBTTagType<NBTTagDouble> TYPE = new NBTTagType<NBTTagDouble>() {
        @Override
        public NBTTagDouble load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(128L);
            return NBTTagDouble.valueOf(dataInput.readDouble());
        }

        @Override
        public String getName() {
            return "DOUBLE";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Double";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final double data;

    private NBTTagDouble(double value) {
        this.data = value;
    }

    public static NBTTagDouble valueOf(double value) {
        return value == 0.0D ? ZERO : new NBTTagDouble(value);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeDouble(this.data);
    }

    @Override
    public byte getTypeId() {
        return 6;
    }

    @Override
    public NBTTagType<NBTTagDouble> getType() {
        return TYPE;
    }

    @Override
    public NBTTagDouble copy() {
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagDouble && this.data == ((NBTTagDouble)object).data;
        }
    }

    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(this.data);
        return (int)(l ^ l >>> 32);
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitDouble(this);
    }

    @Override
    public long asLong() {
        return (long)Math.floor(this.data);
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
        return this.data;
    }

    @Override
    public float asFloat() {
        return (float)this.data;
    }

    @Override
    public Number getAsNumber() {
        return this.data;
    }
}
