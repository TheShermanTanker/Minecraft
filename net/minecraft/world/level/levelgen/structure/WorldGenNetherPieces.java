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
import net.minecraft.world.level.block.BlockFence;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenNetherPieces {
    private static final int MAX_DEPTH = 30;
    private static final int LOWEST_Y_POSITION = 10;
    static final WorldGenNetherPieces.WorldGenNetherPieceWeight[] BRIDGE_PIECE_WEIGHTS = new WorldGenNetherPieces.WorldGenNetherPieceWeight[]{new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece3.class, 30, 0, true), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece1.class, 10, 4), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece13.class, 10, 4), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece14.class, 10, 3), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece12.class, 5, 2), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece6.class, 5, 1)};
    static final WorldGenNetherPieces.WorldGenNetherPieceWeight[] CASTLE_PIECE_WEIGHTS = new WorldGenNetherPieces.WorldGenNetherPieceWeight[]{new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece9.class, 25, 0, true), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece7.class, 15, 5), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece10.class, 5, 10), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece8.class, 5, 10), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece4.class, 10, 3, true), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece5.class, 7, 2), new WorldGenNetherPieces.WorldGenNetherPieceWeight(WorldGenNetherPieces.WorldGenNetherPiece11.class, 5, 2)};

    static WorldGenNetherPieces.WorldGenNetherPiece findAndCreateBridgePieceFactory(WorldGenNetherPieces.WorldGenNetherPieceWeight pieceData, StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
        Class<? extends WorldGenNetherPieces.WorldGenNetherPiece> class_ = pieceData.pieceClass;
        WorldGenNetherPieces.WorldGenNetherPiece netherBridgePiece = null;
        if (class_ == WorldGenNetherPieces.WorldGenNetherPiece3.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece3.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece1.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece1.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece13.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece13.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece14.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece14.createPiece(structurePieceAccessor, x, y, z, chainLength, orientation);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece12.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece12.createPiece(structurePieceAccessor, x, y, z, chainLength, orientation);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece6.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece6.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece9.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece9.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece10.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece10.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece8.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece8.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece4.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece4.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece5.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece5.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece7.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece7.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        } else if (class_ == WorldGenNetherPieces.WorldGenNetherPiece11.class) {
            netherBridgePiece = WorldGenNetherPieces.WorldGenNetherPiece11.createPiece(structurePieceAccessor, x, y, z, orientation, chainLength);
        }

        return netherBridgePiece;
    }

    abstract static class WorldGenNetherPiece extends StructurePiece {
        protected WorldGenNetherPiece(WorldGenFeatureStructurePieceType type, int length, StructureBoundingBox boundingBox) {
            super(type, length, boundingBox);
        }

        public WorldGenNetherPiece(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
            super(type, nbt);
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
        }

        private int updatePieceWeight(List<WorldGenNetherPieces.WorldGenNetherPieceWeight> possiblePieces) {
            boolean bl = false;
            int i = 0;

            for(WorldGenNetherPieces.WorldGenNetherPieceWeight pieceWeight : possiblePieces) {
                if (pieceWeight.maxPlaceCount > 0 && pieceWeight.placeCount < pieceWeight.maxPlaceCount) {
                    bl = true;
                }

                i += pieceWeight.weight;
            }

            return bl ? i : -1;
        }

        private WorldGenNetherPieces.WorldGenNetherPiece generatePiece(WorldGenNetherPieces.WorldGenNetherPiece15 start, List<WorldGenNetherPieces.WorldGenNetherPieceWeight> possiblePieces, StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            int i = this.updatePieceWeight(possiblePieces);
            boolean bl = i > 0 && chainLength <= 30;
            int j = 0;

            while(j < 5 && bl) {
                ++j;
                int k = random.nextInt(i);

                for(WorldGenNetherPieces.WorldGenNetherPieceWeight pieceWeight : possiblePieces) {
                    k -= pieceWeight.weight;
                    if (k < 0) {
                        if (!pieceWeight.doPlace(chainLength) || pieceWeight == start.previousPiece && !pieceWeight.allowInRow) {
                            break;
                        }

                        WorldGenNetherPieces.WorldGenNetherPiece netherBridgePiece = WorldGenNetherPieces.findAndCreateBridgePieceFactory(pieceWeight, structurePieceAccessor, random, x, y, z, orientation, chainLength);
                        if (netherBridgePiece != null) {
                            ++pieceWeight.placeCount;
                            start.previousPiece = pieceWeight;
                            if (!pieceWeight.isValid()) {
                                possiblePieces.remove(pieceWeight);
                            }

                            return netherBridgePiece;
                        }
                    }
                }
            }

            return WorldGenNetherPieces.WorldGenNetherPiece2.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
        }

        private StructurePiece generateAndAddPiece(WorldGenNetherPieces.WorldGenNetherPiece15 start, StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, @Nullable EnumDirection orientation, int chainLength, boolean inside) {
            if (Math.abs(x - start.getBoundingBox().minX()) <= 112 && Math.abs(z - start.getBoundingBox().minZ()) <= 112) {
                List<WorldGenNetherPieces.WorldGenNetherPieceWeight> list = start.availableBridgePieces;
                if (inside) {
                    list = start.availableCastlePieces;
                }

                StructurePiece structurePiece = this.generatePiece(start, list, structurePieceAccessor, random, x, y, z, orientation, chainLength + 1);
                if (structurePiece != null) {
                    structurePieceAccessor.addPiece(structurePiece);
                    start.pendingChildren.add(structurePiece);
                }

                return structurePiece;
            } else {
                return WorldGenNetherPieces.WorldGenNetherPiece2.createPiece(structurePieceAccessor, random, x, y, z, orientation, chainLength);
            }
        }

        @Nullable
        protected StructurePiece generateChildForward(WorldGenNetherPieces.WorldGenNetherPiece15 start, StructurePieceAccessor structurePieceAccessor, Random random, int leftRightOffset, int heightOffset, boolean inside) {
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, direction, this.getGenDepth(), inside);
                case SOUTH:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, direction, this.getGenDepth(), inside);
                case WEST:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth(), inside);
                case EAST:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth(), inside);
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateChildLeft(WorldGenNetherPieces.WorldGenNetherPiece15 start, StructurePieceAccessor structurePieceAccessor, Random random, int heightOffset, int leftRightOffset, boolean inside) {
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.WEST, this.getGenDepth(), inside);
                case SOUTH:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.WEST, this.getGenDepth(), inside);
                case WEST:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, EnumDirection.NORTH, this.getGenDepth(), inside);
                case EAST:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, EnumDirection.NORTH, this.getGenDepth(), inside);
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateChildRight(WorldGenNetherPieces.WorldGenNetherPiece15 start, StructurePieceAccessor structurePieceAccessor, Random random, int heightOffset, int leftRightOffset, boolean inside) {
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.EAST, this.getGenDepth(), inside);
                case SOUTH:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, EnumDirection.EAST, this.getGenDepth(), inside);
                case WEST:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, this.getGenDepth(), inside);
                case EAST:
                    return this.generateAndAddPiece(start, structurePieceAccessor, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, this.getGenDepth(), inside);
                }
            }

            return null;
        }

        protected static boolean isOkBox(StructureBoundingBox boundingBox) {
            return boundingBox != null && boundingBox.minY() > 10;
        }
    }

    public static class WorldGenNetherPiece1 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 19;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public WorldGenNetherPiece1(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        protected WorldGenNetherPiece1(int x, int z, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(x, 64, z, orientation, 19, 10, 19));
            this.setOrientation(orientation);
        }

        protected WorldGenNetherPiece1(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
            super(type, nbt);
        }

        public WorldGenNetherPiece1(WorldServer world, NBTTagCompound nbt) {
            this(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 8, 3, false);
            this.generateChildLeft((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 3, 8, false);
            this.generateChildRight((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 3, 8, false);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece1 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -8, -3, 0, 19, 10, 19, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece1(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 0, 10, 7, 18, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 8, 18, 7, 10, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int i = 7; i <= 11; ++i) {
                for(int j = 0; j <= 2; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, 18 - j, boundingBox);
                }
            }

            this.generateBox(world, boundingBox, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int k = 0; k <= 2; ++k) {
                for(int l = 7; l <= 11; ++l) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), k, -1, l, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), 18 - k, -1, l, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece10 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public WorldGenNetherPiece10(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.isNeedingChest = random.nextInt(3) == 0;
        }

        public WorldGenNetherPiece10(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, nbt);
            this.isNeedingChest = nbt.getBoolean("Chest");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildRight((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, 1, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece10 createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece10(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 3, 1, 0, 4, 1, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 0, 3, 3, 0, 4, 3, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 3, 4, 1, 4, 4, blockState, blockState, false);
            this.generateBox(world, boundingBox, 3, 3, 4, 3, 4, 4, blockState, blockState, false);
            if (this.isNeedingChest && boundingBox.isInside(this.getWorldPos(1, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(world, boundingBox, random, 1, 2, 3, LootTables.NETHER_BRIDGE);
            }

            this.generateBox(world, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece11 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public WorldGenNetherPiece11(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece11(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 5, 3, true);
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 5, 11, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece11 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainlength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -5, -3, 0, 13, 14, 13, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece11(chainlength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            IBlockData blockState3 = blockState2.set(BlockFence.WEST, Boolean.valueOf(true));
            IBlockData blockState4 = blockState2.set(BlockFence.EAST, Boolean.valueOf(true));

            for(int i = 1; i <= 11; i += 2) {
                this.generateBox(world, boundingBox, i, 10, 0, i, 11, 0, blockState, blockState, false);
                this.generateBox(world, boundingBox, i, 10, 12, i, 11, 12, blockState, blockState, false);
                this.generateBox(world, boundingBox, 0, 10, i, 0, 11, i, blockState2, blockState2, false);
                this.generateBox(world, boundingBox, 12, 10, i, 12, 11, i, blockState2, blockState2, false);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), i, 13, 0, boundingBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), i, 13, 12, boundingBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), 0, 13, i, boundingBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), 12, 13, i, boundingBox);
                if (i != 11) {
                    this.placeBlock(world, blockState, i + 1, 13, 0, boundingBox);
                    this.placeBlock(world, blockState, i + 1, 13, 12, boundingBox);
                    this.placeBlock(world, blockState2, 0, 13, i + 1, boundingBox);
                    this.placeBlock(world, blockState2, 12, 13, i + 1, boundingBox);
                }
            }

            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 0, 13, 0, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.SOUTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 0, 13, 12, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.SOUTH, Boolean.valueOf(true)).set(BlockFence.WEST, Boolean.valueOf(true)), 12, 13, 12, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.WEST, Boolean.valueOf(true)), 12, 13, 0, boundingBox);

            for(int j = 3; j <= 9; j += 2) {
                this.generateBox(world, boundingBox, 1, 7, j, 1, 8, j, blockState3, blockState3, false);
                this.generateBox(world, boundingBox, 11, 7, j, 11, 8, j, blockState4, blockState4, false);
            }

            IBlockData blockState5 = Blocks.NETHER_BRICK_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.NORTH);

            for(int k = 0; k <= 6; ++k) {
                int l = k + 4;

                for(int m = 5; m <= 7; ++m) {
                    this.placeBlock(world, blockState5, m, 5 + k, l, boundingBox);
                }

                if (l >= 5 && l <= 8) {
                    this.generateBox(world, boundingBox, 5, 5, l, 7, k + 4, l, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                } else if (l >= 9 && l <= 10) {
                    this.generateBox(world, boundingBox, 5, 8, l, 7, k + 4, l, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                }

                if (k >= 1) {
                    this.generateBox(world, boundingBox, 5, 6 + k, l, 7, 9 + k, l, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
                }
            }

            for(int n = 5; n <= 7; ++n) {
                this.placeBlock(world, blockState5, n, 12, 11, boundingBox);
            }

            this.generateBox(world, boundingBox, 5, 6, 7, 5, 7, 7, blockState4, blockState4, false);
            this.generateBox(world, boundingBox, 7, 6, 7, 7, 7, 7, blockState3, blockState3, false);
            this.generateBox(world, boundingBox, 5, 13, 12, 7, 13, 12, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            IBlockData blockState6 = blockState5.set(BlockStairs.FACING, EnumDirection.EAST);
            IBlockData blockState7 = blockState5.set(BlockStairs.FACING, EnumDirection.WEST);
            this.placeBlock(world, blockState7, 4, 5, 2, boundingBox);
            this.placeBlock(world, blockState7, 4, 5, 3, boundingBox);
            this.placeBlock(world, blockState7, 4, 5, 9, boundingBox);
            this.placeBlock(world, blockState7, 4, 5, 10, boundingBox);
            this.placeBlock(world, blockState6, 8, 5, 2, boundingBox);
            this.placeBlock(world, blockState6, 8, 5, 3, boundingBox);
            this.placeBlock(world, blockState6, 8, 5, 9, boundingBox);
            this.placeBlock(world, blockState6, 8, 5, 10, boundingBox);
            this.generateBox(world, boundingBox, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.getBlockData(), Blocks.SOUL_SAND.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.getBlockData(), Blocks.SOUL_SAND.getBlockData(), false);
            this.generateBox(world, boundingBox, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.getBlockData(), Blocks.NETHER_WART.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.getBlockData(), Blocks.NETHER_WART.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int o = 4; o <= 8; ++o) {
                for(int p = 0; p <= 2; ++p) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), o, -1, p, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), o, -1, 12 - p, boundingBox);
                }
            }

            for(int q = 0; q <= 2; ++q) {
                for(int r = 4; r <= 8; ++r) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), q, -1, r, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), 12 - q, -1, r, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece12 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 8;
        private static final int DEPTH = 9;
        private boolean hasPlacedSpawner;

        public WorldGenNetherPiece12(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece12(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, nbt);
            this.hasPlacedSpawner = nbt.getBoolean("Mob");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Mob", this.hasPlacedSpawner);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece12 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, int chainLength, EnumDirection orientation) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -2, 0, 0, 7, 8, 9, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece12(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 2, 0, 6, 7, 7, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)), 1, 6, 3, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true)), 5, 6, 3, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true)).set(BlockFence.NORTH, Boolean.valueOf(true)), 0, 6, 3, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.NORTH, Boolean.valueOf(true)), 6, 6, 3, boundingBox);
            this.generateBox(world, boundingBox, 0, 6, 4, 0, 6, 7, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 6, 6, 4, 6, 6, 7, blockState2, blockState2, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true)), 0, 6, 8, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true)), 6, 6, 8, boundingBox);
            this.generateBox(world, boundingBox, 1, 6, 8, 5, 6, 8, blockState, blockState, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true)), 1, 7, 8, boundingBox);
            this.generateBox(world, boundingBox, 2, 7, 8, 4, 7, 8, blockState, blockState, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)), 5, 7, 8, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true)), 2, 8, 8, boundingBox);
            this.placeBlock(world, blockState, 3, 8, 8, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)), 4, 8, 8, boundingBox);
            if (!this.hasPlacedSpawner) {
                BlockPosition blockPos = this.getWorldPos(3, 5, 5);
                if (boundingBox.isInside(blockPos)) {
                    this.hasPlacedSpawner = true;
                    world.setTypeAndData(blockPos, Blocks.SPAWNER.getBlockData(), 2);
                    TileEntity blockEntity = world.getTileEntity(blockPos);
                    if (blockEntity instanceof TileEntityMobSpawner) {
                        ((TileEntityMobSpawner)blockEntity).getSpawner().setMobName(EntityTypes.BLAZE);
                    }
                }
            }

            for(int i = 0; i <= 6; ++i) {
                for(int j = 0; j <= 6; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece13 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 9;
        private static final int DEPTH = 7;

        public WorldGenNetherPiece13(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece13(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 2, 0, false);
            this.generateChildLeft((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, 2, false);
            this.generateChildRight((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, 2, false);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece13 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -2, 0, 0, 7, 9, 7, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece13(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 6, 7, 6, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 0, 4, 5, 0, blockState, blockState, false);
            this.generateBox(world, boundingBox, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 6, 4, 5, 6, blockState, blockState, false);
            this.generateBox(world, boundingBox, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 2, 0, 5, 4, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 5, 2, 6, 5, 4, blockState2, blockState2, false);

            for(int i = 0; i <= 6; ++i) {
                for(int j = 0; j <= 6; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece14 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 7;

        public WorldGenNetherPiece14(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece14(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildRight((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 6, 2, false);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece14 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, int chainlength, EnumDirection orientation) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -2, 0, 0, 7, 11, 7, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece14(chainlength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 6, 10, 6, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 0, 3, 2, 0, 5, 4, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 6, 3, 2, 6, 5, 2, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 6, 3, 4, 6, 5, 4, blockState2, blockState2, false);
            this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), 5, 2, 5, boundingBox);
            this.generateBox(world, boundingBox, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 8, 2, 6, 8, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 0, 4, 5, 0, blockState, blockState, false);

            for(int i = 0; i <= 6; ++i) {
                for(int j = 0; j <= 6; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece15 extends WorldGenNetherPieces.WorldGenNetherPiece1 {
        public WorldGenNetherPieces.WorldGenNetherPieceWeight previousPiece;
        public List<WorldGenNetherPieces.WorldGenNetherPieceWeight> availableBridgePieces;
        public List<WorldGenNetherPieces.WorldGenNetherPieceWeight> availableCastlePieces;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public WorldGenNetherPiece15(Random random, int x, int z) {
            super(x, z, getRandomHorizontalDirection(random));
            this.availableBridgePieces = Lists.newArrayList();

            for(WorldGenNetherPieces.WorldGenNetherPieceWeight pieceWeight : WorldGenNetherPieces.BRIDGE_PIECE_WEIGHTS) {
                pieceWeight.placeCount = 0;
                this.availableBridgePieces.add(pieceWeight);
            }

            this.availableCastlePieces = Lists.newArrayList();

            for(WorldGenNetherPieces.WorldGenNetherPieceWeight pieceWeight2 : WorldGenNetherPieces.CASTLE_PIECE_WEIGHTS) {
                pieceWeight2.placeCount = 0;
                this.availableCastlePieces.add(pieceWeight2);
            }

        }

        public WorldGenNetherPiece15(WorldServer serverLevel, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_START, nbt);
        }
    }

    public static class WorldGenNetherPiece2 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 8;
        private final int selfSeed;

        public WorldGenNetherPiece2(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.selfSeed = random.nextInt();
        }

        public WorldGenNetherPiece2(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, nbt);
            this.selfSeed = nbt.getInt("Seed");
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece2 createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -3, 0, 5, 10, 8, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece2(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setInt("Seed", this.selfSeed);
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            Random random2 = new Random((long)this.selfSeed);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 3; j <= 4; ++j) {
                    int k = random2.nextInt(8);
                    this.generateBox(world, boundingBox, i, j, 0, i, j, k, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                }
            }

            int l = random2.nextInt(8);
            this.generateBox(world, boundingBox, 0, 5, 0, 0, 5, l, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            l = random2.nextInt(8);
            this.generateBox(world, boundingBox, 4, 5, 0, 4, 5, l, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int n = 0; n <= 4; ++n) {
                int o = random2.nextInt(5);
                this.generateBox(world, boundingBox, n, 2, 0, n, 2, o, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            }

            for(int p = 0; p <= 4; ++p) {
                for(int q = 0; q <= 1; ++q) {
                    int r = random2.nextInt(3);
                    this.generateBox(world, boundingBox, p, q, 0, p, q, r, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece3 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public WorldGenNetherPiece3(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece3(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 1, 3, false);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece3 createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -3, 0, 5, 10, 19, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece3(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 5, 0, 3, 7, 18, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 2; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, 18 - j, boundingBox);
                }
            }

            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            IBlockData blockState2 = blockState.set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState3 = blockState.set(BlockFence.WEST, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 0, 1, 1, 0, 4, 1, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 0, 3, 4, 0, 4, 4, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 0, 3, 14, 0, 4, 14, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 0, 1, 17, 0, 4, 17, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 4, 1, 1, 4, 4, 1, blockState3, blockState3, false);
            this.generateBox(world, boundingBox, 4, 3, 4, 4, 4, 4, blockState3, blockState3, false);
            this.generateBox(world, boundingBox, 4, 3, 14, 4, 4, 14, blockState3, blockState3, false);
            this.generateBox(world, boundingBox, 4, 1, 17, 4, 4, 17, blockState3, blockState3, false);
            return true;
        }
    }

    public static class WorldGenNetherPiece4 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 10;

        public WorldGenNetherPiece4(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece4(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 1, 0, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece4 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, -7, 0, 5, 14, 10, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece4(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            IBlockData blockState = Blocks.NETHER_BRICK_STAIRS.getBlockData().set(BlockStairs.FACING, EnumDirection.SOUTH);
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));

            for(int i = 0; i <= 9; ++i) {
                int j = Math.max(1, 7 - i);
                int k = Math.min(Math.max(j + 5, 14 - i), 13);
                int l = i;
                this.generateBox(world, boundingBox, 0, 0, i, 4, j, i, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                this.generateBox(world, boundingBox, 1, j + 1, i, 3, k - 1, i, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
                if (i <= 6) {
                    this.placeBlock(world, blockState, 1, j + 1, i, boundingBox);
                    this.placeBlock(world, blockState, 2, j + 1, i, boundingBox);
                    this.placeBlock(world, blockState, 3, j + 1, i, boundingBox);
                }

                this.generateBox(world, boundingBox, 0, k, i, 4, k, i, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                this.generateBox(world, boundingBox, 0, j + 1, i, 0, k - 1, i, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                this.generateBox(world, boundingBox, 4, j + 1, i, 4, k - 1, i, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
                if ((i & 1) == 0) {
                    this.generateBox(world, boundingBox, 0, j + 2, i, 0, j + 3, i, blockState2, blockState2, false);
                    this.generateBox(world, boundingBox, 4, j + 2, i, 4, j + 3, i, blockState2, blockState2, false);
                }

                for(int m = 0; m <= 4; ++m) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), m, -1, l, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece5 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 9;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 9;

        public WorldGenNetherPiece5(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece5(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            int i = 1;
            EnumDirection direction = this.getOrientation();
            if (direction == EnumDirection.WEST || direction == EnumDirection.NORTH) {
                i = 5;
            }

            this.generateChildLeft((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, i, random.nextInt(8) > 0);
            this.generateChildRight((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, i, random.nextInt(8) > 0);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece5 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -3, 0, 0, 9, 7, 9, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece5(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 8, 5, 8, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 3, 0, 1, 4, 0, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 7, 3, 0, 7, 4, 0, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 1, 4, 2, 2, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 1, 4, 7, 2, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 3, 8, 7, 3, 8, blockState2, blockState2, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.EAST, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true)), 0, 3, 8, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true)), 8, 3, 8, boundingBox);
            this.generateBox(world, boundingBox, 0, 3, 6, 0, 3, 7, blockState, blockState, false);
            this.generateBox(world, boundingBox, 8, 3, 6, 8, 3, 7, blockState, blockState, false);
            this.generateBox(world, boundingBox, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 4, 5, 1, 5, 5, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 7, 4, 5, 7, 5, 5, blockState2, blockState2, false);

            for(int i = 0; i <= 5; ++i) {
                for(int j = 0; j <= 8; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), j, -1, i, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece6 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public WorldGenNetherPiece6(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece6(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 5, 3, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece6 createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -5, -3, 0, 13, 14, 13, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece6(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.getBlockData(), Blocks.NETHER_BRICK_FENCE.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));

            for(int i = 1; i <= 11; i += 2) {
                this.generateBox(world, boundingBox, i, 10, 0, i, 11, 0, blockState, blockState, false);
                this.generateBox(world, boundingBox, i, 10, 12, i, 11, 12, blockState, blockState, false);
                this.generateBox(world, boundingBox, 0, 10, i, 0, 11, i, blockState2, blockState2, false);
                this.generateBox(world, boundingBox, 12, 10, i, 12, 11, i, blockState2, blockState2, false);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), i, 13, 0, boundingBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), i, 13, 12, boundingBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), 0, 13, i, boundingBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), 12, 13, i, boundingBox);
                if (i != 11) {
                    this.placeBlock(world, blockState, i + 1, 13, 0, boundingBox);
                    this.placeBlock(world, blockState, i + 1, 13, 12, boundingBox);
                    this.placeBlock(world, blockState2, 0, 13, i + 1, boundingBox);
                    this.placeBlock(world, blockState2, 12, 13, i + 1, boundingBox);
                }
            }

            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 0, 13, 0, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.SOUTH, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true)), 0, 13, 12, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.SOUTH, Boolean.valueOf(true)).set(BlockFence.WEST, Boolean.valueOf(true)), 12, 13, 12, boundingBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.WEST, Boolean.valueOf(true)), 12, 13, 0, boundingBox);

            for(int j = 3; j <= 9; j += 2) {
                this.generateBox(world, boundingBox, 1, 7, j, 1, 8, j, blockState2.set(BlockFence.WEST, Boolean.valueOf(true)), blockState2.set(BlockFence.WEST, Boolean.valueOf(true)), false);
                this.generateBox(world, boundingBox, 11, 7, j, 11, 8, j, blockState2.set(BlockFence.EAST, Boolean.valueOf(true)), blockState2.set(BlockFence.EAST, Boolean.valueOf(true)), false);
            }

            this.generateBox(world, boundingBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int k = 4; k <= 8; ++k) {
                for(int l = 0; l <= 2; ++l) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), k, -1, l, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), k, -1, 12 - l, boundingBox);
                }
            }

            for(int m = 0; m <= 2; ++m) {
                for(int n = 4; n <= 8; ++n) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), m, -1, n, boundingBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), 12 - m, -1, n, boundingBox);
                }
            }

            this.generateBox(world, boundingBox, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 6, 1, 6, 6, 4, 6, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.placeBlock(world, Blocks.NETHER_BRICKS.getBlockData(), 6, 0, 6, boundingBox);
            this.placeBlock(world, Blocks.LAVA.getBlockData(), 6, 5, 6, boundingBox);
            BlockPosition blockPos = this.getWorldPos(6, 5, 6);
            if (boundingBox.isInside(blockPos)) {
                world.getFluidTickList().scheduleTick(blockPos, FluidTypes.LAVA, 0);
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece7 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public WorldGenNetherPiece7(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece7(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 1, 0, true);
            this.generateChildLeft((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, 1, true);
            this.generateChildRight((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, 1, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece7 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece7(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece8 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public WorldGenNetherPiece8(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.isNeedingChest = random.nextInt(3) == 0;
        }

        public WorldGenNetherPiece8(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, nbt);
            this.isNeedingChest = nbt.getBoolean("Chest");
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildLeft((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 0, 1, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece8 createPiece(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece8(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.WEST, Boolean.valueOf(true)).set(BlockFence.EAST, Boolean.valueOf(true));
            IBlockData blockState2 = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 3, 1, 4, 4, 1, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 4, 3, 3, 4, 4, 3, blockState2, blockState2, false);
            this.generateBox(world, boundingBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 1, 3, 4, 1, 4, 4, blockState, blockState, false);
            this.generateBox(world, boundingBox, 3, 3, 4, 3, 4, 4, blockState, blockState, false);
            if (this.isNeedingChest && boundingBox.isInside(this.getWorldPos(3, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(world, boundingBox, random, 3, 2, 3, LootTables.NETHER_BRIDGE);
            }

            this.generateBox(world, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    public static class WorldGenNetherPiece9 extends WorldGenNetherPieces.WorldGenNetherPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public WorldGenNetherPiece9(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenNetherPiece9(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor structurePieceAccessor, Random random) {
            this.generateChildForward((WorldGenNetherPieces.WorldGenNetherPiece15)start, structurePieceAccessor, random, 1, 0, true);
        }

        public static WorldGenNetherPieces.WorldGenNetherPiece9 createPiece(StructurePieceAccessor structurePieceAccessor, int x, int y, int z, EnumDirection orientation, int chainLength) {
            StructureBoundingBox boundingBox = StructureBoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null ? new WorldGenNetherPieces.WorldGenNetherPiece9(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.generateBox(world, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.getBlockData(), Blocks.AIR.getBlockData(), false);
            IBlockData blockState = Blocks.NETHER_BRICK_FENCE.getBlockData().set(BlockFence.NORTH, Boolean.valueOf(true)).set(BlockFence.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, boundingBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);
            this.generateBox(world, boundingBox, 0, 3, 1, 0, 4, 1, blockState, blockState, false);
            this.generateBox(world, boundingBox, 0, 3, 3, 0, 4, 3, blockState, blockState, false);
            this.generateBox(world, boundingBox, 4, 3, 1, 4, 4, 1, blockState, blockState, false);
            this.generateBox(world, boundingBox, 4, 3, 3, 4, 4, 3, blockState, blockState, false);
            this.generateBox(world, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.getBlockData(), Blocks.NETHER_BRICKS.getBlockData(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.getBlockData(), i, -1, j, boundingBox);
                }
            }

            return true;
        }
    }

    static class WorldGenNetherPieceWeight {
        public final Class<? extends WorldGenNetherPieces.WorldGenNetherPiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;
        public final boolean allowInRow;

        public WorldGenNetherPieceWeight(Class<? extends WorldGenNetherPieces.WorldGenNetherPiece> pieceType, int weight, int limit, boolean repeatable) {
            this.pieceClass = pieceType;
            this.weight = weight;
            this.maxPlaceCount = limit;
            this.allowInRow = repeatable;
        }

        public WorldGenNetherPieceWeight(Class<? extends WorldGenNetherPieces.WorldGenNetherPiece> pieceType, int weight, int limit) {
            this(pieceType, weight, limit, false);
        }

        public boolean doPlace(int chainLength) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }
}
