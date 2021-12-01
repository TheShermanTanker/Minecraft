package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

@FunctionalInterface
public interface PostPlacementProcessor {
    PostPlacementProcessor NONE = (world, structureAccessor, chunkGenerator, random, chunkBox, pos, children) -> {
    };

    void afterPlace(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair pos, PiecesContainer children);
}
