package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.WorldGenEndCityPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class WorldGenEndCity extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final int RANDOM_SALT = 10387313;

    public WorldGenEndCity(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, WorldGenEndCity::pieceGeneratorSupplier);
    }

    @Override
    protected boolean linearSeparation() {
        return false;
    }

    private static int getYPositionForFeature(ChunkCoordIntPair pos, ChunkGenerator chunkGenerator, IWorldHeightAccess world) {
        Random random = new Random((long)(pos.x + pos.z * 10387313));
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(random);
        int i = 5;
        int j = 5;
        if (rotation == EnumBlockRotation.CLOCKWISE_90) {
            i = -5;
        } else if (rotation == EnumBlockRotation.CLOCKWISE_180) {
            i = -5;
            j = -5;
        } else if (rotation == EnumBlockRotation.COUNTERCLOCKWISE_90) {
            j = -5;
        }

        int k = pos.getBlockX(7);
        int l = pos.getBlockZ(7);
        int m = chunkGenerator.getFirstOccupiedHeight(k, l, HeightMap.Type.WORLD_SURFACE_WG, world);
        int n = chunkGenerator.getFirstOccupiedHeight(k, l + j, HeightMap.Type.WORLD_SURFACE_WG, world);
        int o = chunkGenerator.getFirstOccupiedHeight(k + i, l, HeightMap.Type.WORLD_SURFACE_WG, world);
        int p = chunkGenerator.getFirstOccupiedHeight(k + i, l + j, HeightMap.Type.WORLD_SURFACE_WG, world);
        return Math.min(Math.min(m, n), Math.min(o, p));
    }

    private static Optional<PieceGenerator<WorldGenFeatureEmptyConfiguration>> pieceGeneratorSupplier(PieceGeneratorSupplier.Context<WorldGenFeatureEmptyConfiguration> context) {
        int i = getYPositionForFeature(context.chunkPos(), context.chunkGenerator(), context.heightAccessor());
        if (i < 60) {
            return Optional.empty();
        } else {
            BlockPosition blockPos = context.chunkPos().getMiddleBlockPosition(i);
            return !context.validBiome().test(context.chunkGenerator().getBiome(QuartPos.fromBlock(blockPos.getX()), QuartPos.fromBlock(blockPos.getY()), QuartPos.fromBlock(blockPos.getZ()))) ? Optional.empty() : Optional.of((structurePiecesBuilder, contextx) -> {
                EnumBlockRotation rotation = EnumBlockRotation.getRandom(contextx.random());
                List<StructurePiece> list = Lists.newArrayList();
                WorldGenEndCityPieces.startHouseTower(contextx.structureManager(), blockPos, rotation, list, contextx.random());
                list.forEach(structurePiecesBuilder::addPiece);
            });
        }
    }
}
