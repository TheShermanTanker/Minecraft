package net.minecraft.nbt.visitors;

import net.minecraft.nbt.NBTTagType;
import net.minecraft.nbt.StreamTagVisitor;

public interface SkipAll extends StreamTagVisitor {
    SkipAll INSTANCE = new SkipAll() {
    };

    @Override
    default StreamTagVisitor.ValueResult visitEnd() {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(String value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(byte value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(short value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(int value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(long value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(float value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(double value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(byte[] value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(int[] value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visit(long[] value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visitList(NBTTagType<?> entryType, int length) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.EntryResult visitElement(NBTTagType<?> type, int index) {
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    default StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type) {
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    default StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type, String key) {
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    default StreamTagVisitor.ValueResult visitContainerEnd() {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default StreamTagVisitor.ValueResult visitRootEntry(NBTTagType<?> rootType) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }
}
