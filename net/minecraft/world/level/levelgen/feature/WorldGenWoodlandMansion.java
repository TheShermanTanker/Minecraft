package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.WorldGenWoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

public class WorldGenWoodlandMansion extends StructureGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenWoodlandMansion(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec, WorldGenWoodlandMansion::pieceGeneratorSupplier, WorldGenWoodlandMansion::afterPlace);
    }

    @Override
    protected boolean linearSeparation() {
        return false;
    }

    private static Optional<PieceGenerator<WorldGenFeatureEmptyConfiguration>> pieceGeneratorSupplier(PieceGeneratorSupplier.Context<WorldGenFeatureEmptyConfiguration> context) {
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(worldgenRandom);
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

        int k = context.chunkPos().getBlockX(7);
        int l = context.chunkPos().getBlockZ(7);
        int[] is = context.getCornerHeights(k, i, l, j);
        int m = Math.min(Math.min(is[0], is[1]), Math.min(is[2], is[3]));
        if (m < 60) {
            return Optional.empty();
        } else if (!context.validBiome().test(context.chunkGenerator().getBiome(QuartPos.fromBlock(k), QuartPos.fromBlock(is[0]), QuartPos.fromBlock(l)))) {
            return Optional.empty();
        } else {
            BlockPosition blockPos = new BlockPosition(context.chunkPos().getMiddleBlockX(), m + 1, context.chunkPos().getMiddleBlockZ());
            return Optional.of((structurePiecesBuilder, contextx) -> {
                List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list = Lists.newLinkedList();
                WorldGenWoodlandMansionPieces.generateMansion(contextx.structureManager(), blockPos, rotation, list, worldgenRandom);
                list.forEach(structurePiecesBuilder::addPiece);
            });
        }
    }

    private static void afterPlace(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, PiecesContainer children) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int i = world.getMinBuildHeight();
        StructureBoundingBox boundingBox = children.calculateBoundingBox();
        int j = boundingBox.minY();

        for(int k = chunkBox.minX(); k <= chunkBox.maxX(); ++k) {
            for(int l = chunkBox.minZ(); l <= chunkBox.maxZ(); ++l) {
                mutableBlockPos.set(k, j, l);
                if (!world.isEmpty(mutableBlockPos) && boundingBox.isInside(mutableBlockPos) && children.isInsidePiece(mutableBlockPos)) {
                    for(int m = j - 1; m > i; --m) {
                        mutableBlockPos.setY(m);
                        if (!world.isEmpty(mutableBlockPos) && !world.getType(mutableBlockPos).getMaterial().isLiquid()) {
                            break;
                        }

                        world.setTypeAndData(mutableBlockPos, Blocks.COBBLESTONE.getBlockData(), 2);
                    }
                }
            }
        }

    }
}
