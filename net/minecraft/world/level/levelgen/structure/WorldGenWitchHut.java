package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.monster.EntityWitch;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyStairsShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;

public class WorldGenWitchHut extends WorldGenScatteredPiece {
    private boolean spawnedWitch;
    private boolean spawnedCat;

    public WorldGenWitchHut(Random random, int x, int z) {
        super(WorldGenFeatureStructurePieceType.SWAMPLAND_HUT, x, 64, z, 7, 7, 9, getRandomHorizontalDirection(random));
    }

    public WorldGenWitchHut(WorldServer world, NBTTagCompound nbt) {
        super(WorldGenFeatureStructurePieceType.SWAMPLAND_HUT, nbt);
        this.spawnedWitch = nbt.getBoolean("Witch");
        this.spawnedCat = nbt.getBoolean("Cat");
    }

    @Override
    protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
        super.addAdditionalSaveData(world, nbt);
        nbt.setBoolean("Witch", this.spawnedWitch);
        nbt.setBoolean("Cat", this.spawnedCat);
    }

    @Override
    public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
        if (!this.updateAverageGroundHeight(world, boundingBox, 0)) {
            return false;
        } else {
            this.generateBox(world, boundingBox, 1, 1, 1, 5, 1, 7, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 4, 2, 5, 4, 7, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 1, 0, 4, 1, 0, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 2, 2, 3, 3, 2, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 2, 3, 1, 3, 6, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 2, 3, 5, 3, 6, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 2, 7, 4, 3, 7, Blocks.SPRUCE_PLANKS.getBlockData(), Blocks.SPRUCE_PLANKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 0, 2, 1, 3, 2, Blocks.OAK_LOG.getBlockData(), Blocks.OAK_LOG.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 0, 2, 5, 3, 2, Blocks.OAK_LOG.getBlockData(), Blocks.OAK_LOG.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 0, 7, 1, 3, 7, Blocks.OAK_LOG.getBlockData(), Blocks.OAK_LOG.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 0, 7, 5, 3, 7, Blocks.OAK_LOG.getBlockData(), Blocks.OAK_LOG.getBlockData(), false);
            this.placeBlock(world, Blocks.OAK_FENCE.getBlockData(), 2, 3, 2, boundingBox);
            this.placeBlock(world, Blocks.OAK_FENCE.getBlockData(), 3, 3, 7, boundingBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 1, 3, 4, boundingBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 5, 3, 4, boundingBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 5, 3, 5, boundingBox);
            this.placeBlock(world, Blocks.POTTED_RED_MUSHROOM.getBlockData(), 1, 3, 5, boundingBox);
            this.placeBlock(world, Blocks.CRAFTING_TABLE.getBlockData(), 3, 2, 6, boundingBox);
            this.placeBlock(world, Blocks.CAULDRON.getBlockData(), 4, 2, 6, boundingBox);
            this.placeBlock(world, Blocks.OAK_FENCE.getBlockData(), 1, 2, 1, boundingBox);
            this.placeBlock(world, Blocks.OAK_FENCE.getBlockData(), 5, 2, 1, boundingBox);
            IBlockData blockState = Blocks.SPRUCE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.NORTH);
            IBlockData blockState2 = Blocks.SPRUCE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.EAST);
            IBlockData blockState3 = Blocks.SPRUCE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.WEST);
            IBlockData blockState4 = Blocks.SPRUCE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.SOUTH);
            this.generateBox(world, boundingBox, 0, 4, 1, 6, 4, 1, blockState, blockState, false);
            this.generateBox(world, boundingBox, 0, 4, 2, 0, 4, 7, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 6, 4, 2, 6, 4, 7, blockState3, blockState3, false);
            this.generateBox(world, boundingBox, 0, 4, 8, 6, 4, 8, blockState4, blockState4, false);
            this.placeBlock(world, blockState.set(BlockStairs.SHAPE, BlockPropertyStairsShape.OUTER_RIGHT), 0, 4, 1, boundingBox);
            this.placeBlock(world, blockState.set(BlockStairs.SHAPE, BlockPropertyStairsShape.OUTER_LEFT), 6, 4, 1, boundingBox);
            this.placeBlock(world, blockState4.set(BlockStairs.SHAPE, BlockPropertyStairsShape.OUTER_LEFT), 0, 4, 8, boundingBox);
            this.placeBlock(world, blockState4.set(BlockStairs.SHAPE, BlockPropertyStairsShape.OUTER_RIGHT), 6, 4, 8, boundingBox);

            for(int i = 2; i <= 7; i += 5) {
                for(int j = 1; j <= 5; j += 4) {
                    this.fillColumnDown(world, Blocks.OAK_LOG.getBlockData(), j, -1, i, boundingBox);
                }
            }

            if (!this.spawnedWitch) {
                BlockPosition blockPos = this.getWorldPos(2, 2, 5);
                if (boundingBox.isInside(blockPos)) {
                    this.spawnedWitch = true;
                    EntityWitch witch = EntityTypes.WITCH.create(world.getLevel());
                    witch.setPersistent();
                    witch.setPositionRotation((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                    witch.prepare(world, world.getDamageScaler(blockPos), EnumMobSpawn.STRUCTURE, (GroupDataEntity)null, (NBTTagCompound)null);
                    world.addAllEntities(witch);
                }
            }

            this.spawnCat(world, boundingBox);
            return true;
        }
    }

    private void spawnCat(WorldAccess world, StructureBoundingBox box) {
        if (!this.spawnedCat) {
            BlockPosition blockPos = this.getWorldPos(2, 2, 5);
            if (box.isInside(blockPos)) {
                this.spawnedCat = true;
                EntityCat cat = EntityTypes.CAT.create(world.getLevel());
                cat.setPersistent();
                cat.setPositionRotation((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                cat.prepare(world, world.getDamageScaler(blockPos), EnumMobSpawn.STRUCTURE, (GroupDataEntity)null, (NBTTagCompound)null);
                world.addAllEntities(cat);
            }
        }

    }
}
