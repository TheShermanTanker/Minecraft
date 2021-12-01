package net.minecraft.world.ticks;

import java.util.function.Function;
import net.minecraft.nbt.NBTBase;

public interface SerializableTickContainer<T> {
    NBTBase save(long time, Function<T, String> typeToNameFunction);
}
