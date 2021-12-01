package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class WorldGenFeaturePillagerOutpost extends WorldGenFeatureJigsaw {
    public static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> OUTPOST_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.PILLAGER, 1, 1, 1));

    public WorldGenFeaturePillagerOutpost(Codec<WorldGenFeatureVillageConfiguration> configCodec) {
        super(configCodec, 0, true, true, WorldGenFeaturePillagerOutpost::checkLocation);
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureVillageConfiguration> context) {
        int i = context.chunkPos().x >> 4;
        int j = context.chunkPos().z >> 4;
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setSeed((long)(i ^ j << 4) ^ context.seed());
        worldgenRandom.nextInt();
        if (worldgenRandom.nextInt(5) != 0) {
            return false;
        } else {
            return !isNearVillage(context.chunkGenerator(), context.seed(), context.chunkPos());
        }
    }

    private static boolean isNearVillage(ChunkGenerator chunkGenerator, long seed, ChunkCoordIntPair chunkPos) {
        StructureSettingsFeature structureFeatureConfiguration = chunkGenerator.getSettings().getConfig(StructureGenerator.VILLAGE);
        if (structureFeatureConfiguration == null) {
            return false;
        } else {
            int i = chunkPos.x;
            int j = chunkPos.z;

            for(int k = i - 10; k <= i + 10; ++k) {
                for(int l = j - 10; l <= j + 10; ++l) {
                    ChunkCoordIntPair chunkPos2 = StructureGenerator.VILLAGE.getPotentialFeatureChunk(structureFeatureConfiguration, seed, k, l);
                    if (k == chunkPos2.x && l == chunkPos2.z) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
