package net.minecraft.nbt;

public interface StreamTagVisitor {
    StreamTagVisitor.ValueResult visitEnd();

    StreamTagVisitor.ValueResult visit(String value);

    StreamTagVisitor.ValueResult visit(byte value);

    StreamTagVisitor.ValueResult visit(short value);

    StreamTagVisitor.ValueResult visit(int value);

    StreamTagVisitor.ValueResult visit(long value);

    StreamTagVisitor.ValueResult visit(float value);

    StreamTagVisitor.ValueResult visit(double value);

    StreamTagVisitor.ValueResult visit(byte[] value);

    StreamTagVisitor.ValueResult visit(int[] value);

    StreamTagVisitor.ValueResult visit(long[] value);

    StreamTagVisitor.ValueResult visitList(NBTTagType<?> entryType, int length);

    StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type);

    StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type, String key);

    StreamTagVisitor.EntryResult visitElement(NBTTagType<?> type, int index);

    StreamTagVisitor.ValueResult visitContainerEnd();

    StreamTagVisitor.ValueResult visitRootEntry(NBTTagType<?> rootType);

    public static enum EntryResult {
        ENTER,
        SKIP,
        BREAK,
        HALT;
    }

    public static enum ValueResult {
        CONTINUE,
        BREAK,
        HALT;
    }
}
