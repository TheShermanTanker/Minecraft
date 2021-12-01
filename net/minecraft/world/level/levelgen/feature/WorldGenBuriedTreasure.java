package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;
import net.minecraft.world.level.levelgen.structure.WorldGenBuriedTreasurePieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenBuriedTreasure extends StructureGenerator<WorldGenFeatureConfigurationChance> {
    private static final int RANDOM_SALT = 10387320;

    public WorldGenBuriedTreasure(Codec<WorldGenFeatureConfigurationChance> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenBuriedTreasure::checkLocation, WorldGenBuriedTreasure::generatePieces));
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureConfigurationChance> context) {
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureWithSalt(context.seed(), context.chunkPos().x, context.chunkPos().z, 10387320);
        return worldgenRandom.nextFloat() < (context.config()).probability && context.validBiomeOnTop(HeightMap.Type.OCEAN_FLOOR_WG);
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureConfigurationChance> context) {
        BlockPosition blockPos = new BlockPosition(context.chunkPos().getBlockX(9), 90, context.chunkPos().getBlockZ(9));
        collector.addPiece(new WorldGenBuriedTreasurePieces.BuriedTreasurePiece(blockPos));
    }

    @Override
    public BlockPosition getLocatePos(ChunkCoordIntPair chunkPos) {
        return new BlockPosition(chunkPos.getBlockX(9), 0, chunkPos.getBlockZ(9));
    }
}
