package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenJunglePyramidPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureJunglePyramid extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureJunglePyramid(Codec<WorldGenFeatureEmptyConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureEmptyConfiguration> getStartFactory() {
        return WorldGenFeatureJunglePyramid.FeatureStart::new;
    }

    public static class FeatureStart extends StructureStart<WorldGenFeatureEmptyConfiguration> {
        public FeatureStart(StructureGenerator<WorldGenFeatureEmptyConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureEmptyConfiguration config, IWorldHeightAccess world) {
            WorldGenJunglePyramidPiece junglePyramidPiece = new WorldGenJunglePyramidPiece(this.random, pos.getMinBlockX(), pos.getMinBlockZ());
            this.addPiece(junglePyramidPiece);
        }
    }
}
