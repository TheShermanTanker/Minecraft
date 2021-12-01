package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.WorldGenShipwreck;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WorldGenFeatureShipwreck extends StructureGenerator<WorldGenFeatureShipwreckConfiguration> {
    public WorldGenFeatureShipwreck(Codec<WorldGenFeatureShipwreckConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(WorldGenFeatureShipwreck::checkLocation, WorldGenFeatureShipwreck::generatePieces));
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<WorldGenFeatureShipwreckConfiguration> context) {
        HeightMap.Type types = (context.config()).isBeached ? HeightMap.Type.WORLD_SURFACE_WG : HeightMap.Type.OCEAN_FLOOR_WG;
        return context.validBiomeOnTop(types);
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<WorldGenFeatureShipwreckConfiguration> context) {
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(context.random());
        BlockPosition blockPos = new BlockPosition(context.chunkPos().getMinBlockX(), 90, context.chunkPos().getMinBlockZ());
        WorldGenShipwreck.addPieces(context.structureManager(), blockPos, rotation, collector, context.random(), context.config());
    }
}
