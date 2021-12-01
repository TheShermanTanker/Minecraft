package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class NBTTagByteArray extends NBTList<NBTTagByte> {
    private static final int SELF_SIZE_IN_BITS = 192;
    public static final NBTTagType<NBTTagByteArray> TYPE = new TagType$VariableSize<NBTTagByteArray>() {
        @Override
        public NBTTagByteArray load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(192L);
            int j = dataInput.readInt();
            nbtAccounter.accountBits(8L * (long)j);
            byte[] bs = new byte[j];
            dataInput.readFully(bs);
            return new NBTTagByteArray(bs);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            int i = input.readInt();
            byte[] bs = new byte[i];
            input.readFully(bs);
            return visitor.visit(bs);
        }

        @Override
        public void skip(DataInput input) throws IOException {
            input.skipBytes(input.readInt() * 1);
        }

        @Override
        public String getName() {
            return "BYTE[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Byte_Array";
        }
    };
    private byte[] data;

    public NBTTagByteArray(byte[] value) {
        this.data = value;
    }

    public NBTTagByteArray(List<Byte> value) {
        this(toArray(value));
    }

    private static byte[] toArray(List<Byte> list) {
        byte[] bs = new byte[list.size()];

        for(int i = 0; i < list.size(); ++i) {
            Byte byte_ = list.get(i);
            bs[i] = byte_ == null ? 0 : byte_;
        }

        return bs;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data.length);
        output.write(this.data);
    }

    @Override
    public byte getTypeId() {
        return 7;
    }

    @Override
    public NBTTagType<NBTTagByteArray> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public NBTBase clone() {
        byte[] bs = new byte[this.data.length];
        System.arraycopy(this.data, 0, bs, 0, this.data.length);
        return new NBTTagByteArray(bs);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagByteArray && Arrays.equals(this.data, ((NBTTagByteArray)object).data);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitByteArray(this);
    }

    public byte[] getBytes() {
        return this.data;
    }

    @Override
    public int size() {
        return this.data.length;
    }

    @Override
    public NBTTagByte get(int i) {
        return NBTTagByte.valueOf(this.data[i]);
    }

    @Override
    public NBTTagByte set(int i, NBTTagByte byteTag) {
        byte b = this.data[i];
        this.data[i] = byteTag.asByte();
        return NBTTagByte.valueOf(b);
    }

    @Override
    public void add(int i, NBTTagByte byteTag) {
        this.data = ArrayUtils.add(this.data, i, byteTag.asByte());
    }

    @Override
    public boolean setTag(int index, NBTBase element) {
        if (element instanceof NBTNumber) {
            this.data[index] = ((NBTNumber)element).asByte();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, NBTBase element) {
        if (element instanceof NBTNumber) {
            this.data = ArrayUtils.add(this.data, index, ((NBTNumber)element).asByte());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public NBTTagByte remove(int i) {
        byte b = this.data[i];
        this.data = ArrayUtils.remove(this.data, i);
        return NBTTagByte.valueOf(b);
    }

    @Override
    public byte getElementType() {
        return 1;
    }

    @Override
    public void clear() {
        this.data = new byte[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.data);
    }
}
