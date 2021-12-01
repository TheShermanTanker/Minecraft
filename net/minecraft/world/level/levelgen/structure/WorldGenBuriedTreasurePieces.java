package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenBuriedTreasurePieces {
    public static class BuriedTreasurePiece extends StructurePiece {
        public BuriedTreasurePiece(BlockPosition pos) {
            super(WorldGenFeatureStructurePieceType.BURIED_TREASURE_PIECE, 0, new StructureBoundingBox(pos));
        }

        public BuriedTreasurePiece(NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.BURIED_TREASURE_PIECE, nbt);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            int i = world.getHeight(HeightMap.Type.OCEAN_FLOOR_WG, this.boundingBox.minX(), this.boundingBox.minZ());
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(this.boundingBox.minX(), i, this.boundingBox.minZ());

            while(mutableBlockPos.getY() > world.getMinBuildHeight()) {
                IBlockData blockState = world.getType(mutableBlockPos);
                IBlockData blockState2 = world.getType(mutableBlockPos.below());
                if (blockState2 == Blocks.SANDSTONE.getBlockData() || blockState2 == Blocks.STONE.getBlockData() || blockState2 == Blocks.ANDESITE.getBlockData() || blockState2 == Blocks.GRANITE.getBlockData() || blockState2 == Blocks.DIORITE.getBlockData()) {
                    IBlockData blockState3 = !blockState.isAir() && !this.isLiquid(blockState) ? blockState : Blocks.SAND.getBlockData();

                    for(EnumDirection direction : EnumDirection.values()) {
                        BlockPosition blockPos = mutableBlockPos.relative(direction);
                        IBlockData blockState4 = world.getType(blockPos);
                        if (blockState4.isAir() || this.isLiquid(blockState4)) {
                            BlockPosition blockPos2 = blockPos.below();
                            IBlockData blockState5 = world.getType(blockPos2);
                            if ((blockState5.isAir() || this.isLiquid(blockState5)) && direction != EnumDirection.UP) {
                                world.setTypeAndData(blockPos, blockState2, 3);
                            } else {
                                world.setTypeAndData(blockPos, blockState3, 3);
                            }
                        }
                    }

                    this.boundingBox = new StructureBoundingBox(mutableBlockPos);
                    this.createChest(world, chunkBox, random, mutableBlockPos, LootTables.BURIED_TREASURE, (IBlockData)null);
                    return;
                }

                mutableBlockPos.move(0, -1, 0);
            }

        }

        private boolean isLiquid(IBlockData state) {
            return state == Blocks.WATER.getBlockData() || state == Blocks.LAVA.getBlockData();
        }
    }
}
