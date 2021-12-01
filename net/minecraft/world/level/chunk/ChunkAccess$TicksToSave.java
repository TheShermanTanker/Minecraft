package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.ticks.SerializableTickContainer;

public record ChunkAccess$TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<FluidType> fluids) {
    public ChunkAccess$TicksToSave(SerializableTickContainer<Block> serializableTickContainer, SerializableTickContainer<FluidType> serializableTickContainer2) {
        this.blocks = serializableTickContainer;
        this.fluids = serializableTickContainer2;
    }

    public SerializableTickContainer<Block> blocks() {
        return this.blocks;
    }

    public SerializableTickContainer<FluidType> fluids() {
        return this.fluids;
    }
}
