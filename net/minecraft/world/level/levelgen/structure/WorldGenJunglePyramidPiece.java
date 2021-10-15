package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.BlockLever;
import net.minecraft.world.level.block.BlockRedstoneWire;
import net.minecraft.world.level.block.BlockRepeater;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.BlockTripwire;
import net.minecraft.world.level.block.BlockTripwireHook;
import net.minecraft.world.level.block.BlockVine;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.BlockPiston;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyAttachPosition;
import net.minecraft.world.level.block.state.properties.BlockPropertyRedstoneSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenJunglePyramidPiece extends WorldGenScatteredPiece {
    private boolean placedMainChest;
    private boolean placedHiddenChest;
    private boolean placedTrap1;
    private boolean placedTrap2;
    private static final WorldGenJunglePyramidPiece.MossStoneSelector STONE_SELECTOR = new WorldGenJunglePyramidPiece.MossStoneSelector();

    public WorldGenJunglePyramidPiece(Random random, int x, int z) {
        super(WorldGenFeatureStructurePieceType.JUNGLE_PYRAMID_PIECE, x, 64, z, 12, 10, 15, getRandomHorizontalDirection(random));
    }

    public WorldGenJunglePyramidPiece(WorldServer world, NBTTagCompound nbt) {
        super(WorldGenFeatureStructurePieceType.JUNGLE_PYRAMID_PIECE, nbt);
        this.placedMainChest = nbt.getBoolean("placedMainChest");
        this.placedHiddenChest = nbt.getBoolean("placedHiddenChest");
        this.placedTrap1 = nbt.getBoolean("placedTrap1");
        this.placedTrap2 = nbt.getBoolean("placedTrap2");
    }

    @Override
    protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
        super.addAdditionalSaveData(world, nbt);
        nbt.setBoolean("placedMainChest", this.placedMainChest);
        nbt.setBoolean("placedHiddenChest", this.placedHiddenChest);
        nbt.setBoolean("placedTrap1", this.placedTrap1);
        nbt.setBoolean("placedTrap2", this.placedTrap2);
    }

    @Override
    public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
        if (!this.updateAverageGroundHeight(world, boundingBox, 0)) {
            return false;
        } else {
            this.generateBox(world, boundingBox, 0, -4, 0, this.width - 1, 0, this.depth - 1, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, 1, 2, 9, 2, 2, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, 1, 12, 9, 2, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, 1, 3, 2, 2, 11, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 9, 1, 3, 9, 2, 11, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 3, 1, 10, 6, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 3, 13, 10, 6, 13, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 3, 2, 1, 6, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 10, 3, 2, 10, 6, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, 3, 2, 9, 3, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, 6, 2, 9, 6, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 3, 7, 3, 8, 7, 11, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 8, 4, 7, 8, 10, false, random, STONE_SELECTOR);
            this.generateAirBox(world, boundingBox, 3, 1, 3, 8, 2, 11);
            this.generateAirBox(world, boundingBox, 4, 3, 6, 7, 3, 9);
            this.generateAirBox(world, boundingBox, 2, 4, 2, 9, 5, 12);
            this.generateAirBox(world, boundingBox, 4, 6, 5, 7, 6, 9);
            this.generateAirBox(world, boundingBox, 5, 7, 6, 6, 7, 8);
            this.generateAirBox(world, boundingBox, 5, 1, 2, 6, 2, 2);
            this.generateAirBox(world, boundingBox, 5, 2, 12, 6, 2, 12);
            this.generateAirBox(world, boundingBox, 5, 5, 1, 6, 5, 1);
            this.generateAirBox(world, boundingBox, 5, 5, 13, 6, 5, 13);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 1, 5, 5, boundingBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 10, 5, 5, boundingBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 1, 5, 9, boundingBox);
            this.placeBlock(world, Blocks.AIR.getBlockData(), 10, 5, 9, boundingBox);

            for(int i = 0; i <= 14; i += 14) {
                this.generateBox(world, boundingBox, 2, 4, i, 2, 5, i, false, random, STONE_SELECTOR);
                this.generateBox(world, boundingBox, 4, 4, i, 4, 5, i, false, random, STONE_SELECTOR);
                this.generateBox(world, boundingBox, 7, 4, i, 7, 5, i, false, random, STONE_SELECTOR);
                this.generateBox(world, boundingBox, 9, 4, i, 9, 5, i, false, random, STONE_SELECTOR);
            }

            this.generateBox(world, boundingBox, 5, 6, 0, 6, 6, 0, false, random, STONE_SELECTOR);

            for(int j = 0; j <= 11; j += 11) {
                for(int k = 2; k <= 12; k += 2) {
                    this.generateBox(world, boundingBox, j, 4, k, j, 5, k, false, random, STONE_SELECTOR);
                }

                this.generateBox(world, boundingBox, j, 6, 5, j, 6, 5, false, random, STONE_SELECTOR);
                this.generateBox(world, boundingBox, j, 6, 9, j, 6, 9, false, random, STONE_SELECTOR);
            }

            this.generateBox(world, boundingBox, 2, 7, 2, 2, 9, 2, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 9, 7, 2, 9, 9, 2, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, 7, 12, 2, 9, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 9, 7, 12, 9, 9, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 9, 4, 4, 9, 4, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 7, 9, 4, 7, 9, 4, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 9, 10, 4, 9, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 7, 9, 10, 7, 9, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 5, 9, 7, 6, 9, 7, false, random, STONE_SELECTOR);
            IBlockData blockState = Blocks.COBBLESTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.EAST);
            IBlockData blockState2 = Blocks.COBBLESTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.WEST);
            IBlockData blockState3 = Blocks.COBBLESTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.SOUTH);
            IBlockData blockState4 = Blocks.COBBLESTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.NORTH);
            this.placeBlock(world, blockState4, 5, 9, 6, boundingBox);
            this.placeBlock(world, blockState4, 6, 9, 6, boundingBox);
            this.placeBlock(world, blockState3, 5, 9, 8, boundingBox);
            this.placeBlock(world, blockState3, 6, 9, 8, boundingBox);
            this.placeBlock(world, blockState4, 4, 0, 0, boundingBox);
            this.placeBlock(world, blockState4, 5, 0, 0, boundingBox);
            this.placeBlock(world, blockState4, 6, 0, 0, boundingBox);
            this.placeBlock(world, blockState4, 7, 0, 0, boundingBox);
            this.placeBlock(world, blockState4, 4, 1, 8, boundingBox);
            this.placeBlock(world, blockState4, 4, 2, 9, boundingBox);
            this.placeBlock(world, blockState4, 4, 3, 10, boundingBox);
            this.placeBlock(world, blockState4, 7, 1, 8, boundingBox);
            this.placeBlock(world, blockState4, 7, 2, 9, boundingBox);
            this.placeBlock(world, blockState4, 7, 3, 10, boundingBox);
            this.generateBox(world, boundingBox, 4, 1, 9, 4, 1, 9, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 7, 1, 9, 7, 1, 9, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 1, 10, 7, 2, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 5, 4, 5, 6, 4, 5, false, random, STONE_SELECTOR);
            this.placeBlock(world, blockState, 4, 4, 5, boundingBox);
            this.placeBlock(world, blockState2, 7, 4, 5, boundingBox);

            for(int l = 0; l < 4; ++l) {
                this.placeBlock(world, blockState3, 5, 0 - l, 6 + l, boundingBox);
                this.placeBlock(world, blockState3, 6, 0 - l, 6 + l, boundingBox);
                this.generateAirBox(world, boundingBox, 5, 0 - l, 7 + l, 6, 0 - l, 9 + l);
            }

            this.generateAirBox(world, boundingBox, 1, -3, 12, 10, -1, 13);
            this.generateAirBox(world, boundingBox, 1, -3, 1, 3, -1, 13);
            this.generateAirBox(world, boundingBox, 1, -3, 1, 9, -1, 5);

            for(int m = 1; m <= 13; m += 2) {
                this.generateBox(world, boundingBox, 1, -3, m, 1, -2, m, false, random, STONE_SELECTOR);
            }

            for(int n = 2; n <= 12; n += 2) {
                this.generateBox(world, boundingBox, 1, -1, n, 3, -1, n, false, random, STONE_SELECTOR);
            }

            this.generateBox(world, boundingBox, 2, -2, 1, 5, -2, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 7, -2, 1, 9, -2, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 6, -3, 1, 6, -3, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 6, -1, 1, 6, -1, 1, false, random, STONE_SELECTOR);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.getBlockData().set(BlockTripwireHook.FACING, EnumDirection.EAST).set(BlockTripwireHook.ATTACHED, Boolean.valueOf(true)), 1, -3, 8, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.getBlockData().set(BlockTripwireHook.FACING, EnumDirection.WEST).set(BlockTripwireHook.ATTACHED, Boolean.valueOf(true)), 4, -3, 8, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE.getBlockData().set(BlockTripwire.EAST, Boolean.valueOf(true)).set(BlockTripwire.WEST, Boolean.valueOf(true)).set(BlockTripwire.ATTACHED, Boolean.valueOf(true)), 2, -3, 8, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE.getBlockData().set(BlockTripwire.EAST, Boolean.valueOf(true)).set(BlockTripwire.WEST, Boolean.valueOf(true)).set(BlockTripwire.ATTACHED, Boolean.valueOf(true)), 3, -3, 8, boundingBox);
            IBlockData blockState5 = Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.NORTH, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.SOUTH, BlockPropertyRedstoneSide.SIDE);
            this.placeBlock(world, blockState5, 5, -3, 7, boundingBox);
            this.placeBlock(world, blockState5, 5, -3, 6, boundingBox);
            this.placeBlock(world, blockState5, 5, -3, 5, boundingBox);
            this.placeBlock(world, blockState5, 5, -3, 4, boundingBox);
            this.placeBlock(world, blockState5, 5, -3, 3, boundingBox);
            this.placeBlock(world, blockState5, 5, -3, 2, boundingBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.NORTH, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.WEST, BlockPropertyRedstoneSide.SIDE), 5, -3, 1, boundingBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.EAST, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.WEST, BlockPropertyRedstoneSide.SIDE), 4, -3, 1, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 3, -3, 1, boundingBox);
            if (!this.placedTrap1) {
                this.placedTrap1 = this.createDispenser(world, boundingBox, random, 3, -2, 1, EnumDirection.NORTH, LootTables.JUNGLE_TEMPLE_DISPENSER);
            }

            this.placeBlock(world, Blocks.VINE.getBlockData().set(BlockVine.SOUTH, Boolean.valueOf(true)), 3, -2, 2, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.getBlockData().set(BlockTripwireHook.FACING, EnumDirection.NORTH).set(BlockTripwireHook.ATTACHED, Boolean.valueOf(true)), 7, -3, 1, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.getBlockData().set(BlockTripwireHook.FACING, EnumDirection.SOUTH).set(BlockTripwireHook.ATTACHED, Boolean.valueOf(true)), 7, -3, 5, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE.getBlockData().set(BlockTripwire.NORTH, Boolean.valueOf(true)).set(BlockTripwire.SOUTH, Boolean.valueOf(true)).set(BlockTripwire.ATTACHED, Boolean.valueOf(true)), 7, -3, 2, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE.getBlockData().set(BlockTripwire.NORTH, Boolean.valueOf(true)).set(BlockTripwire.SOUTH, Boolean.valueOf(true)).set(BlockTripwire.ATTACHED, Boolean.valueOf(true)), 7, -3, 3, boundingBox);
            this.placeBlock(world, Blocks.TRIPWIRE.getBlockData().set(BlockTripwire.NORTH, Boolean.valueOf(true)).set(BlockTripwire.SOUTH, Boolean.valueOf(true)).set(BlockTripwire.ATTACHED, Boolean.valueOf(true)), 7, -3, 4, boundingBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.EAST, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.WEST, BlockPropertyRedstoneSide.SIDE), 8, -3, 6, boundingBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.WEST, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.SOUTH, BlockPropertyRedstoneSide.SIDE), 9, -3, 6, boundingBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.NORTH, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.SOUTH, BlockPropertyRedstoneSide.UP), 9, -3, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 9, -3, 4, boundingBox);
            this.placeBlock(world, blockState5, 9, -2, 4, boundingBox);
            if (!this.placedTrap2) {
                this.placedTrap2 = this.createDispenser(world, boundingBox, random, 9, -2, 3, EnumDirection.WEST, LootTables.JUNGLE_TEMPLE_DISPENSER);
            }

            this.placeBlock(world, Blocks.VINE.getBlockData().set(BlockVine.EAST, Boolean.valueOf(true)), 8, -1, 3, boundingBox);
            this.placeBlock(world, Blocks.VINE.getBlockData().set(BlockVine.EAST, Boolean.valueOf(true)), 8, -2, 3, boundingBox);
            if (!this.placedMainChest) {
                this.placedMainChest = this.createChest(world, boundingBox, random, 8, -3, 3, LootTables.JUNGLE_TEMPLE);
            }

            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 9, -3, 2, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 8, -3, 1, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 4, -3, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 5, -2, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 5, -1, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 6, -3, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 7, -2, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 7, -1, 5, boundingBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 8, -3, 5, boundingBox);
            this.generateBox(world, boundingBox, 9, -1, 1, 9, -1, 5, false, random, STONE_SELECTOR);
            this.generateAirBox(world, boundingBox, 8, -3, 8, 10, -1, 10);
            this.placeBlock(world, Blocks.CHISELED_STONE_BRICKS.getBlockData(), 8, -2, 11, boundingBox);
            this.placeBlock(world, Blocks.CHISELED_STONE_BRICKS.getBlockData(), 9, -2, 11, boundingBox);
            this.placeBlock(world, Blocks.CHISELED_STONE_BRICKS.getBlockData(), 10, -2, 11, boundingBox);
            IBlockData blockState6 = Blocks.LEVER.getBlockData().set(BlockLever.FACING, EnumDirection.NORTH).set(BlockLever.FACE, BlockPropertyAttachPosition.WALL);
            this.placeBlock(world, blockState6, 8, -2, 12, boundingBox);
            this.placeBlock(world, blockState6, 9, -2, 12, boundingBox);
            this.placeBlock(world, blockState6, 10, -2, 12, boundingBox);
            this.generateBox(world, boundingBox, 8, -3, 8, 8, -3, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, boundingBox, 10, -3, 8, 10, -3, 10, false, random, STONE_SELECTOR);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.getBlockData(), 10, -2, 9, boundingBox);
            this.placeBlock(world, blockState5, 8, -2, 9, boundingBox);
            this.placeBlock(world, blockState5, 8, -2, 10, boundingBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.getBlockData().set(BlockRedstoneWire.NORTH, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.SOUTH, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.EAST, BlockPropertyRedstoneSide.SIDE).set(BlockRedstoneWire.WEST, BlockPropertyRedstoneSide.SIDE), 10, -1, 9, boundingBox);
            this.placeBlock(world, Blocks.STICKY_PISTON.getBlockData().set(BlockPiston.FACING, EnumDirection.UP), 9, -2, 8, boundingBox);
            this.placeBlock(world, Blocks.STICKY_PISTON.getBlockData().set(BlockPiston.FACING, EnumDirection.WEST), 10, -2, 8, boundingBox);
            this.placeBlock(world, Blocks.STICKY_PISTON.getBlockData().set(BlockPiston.FACING, EnumDirection.WEST), 10, -1, 8, boundingBox);
            this.placeBlock(world, Blocks.REPEATER.getBlockData().set(BlockRepeater.FACING, EnumDirection.NORTH), 10, -2, 10, boundingBox);
            if (!this.placedHiddenChest) {
                this.placedHiddenChest = this.createChest(world, boundingBox, random, 9, -3, 10, LootTables.JUNGLE_TEMPLE);
            }

            return true;
        }
    }

    static class MossStoneSelector extends StructurePiece.StructurePieceBlockSelector {
        @Override
        public void next(Random random, int x, int y, int z, boolean placeBlock) {
            if (random.nextFloat() < 0.4F) {
                this.next = Blocks.COBBLESTONE.getBlockData();
            } else {
                this.next = Blocks.MOSSY_COBBLESTONE.getBlockData();
            }

        }
    }
}
