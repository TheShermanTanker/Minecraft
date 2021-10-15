package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenMonumentPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenMonument extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> MONUMENT_ENEMIES = WeightedRandomList.create(new BiomeSettingsMobs.SpawnerData(EntityTypes.GUARDIAN, 1, 2, 4));

    public WorldGenMonument(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean linearSeparation() {
        return false;
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
        int i = pos.getBlockX(9);
        int j = pos.getBlockZ(9);

        for(BiomeBase biome2 : biomeSource.getBiomesWithin(i, chunkGenerator.getSeaLevel(), j, 16)) {
            if (!biome2.getGenerationSettings().isValidStart(this)) {
                return false;
            }
        }

        for(BiomeBase biome3 : biomeSource.getBiomesWithin(i, chunkGenerator.getSeaLevel(), j, 29)) {
            if (biome3.getBiomeCategory() != BiomeBase.Geography.OCEAN && biome3.getBiomeCategory() != BiomeBase.Geography.RIVER) {
                return false;
            }
        }

        return true;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenMonument.OceanMonumentStart::new;
    }

    @Override
    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialEnemies() {
        return MONUMENT_ENEMIES;
    }

    public static class OceanMonumentStart extends StructureStart<WorldGenFeatureEmptyConfiguration> {
        private boolean isCreated;

        public OceanMonumentStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            this.generatePieces(pos);
        }

        private void generatePieces(ChunkCoordIntPair chunkPos) {
            int i = chunkPos.getMinBlockX() - 29;
            int j = chunkPos.getMinBlockZ() - 29;
            EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(this.random);
            this.addPiece(new WorldGenMonumentPieces.WorldGenMonumentPiece1(this.random, i, j, direction));
            this.isCreated = true;
        }

        @Override
        public void placeInChunk(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox box, ChunkCoordIntPair chunkPos) {
            if (!this.isCreated) {
                this.pieces.clear();
                this.generatePieces(this.getChunkPos());
            }

            super.placeInChunk(world, structureAccessor, chunkGenerator, random, box, chunkPos);
        }
    }
}
