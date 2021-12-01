package net.minecraft.util;

import java.util.function.IntConsumer;

public interface DataBits {
    int getAndSet(int index, int value);

    void set(int index, int value);

    int get(int index);

    long[] getRaw();

    int getSize();

    int getBits();

    void getAll(IntConsumer action);

    void unpack(int[] is);

    DataBits copy();
}
