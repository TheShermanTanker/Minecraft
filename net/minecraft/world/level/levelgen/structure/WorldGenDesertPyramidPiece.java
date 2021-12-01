package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenDesertPyramidPiece extends WorldGenScatteredPiece {
    public static final int WIDTH = 21;
    public static final int DEPTH = 21;
    private final boolean[] hasPlacedChest = new boolean[4];

    public WorldGenDesertPyramidPiece(Random random, int x, int z) {
        super(WorldGenFeatureStructurePieceType.DESERT_PYRAMID_PIECE, x, 64, z, 21, 15, 21, getRandomHorizontalDirection(random));
    }

    public WorldGenDesertPyramidPiece(NBTTagCompound nbt) {
        super(WorldGenFeatureStructurePieceType.DESERT_PYRAMID_PIECE, nbt);
        this.hasPlacedChest[0] = nbt.getBoolean("hasPlacedChest0");
        this.hasPlacedChest[1] = nbt.getBoolean("hasPlacedChest1");
        this.hasPlacedChest[2] = nbt.getBoolean("hasPlacedChest2");
        this.hasPlacedChest[3] = nbt.getBoolean("hasPlacedChest3");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        super.addAdditionalSaveData(context, nbt);
        nbt.setBoolean("hasPlacedChest0", this.hasPlacedChest[0]);
        nbt.setBoolean("hasPlacedChest1", this.hasPlacedChest[1]);
        nbt.setBoolean("hasPlacedChest2", this.hasPlacedChest[2]);
        nbt.setBoolean("hasPlacedChest3", this.hasPlacedChest[3]);
    }

    @Override
    public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
        if (this.updateHeightPositionToLowestGroundHeight(world, -random.nextInt(3))) {
            this.generateBox(world, chunkBox, 0, -4, 0, this.width - 1, 0, this.depth - 1, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);

            for(int i = 1; i <= 9; ++i) {
                this.generateBox(world, chunkBox, i, i, i, this.width - 1 - i, i, this.depth - 1 - i, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
                this.generateBox(world, chunkBox, i + 1, i, i + 1, this.width - 2 - i, i, this.depth - 2 - i, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            }

            for(int j = 0; j < this.width; ++j) {
                for(int k = 0; k < this.depth; ++k) {
                    int l = -5;
                    this.fillColumnDown(world, Blocks.SANDSTONE.getBlockData(), j, -5, k, chunkBox);
                }
            }

            IBlockData blockState = Blocks.SANDSTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.NORTH);
            IBlockData blockState2 = Blocks.SANDSTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.SOUTH);
            IBlockData blockState3 = Blocks.SANDSTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.EAST);
            IBlockData blockState4 = Blocks.SANDSTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.WEST);
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 9, 4, Blocks.SANDSTONE.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 1, 10, 1, 3, 10, 3, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.placeBlock(world, blockState, 2, 10, 0, chunkBox);
            this.placeBlock(world, blockState2, 2, 10, 4, chunkBox);
            this.placeBlock(world, blockState3, 0, 10, 2, chunkBox);
            this.placeBlock(world, blockState4, 4, 10, 2, chunkBox);
            this.generateBox(world, chunkBox, this.width - 5, 0, 0, this.width - 1, 9, 4, Blocks.SANDSTONE.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 4, 10, 1, this.width - 2, 10, 3, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.placeBlock(world, blockState, this.width - 3, 10, 0, chunkBox);
            this.placeBlock(world, blockState2, this.width - 3, 10, 4, chunkBox);
            this.placeBlock(world, blockState3, this.width - 5, 10, 2, chunkBox);
            this.placeBlock(world, blockState4, this.width - 1, 10, 2, chunkBox);
            this.generateBox(world, chunkBox, 8, 0, 0, 12, 4, 4, Blocks.SANDSTONE.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 9, 1, 0, 11, 3, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 9, 1, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 9, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 9, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 10, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 11, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 11, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 11, 1, 1, chunkBox);
            this.generateBox(world, chunkBox, 4, 1, 1, 8, 3, 3, Blocks.SANDSTONE.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 4, 1, 2, 8, 2, 2, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 12, 1, 1, 16, 3, 3, Blocks.SANDSTONE.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 12, 1, 2, 16, 2, 2, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 5, 4, 5, this.width - 6, 4, this.depth - 6, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 9, 4, 9, 11, 4, 11, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, 8, 1, 8, 8, 3, 8, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 12, 1, 8, 12, 3, 8, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 8, 1, 12, 8, 3, 12, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 12, 1, 12, 12, 3, 12, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 1, 1, 5, 4, 4, 11, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 5, 1, 5, this.width - 2, 4, 11, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 6, 7, 9, 6, 7, 11, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 7, 7, 9, this.width - 7, 7, 11, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 5, 5, 9, 5, 7, 11, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 6, 5, 9, this.width - 6, 7, 11, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 5, 5, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 5, 6, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 6, 6, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), this.width - 6, 5, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), this.width - 6, 6, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), this.width - 7, 6, 10, chunkBox);
            this.generateBox(world, chunkBox, 2, 4, 4, 2, 6, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 3, 4, 4, this.width - 3, 6, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.placeBlock(world, blockState, 2, 4, 5, chunkBox);
            this.placeBlock(world, blockState, 2, 3, 4, chunkBox);
            this.placeBlock(world, blockState, this.width - 3, 4, 5, chunkBox);
            this.placeBlock(world, blockState, this.width - 3, 3, 4, chunkBox);
            this.generateBox(world, chunkBox, 1, 1, 3, 2, 2, 3, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 3, 1, 3, this.width - 2, 2, 3, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.placeBlock(world, Blocks.SANDSTONE.getBlockData(), 1, 1, 2, chunkBox);
            this.placeBlock(world, Blocks.SANDSTONE.getBlockData(), this.width - 2, 1, 2, chunkBox);
            this.placeBlock(world, Blocks.SANDSTONE_SLAB.getBlockData(), 1, 2, 2, chunkBox);
            this.placeBlock(world, Blocks.SANDSTONE_SLAB.getBlockData(), this.width - 2, 2, 2, chunkBox);
            this.placeBlock(world, blockState4, 2, 1, 2, chunkBox);
            this.placeBlock(world, blockState3, this.width - 3, 1, 2, chunkBox);
            this.generateBox(world, chunkBox, 4, 3, 5, 4, 3, 17, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 5, 3, 5, this.width - 5, 3, 17, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 3, 1, 5, 4, 2, 16, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, chunkBox, this.width - 6, 1, 5, this.width - 5, 2, 16, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);

            for(int m = 5; m <= 17; m += 2) {
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 4, 1, m, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), 4, 2, m, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), this.width - 5, 1, m, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), this.width - 5, 2, m, chunkBox);
            }

            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 10, 0, 7, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 10, 0, 8, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 9, 0, 9, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 11, 0, 9, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 8, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 12, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 7, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 13, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 9, 0, 11, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 11, 0, 11, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 10, 0, 12, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 10, 0, 13, chunkBox);
            this.placeBlock(world, Blocks.BLUE_TERRACOTTA.getBlockData(), 10, 0, 10, chunkBox);

            for(int n = 0; n <= this.width - 1; n += this.width - 1) {
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 2, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 2, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 2, 3, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 3, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 3, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 3, 3, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 4, 1, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), n, 4, 2, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 4, 3, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 5, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 5, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 5, 3, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 6, 1, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), n, 6, 2, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 6, 3, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 7, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 7, 2, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), n, 7, 3, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 8, 1, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 8, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), n, 8, 3, chunkBox);
            }

            for(int o = 2; o <= this.width - 3; o += this.width - 3 - 2) {
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o - 1, 2, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o, 2, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o + 1, 2, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o - 1, 3, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o, 3, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o + 1, 3, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o - 1, 4, 0, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), o, 4, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o + 1, 4, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o - 1, 5, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o, 5, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o + 1, 5, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o - 1, 6, 0, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), o, 6, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o + 1, 6, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o - 1, 7, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o, 7, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), o + 1, 7, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o - 1, 8, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o, 8, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), o + 1, 8, 0, chunkBox);
            }

            this.generateBox(world, chunkBox, 8, 4, 0, 12, 6, 0, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 8, 6, 0, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 12, 6, 0, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 9, 5, 0, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), 10, 5, 0, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.getBlockData(), 11, 5, 0, chunkBox);
            this.generateBox(world, chunkBox, 8, -14, 8, 12, -11, 12, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 8, -10, 8, 12, -10, 12, Blocks.CHISELED_SANDSTONE.getBlockData(), Blocks.CHISELED_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 8, -9, 8, 12, -9, 12, Blocks.CUT_SANDSTONE.getBlockData(), Blocks.CUT_SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 8, -8, 8, 12, -1, 12, Blocks.SANDSTONE.getBlockData(), Blocks.SANDSTONE.getBlockData(), false);
            this.generateBox(world, chunkBox, 9, -11, 9, 11, -1, 11, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.placeBlock(world, Blocks.STONE_PRESSURE_PLATE.getBlockData(), 10, -11, 10, chunkBox);
            this.generateBox(world, chunkBox, 9, -13, 9, 11, -13, 11, Blocks.TNT.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 8, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 8, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), 7, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 7, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 12, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 12, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), 13, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 13, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 10, -11, 8, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 10, -10, 8, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), 10, -10, 7, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 10, -11, 7, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 10, -11, 12, chunkBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 10, -10, 12, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.getBlockData(), 10, -10, 13, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.getBlockData(), 10, -11, 13, chunkBox);

            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                if (!this.hasPlacedChest[direction.get2DRotationValue()]) {
                    int p = direction.getAdjacentX() * 2;
                    int q = direction.getAdjacentZ() * 2;
                    this.hasPlacedChest[direction.get2DRotationValue()] = this.createChest(world, chunkBox, random, 10 + p, -11, 10 + q, LootTables.DESERT_PYRAMID);
                }
            }

        }
    }
}
