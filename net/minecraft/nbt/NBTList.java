package net.minecraft.nbt;

import java.util.AbstractList;

public abstract class NBTList<T extends NBTBase> extends AbstractList<T> implements NBTBase {
    @Override
    public abstract T set(int i, T tag);

    @Override
    public abstract void add(int i, T tag);

    @Override
    public abstract T remove(int i);

    public abstract boolean setTag(int index, NBTBase element);

    public abstract boolean addTag(int index, NBTBase element);

    public abstract byte getElementType();
}
