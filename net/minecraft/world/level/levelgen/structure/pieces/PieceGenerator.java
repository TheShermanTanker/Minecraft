package net.minecraft.world.level.levelgen.structure.pieces;

import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

@FunctionalInterface
public interface PieceGenerator<C extends WorldGenFeatureConfiguration> {
    void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<C> context);

    public static record Context<C extends WorldGenFeatureConfiguration>(C config, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, ChunkCoordIntPair chunkPos, IWorldHeightAccess heightAccessor, SeededRandom random, long seed) {
        public Context(C featureConfiguration, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, ChunkCoordIntPair chunkPos, IWorldHeightAccess levelHeightAccessor, SeededRandom worldgenRandom, long l) {
            this.config = featureConfiguration;
            this.chunkGenerator = chunkGenerator;
            this.structureManager = structureManager;
            this.chunkPos = chunkPos;
            this.heightAccessor = levelHeightAccessor;
            this.random = worldgenRandom;
            this.seed = l;
        }

        public C config() {
            return this.config;
        }

        public ChunkGenerator chunkGenerator() {
            return this.chunkGenerator;
        }

        public DefinedStructureManager structureManager() {
            return this.structureManager;
        }

        public ChunkCoordIntPair chunkPos() {
            return this.chunkPos;
        }

        public IWorldHeightAccess heightAccessor() {
            return this.heightAccessor;
        }

        public SeededRandom random() {
            return this.random;
        }

        public long seed() {
            return this.seed;
        }
    }
}
