package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;

public class WorldGenFeatureBastionRemnant extends WorldGenFeatureJigsaw {
    private static final int BASTION_SPAWN_HEIGHT = 33;

    public WorldGenFeatureBastionRemnant(Codec<WorldGenFeatureVillageConfiguration> codec) {
        super(codec, 33, false, false);
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureVillageConfiguration config, IWorldHeightAccess world) {
        return random.nextInt(5) >= 2;
    }
}
