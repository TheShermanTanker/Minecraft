package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface NBTTagType<T extends NBTBase> {
    T load(DataInput input, int depth, NBTReadLimiter tracker) throws IOException;

    default boolean isValue() {
        return false;
    }

    String getName();

    String getPrettyName();

    static NBTTagType<NBTTagEnd> createInvalid(int type) {
        return new NBTTagType<NBTTagEnd>() {
            @Override
            public NBTTagEnd load(DataInput dataInput, int i, NBTReadLimiter nbtAccounter) {
                throw new IllegalArgumentException("Invalid tag id: " + type);
            }

            @Override
            public String getName() {
                return "INVALID[" + type + "]";
            }

            @Override
            public String getPrettyName() {
                return "UNKNOWN_" + type;
            }
        };
    }
}
