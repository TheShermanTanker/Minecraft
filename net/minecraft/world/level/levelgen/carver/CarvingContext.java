package net.minecraft.world.level.levelgen.carver;

import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class CarvingContext extends WorldGenerationContext {
    public CarvingContext(ChunkGenerator generator, IWorldHeightAccess world) {
        super(generator, world);
    }
}
