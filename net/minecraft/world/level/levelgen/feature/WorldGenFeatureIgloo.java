package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.WorldGenIglooPiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenFeatureIgloo extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureIgloo(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(PieceGeneratorSupplier.checkForBiomeOnTop(HeightMap.Type.WORLD_SURFACE_WG), WorldGenFeatureIgloo::generatePieces));
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        BlockPosition blockPos = new BlockPosition(context.chunkPos().getMinBlockX(), 90, context.chunkPos().getMinBlockZ());
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(context.random());
        WorldGenIglooPiece.addPieces(context.structureManager(), blockPos, rotation, collector, context.random());
    }
}
