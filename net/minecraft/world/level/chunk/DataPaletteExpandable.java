package net.minecraft.world.level.chunk;

interface DataPaletteExpandable<T> {
    int onResize(int newBits, T object);
}
