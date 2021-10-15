package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.BlockButtonAbstract;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.block.BlockEnderPortalFrame;
import net.minecraft.world.level.block.BlockFence;
import net.minecraft.world.level.block.BlockIronBars;
import net.minecraft.world.level.block.BlockLadder;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.BlockStepAbstract;
import net.minecraft.world.level.block.BlockTorchWall;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockPropertySlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenStrongholdPieces {
    private static final int SMALL_DOOR_WIDTH = 3;
    private static final int SMALL_DOOR_HEIGHT = 3;
    private static final int MAX_DEPTH = 50;
    private static final int LOWEST_Y_POSITION = 10;
    private static final boolean CHECK_AIR = true;
    private static final WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight[] STRONGHOLD_PIECE_WEIGHTS = new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight[]{new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdStairs.class, 40, 0), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdPrison.class, 5, 5), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdLeftTurn.class, 20, 0), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdRightTurn.class, 20, 0), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdRoomCrossing.class, 10, 6), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdStairsStraight.class, 5, 5), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdStairs2.class, 5, 5), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdCrossing.class, 5, 4), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdChestCorridor.class, 5, 4), new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdLibrary.class, 10, 2) {
        @Override
        public boolean doPlace(int chainLength) {
            return super.doPlace(chainLength) && chainLength > 4;
        }
    }, new WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight(WorldGenStrongholdPieces.WorldGenStrongholdPortalRoom.class, 20, 1) {
        @Override
        public boolean doPlace(int chainLength) {
            return super.doPlace(chainLength) && chainLength > 5;
        }
    }};
    private static List<WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight> currentPieces;
    static Class<? extends WorldGenStrongholdPieces.WorldGenStrongholdPiece> imposedPiece;
    private static int totalWeight;
    static final WorldGenStrongholdPieces.WorldGenStrongholdStones SMOOTH_STONE_SELECTOR = new WorldGenStrongholdPieces.WorldGenStrongholdStones();

    public static void resetPieces() {
        currentPieces = Lists.newArrayList();

        for(WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight pieceWeight : STRONGHOLD_PIECE_WEIGHTS) {
            pieceWeight.placeCount = 0;
            currentPieces.add(pieceWeight);
        }

        imposedPiece = null;
    }

    private static boolean updatePieceWeight() {
        boolean bl = false;
        totalWeight = 0;

        for(WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight pieceWeight : currentPieces) {
            if (pieceWeight.maxPlaceCount > 0 && pieceWeight.placeCount < pieceWeight.maxPlaceCount) {
                bl = true;
            }

            totalWeight += pieceWeight.weight;
        }

        return bl;
    }

    private static WorldGenStrongholdPieces.WorldGenStrongholdPiece findAndCreatePieceFactory(Class<? extends WorldGenStrongholdPieces.WorldGenStrongholdPiece> pieceType, StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, @Nullable EnumDirection orientation, int chainLength) {
        WorldGenStrongholdPieces.WorldGenStrongholdPiece strongholdPiece = null;
        if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdStairs.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdStairs.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdPrison.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdPrison.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdLeftTurn.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdLeftTurn.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdRightTurn.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdRightTurn.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdRoomCrossing.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdRoomCrossing.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdStairsStraight.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdStairsStraight.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdStairs2.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdStairs2.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdCrossing.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdCrossing.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdChestCorridor.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdChestCorridor.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdLibrary.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdLibrary.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (pieceType == WorldGenStrongholdPieces.WorldGenStrongholdPortalRoom.class) {
            strongholdPiece = WorldGenStrongholdPieces.WorldGenStrongholdPortalRoom.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        }

        return strongholdPiece;
    }

    private static WorldGenStrongholdPieces.WorldGenStrongholdPiece generatePieceFromSmallDoor(WorldGenStrongholdPieces.WorldGenStrongholdStart start, StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
        if (!updatePieceWeight()) {
            return null;
        } else {
            if (imposedPiece != null) {
                WorldGenStrongholdPieces.WorldGenStrongholdPiece strongholdPiece = findAndCreatePieceFactory(imposedPiece, structurePieceAccessor, random, x, y, z, orientation, chainLength);
                imposedPiece = null;
                if (strongholdPiece != null) {
                    return strongholdPiece;
                }
            }

            int i = 0;

            while(i < 5) {
                ++i;
                int j = random.nextInt(totalWeight);

                for(WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight pieceWeight : currentPieces) {
                    j -= pieceWeight.weight;
                    if (j < 0) {
                        if (!pieceWeight.doPlace(chainLength) || pieceWeight == start.previousPiece) {
                            break;
                        }

                        WorldGenStrongholdPieces.WorldGenStrongholdPiece strongholdPiece2 = findAndCreatePieceFactory(pieceWeight.pieceClass, structurePieceAccessor, random, x, y, z, orientation, chainLength);
                        if (strongholdPiece2 != null) {
                            ++pieceWeight.placeCount;
                            start.previousPiece = pieceWeight;
                            if (!pieceWeight.isValid()) {
                                currentPieces.remove(pieceWeight);
                            }

                            return strongholdPiece2;
                        }
                    }
                }
            }

            StructureBoundingBox boundingBox = WorldGenStrongholdPieces.WorldGenStrongholdCorridor.findPieceBox(structurePieceAccessor, random, x, y, z, orientation);
            return boundingBox != null && boundingBox.minY() > 1 ? new WorldGenStrongholdPieces.WorldGenStrongholdCorridor(chainLength, boundingBox, orientation) : null;
        }
    }

    static StructurePiece generateAndAddPiece(WorldGenStrongholdPieces.WorldGenStrongholdStart start, StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, @Nullable EnumDirection orientation, int chainLength) {
        if (chainLength > 50) {
            return null;
        } else if (Math.abs(x - start.getBoundingBox().minX()) <= 112 && Math.abs(z - start.getBoundingBox().minZ()) <= 112) {
            StructurePiece structurePiece = generatePieceFromSmallDoor(start, structurePieceAccessor, random, x, y, z, orientation, chainLength + 1);
            if (structurePiece != null) {
                structurePieceAccessor.addPiece(structurePiece);
                start.pendingChildren.add(structurePiece);
            }

            return structurePiece;
        } else {
            return null;
        }
    }

    public abstract static class Turn extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        protected static final int WIDTH = 5;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 5;

        protected Turn(WorldGenFeatureStructurePieceType type, int length, StructureBoundingBox boundingBox) {
            super(type, length, boundingBox);
        }

        public Turn(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
            super(type, nbt);
        }
    }

    public static class WorldGenStrongholdChestCorridor extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private boolean hasPlacedChest;

        public WorldGenStrongholdChestCorridor(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_CHEST_CORRIDOR, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public WorldGenStrongholdChestCorridor(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_CHEST_CORRIDOR, nbt);
            this.hasPlacedChest = nbt.getBoolean("Chest");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Chest", this.hasPlacedChest);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdChestCorridor createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainlength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 7, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdChestCorridor(chainlength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 4, 6, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(world, random, boundingBox, WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING, 1, 1, 6);
            this.generateBox(world, boundingBox, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.getBlockData(), Blocks.STONE_BRICKS.getBlockData(), false);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.getBlockData(), 3, 1, 1, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.getBlockData(), 3, 1, 5, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.getBlockData(), 3, 2, 2, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.getBlockData(), 3, 2, 4, boundingBox);

            for(int i = 2; i <= 4; ++i) {
                this.placeBlock(world, Blocks.STONE_BRICK_SLAB.getBlockData(), 2, 1, i, boundingBox);
            }

            if (!this.hasPlacedChest && boundingBox.isInside(this.getWorldPos(3, 2, 3))) {
                this.hasPlacedChest = true;
                this.createChest(world, boundingBox, random, 3, 2, 3, LootTables.STRONGHOLD_CORRIDOR);
            }

            return true;
        }
    }

    public static class WorldGenStrongholdCorridor extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        private final int steps;

        public WorldGenStrongholdCorridor(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FILLER_CORRIDOR, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.steps = orientation != EnumDirection.NORTH && orientation != EnumDirection.SOUTH ? boundingBox.getXSpan() : boundingBox.getZSpan();
        }

        public WorldGenStrongholdCorridor(WorldServer serverLevel, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FILLER_CORRIDOR, nbt);
            this.steps = nbt.getInt("Steps");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setInt("Steps", this.steps);
        }

        public static StructureBoundingBox findPieceBox(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation) {
            int i = 3;
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 4, orientation);
            StructurePiece structurePiece = structurePieceAccessor.findCollisionPiece(boundingBox);
            if (structurePiece == null) {
                return null;
            } else {
                if (structurePiece.getBoundingBox().minY() == boundingBox.minY()) {
                    for(int j = 2; j >= 1; --j) {
                        boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, j, orientation);
                        if (!structurePiece.getBoundingBox().intersects(boundingBox)) {
                            return StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, j + 1, orientation);
                        }
                    }
                }

                return null;
            }
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            for(int i = 0; i < this.steps; ++i) {
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 0, 0, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 0, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 2, 0, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 0, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 4, 0, i, boundingBox);

                for(int j = 1; j <= 3; ++j) {
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 0, j, i, boundingBox);
                    this.placeBlock(world, Blocks.CAVE_AIR.getBlockData(), 1, j, i, boundingBox);
                    this.placeBlock(world, Blocks.CAVE_AIR.getBlockData(), 2, j, i, boundingBox);
                    this.placeBlock(world, Blocks.CAVE_AIR.getBlockData(), 3, j, i, boundingBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 4, j, i, boundingBox);
                }

                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 0, 4, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 4, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 2, 4, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 4, i, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 4, 4, i, boundingBox);
            }

            return true;
        }
    }

    public static class WorldGenStrongholdCrossing extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        protected static final int WIDTH = 10;
        protected static final int HEIGHT = 9;
        protected static final int DEPTH = 11;
        private final boolean leftLow;
        private final boolean leftHigh;
        private final boolean rightLow;
        private final boolean rightHigh;

        public WorldGenStrongholdCrossing(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FIVE_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.leftLow = random.nextBoolean();
            this.leftHigh = random.nextBoolean();
            this.rightLow = random.nextBoolean();
            this.rightHigh = random.nextInt(3) > 0;
        }

        public WorldGenStrongholdCrossing(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_FIVE_CROSSING, nbt);
            this.leftLow = nbt.getBoolean("leftLow");
            this.leftHigh = nbt.getBoolean("leftHigh");
            this.rightLow = nbt.getBoolean("rightLow");
            this.rightHigh = nbt.getBoolean("rightHigh");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("leftLow", this.leftLow);
            nbt.setBoolean("leftHigh", this.leftHigh);
            nbt.setBoolean("rightLow", this.rightLow);
            nbt.setBoolean("rightHigh", this.rightHigh);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            int i = 3;
            int j = 5;
            EnumDirection direction = this.getOrientation();
            if (direction == EnumDirection.WEST || direction == EnumDirection.NORTH) {
                i = 8 - i;
                j = 8 - j;
            }

            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 5, 1);
            if (this.leftLow) {
                this.generateSmallDoorChildLeft((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, i, 1);
            }

            if (this.leftHigh) {
                this.generateSmallDoorChildLeft((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, j, 7);
            }

            if (this.rightLow) {
                this.generateSmallDoorChildRight((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, i, 1);
            }

            if (this.rightHigh) {
                this.generateSmallDoorChildRight((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, j, 7);
            }

        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdCrossing createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -4, -3, 0, 10, 9, 11, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdCrossing(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 9, 8, 10, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 4, 3, 0);
            if (this.leftLow) {
                this.generateBox(world, boundingBox, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightLow) {
                this.generateBox(world, boundingBox, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.leftHigh) {
                this.generateBox(world, boundingBox, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightHigh) {
                this.generateBox(world, boundingBox, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
            }

            this.generateBox(world, boundingBox, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, boundingBox, 1, 2, 1, 8, 2, 6, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 1, 5, 4, 4, 9, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 8, 1, 5, 8, 4, 9, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 4, 7, 3, 4, 9, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 3, 5, 3, 3, 6, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.getBlockData(), Blocks.SMOOTH_STONE_SLAB.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.getBlockData(), Blocks.SMOOTH_STONE_SLAB.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 1, 7, 7, 1, 8, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.getBlockData(), Blocks.SMOOTH_STONE_SLAB.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.getBlockData(), Blocks.SMOOTH_STONE_SLAB.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.getBlockData(), Blocks.SMOOTH_STONE_SLAB.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.getBlockData(), Blocks.SMOOTH_STONE_SLAB.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 5, 7, 7, 5, 9, Blocks.SMOOTH_STONE_SLAB.getBlockData().set(BlockStepAbstract.TYPE, BlockPropertySlabType.DOUBLE), Blocks.SMOOTH_STONE_SLAB.getBlockData().set(BlockStepAbstract.TYPE, BlockPropertySlabType.DOUBLE), false);
            this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.SOUTH), 6, 5, 6, boundingBox);
            return true;
        }
    }

    public static class WorldGenStrongholdLeftTurn extends WorldGenStrongholdPieces.Turn {
        public WorldGenStrongholdLeftTurn(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LEFT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public WorldGenStrongholdLeftTurn(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LEFT_TURN, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            EnumDirection direction = this.getOrientation();
            if (direction != EnumDirection.NORTH && direction != EnumDirection.EAST) {
                this.generateSmallDoorChildRight((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
            } else {
                this.generateSmallDoorChildLeft((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
            }

        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdLeftTurn createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdLeftTurn(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 4, 4, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 1, 0);
            EnumDirection direction = this.getOrientation();
            if (direction != EnumDirection.NORTH && direction != EnumDirection.EAST) {
                this.generateBox(world, boundingBox, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(world, boundingBox, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
            }

            return true;
        }
    }

    public static class WorldGenStrongholdLibrary extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        protected static final int WIDTH = 14;
        protected static final int HEIGHT = 6;
        protected static final int TALL_HEIGHT = 11;
        protected static final int DEPTH = 15;
        private final boolean isTall;

        public WorldGenStrongholdLibrary(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LIBRARY, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.isTall = boundingBox.getYSpan() > 6;
        }

        public WorldGenStrongholdLibrary(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_LIBRARY, nbt);
            this.isTall = nbt.getBoolean("Tall");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Tall", this.isTall);
        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdLibrary createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -4, -1, 0, 14, 11, 15, orientation);
            if (!isOkBox(boundingBox) || structurePieceAccessor.findCollisionPiece(boundingBox) != null) {
                boundingBox = StructureBoundingBox.orientBox(x, y, z, -4, -1, 0, 14, 6, 15, orientation);
                if (!isOkBox(boundingBox) || structurePieceAccessor.findCollisionPiece(boundingBox) != null) {
                    return null;
                }
            }

            return new WorldGenStrongholdPieces.WorldGenStrongholdLibrary(chainLength, random, boundingBox, orientation);
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            int i = 11;
            if (!this.isTall) {
                i = 6;
            }

            this.generateBox(world, boundingBox, 0, 0, 0, 13, i - 1, 14, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 4, 1, 0);
            this.generateMaybeBox(world, boundingBox, random, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.getBlockData(), Blocks.COBWEB.getBlockData(), false, false);
            int j = 1;
            int k = 12;

            for(int l = 1; l <= 13; ++l) {
                if ((l - 1) % 4 == 0) {
                    this.generateBox(world, boundingBox, 1, 1, l, 1, 4, l, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                    this.generateBox(world, boundingBox, 12, 1, l, 12, 4, l, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                    this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.EAST), 2, 3, l, boundingBox);
                    this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.WEST), 11, 3, l, boundingBox);
                    if (this.isTall) {
                        this.generateBox(world, boundingBox, 1, 6, l, 1, 9, l, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                        this.generateBox(world, boundingBox, 12, 6, l, 12, 9, l, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                    }
                } else {
                    this.generateBox(world, boundingBox, 1, 1, l, 1, 4, l, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
                    this.generateBox(world, boundingBox, 12, 1, l, 12, 4, l, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
                    if (this.isTall) {
                        this.generateBox(world, boundingBox, 1, 6, l, 1, 9, l, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
                        this.generateBox(world, boundingBox, 12, 6, l, 12, 9, l, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
                    }
                }
            }

            for(int m = 3; m < 12; m += 2) {
                this.generateBox(world, boundingBox, 3, 1, m, 4, 3, m, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
                this.generateBox(world, boundingBox, 6, 1, m, 7, 3, m, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
                this.generateBox(world, boundingBox, 9, 1, m, 10, 3, m, Blocks.BOOKSHELF.getBlockData(), Blocks.BOOKSHELF.getBlockData(), false);
            }

            if (this.isTall) {
                this.generateBox(world, boundingBox, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                this.generateBox(world, boundingBox, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                this.generateBox(world, boundingBox, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                this.generateBox(world, boundingBox, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.getBlockData(), Blocks.OAK_PLANKS.getBlockData(), false);
                this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 9, 5, 11, boundingBox);
                this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 8, 5, 11, boundingBox);
                this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 9, 5, 10, boundingBox);
                IBlockData blockState = Blocks.OAK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
                IBlockData blockState2 = Blocks.OAK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
                this.generateBox(world, boundingBox, 3, 6, 3, 3, 6, 11, blockState2, blockState2, false);
                this.generateBox(world, boundingBox, 10, 6, 3, 10, 6, 9, blockState2, blockState2, false);
                this.generateBox(world, boundingBox, 4, 6, 2, 9, 6, 2, blockState, blockState, false);
                this.generateBox(world, boundingBox, 4, 6, 12, 7, 6, 12, blockState, blockState, false);
                this.placeBlock(world, Blocks.OAK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 3, 6, 2, boundingBox);
                this.placeBlock(world, Blocks.OAK_FENCE.getBlockData().set(BlockFence.SOUTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 3, 6, 12, boundingBox);
                this.placeBlock(world, Blocks.OAK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.WEST, Boolean.valueOf(true)), 10, 6, 2, boundingBox);

                for(int n = 0; n <= 2; ++n) {
                    this.placeBlock(world, Blocks.OAK_FENCE.getBlockData().set(BlockFence.SOUTH, Boolean.valueOf(true)).set(BlockFence.WEST, Boolean.valueOf(true)), 8 + n, 6, 12 - n, boundingBox);
                    if (n != 2) {
                        this.placeBlock(world, Blocks.OAK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 8 + n, 6, 11 - n, boundingBox);
                    }
                }

                IBlockData blockState3 = Blocks.LADDER.getBlockData().set(BlockLadder.FACING, EnumDirection.SOUTH);
                this.placeBlock(world, blockState3, 10, 1, 13, boundingBox);
                this.placeBlock(world, blockState3, 10, 2, 13, boundingBox);
                this.placeBlock(world, blockState3, 10, 3, 13, boundingBox);
                this.placeBlock(world, blockState3, 10, 4, 13, boundingBox);
                this.placeBlock(world, blockState3, 10, 5, 13, boundingBox);
                this.placeBlock(world, blockState3, 10, 6, 13, boundingBox);
                this.placeBlock(world, blockState3, 10, 7, 13, boundingBox);
                int o = 7;
                int p = 7;
                IBlockData blockState4 = Blocks.OAK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true));
                this.placeBlock(world, blockState4, 6, 9, 7, boundingBox);
                IBlockData blockState5 = Blocks.OAK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true));
                this.placeBlock(world, blockState5, 7, 9, 7, boundingBox);
                this.placeBlock(world, blockState4, 6, 8, 7, boundingBox);
                this.placeBlock(world, blockState5, 7, 8, 7, boundingBox);
                IBlockData blockState6 = blockState2.set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
                this.placeBlock(world, blockState6, 6, 7, 7, boundingBox);
                this.placeBlock(world, blockState6, 7, 7, 7, boundingBox);
                this.placeBlock(world, blockState4, 5, 7, 7, boundingBox);
                this.placeBlock(world, blockState5, 8, 7, 7, boundingBox);
                this.placeBlock(world, blockState4.set(BlockFence.NORTH, Boolean.valueOf(true)), 6, 7, 6, boundingBox);
                this.placeBlock(world, blockState4.set(BlockFence.SOUTH, Boolean.valueOf(true)), 6, 7, 8, boundingBox);
                this.placeBlock(world, blockState5.set(BlockFence.NORTH, Boolean.valueOf(true)), 7, 7, 6, boundingBox);
                this.placeBlock(world, blockState5.set(BlockFence.SOUTH, Boolean.valueOf(true)), 7, 7, 8, boundingBox);
                IBlockData blockState7 = Blocks.TORCH.getBlockData();
                this.placeBlock(world, blockState7, 5, 8, 7, boundingBox);
                this.placeBlock(world, blockState7, 8, 8, 7, boundingBox);
                this.placeBlock(world, blockState7, 6, 8, 6, boundingBox);
                this.placeBlock(world, blockState7, 6, 8, 8, boundingBox);
                this.placeBlock(world, blockState7, 7, 8, 6, boundingBox);
                this.placeBlock(world, blockState7, 7, 8, 8, boundingBox);
            }

            this.createChest(world, boundingBox, random, 3, 3, 5, LootTables.STRONGHOLD_LIBRARY);
            if (this.isTall) {
                this.placeBlock(world, CAVE_AIR, 12, 9, 1, boundingBox);
                this.createChest(world, boundingBox, random, 12, 8, 1, LootTables.STRONGHOLD_LIBRARY);
            }

            return true;
        }
    }

    abstract static class WorldGenStrongholdPiece extends StructurePiece {
        protected WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType entryDoor = WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING;

        protected WorldGenStrongholdPiece(WorldGenFeatureStructurePieceType type, int length, StructureBoundingBox boundingBox) {
            super(type, length, boundingBox);
        }

        public WorldGenStrongholdPiece(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
            super(type, nbt);
            this.entryDoor = WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.valueOf(nbt.getString("EntryDoor"));
        }

        @Override
        public NoiseEffect getNoiseEffect() {
            return NoiseEffect.BURY;
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            nbt.setString("EntryDoor", this.entryDoor.name());
        }

        protected void generateSmallDoor(GeneratorAccessSeed world, Random random, StructureBoundingBox boundingBox, WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType type, int x, int y, int z) {
            switch(type) {
            case OPENING:
                this.generateBox(world, boundingBox, x, y, z, x + 3 - 1, y + 3 - 1, z, CAVE_AIR, CAVE_AIR, false);
                break;
            case WOOD_DOOR:
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x, y, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 1, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 2, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 2, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 2, y, z, boundingBox);
                this.placeBlock(world, Blocks.OAK_DOOR.getBlockData(), x + 1, y, z, boundingBox);
                this.placeBlock(world, Blocks.OAK_DOOR.getBlockData().set(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER), x + 1, y + 1, z, boundingBox);
                break;
            case GRATES:
                this.placeBlock(world, Blocks.CAVE_AIR.getBlockData(), x + 1, y, z, boundingBox);
                this.placeBlock(world, Blocks.CAVE_AIR.getBlockData(), x + 1, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.WEST, Boolean.valueOf(true)), x, y, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.WEST, Boolean.valueOf(true)), x, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.EAST, Boolean.valueOf(true)).set(BlockIronBars.WEST, Boolean.valueOf(true)), x, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.EAST, Boolean.valueOf(true)).set(BlockIronBars.WEST, Boolean.valueOf(true)), x + 1, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.EAST, Boolean.valueOf(true)).set(BlockIronBars.WEST, Boolean.valueOf(true)), x + 2, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.EAST, Boolean.valueOf(true)), x + 2, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.EAST, Boolean.valueOf(true)), x + 2, y, z, boundingBox);
                break;
            case IRON_DOOR:
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x, y, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 1, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 2, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 2, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), x + 2, y, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_DOOR.getBlockData(), x + 1, y, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_DOOR.getBlockData().set(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER), x + 1, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BUTTON.getBlockData().set(BlockButtonAbstract.FACING, EnumDirection.NORTH), x + 2, y + 1, z + 1, boundingBox);
                this.placeBlock(world, Blocks.STONE_BUTTON.getBlockData().set(BlockButtonAbstract.FACING, EnumDirection.SOUTH), x + 2, y + 1, z - 1, boundingBox);
            }

        }

        protected WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType randomSmallDoor(Random random) {
            int i = random.nextInt(5);
            switch(i) {
            case 0:
            case 1:
            default:
                return WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING;
            case 2:
                return WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.WOOD_DOOR;
            case 3:
                return WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.GRATES;
            case 4:
                return WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.IRON_DOOR;
            }
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildForward(WorldGenStrongholdPieces.WorldGenStrongholdStart start, StructurePieceAccessor structurePieceAccessor, Random random, int leftRightOffset, int heightOffset) {
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, direction, this.getGenDepth());
                case SOUTH:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, direction, this.getGenDepth());
                case WEST:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth());
                case EAST:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth());
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildLeft(WorldGenStrongholdPieces.WorldGenStrongholdStart start, StructurePieceAccessor structurePieceAccessor, Random random, int heightOffset, int leftRightOffset) {
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.WEST, this.getGenDepth());
                case SOUTH:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.WEST, this.getGenDepth());
                case WEST:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, EnumDirection.NORTH, this.getGenDepth());
                case EAST:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, EnumDirection.NORTH, this.getGenDepth());
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildRight(WorldGenStrongholdPieces.WorldGenStrongholdStart start, StructurePieceAccessor structurePieceAccessor, Random random, int heightOffset, int leftRightOffset) {
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.EAST, this.getGenDepth());
                case SOUTH:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.EAST, this.getGenDepth());
                case WEST:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, this.getGenDepth());
                case EAST:
                    return WorldGenStrongholdPieces.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, this.getGenDepth());
                }
            }

            return null;
        }

        protected static boolean isOkBox(StructureBoundingBox boundingBox) {
            return boundingBox != null && boundingBox.minY() > 10;
        }

        protected static enum WorldGenStrongholdDoorType {
            OPENING,
            WOOD_DOOR,
            GRATES,
            IRON_DOOR;
        }
    }

    static class WorldGenStrongholdPieceWeight {
        public final Class<? extends WorldGenStrongholdPieces.WorldGenStrongholdPiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;

        public WorldGenStrongholdPieceWeight(Class<? extends WorldGenStrongholdPieces.WorldGenStrongholdPiece> pieceType, int weight, int limit) {
            this.pieceClass = pieceType;
            this.weight = weight;
            this.maxPlaceCount = limit;
        }

        public boolean doPlace(int chainLength) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    public static class WorldGenStrongholdPortalRoom extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 8;
        protected static final int DEPTH = 16;
        private boolean hasPlacedSpawner;

        public WorldGenStrongholdPortalRoom(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PORTAL_ROOM, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenStrongholdPortalRoom(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PORTAL_ROOM, nbt);
            this.hasPlacedSpawner = nbt.getBoolean("Mob");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Mob", this.hasPlacedSpawner);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            if (start != null) {
                ((WorldGenStrongholdPieces.WorldGenStrongholdStart)start).portalRoomPiece = this;
            }

        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdPortalRoom createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -4, -1, 0, 11, 8, 16, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdPortalRoom(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 10, 7, 15, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.GRATES, 4, 1, 0);
            int i = 6;
            this.generateBox(world, boundingBox, 1, i, 1, 1, i, 14, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 9, i, 1, 9, i, 14, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, i, 1, 8, i, 2, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 2, i, 14, 8, i, 14, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 1, 1, 2, 1, 4, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 8, 1, 1, 9, 1, 4, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 1, 1, 1, 1, 1, 3, Blocks.LAVA.getBlockData(), Blocks.LAVA.getBlockData(), false);
            this.generateBox(world, boundingBox, 9, 1, 1, 9, 1, 3, Blocks.LAVA.getBlockData(), Blocks.LAVA.getBlockData(), false);
            this.generateBox(world, boundingBox, 3, 1, 8, 7, 1, 12, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 1, 9, 6, 1, 11, Blocks.LAVA.getBlockData(), Blocks.LAVA.getBlockData(), false);
            IBlockData blockState = Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(true)).set(BlockIronBars.SOUTH, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.IRON_BARS.getBlockData().set(BlockIronBars.WEST, Boolean.valueOf(true)).set(BlockIronBars.EAST, Boolean.valueOf(true));

            for(int j = 3; j < 14; j += 2) {
                this.generateBox(world, boundingBox, 0, 3, j, 0, 4, j, blockState, blockState, false);
                this.generateBox(world, boundingBox, 10, 3, j, 10, 4, j, blockState, blockState, false);
            }

            for(int k = 2; k < 9; k += 2) {
                this.generateBox(world, boundingBox, k, 3, 15, k, 4, 15, blockState2, blockState2, false);
            }

            IBlockData blockState3 = Blocks.STONE_BRICK_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.NORTH);
            this.generateBox(world, boundingBox, 4, 1, 5, 6, 1, 7, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 2, 6, 6, 2, 7, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 3, 7, 6, 3, 7, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);

            for(int l = 4; l <= 6; ++l) {
                this.placeBlock(world, blockState3, l, 1, 4, boundingBox);
                this.placeBlock(world, blockState3, l, 2, 5, boundingBox);
                this.placeBlock(world, blockState3, l, 3, 6, boundingBox);
            }

            IBlockData blockState4 = Blocks.END_PORTAL_FRAME.getBlockData().set(BlockEnderPortalFrame.FACING, EnumDirection.NORTH);
            IBlockData blockState5 = Blocks.END_PORTAL_FRAME.getBlockData().set(BlockEnderPortalFrame.FACING, EnumDirection.SOUTH);
            IBlockData blockState6 = Blocks.END_PORTAL_FRAME.getBlockData().set(BlockEnderPortalFrame.FACING, EnumDirection.EAST);
            IBlockData blockState7 = Blocks.END_PORTAL_FRAME.getBlockData().set(BlockEnderPortalFrame.FACING, EnumDirection.WEST);
            boolean bl = true;
            boolean[] bls = new boolean[12];

            for(int m = 0; m < bls.length; ++m) {
                bls[m] = random.nextFloat() > 0.9F;
                bl &= bls[m];
            }

            this.placeBlock(world, blockState4.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[0])), 4, 3, 8, boundingBox);
            this.placeBlock(world, blockState4.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[1])), 5, 3, 8, boundingBox);
            this.placeBlock(world, blockState4.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[2])), 6, 3, 8, boundingBox);
            this.placeBlock(world, blockState5.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[3])), 4, 3, 12, boundingBox);
            this.placeBlock(world, blockState5.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[4])), 5, 3, 12, boundingBox);
            this.placeBlock(world, blockState5.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[5])), 6, 3, 12, boundingBox);
            this.placeBlock(world, blockState6.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[6])), 3, 3, 9, boundingBox);
            this.placeBlock(world, blockState6.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[7])), 3, 3, 10, boundingBox);
            this.placeBlock(world, blockState6.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[8])), 3, 3, 11, boundingBox);
            this.placeBlock(world, blockState7.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[9])), 7, 3, 9, boundingBox);
            this.placeBlock(world, blockState7.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[10])), 7, 3, 10, boundingBox);
            this.placeBlock(world, blockState7.set(BlockEnderPortalFrame.HAS_EYE, Boolean.valueOf(bls[11])), 7, 3, 11, boundingBox);
            if (bl) {
                IBlockData blockState8 = Blocks.END_PORTAL.getBlockData();
                this.placeBlock(world, blockState8, 4, 3, 9, boundingBox);
                this.placeBlock(world, blockState8, 5, 3, 9, boundingBox);
                this.placeBlock(world, blockState8, 6, 3, 9, boundingBox);
                this.placeBlock(world, blockState8, 4, 3, 10, boundingBox);
                this.placeBlock(world, blockState8, 5, 3, 10, boundingBox);
                this.placeBlock(world, blockState8, 6, 3, 10, boundingBox);
                this.placeBlock(world, blockState8, 4, 3, 11, boundingBox);
                this.placeBlock(world, blockState8, 5, 3, 11, boundingBox);
                this.placeBlock(world, blockState8, 6, 3, 11, boundingBox);
            }

            if (!this.hasPlacedSpawner) {
                BlockPosition blockPos = this.getWorldPos(5, 3, 6);
                if (boundingBox.isInside(blockPos)) {
                    this.hasPlacedSpawner = true;
                    world.setTypeAndData(blockPos, Blocks.SPAWNER.getBlockData(), 2);
                    TileEntity blockEntity = world.getTileEntity(blockPos);
                    if (blockEntity instanceof TileEntityMobSpawner) {
                        ((TileEntityMobSpawner)blockEntity).getSpawner().setMobName(EntityTypes.SILVERFISH);
                    }
                }
            }

            return true;
        }
    }

    public static class WorldGenStrongholdPrison extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        protected static final int WIDTH = 9;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 11;

        public WorldGenStrongholdPrison(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PRISON_HALL, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public WorldGenStrongholdPrison(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_PRISON_HALL, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdPrison createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 9, 5, 11, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdPrison(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 8, 4, 10, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 1, 0);
            this.generateBox(world, boundingBox, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, boundingBox, 4, 1, 1, 4, 3, 1, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 1, 3, 4, 3, 3, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 1, 7, 4, 3, 7, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, boundingBox, 4, 1, 9, 4, 3, 9, false, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);

            for(int i = 1; i <= 3; ++i) {
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(true)).set(BlockIronBars.SOUTH, Boolean.valueOf(true)), 4, i, 4, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(true)).set(BlockIronBars.SOUTH, Boolean.valueOf(true)).set(BlockIronBars.EAST, Boolean.valueOf(true)), 4, i, 5, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(true)).set(BlockIronBars.SOUTH, Boolean.valueOf(true)), 4, i, 6, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.WEST, Boolean.valueOf(true)).set(BlockIronBars.EAST, Boolean.valueOf(true)), 5, i, 5, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.WEST, Boolean.valueOf(true)).set(BlockIronBars.EAST, Boolean.valueOf(true)), 6, i, 5, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.WEST, Boolean.valueOf(true)).set(BlockIronBars.EAST, Boolean.valueOf(true)), 7, i, 5, boundingBox);
            }

            this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(true)).set(BlockIronBars.SOUTH, Boolean.valueOf(true)), 4, 3, 2, boundingBox);
            this.placeBlock(world, Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(true)).set(BlockIronBars.SOUTH, Boolean.valueOf(true)), 4, 3, 8, boundingBox);
            IBlockData blockState = Blocks.IRON_DOOR.getBlockData().set(BlockDoor.FACING, EnumDirection.WEST);
            IBlockData blockState2 = Blocks.IRON_DOOR.getBlockData().set(BlockDoor.FACING, EnumDirection.WEST).set(BlockDoor.HALF, BlockPropertyDoubleBlockHalf.UPPER);
            this.placeBlock(world, blockState, 4, 1, 2, boundingBox);
            this.placeBlock(world, blockState2, 4, 2, 2, boundingBox);
            this.placeBlock(world, blockState, 4, 1, 8, boundingBox);
            this.placeBlock(world, blockState2, 4, 2, 8, boundingBox);
            return true;
        }
    }

    public static class WorldGenStrongholdRightTurn extends WorldGenStrongholdPieces.Turn {
        public WorldGenStrongholdRightTurn(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_RIGHT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public WorldGenStrongholdRightTurn(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_RIGHT_TURN, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            EnumDirection direction = this.getOrientation();
            if (direction != EnumDirection.NORTH && direction != EnumDirection.EAST) {
                this.generateSmallDoorChildLeft((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
            } else {
                this.generateSmallDoorChildRight((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
            }

        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdRightTurn createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdRightTurn(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 4, 4, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 1, 0);
            EnumDirection direction = this.getOrientation();
            if (direction != EnumDirection.NORTH && direction != EnumDirection.EAST) {
                this.generateBox(world, boundingBox, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(world, boundingBox, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
            }

            return true;
        }
    }

    public static class WorldGenStrongholdRoomCrossing extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 7;
        protected static final int DEPTH = 11;
        protected final int type;

        public WorldGenStrongholdRoomCrossing(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_ROOM_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.type = random.nextInt(5);
        }

        public WorldGenStrongholdRoomCrossing(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_ROOM_CROSSING, nbt);
            this.type = nbt.getInt("Type");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setInt("Type", this.type);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 4, 1);
            this.generateSmallDoorChildLeft((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 4);
            this.generateSmallDoorChildRight((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 4);
        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdRoomCrossing createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -4, -1, 0, 11, 7, 11, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdRoomCrossing(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 10, 6, 10, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 4, 1, 0);
            this.generateBox(world, boundingBox, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, boundingBox, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, boundingBox, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
            switch(this.type) {
            case 0:
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 5, 1, 5, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 5, 2, 5, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 5, 3, 5, boundingBox);
                this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.WEST), 4, 3, 5, boundingBox);
                this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.EAST), 6, 3, 5, boundingBox);
                this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.SOUTH), 5, 3, 4, boundingBox);
                this.placeBlock(world, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.NORTH), 5, 3, 6, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 4, 1, 4, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 4, 1, 5, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 4, 1, 6, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 6, 1, 4, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 6, 1, 5, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 6, 1, 6, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 5, 1, 4, boundingBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 5, 1, 6, boundingBox);
                break;
            case 1:
                for(int i = 0; i < 5; ++i) {
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 1, 3 + i, boundingBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 7, 1, 3 + i, boundingBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3 + i, 1, 3, boundingBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3 + i, 1, 7, boundingBox);
                }

                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 5, 1, 5, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 5, 2, 5, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 5, 3, 5, boundingBox);
                this.placeBlock(world, Blocks.WATER.getBlockData(), 5, 4, 5, boundingBox);
                break;
            case 2:
                for(int j = 1; j <= 9; ++j) {
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 1, 3, j, boundingBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 9, 3, j, boundingBox);
                }

                for(int k = 1; k <= 9; ++k) {
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), k, 3, 1, boundingBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), k, 3, 9, boundingBox);
                }

                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 5, 1, 4, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 5, 1, 6, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 5, 3, 4, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 5, 3, 6, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 4, 1, 5, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 6, 1, 5, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 4, 3, 5, boundingBox);
                this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 6, 3, 5, boundingBox);

                for(int l = 1; l <= 3; ++l) {
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 4, l, 4, boundingBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 6, l, 4, boundingBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 4, l, 6, boundingBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.getBlockData(), 6, l, 6, boundingBox);
                }

                this.placeBlock(world, Blocks.TORCH.getBlockData(), 5, 3, 5, boundingBox);

                for(int m = 2; m <= 8; ++m) {
                    this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 2, 3, m, boundingBox);
                    this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 3, 3, m, boundingBox);
                    if (m <= 3 || m >= 7) {
                        this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 4, 3, m, boundingBox);
                        this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 5, 3, m, boundingBox);
                        this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 6, 3, m, boundingBox);
                    }

                    this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 7, 3, m, boundingBox);
                    this.placeBlock(world, Blocks.OAK_PLANKS.getBlockData(), 8, 3, m, boundingBox);
                }

                IBlockData blockState = Blocks.LADDER.getBlockData().set(BlockLadder.FACING, EnumDirection.WEST);
                this.placeBlock(world, blockState, 9, 1, 3, boundingBox);
                this.placeBlock(world, blockState, 9, 2, 3, boundingBox);
                this.placeBlock(world, blockState, 9, 3, 3, boundingBox);
                this.createChest(world, boundingBox, random, 3, 4, 8, LootTables.STRONGHOLD_CROSSING);
            }

            return true;
        }
    }

    public static class WorldGenStrongholdStairs extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private final boolean leftChild;
        private final boolean rightChild;

        public WorldGenStrongholdStairs(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.leftChild = random.nextInt(2) == 0;
            this.rightChild = random.nextInt(2) == 0;
        }

        public WorldGenStrongholdStairs(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT, nbt);
            this.leftChild = nbt.getBoolean("Left");
            this.rightChild = nbt.getBoolean("Right");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Left", this.leftChild);
            nbt.setBoolean("Right", this.rightChild);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
            if (this.leftChild) {
                this.generateSmallDoorChildLeft((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 2);
            }

            if (this.rightChild) {
                this.generateSmallDoorChildRight((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 2);
            }

        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdStairs createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 7, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdStairs(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 4, 6, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(world, random, boundingBox, WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING, 1, 1, 6);
            IBlockData blockState = Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.EAST);
            IBlockData blockState2 = Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.WEST);
            this.maybeGenerateBlock(world, boundingBox, random, 0.1F, 1, 2, 1, blockState);
            this.maybeGenerateBlock(world, boundingBox, random, 0.1F, 3, 2, 1, blockState2);
            this.maybeGenerateBlock(world, boundingBox, random, 0.1F, 1, 2, 5, blockState);
            this.maybeGenerateBlock(world, boundingBox, random, 0.1F, 3, 2, 5, blockState2);
            if (this.leftChild) {
                this.generateBox(world, boundingBox, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightChild) {
                this.generateBox(world, boundingBox, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
            }

            return true;
        }
    }

    public static class WorldGenStrongholdStairs2 extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 5;
        private final boolean isSource;

        public WorldGenStrongholdStairs2(WorldGenFeatureStructurePieceType structurePieceType, int chainLength, int x, int z, EnumDirection orientation) {
            super(structurePieceType, chainLength, makeBoundingBox(x, 64, z, orientation, 5, 11, 5));
            this.isSource = true;
            this.setOrientation(orientation);
            this.entryDoor = WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING;
        }

        public WorldGenStrongholdStairs2(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STAIRS_DOWN, chainLength, boundingBox);
            this.isSource = false;
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public WorldGenStrongholdStairs2(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
            super(type, nbt);
            this.isSource = nbt.getBoolean("Source");
        }

        public WorldGenStrongholdStairs2(WorldServer world, NBTTagCompound nbt) {
            this(WorldGenFeatureStructurePieceType.STRONGHOLD_STAIRS_DOWN, nbt);
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Source", this.isSource);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            if (this.isSource) {
                WorldGenStrongholdPieces.imposedPiece = WorldGenStrongholdPieces.WorldGenStrongholdCrossing.class;
            }

            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdStairs2 createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -7, 0, 5, 11, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdStairs2(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 10, 4, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(world, random, boundingBox, WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING, 1, 1, 4);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 2, 6, 1, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 5, 1, boundingBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 1, 6, 1, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 5, 2, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 4, 3, boundingBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 1, 5, 3, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 2, 4, 3, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 3, 3, boundingBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 3, 4, 3, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 3, 2, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 2, 1, boundingBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 3, 3, 1, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 2, 2, 1, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 1, 1, boundingBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 1, 2, 1, boundingBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 1, 2, boundingBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.getBlockData(), 1, 1, 3, boundingBox);
            return true;
        }
    }

    public static class WorldGenStrongholdStairsStraight extends WorldGenStrongholdPieces.WorldGenStrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 8;

        public WorldGenStrongholdStairsStraight(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public WorldGenStrongholdStairsStraight(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateSmallDoorChildForward((WorldGenStrongholdPieces.WorldGenStrongholdStart)start, structurePieceAccessor, random, 1, 1);
        }

        public static WorldGenStrongholdPieces.WorldGenStrongholdStairsStraight createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -7, 0, 5, 11, 8, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenStrongholdPieces.WorldGenStrongholdStairsStraight(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 10, 7, true, random, WorldGenStrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, boundingBox, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(world, random, boundingBox, WorldGenStrongholdPieces.WorldGenStrongholdPiece.WorldGenStrongholdDoorType.OPENING, 1, 1, 7);
            IBlockData blockState = Blocks.COBBLESTONE_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.SOUTH);

            for(int i = 0; i < 6; ++i) {
                this.placeBlock(world, blockState, 1, 6 - i, 1 + i, boundingBox);
                this.placeBlock(world, blockState, 2, 6 - i, 1 + i, boundingBox);
                this.placeBlock(world, blockState, 3, 6 - i, 1 + i, boundingBox);
                if (i < 5) {
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 1, 5 - i, 1 + i, boundingBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 2, 5 - i, 1 + i, boundingBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.getBlockData(), 3, 5 - i, 1 + i, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenStrongholdStart extends WorldGenStrongholdPieces.WorldGenStrongholdStairs2 {
        public WorldGenStrongholdPieces.WorldGenStrongholdPieceWeight previousPiece;
        @Nullable
        public WorldGenStrongholdPieces.WorldGenStrongholdPortalRoom portalRoomPiece;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public WorldGenStrongholdStart(Random random, int i, int j) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_START, 0, i, j, getRandomHorizontalDirection(random));
        }

        public WorldGenStrongholdStart(WorldServer serverLevel, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.STRONGHOLD_START, nbt);
        }

        @Override
        public BlockPosition getLocatorPosition() {
            return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
        }
    }

    static class WorldGenStrongholdStones extends StructurePiece.StructurePieceBlockSelector {
        @Override
        public void next(Random random, int x, int y, int z, boolean placeBlock) {
            if (placeBlock) {
                float f = random.nextFloat();
                if (f < 0.2F) {
                    this.next = Blocks.CRACKED_STONE_BRICKS.getBlockData();
                } else if (f < 0.5F) {
                    this.next = Blocks.MOSSY_STONE_BRICKS.getBlockData();
                } else if (f < 0.55F) {
                    this.next = Blocks.INFESTED_STONE_BRICKS.getBlockData();
                } else {
                    this.next = Blocks.STONE_BRICKS.getBlockData();
                }
            } else {
                this.next = Blocks.CAVE_AIR.getBlockData();
            }

        }
    }
}
