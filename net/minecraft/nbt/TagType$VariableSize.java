package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface TagType$VariableSize<T extends NBTBase> extends NBTTagType<T> {
    @Override
    default void skip(DataInput input, int count) throws IOException {
        for(int i = 0; i < count; ++i) {
            this.skip(input);
        }

    }
}
