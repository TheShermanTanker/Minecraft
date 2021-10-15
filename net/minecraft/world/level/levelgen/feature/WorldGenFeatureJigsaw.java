package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.data.worldgen.WorldGenFeaturePieces;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructureJigsawPlacement;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenFeaturePillagerOutpostPoolPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureJigsaw extends StructureGenerator<WorldGenFeatureVillageConfiguration> {
    final int startY;
    final boolean doExpansionHack;
    final boolean projectStartToHeightmap;

    public WorldGenFeatureJigsaw(Codec<WorldGenFeatureVillageConfiguration> codec, int structureStartY, boolean modifyBoundingBox, boolean surface) {
        super(codec);
        this.startY = structureStartY;
        this.doExpansionHack = modifyBoundingBox;
        this.projectStartToHeightmap = surface;
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureVillageConfiguration> getStartFactory() {
        return (feature, pos, references, seed) -> {
            return new WorldGenFeatureJigsaw.FeatureStart(this, pos, references, seed);
        };
    }

    public static class FeatureStart extends NoiseAffectingStructureStart<WorldGenFeatureVillageConfiguration> {
        private final WorldGenFeatureJigsaw feature;

        public FeatureStart(WorldGenFeatureJigsaw feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
            this.feature = feature;
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureVillageConfiguration config, IWorldHeightAccess world) {
            BlockPosition blockPos = new BlockPosition(pos.getMinBlockX(), this.feature.startY, pos.getMinBlockZ());
            WorldGenFeaturePieces.bootstrap();
            WorldGenFeatureDefinedStructureJigsawPlacement.addPieces(registryManager, config, WorldGenFeaturePillagerOutpostPoolPiece::new, chunkGenerator, manager, blockPos, this, this.random, this.feature.doExpansionHack, this.feature.projectStartToHeightmap, world);
        }
    }
}
