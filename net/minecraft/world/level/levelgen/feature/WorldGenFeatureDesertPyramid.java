package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.WorldGenDesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenFeatureDesertPyramid extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureDesertPyramid(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenFeatureDesertPyramid::checkLocation, WorldGenFeatureDesertPyramid::generatePieces));
    }

    private static <C extends WorldGenFeatureConfiguration> boolean checkLocation(PieceGeneratorSupplier.Context<C> context) {
        if (!context.validBiomeOnTop(HeightMap.Type.WORLD_SURFACE_WG)) {
            return false;
        } else {
            return context.getLowestY(21, 21) >= context.chunkGenerator().getSeaLevel();
        }
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        collector.addPiece(new WorldGenDesertPyramidPiece(context.random(), context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ()));
    }
}
