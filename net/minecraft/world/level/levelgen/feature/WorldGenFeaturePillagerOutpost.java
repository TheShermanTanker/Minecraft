package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;

public class WorldGenFeaturePillagerOutpost extends WorldGenFeatureJigsaw {
    private static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> OUTPOST_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.PILLAGER, 1, 1, 1));

    public WorldGenFeaturePillagerOutpost(Codec<WorldGenFeatureVillageConfiguration> codec) {
        super(codec, 0, true, true);
    }

    @Override
    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialEnemies() {
        return OUTPOST_ENEMIES;
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureVillageConfiguration config, IWorldHeightAccess world) {
        int i = pos.x >> 4;
        int j = pos.z >> 4;
        random.setSeed((long)(i ^ j << 4) ^ worldSeed);
        random.nextInt();
        if (random.nextInt(5) != 0) {
            return false;
        } else {
            return !this.isNearVillage(chunkGenerator, worldSeed, random, pos);
        }
    }

    private boolean isNearVillage(ChunkGenerator generator, long worldSeed, SeededRandom random, ChunkCoordIntPair pos) {
        StructureSettingsFeature structureFeatureConfiguration = generator.getSettings().getConfig(StructureGenerator.VILLAGE);
        if (structureFeatureConfiguration == null) {
            return false;
        } else {
            int i = pos.x;
            int j = pos.z;

            for(int k = i - 10; k <= i + 10; ++k) {
                for(int l = j - 10; l <= j + 10; ++l) {
                    ChunkCoordIntPair chunkPos = StructureGenerator.VILLAGE.getPotentialFeatureChunk(structureFeatureConfiguration, worldSeed, random, k, l);
                    if (k == chunkPos.x && l == chunkPos.z) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
