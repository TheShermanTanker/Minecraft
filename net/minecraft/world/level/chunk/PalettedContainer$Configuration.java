package net.minecraft.world.level.chunk;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.util.DataBits;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ZeroBitStorage;

record PalettedContainer$Configuration<T>(Palette$Factory factory, int bits) {
    PalettedContainer$Configuration(Palette$Factory factory, int i) {
        this.factory = factory;
        this.bits = i;
    }

    public PalettedContainer$Data<T> createData(Registry<T> idList, DataPaletteExpandable<T> listener, int size) {
        DataBits bitStorage = (DataBits)(this.bits == 0 ? new ZeroBitStorage(size) : new SimpleBitStorage(this.bits, size));
        DataPalette<T> palette = this.factory.create(this.bits, idList, listener, List.of());
        return new PalettedContainer$Data<>(this, bitStorage, palette);
    }

    public Palette$Factory factory() {
        return this.factory;
    }

    public int bits() {
        return this.bits;
    }
}
