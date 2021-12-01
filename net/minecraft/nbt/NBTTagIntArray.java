package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class NBTTagIntArray extends NBTList<NBTTagInt> {
    private static final int SELF_SIZE_IN_BITS = 192;
    public static final NBTTagType<NBTTagIntArray> TYPE = new TagType$VariableSize<NBTTagIntArray>() {
        @Override
        public NBTTagIntArray load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(192L);
            int j = dataInput.readInt();
            nbtAccounter.accountBits(32L * (long)j);
            int[] is = new int[j];

            for(int k = 0; k < j; ++k) {
                is[k] = dataInput.readInt();
            }

            return new NBTTagIntArray(is);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            int i = input.readInt();
            int[] is = new int[i];

            for(int j = 0; j < i; ++j) {
                is[j] = input.readInt();
            }

            return visitor.visit(is);
        }

        @Override
        public void skip(DataInput input) throws IOException {
            input.skipBytes(input.readInt() * 4);
        }

        @Override
        public String getName() {
            return "INT[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Int_Array";
        }
    };
    private int[] data;

    public NBTTagIntArray(int[] value) {
        this.data = value;
    }

    public NBTTagIntArray(List<Integer> value) {
        this(toArray(value));
    }

    private static int[] toArray(List<Integer> list) {
        int[] is = new int[list.size()];

        for(int i = 0; i < list.size(); ++i) {
            Integer integer = list.get(i);
            is[i] = integer == null ? 0 : integer;
        }

        return is;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data.length);

        for(int i : this.data) {
            output.writeInt(i);
        }

    }

    @Override
    public byte getTypeId() {
        return 11;
    }

    @Override
    public NBTTagType<NBTTagIntArray> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public NBTTagIntArray copy() {
        int[] is = new int[this.data.length];
        System.arraycopy(this.data, 0, is, 0, this.data.length);
        return new NBTTagIntArray(is);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagIntArray && Arrays.equals(this.data, ((NBTTagIntArray)object).data);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    public int[] getInts() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitIntArray(this);
    }

    @Override
    public int size() {
        return this.data.length;
    }

    @Override
    public NBTTagInt get(int i) {
        return NBTTagInt.valueOf(this.data[i]);
    }

    @Override
    public NBTTagInt set(int i, NBTTagInt intTag) {
        int j = this.data[i];
        this.data[i] = intTag.asInt();
        return NBTTagInt.valueOf(j);
    }

    @Override
    public void add(int i, NBTTagInt intTag) {
        this.data = ArrayUtils.add(this.data, i, intTag.asInt());
    }

    @Override
    public boolean setTag(int index, NBTBase element) {
        if (element instanceof NBTNumber) {
            this.data[index] = ((NBTNumber)element).asInt();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, NBTBase element) {
        if (element instanceof NBTNumber) {
            this.data = ArrayUtils.add(this.data, index, ((NBTNumber)element).asInt());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public NBTTagInt remove(int i) {
        int j = this.data[i];
        this.data = ArrayUtils.remove(this.data, i);
        return NBTTagInt.valueOf(j);
    }

    @Override
    public byte getElementType() {
        return 3;
    }

    @Override
    public void clear() {
        this.data = new int[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.data);
    }
}
