package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.WorldGenJunglePyramidPiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenFeatureJunglePyramid extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureJunglePyramid(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenFeatureJunglePyramid::checkLocation, WorldGenFeatureJunglePyramid::generatePieces));
    }

    private static <C extends WorldGenFeatureConfiguration> boolean checkLocation(PieceGeneratorSupplier.Context<C> context) {
        if (!context.validBiomeOnTop(HeightMap.Type.WORLD_SURFACE_WG)) {
            return false;
        } else {
            return context.getLowestY(12, 15) >= context.chunkGenerator().getSeaLevel();
        }
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        collector.addPiece(new WorldGenJunglePyramidPiece(context.random(), context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ()));
    }
}
