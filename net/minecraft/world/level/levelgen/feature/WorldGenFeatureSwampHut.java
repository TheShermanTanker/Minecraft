package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenWitchHut;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureSwampHut extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> SWAMPHUT_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.WITCH, 1, 1, 1));
    private static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> SWAMPHUT_ANIMALS = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.CAT, 1, 1, 1));

    public WorldGenFeatureSwampHut(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenFeatureSwampHut.FeatureStart::new;
    }

    @Override
    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialEnemies() {
        return SWAMPHUT_ENEMIES;
    }

    @Override
    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialAnimals() {
        return SWAMPHUT_ANIMALS;
    }

    public static class FeatureStart extends StructureStart<WorldGenFeatureEmptyConfiguration> {
        public FeatureStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            WorldGenWitchHut swamplandHutPiece = new WorldGenWitchHut(this.random, pos.getMinBlockX(), pos.getMinBlockZ());
            this.addPiece(swamplandHutPiece);
        }
    }
}
