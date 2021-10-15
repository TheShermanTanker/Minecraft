package net.minecraft.nbt;

public class NBTTagTypes {
    private static final NBTTagType<?>[] TYPES = new NBTTagType[]{NBTTagEnd.TYPE, NBTTagByte.TYPE, NBTTagShort.TYPE, NBTTagInt.TYPE, NBTTagLong.TYPE, NBTTagFloat.TYPE, NBTTagDouble.TYPE, NBTTagByteArray.TYPE, NBTTagString.TYPE, NBTTagList.TYPE, NBTTagCompound.TYPE, NBTTagIntArray.TYPE, NBTTagLongArray.TYPE};

    public static NBTTagType<?> getType(int id) {
        return id >= 0 && id < TYPES.length ? TYPES[id] : NBTTagType.createInvalid(id);
    }
}
