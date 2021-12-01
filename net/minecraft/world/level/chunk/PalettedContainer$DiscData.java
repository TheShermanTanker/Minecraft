package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

record PalettedContainer$DiscData<T>(List<T> paletteEntries, Optional<LongStream> storage) {
    PalettedContainer$DiscData(List<T> list, Optional<LongStream> optional) {
        this.paletteEntries = list;
        this.storage = optional;
    }

    public List<T> paletteEntries() {
        return this.paletteEntries;
    }

    public Optional<LongStream> storage() {
        return this.storage;
    }
}
