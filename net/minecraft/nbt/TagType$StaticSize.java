package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface TagType$StaticSize<T extends NBTBase> extends NBTTagType<T> {
    @Override
    default void skip(DataInput input) throws IOException {
        input.skipBytes(this.size());
    }

    @Override
    default void skip(DataInput input, int count) throws IOException {
        input.skipBytes(this.size() * count);
    }

    int size();
}
