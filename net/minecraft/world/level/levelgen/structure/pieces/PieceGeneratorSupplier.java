package net.minecraft.world.level.levelgen.structure.pieces;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

@FunctionalInterface
public interface PieceGeneratorSupplier<C extends WorldGenFeatureConfiguration> {
    Optional<PieceGenerator<C>> createGenerator(PieceGeneratorSupplier.Context<C> context);

    static <C extends WorldGenFeatureConfiguration> PieceGeneratorSupplier<C> simple(Predicate<PieceGeneratorSupplier.Context<C>> predicate, PieceGenerator<C> generator) {
        Optional<PieceGenerator<C>> optional = Optional.of(generator);
        return (context) -> {
            return predicate.test(context) ? optional : Optional.empty();
        };
    }

    static <C extends WorldGenFeatureConfiguration> Predicate<PieceGeneratorSupplier.Context<C>> checkForBiomeOnTop(HeightMap.Type heightmapType) {
        return (context) -> {
            return context.validBiomeOnTop(heightmapType);
        };
    }

    public static record Context<C extends WorldGenFeatureConfiguration>(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long seed, ChunkCoordIntPair chunkPos, C config, IWorldHeightAccess heightAccessor, Predicate<BiomeBase> validBiome, DefinedStructureManager structureManager, IRegistryCustom registryAccess) {
        public Context(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long l, ChunkCoordIntPair chunkPos, C featureConfiguration, IWorldHeightAccess levelHeightAccessor, Predicate<BiomeBase> predicate, DefinedStructureManager structureManager, IRegistryCustom registryAccess) {
            this.chunkGenerator = chunkGenerator;
            this.biomeSource = biomeSource;
            this.seed = l;
            this.chunkPos = chunkPos;
            this.config = featureConfiguration;
            this.heightAccessor = levelHeightAccessor;
            this.validBiome = predicate;
            this.structureManager = structureManager;
            this.registryAccess = registryAccess;
        }

        public boolean validBiomeOnTop(HeightMap.Type heightmapType) {
            int i = this.chunkPos.getMiddleBlockX();
            int j = this.chunkPos.getMiddleBlockZ();
            int k = this.chunkGenerator.getFirstOccupiedHeight(i, j, heightmapType, this.heightAccessor);
            BiomeBase biome = this.chunkGenerator.getBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(k), QuartPos.fromBlock(j));
            return this.validBiome.test(biome);
        }

        public int[] getCornerHeights(int x, int width, int z, int height) {
            return new int[]{this.chunkGenerator.getFirstOccupiedHeight(x, z, HeightMap.Type.WORLD_SURFACE_WG, this.heightAccessor), this.chunkGenerator.getFirstOccupiedHeight(x, z + height, HeightMap.Type.WORLD_SURFACE_WG, this.heightAccessor), this.chunkGenerator.getFirstOccupiedHeight(x + width, z, HeightMap.Type.WORLD_SURFACE_WG, this.heightAccessor), this.chunkGenerator.getFirstOccupiedHeight(x + width, z + height, HeightMap.Type.WORLD_SURFACE_WG, this.heightAccessor)};
        }

        public int getLowestY(int width, int height) {
            int i = this.chunkPos.getMinBlockX();
            int j = this.chunkPos.getMinBlockZ();
            int[] is = this.getCornerHeights(i, width, j, height);
            return Math.min(Math.min(is[0], is[1]), Math.min(is[2], is[3]));
        }

        public ChunkGenerator chunkGenerator() {
            return this.chunkGenerator;
        }

        public WorldChunkManager biomeSource() {
            return this.biomeSource;
        }

        public long seed() {
            return this.seed;
        }

        public ChunkCoordIntPair chunkPos() {
            return this.chunkPos;
        }

        public C config() {
            return this.config;
        }

        public IWorldHeightAccess heightAccessor() {
            return this.heightAccessor;
        }

        public Predicate<BiomeBase> validBiome() {
            return this.validBiome;
        }

        public DefinedStructureManager structureManager() {
            return this.structureManager;
        }

        public IRegistryCustom registryAccess() {
            return this.registryAccess;
        }
    }
}
