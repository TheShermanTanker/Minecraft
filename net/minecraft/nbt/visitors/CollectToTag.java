package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTTagType;
import net.minecraft.nbt.StreamTagVisitor;

public class CollectToTag implements StreamTagVisitor {
    private String lastId = "";
    @Nullable
    private NBTBase rootTag;
    private final Deque<Consumer<NBTBase>> consumerStack = new ArrayDeque<>();

    @Nullable
    public NBTBase getResult() {
        return this.rootTag;
    }

    protected int depth() {
        return this.consumerStack.size();
    }

    private void appendEntry(NBTBase nbt) {
        this.consumerStack.getLast().accept(nbt);
    }

    @Override
    public StreamTagVisitor.ValueResult visitEnd() {
        this.appendEntry(NBTTagEnd.INSTANCE);
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(String value) {
        this.appendEntry(NBTTagString.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte value) {
        this.appendEntry(NBTTagByte.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(short value) {
        this.appendEntry(NBTTagShort.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int value) {
        this.appendEntry(NBTTagInt.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long value) {
        this.appendEntry(NBTTagLong.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(float value) {
        this.appendEntry(NBTTagFloat.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(double value) {
        this.appendEntry(NBTTagDouble.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte[] value) {
        this.appendEntry(new NBTTagByteArray(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int[] value) {
        this.appendEntry(new NBTTagIntArray(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long[] value) {
        this.appendEntry(new NBTTagLongArray(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitList(NBTTagType<?> entryType, int length) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.EntryResult visitElement(NBTTagType<?> type, int index) {
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type) {
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(NBTTagType<?> type, String key) {
        this.lastId = key;
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    private void enterContainerIfNeeded(NBTTagType<?> type) {
        if (type == NBTTagList.TYPE) {
            NBTTagList listTag = new NBTTagList();
            this.appendEntry(listTag);
            this.consumerStack.addLast(listTag::add);
        } else if (type == NBTTagCompound.TYPE) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            this.appendEntry(compoundTag);
            this.consumerStack.addLast((nbt) -> {
                compoundTag.set(this.lastId, nbt);
            });
        }

    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        this.consumerStack.removeLast();
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(NBTTagType<?> rootType) {
        if (rootType == NBTTagList.TYPE) {
            NBTTagList listTag = new NBTTagList();
            this.rootTag = listTag;
            this.consumerStack.addLast(listTag::add);
        } else if (rootType == NBTTagCompound.TYPE) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            this.rootTag = compoundTag;
            this.consumerStack.addLast((nbt) -> {
                compoundTag.set(this.lastId, nbt);
            });
        } else {
            this.consumerStack.addLast((nbt) -> {
                this.rootTag = nbt;
            });
        }

        return StreamTagVisitor.ValueResult.CONTINUE;
    }
}
