package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.WorldGenStrongholdPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenStronghold extends NoiseAffectingStructureFeature<WorldGenFeatureEmptyConfiguration> {
    public WorldGenStronghold(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenStronghold::checkLocation, WorldGenStronghold::generatePieces));
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureEmptyConfiguration> context) {
        return context.chunkGenerator().hasStronghold(context.chunkPos());
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureEmptyConfiguration> context) {
        int i = 0;

        WorldGenStrongholdPieces.WorldGenStrongholdStart startPiece;
        do {
            collector.clear();
            context.random().setLargeFeatureSeed(context.seed() + (long)(i++), context.chunkPos().x, context.chunkPos().z);
            WorldGenStrongholdPieces.resetPieces();
            startPiece = new WorldGenStrongholdPieces.WorldGenStrongholdStart(context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2));
            collector.addPiece(startPiece);
            startPiece.addChildren(startPiece, collector, context.random());
            List<StructurePiece> list = startPiece.pendingChildren;

            while(!list.isEmpty()) {
                int j = context.random().nextInt(list.size());
                StructurePiece structurePiece = list.remove(j);
                structurePiece.addChildren(startPiece, collector, context.random());
            }

            collector.moveBelowSeaLevel(context.chunkGenerator().getSeaLevel(), context.chunkGenerator().getMinY(), context.random(), 10);
        } while(collector.isEmpty() || startPiece.portalRoomPiece == null);

    }
}
