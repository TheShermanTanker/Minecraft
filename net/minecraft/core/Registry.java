package net.minecraft.core;

import javax.annotation.Nullable;

public interface Registry<T> extends Iterable<T> {
    int DEFAULT = -1;

    int getId(T entry);

    @Nullable
    T fromId(int index);

    int size();
}
