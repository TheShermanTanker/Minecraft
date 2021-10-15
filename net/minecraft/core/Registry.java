package net.minecraft.core;

import javax.annotation.Nullable;

public interface Registry<T> extends Iterable<T> {
    int getId(T entry);

    @Nullable
    T fromId(int index);
}
