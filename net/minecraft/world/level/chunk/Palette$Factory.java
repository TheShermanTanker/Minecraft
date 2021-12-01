package net.minecraft.world.level.chunk;

import java.util.List;
import net.minecraft.core.Registry;

public interface Palette$Factory {
    <A> DataPalette<A> create(int bits, Registry<A> idList, DataPaletteExpandable<A> listener, List<A> list);
}
