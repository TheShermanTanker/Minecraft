package net.minecraft.nbt;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class NBTTagList extends NBTList<NBTBase> {
    private static final int SELF_SIZE_IN_BITS = 296;
    public static final NBTTagType<NBTTagList> TYPE = new NBTTagType<NBTTagList>() {
        @Override
        public NBTTagList load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(296L);
            if (i > 512) {
                throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
            } else {
                byte b = dataInput.readByte();
                int j = dataInput.readInt();
                if (b == 0 && j > 0) {
                    throw new RuntimeException("Missing type on ListTag");
                } else {
                    nbtAccounter.accountBits(32L * (long)j);
                    NBTTagType<?> tagType = NBTTagTypes.getType(b);
                    List<NBTBase> list = Lists.newArrayListWithCapacity(j);

                    for(int k = 0; k < j; ++k) {
                        list.add(tagType.load(dataInput, i + 1, nbtAccounter));
                    }

                    return new NBTTagList(list, b);
                }
            }
        }

        @Override
        public String getName() {
            return "LIST";
        }

        @Override
        public String getPrettyName() {
            return "TAG_List";
        }
    };
    private final List<NBTBase> list;
    private byte type;

    NBTTagList(List<NBTBase> list, byte type) {
        this.list = list;
        this.type = type;
    }

    public NBTTagList() {
        this(Lists.newArrayList(), (byte)0);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        if (this.list.isEmpty()) {
            this.type = 0;
        } else {
            this.type = this.list.get(0).getTypeId();
        }

        output.writeByte(this.type);
        output.writeInt(this.list.size());

        for(NBTBase tag : this.list) {
            tag.write(output);
        }

    }

    @Override
    public byte getTypeId() {
        return 9;
    }

    @Override
    public NBTTagType<NBTTagList> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    private void updateTypeAfterRemove() {
        if (this.list.isEmpty()) {
            this.type = 0;
        }

    }

    @Override
    public NBTBase remove(int i) {
        NBTBase tag = this.list.remove(i);
        this.updateTypeAfterRemove();
        return tag;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public NBTTagCompound getCompound(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 10) {
                return (NBTTagCompound)tag;
            }
        }

        return new NBTTagCompound();
    }

    public NBTTagList getList(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 9) {
                return (NBTTagList)tag;
            }
        }

        return new NBTTagList();
    }

    public short getShort(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 2) {
                return ((NBTTagShort)tag).asShort();
            }
        }

        return 0;
    }

    public int getInt(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 3) {
                return ((NBTTagInt)tag).asInt();
            }
        }

        return 0;
    }

    public int[] getIntArray(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 11) {
                return ((NBTTagIntArray)tag).getInts();
            }
        }

        return new int[0];
    }

    public long[] getLongArray(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 11) {
                return ((NBTTagLongArray)tag).getLongs();
            }
        }

        return new long[0];
    }

    public double getDouble(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 6) {
                return ((NBTTagDouble)tag).asDouble();
            }
        }

        return 0.0D;
    }

    public float getFloat(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            if (tag.getTypeId() == 5) {
                return ((NBTTagFloat)tag).asFloat();
            }
        }

        return 0.0F;
    }

    public String getString(int index) {
        if (index >= 0 && index < this.list.size()) {
            NBTBase tag = this.list.get(index);
            return tag.getTypeId() == 8 ? tag.asString() : tag.toString();
        } else {
            return "";
        }
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public NBTBase get(int i) {
        return this.list.get(i);
    }

    @Override
    public NBTBase set(int i, NBTBase tag) {
        NBTBase tag2 = this.get(i);
        if (!this.setTag(i, tag)) {
            throw new UnsupportedOperationException(String.format("Trying to add tag of type %d to list of %d", tag.getTypeId(), this.type));
        } else {
            return tag2;
        }
    }

    @Override
    public void add(int i, NBTBase tag) {
        if (!this.addTag(i, tag)) {
            throw new UnsupportedOperationException(String.format("Trying to add tag of type %d to list of %d", tag.getTypeId(), this.type));
        }
    }

    @Override
    public boolean setTag(int index, NBTBase element) {
        if (this.updateType(element)) {
            this.list.set(index, element);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, NBTBase element) {
        if (this.updateType(element)) {
            this.list.add(index, element);
            return true;
        } else {
            return false;
        }
    }

    private boolean updateType(NBTBase element) {
        if (element.getTypeId() == 0) {
            return false;
        } else if (this.type == 0) {
            this.type = element.getTypeId();
            return true;
        } else {
            return this.type == element.getTypeId();
        }
    }

    @Override
    public NBTTagList copy() {
        Iterable<NBTBase> iterable = (Iterable<NBTBase>)(NBTTagTypes.getType(this.type).isValue() ? this.list : Iterables.transform(this.list, NBTBase::clone));
        List<NBTBase> list = Lists.newArrayList(iterable);
        return new NBTTagList(list, this.type);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof NBTTagList && Objects.equals(this.list, ((NBTTagList)object).list);
        }
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitList(this);
    }

    @Override
    public byte getElementType() {
        return this.type;
    }

    @Override
    public void clear() {
        this.list.clear();
        this.type = 0;
    }
}
