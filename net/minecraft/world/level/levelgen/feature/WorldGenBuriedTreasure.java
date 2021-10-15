package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenBuriedTreasurePieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenBuriedTreasure extends StructureGenerator<WorldGenFeatureConfigurationChance> {
    private static final int RANDOM_SALT = 10387320;

    public WorldGenBuriedTreasure(Codec<WorldGenFeatureConfigurationChance> codec) {
        super(codec);
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureConfigurationChance config, IWorldHeightAccess world) {
        random.setLargeFeatureWithSalt(worldSeed, pos.x, pos.z, 10387320);
        return random.nextFloat() < config.probability;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureConfigurationChance> getStartFactory() {
        return WorldGenBuriedTreasure.BuriedTreasureStart::new;
    }

    public static class BuriedTreasureStart extends StructureStart<WorldGenFeatureConfigurationChance> {
        public BuriedTreasureStart(StructureGenerator<WorldGenFeatureConfigurationChance> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureConfigurationChance config, IWorldHeightAccess world) {
            BlockPosition blockPos = new BlockPosition(pos.getBlockX(9), 90, pos.getBlockZ(9));
            this.addPiece(new WorldGenBuriedTreasurePieces.BuriedTreasurePiece(blockPos));
        }

        @Override
        public BlockPosition getLocatePos() {
            ChunkCoordIntPair chunkPos = this.getChunkPos();
            return new BlockPosition(chunkPos.getBlockX(9), 0, chunkPos.getBlockZ(9));
        }
    }
}
