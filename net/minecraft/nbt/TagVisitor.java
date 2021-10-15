package net.minecraft.nbt;

public interface TagVisitor {
    void visitString(NBTTagString element);

    void visitByte(NBTTagByte element);

    void visitShort(NBTTagShort element);

    void visitInt(NBTTagInt element);

    void visitLong(NBTTagLong element);

    void visitFloat(NBTTagFloat element);

    void visitDouble(NBTTagDouble element);

    void visitByteArray(NBTTagByteArray element);

    void visitIntArray(NBTTagIntArray element);

    void visitLongArray(NBTTagLongArray element);

    void visitList(NBTTagList element);

    void visitCompound(NBTTagCompound compound);

    void visitEnd(NBTTagEnd element);
}
