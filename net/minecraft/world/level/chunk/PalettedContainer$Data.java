package net.minecraft.world.level.chunk;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.DataBits;

record PalettedContainer$Data<T>(PalettedContainer$Configuration<T> configuration, DataBits storage, DataPalette<T> palette) {
    PalettedContainer$Data(PalettedContainer$Configuration<T> configuration, DataBits storage, DataPalette<T> palette) {
        this.configuration = configuration;
        this.storage = storage;
        this.palette = palette;
    }

    public void copyFrom(DataPalette<T> palette, DataBits storage) {
        for(int i = 0; i < storage.getSize(); ++i) {
            T object = palette.valueFor(storage.get(i));
            this.storage.set(i, this.palette.idFor(object));
        }

    }

    public int getSerializedSize() {
        return 1 + this.palette.getSerializedSize() + PacketDataSerializer.getVarIntSize(this.storage.getSize()) + this.storage.getRaw().length * 8;
    }

    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.storage.getBits());
        this.palette.write(buf);
        buf.writeLongArray(this.storage.getRaw());
    }

    public PalettedContainer$Configuration<T> configuration() {
        return this.configuration;
    }

    public DataBits storage() {
        return this.storage;
    }

    public DataPalette<T> palette() {
        return this.palette;
    }
}
