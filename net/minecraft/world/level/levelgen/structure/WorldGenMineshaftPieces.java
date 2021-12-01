package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.EntityMinecartChest;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFalling;
import net.minecraft.world.level.block.BlockFence;
import net.minecraft.world.level.block.BlockMinecartTrack;
import net.minecraft.world.level.block.BlockTorchWall;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.feature.WorldGenMineshaft;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenMineshaftPieces {
    static final Logger LOGGER = LogManager.getLogger();
    private static final int DEFAULT_SHAFT_WIDTH = 3;
    private static final int DEFAULT_SHAFT_HEIGHT = 3;
    private static final int DEFAULT_SHAFT_LENGTH = 5;
    private static final int MAX_PILLAR_HEIGHT = 20;
    private static final int MAX_CHAIN_HEIGHT = 50;
    private static final int MAX_DEPTH = 8;
    public static final int MAGIC_START_Y = 50;

    private static WorldGenMineshaftPieces.MineShaftPiece createRandomShaftPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, @Nullable EnumDirection orientation, int chainLength, WorldGenMineshaft.Type type) {
        int i = random.nextInt(100);
        if (i >= 80) {
            StructureBoundingBox boundingBox = WorldGenMineshaftPieces.WorldGenMineshaftCross.findCrossing(holder, random, x, y, z, orientation);
            if (boundingBox != null) {
                return new WorldGenMineshaftPieces.WorldGenMineshaftCross(chainLength, boundingBox, orientation, type);
            }
        } else if (i >= 70) {
            StructureBoundingBox boundingBox2 = WorldGenMineshaftPieces.WorldGenMineshaftStairs.findStairs(holder, random, x, y, z, orientation);
            if (boundingBox2 != null) {
                return new WorldGenMineshaftPieces.WorldGenMineshaftStairs(chainLength, boundingBox2, orientation, type);
            }
        } else {
            StructureBoundingBox boundingBox3 = WorldGenMineshaftPieces.WorldGenMineshaftCorridor.findCorridorSize(holder, random, x, y, z, orientation);
            if (boundingBox3 != null) {
                return new WorldGenMineshaftPieces.WorldGenMineshaftCorridor(chainLength, random, boundingBox3, orientation, type);
            }
        }

        return null;
    }

    static WorldGenMineshaftPieces.MineShaftPiece generateAndAddPiece(StructurePiece start, StructurePieceAccessor holder, Random random, int x, int y, int z, EnumDirection orientation, int chainLength) {
        if (chainLength > 8) {
            return null;
        } else if (Math.abs(x - start.getBoundingBox().minX()) <= 80 && Math.abs(z - start.getBoundingBox().minZ()) <= 80) {
            WorldGenMineshaft.Type type = ((WorldGenMineshaftPieces.MineShaftPiece)start).type;
            WorldGenMineshaftPieces.MineShaftPiece mineShaftPiece = createRandomShaftPiece(holder, random, x, y, z, orientation, chainLength + 1, type);
            if (mineShaftPiece != null) {
                holder.addPiece(mineShaftPiece);
                mineShaftPiece.addChildren(start, holder, random);
            }

            return mineShaftPiece;
        } else {
            return null;
        }
    }

    abstract static class MineShaftPiece extends StructurePiece {
        protected WorldGenMineshaft.Type type;

        public MineShaftPiece(WorldGenFeatureStructurePieceType structurePieceType, int chainLength, WorldGenMineshaft.Type type, StructureBoundingBox box) {
            super(structurePieceType, chainLength, box);
            this.type = type;
        }

        public MineShaftPiece(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
            super(type, nbt);
            this.type = WorldGenMineshaft.Type.byId(nbt.getInt("MST"));
        }

        @Override
        protected boolean canBeReplaced(IWorldReader world, int x, int y, int z, StructureBoundingBox box) {
            IBlockData blockState = this.getBlock(world, x, y, z, box);
            return !blockState.is(this.type.getPlanksState().getBlock()) && !blockState.is(this.type.getWoodState().getBlock()) && !blockState.is(this.type.getFenceState().getBlock()) && !blockState.is(Blocks.CHAIN);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            nbt.setInt("MST", this.type.ordinal());
        }

        protected boolean isSupportingBox(IBlockAccess world, StructureBoundingBox boundingBox, int minX, int maxX, int y, int z) {
            for(int i = minX; i <= maxX; ++i) {
                if (this.getBlock(world, i, y + 1, z, boundingBox).isAir()) {
                    return false;
                }
            }

            return true;
        }

        protected boolean edgesLiquid(IBlockAccess world, StructureBoundingBox box) {
            int i = Math.max(this.boundingBox.minX() - 1, box.minX());
            int j = Math.max(this.boundingBox.minY() - 1, box.minY());
            int k = Math.max(this.boundingBox.minZ() - 1, box.minZ());
            int l = Math.min(this.boundingBox.maxX() + 1, box.maxX());
            int m = Math.min(this.boundingBox.maxY() + 1, box.maxY());
            int n = Math.min(this.boundingBox.maxZ() + 1, box.maxZ());
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int o = i; o <= l; ++o) {
                for(int p = k; p <= n; ++p) {
                    if (world.getType(mutableBlockPos.set(o, j, p)).getMaterial().isLiquid()) {
                        return true;
                    }

                    if (world.getType(mutableBlockPos.set(o, m, p)).getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }

            for(int q = i; q <= l; ++q) {
                for(int r = j; r <= m; ++r) {
                    if (world.getType(mutableBlockPos.set(q, r, k)).getMaterial().isLiquid()) {
                        return true;
                    }

                    if (world.getType(mutableBlockPos.set(q, r, n)).getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }

            for(int s = k; s <= n; ++s) {
                for(int t = j; t <= m; ++t) {
                    if (world.getType(mutableBlockPos.set(i, t, s)).getMaterial().isLiquid()) {
                        return true;
                    }

                    if (world.getType(mutableBlockPos.set(l, t, s)).getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }

            return false;
        }

        protected void setPlanksBlock(GeneratorAccessSeed world, StructureBoundingBox box, IBlockData state, int x, int y, int z) {
            if (this.isInterior(world, x, y, z, box)) {
                BlockPosition blockPos = this.getWorldPos(x, y, z);
                IBlockData blockState = world.getType(blockPos);
                if (blockState.isAir() || blockState.is(Blocks.CHAIN)) {
                    world.setTypeAndData(blockPos, state, 2);
                }

            }
        }
    }

    public static class WorldGenMineshaftCorridor extends WorldGenMineshaftPieces.MineShaftPiece {
        private final boolean hasRails;
        private final boolean spiderCorridor;
        private boolean hasPlacedSpider;
        private final int numSections;

        public WorldGenMineshaftCorridor(NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_CORRIDOR, nbt);
            this.hasRails = nbt.getBoolean("hr");
            this.spiderCorridor = nbt.getBoolean("sc");
            this.hasPlacedSpider = nbt.getBoolean("hps");
            this.numSections = nbt.getInt("Num");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.setBoolean("hr", this.hasRails);
            nbt.setBoolean("sc", this.spiderCorridor);
            nbt.setBoolean("hps", this.hasPlacedSpider);
            nbt.setInt("Num", this.numSections);
        }

        public WorldGenMineshaftCorridor(int chainLength, Random random, StructureBoundingBox boundingBox, EnumDirection orientation, WorldGenMineshaft.Type type) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_CORRIDOR, chainLength, type, boundingBox);
            this.setOrientation(orientation);
            this.hasRails = random.nextInt(3) == 0;
            this.spiderCorridor = !this.hasRails && random.nextInt(23) == 0;
            if (this.getOrientation().getAxis() == EnumDirection.EnumAxis.Z) {
                this.numSections = boundingBox.getZSpan() / 5;
            } else {
                this.numSections = boundingBox.getXSpan() / 5;
            }

        }

        @Nullable
        public static StructureBoundingBox findCorridorSize(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, EnumDirection orientation) {
            for(int i = random.nextInt(3) + 2; i > 0; --i) {
                int j = i * 5;
                StructureBoundingBox boundingBox;
                switch(orientation) {
                case NORTH:
                default:
                    boundingBox = new StructureBoundingBox(0, 0, -(j - 1), 2, 2, 0);
                    break;
                case SOUTH:
                    boundingBox = new StructureBoundingBox(0, 0, 0, 2, 2, j - 1);
                    break;
                case WEST:
                    boundingBox = new StructureBoundingBox(-(j - 1), 0, 0, 0, 2, 2);
                    break;
                case EAST:
                    boundingBox = new StructureBoundingBox(0, 0, 0, j - 1, 2, 2);
                }

                boundingBox.move(x, y, z);
                if (structurePieceAccessor.findCollisionPiece(boundingBox) == null) {
                    return boundingBox;
                }
            }

            return null;
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            int j = random.nextInt(4);
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                default:
                    if (j <= 1) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, direction, i);
                    } else if (j == 2) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), EnumDirection.WEST, i);
                    } else {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), EnumDirection.EAST, i);
                    }
                    break;
                case SOUTH:
                    if (j <= 1) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, direction, i);
                    } else if (j == 2) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() - 3, EnumDirection.WEST, i);
                    } else {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() - 3, EnumDirection.EAST, i);
                    }
                    break;
                case WEST:
                    if (j <= 1) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), direction, i);
                    } else if (j == 2) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                    } else {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                    }
                    break;
                case EAST:
                    if (j <= 1) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), direction, i);
                    } else if (j == 2) {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() - 3, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                    } else {
                        WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() - 3, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                    }
                }
            }

            if (i < 8) {
                if (direction != EnumDirection.NORTH && direction != EnumDirection.SOUTH) {
                    for(int m = this.boundingBox.minX() + 3; m + 3 <= this.boundingBox.maxX(); m += 5) {
                        int n = random.nextInt(5);
                        if (n == 0) {
                            WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, m, this.boundingBox.minY(), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i + 1);
                        } else if (n == 1) {
                            WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, m, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i + 1);
                        }
                    }
                } else {
                    for(int k = this.boundingBox.minZ() + 3; k + 3 <= this.boundingBox.maxZ(); k += 5) {
                        int l = random.nextInt(5);
                        if (l == 0) {
                            WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), k, EnumDirection.WEST, i + 1);
                        } else if (l == 1) {
                            WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), k, EnumDirection.EAST, i + 1);
                        }
                    }
                }
            }

        }

        @Override
        protected boolean createChest(GeneratorAccessSeed world, StructureBoundingBox boundingBox, Random random, int x, int y, int z, MinecraftKey lootTableId) {
            BlockPosition blockPos = this.getWorldPos(x, y, z);
            if (boundingBox.isInside(blockPos) && world.getType(blockPos).isAir() && !world.getType(blockPos.below()).isAir()) {
                IBlockData blockState = Blocks.RAIL.getBlockData().set(BlockMinecartTrack.SHAPE, random.nextBoolean() ? BlockPropertyTrackPosition.NORTH_SOUTH : BlockPropertyTrackPosition.EAST_WEST);
                this.placeBlock(world, blockState, x, y, z, boundingBox);
                EntityMinecartChest minecartChest = new EntityMinecartChest(world.getLevel(), (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D);
                minecartChest.setLootTable(lootTableId, random.nextLong());
                world.addEntity(minecartChest);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                int i = 0;
                int j = 2;
                int k = 0;
                int l = 2;
                int m = this.numSections * 5 - 1;
                IBlockData blockState = this.type.getPlanksState();
                this.generateBox(world, chunkBox, 0, 0, 0, 2, 1, m, CAVE_AIR, CAVE_AIR, false);
                this.generateMaybeBox(world, chunkBox, random, 0.8F, 0, 2, 0, 2, 2, m, CAVE_AIR, CAVE_AIR, false, false);
                if (this.spiderCorridor) {
                    this.generateMaybeBox(world, chunkBox, random, 0.6F, 0, 0, 0, 2, 1, m, Blocks.COBWEB.getBlockData(), CAVE_AIR, false, true);
                }

                for(int n = 0; n < this.numSections; ++n) {
                    int o = 2 + n * 5;
                    this.placeSupport(world, chunkBox, 0, 0, o, 2, 2, random);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 0, 2, o - 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 2, 2, o - 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 0, 2, o + 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 2, 2, o + 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 0, 2, o - 2);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 2, 2, o - 2);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 0, 2, o + 2);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 2, 2, o + 2);
                    if (random.nextInt(100) == 0) {
                        this.createChest(world, chunkBox, random, 2, 0, o - 1, LootTables.ABANDONED_MINESHAFT);
                    }

                    if (random.nextInt(100) == 0) {
                        this.createChest(world, chunkBox, random, 0, 0, o + 1, LootTables.ABANDONED_MINESHAFT);
                    }

                    if (this.spiderCorridor && !this.hasPlacedSpider) {
                        int p = 1;
                        int q = o - 1 + random.nextInt(3);
                        BlockPosition blockPos = this.getWorldPos(1, 0, q);
                        if (chunkBox.isInside(blockPos) && this.isInterior(world, 1, 0, q, chunkBox)) {
                            this.hasPlacedSpider = true;
                            world.setTypeAndData(blockPos, Blocks.SPAWNER.getBlockData(), 2);
                            TileEntity blockEntity = world.getTileEntity(blockPos);
                            if (blockEntity instanceof TileEntityMobSpawner) {
                                ((TileEntityMobSpawner)blockEntity).getSpawner().setMobName(EntityTypes.CAVE_SPIDER);
                            }
                        }
                    }
                }

                for(int r = 0; r <= 2; ++r) {
                    for(int s = 0; s <= m; ++s) {
                        this.setPlanksBlock(world, chunkBox, blockState, r, -1, s);
                    }
                }

                int t = 2;
                this.placeDoubleLowerOrUpperSupport(world, chunkBox, 0, -1, 2);
                if (this.numSections > 1) {
                    int u = m - 2;
                    this.placeDoubleLowerOrUpperSupport(world, chunkBox, 0, -1, u);
                }

                if (this.hasRails) {
                    IBlockData blockState2 = Blocks.RAIL.getBlockData().set(BlockMinecartTrack.SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);

                    for(int v = 0; v <= m; ++v) {
                        IBlockData blockState3 = this.getBlock(world, 1, -1, v, chunkBox);
                        if (!blockState3.isAir() && blockState3.isSolidRender(world, this.getWorldPos(1, -1, v))) {
                            float f = this.isInterior(world, 1, 0, v, chunkBox) ? 0.7F : 0.9F;
                            this.maybeGenerateBlock(world, chunkBox, random, f, 1, 0, v, blockState2);
                        }
                    }
                }

            }
        }

        private void placeDoubleLowerOrUpperSupport(GeneratorAccessSeed world, StructureBoundingBox box, int x, int y, int z) {
            IBlockData blockState = this.type.getWoodState();
            IBlockData blockState2 = this.type.getPlanksState();
            if (this.getBlock(world, x, y, z, box).is(blockState2.getBlock())) {
                this.fillPillarDownOrChainUp(world, blockState, x, y, z, box);
            }

            if (this.getBlock(world, x + 2, y, z, box).is(blockState2.getBlock())) {
                this.fillPillarDownOrChainUp(world, blockState, x + 2, y, z, box);
            }

        }

        @Override
        protected void fillColumnDown(GeneratorAccessSeed world, IBlockData state, int x, int y, int z, StructureBoundingBox box) {
            BlockPosition.MutableBlockPosition mutableBlockPos = this.getWorldPos(x, y, z);
            if (box.isInside(mutableBlockPos)) {
                int i = mutableBlockPos.getY();

                while(this.isReplaceableByStructures(world.getType(mutableBlockPos)) && mutableBlockPos.getY() > world.getMinBuildHeight() + 1) {
                    mutableBlockPos.move(EnumDirection.DOWN);
                }

                if (this.canPlaceColumnOnTopOf(world.getType(mutableBlockPos))) {
                    while(mutableBlockPos.getY() < i) {
                        mutableBlockPos.move(EnumDirection.UP);
                        world.setTypeAndData(mutableBlockPos, state, 2);
                    }

                }
            }
        }

        protected void fillPillarDownOrChainUp(GeneratorAccessSeed world, IBlockData state, int x, int y, int z, StructureBoundingBox box) {
            BlockPosition.MutableBlockPosition mutableBlockPos = this.getWorldPos(x, y, z);
            if (box.isInside(mutableBlockPos)) {
                int i = mutableBlockPos.getY();
                int j = 1;
                boolean bl = true;

                for(boolean bl2 = true; bl || bl2; ++j) {
                    if (bl) {
                        mutableBlockPos.setY(i - j);
                        IBlockData blockState = world.getType(mutableBlockPos);
                        boolean bl3 = this.isReplaceableByStructures(blockState) && !blockState.is(Blocks.LAVA);
                        if (!bl3 && this.canPlaceColumnOnTopOf(blockState)) {
                            fillColumnBetween(world, state, mutableBlockPos, i - j + 1, i);
                            return;
                        }

                        bl = j <= 20 && bl3 && mutableBlockPos.getY() > world.getMinBuildHeight() + 1;
                    }

                    if (bl2) {
                        mutableBlockPos.setY(i + j);
                        IBlockData blockState2 = world.getType(mutableBlockPos);
                        boolean bl4 = this.isReplaceableByStructures(blockState2);
                        if (!bl4 && this.canHangChainBelow(world, mutableBlockPos, blockState2)) {
                            world.setTypeAndData(mutableBlockPos.setY(i + 1), this.type.getFenceState(), 2);
                            fillColumnBetween(world, Blocks.CHAIN.getBlockData(), mutableBlockPos, i + 2, i + j);
                            return;
                        }

                        bl2 = j <= 50 && bl4 && mutableBlockPos.getY() < world.getMaxBuildHeight() - 1;
                    }
                }

            }
        }

        private static void fillColumnBetween(GeneratorAccessSeed world, IBlockData state, BlockPosition.MutableBlockPosition pos, int startY, int endY) {
            for(int i = startY; i < endY; ++i) {
                world.setTypeAndData(pos.setY(i), state, 2);
            }

        }

        private boolean canPlaceColumnOnTopOf(IBlockData state) {
            return !state.is(Blocks.RAIL) && !state.is(Blocks.LAVA);
        }

        private boolean canHangChainBelow(IWorldReader world, BlockPosition pos, IBlockData state) {
            return Block.canSupportCenter(world, pos, EnumDirection.DOWN) && !(state.getBlock() instanceof BlockFalling);
        }

        private void placeSupport(GeneratorAccessSeed world, StructureBoundingBox boundingBox, int minX, int minY, int z, int maxY, int maxX, Random random) {
            if (this.isSupportingBox(world, boundingBox, minX, maxX, maxY, z)) {
                IBlockData blockState = this.type.getPlanksState();
                IBlockData blockState2 = this.type.getFenceState();
                this.generateBox(world, boundingBox, minX, minY, z, minX, maxY - 1, z, blockState2.set(BlockFence.WEST, Boolean.valueOf(true)), CAVE_AIR, false);
                this.generateBox(world, boundingBox, maxX, minY, z, maxX, maxY - 1, z, blockState2.set(BlockFence.EAST, Boolean.valueOf(true)), CAVE_AIR, false);
                if (random.nextInt(4) == 0) {
                    this.generateBox(world, boundingBox, minX, maxY, z, minX, maxY, z, blockState, CAVE_AIR, false);
                    this.generateBox(world, boundingBox, maxX, maxY, z, maxX, maxY, z, blockState, CAVE_AIR, false);
                } else {
                    this.generateBox(world, boundingBox, minX, maxY, z, maxX, maxY, z, blockState, CAVE_AIR, false);
                    this.maybeGenerateBlock(world, boundingBox, random, 0.05F, minX + 1, maxY, z - 1, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.SOUTH));
                    this.maybeGenerateBlock(world, boundingBox, random, 0.05F, minX + 1, maxY, z + 1, Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, EnumDirection.NORTH));
                }

            }
        }

        private void maybePlaceCobWeb(GeneratorAccessSeed world, StructureBoundingBox box, Random random, float threshold, int x, int y, int z) {
            if (this.isInterior(world, x, y, z, box) && random.nextFloat() < threshold && this.hasSturdyNeighbours(world, box, x, y, z, 2)) {
                this.placeBlock(world, Blocks.COBWEB.getBlockData(), x, y, z, box);
            }

        }

        private boolean hasSturdyNeighbours(GeneratorAccessSeed world, StructureBoundingBox box, int x, int y, int z, int count) {
            BlockPosition.MutableBlockPosition mutableBlockPos = this.getWorldPos(x, y, z);
            int i = 0;

            for(EnumDirection direction : EnumDirection.values()) {
                mutableBlockPos.move(direction);
                if (box.isInside(mutableBlockPos) && world.getType(mutableBlockPos).isFaceSturdy(world, mutableBlockPos, direction.opposite())) {
                    ++i;
                    if (i >= count) {
                        return true;
                    }
                }

                mutableBlockPos.move(direction.opposite());
            }

            return false;
        }
    }

    public static class WorldGenMineshaftCross extends WorldGenMineshaftPieces.MineShaftPiece {
        private final EnumDirection direction;
        private final boolean isTwoFloored;

        public WorldGenMineshaftCross(NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_CROSSING, nbt);
            this.isTwoFloored = nbt.getBoolean("tf");
            this.direction = EnumDirection.fromType2(nbt.getInt("D"));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.setBoolean("tf", this.isTwoFloored);
            nbt.setInt("D", this.direction.get2DRotationValue());
        }

        public WorldGenMineshaftCross(int chainLength, StructureBoundingBox boundingBox, @Nullable EnumDirection orientation, WorldGenMineshaft.Type type) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_CROSSING, chainLength, type, boundingBox);
            this.direction = orientation;
            this.isTwoFloored = boundingBox.getYSpan() > 3;
        }

        @Nullable
        public static StructureBoundingBox findCrossing(StructurePieceAccessor holder, Random random, int x, int y, int z, EnumDirection orientation) {
            int i;
            if (random.nextInt(4) == 0) {
                i = 6;
            } else {
                i = 2;
            }

            StructureBoundingBox boundingBox;
            switch(orientation) {
            case NORTH:
            default:
                boundingBox = new StructureBoundingBox(-1, 0, -4, 3, i, 0);
                break;
            case SOUTH:
                boundingBox = new StructureBoundingBox(-1, 0, 0, 3, i, 4);
                break;
            case WEST:
                boundingBox = new StructureBoundingBox(-4, 0, -1, 0, i, 3);
                break;
            case EAST:
                boundingBox = new StructureBoundingBox(0, 0, -1, 4, i, 3);
            }

            boundingBox.move(x, y, z);
            return holder.findCollisionPiece(boundingBox) != null ? null : boundingBox;
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            switch(this.direction) {
            case NORTH:
            default:
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, EnumDirection.WEST, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, EnumDirection.EAST, i);
                break;
            case SOUTH:
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, EnumDirection.WEST, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, EnumDirection.EAST, i);
                break;
            case WEST:
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, EnumDirection.WEST, i);
                break;
            case EAST:
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, EnumDirection.EAST, i);
            }

            if (this.isTwoFloored) {
                if (random.nextBoolean()) {
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                }

                if (random.nextBoolean()) {
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, EnumDirection.WEST, i);
                }

                if (random.nextBoolean()) {
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, EnumDirection.EAST, i);
                }

                if (random.nextBoolean()) {
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                }
            }

        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                IBlockData blockState = this.type.getPlanksState();
                if (this.isTwoFloored) {
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.minY() + 3 - 1, this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.minY() + 3 - 1, this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.maxY() - 2, this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.maxY() - 2, this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3, this.boundingBox.minZ() + 1, this.boundingBox.maxX() - 1, this.boundingBox.minY() + 3, this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                } else {
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                }

                this.placeSupportPillar(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
                this.placeSupportPillar(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
                this.placeSupportPillar(world, chunkBox, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
                this.placeSupportPillar(world, chunkBox, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
                int i = this.boundingBox.minY() - 1;

                for(int j = this.boundingBox.minX(); j <= this.boundingBox.maxX(); ++j) {
                    for(int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); ++k) {
                        this.setPlanksBlock(world, chunkBox, blockState, j, i, k);
                    }
                }

            }
        }

        private void placeSupportPillar(GeneratorAccessSeed world, StructureBoundingBox boundingBox, int x, int minY, int z, int maxY) {
            if (!this.getBlock(world, x, maxY + 1, z, boundingBox).isAir()) {
                this.generateBox(world, boundingBox, x, minY, z, x, maxY, z, this.type.getPlanksState(), CAVE_AIR, false);
            }

        }
    }

    public static class WorldGenMineshaftRoom extends WorldGenMineshaftPieces.MineShaftPiece {
        private final List<StructureBoundingBox> childEntranceBoxes = Lists.newLinkedList();

        public WorldGenMineshaftRoom(int chainLength, Random random, int x, int z, WorldGenMineshaft.Type type) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_ROOM, chainLength, type, new StructureBoundingBox(x, 50, z, x + 7 + random.nextInt(6), 54 + random.nextInt(6), z + 7 + random.nextInt(6)));
            this.type = type;
        }

        public WorldGenMineshaftRoom(NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_ROOM, nbt);
            StructureBoundingBox.CODEC.listOf().parse(DynamicOpsNBT.INSTANCE, nbt.getList("Entrances", 11)).resultOrPartial(WorldGenMineshaftPieces.LOGGER::error).ifPresent(this.childEntranceBoxes::addAll);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            int j = this.boundingBox.getYSpan() - 3 - 1;
            if (j <= 0) {
                j = 1;
            }

            int k;
            for(k = 0; k < this.boundingBox.getXSpan(); k = k + 4) {
                k = k + random.nextInt(this.boundingBox.getXSpan());
                if (k + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                WorldGenMineshaftPieces.MineShaftPiece mineShaftPiece = WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + k, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                if (mineShaftPiece != null) {
                    StructureBoundingBox boundingBox = mineShaftPiece.getBoundingBox();
                    this.childEntranceBoxes.add(new StructureBoundingBox(boundingBox.minX(), boundingBox.minY(), this.boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), this.boundingBox.minZ() + 1));
                }
            }

            for(k = 0; k < this.boundingBox.getXSpan(); k = k + 4) {
                k = k + random.nextInt(this.boundingBox.getXSpan());
                if (k + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                WorldGenMineshaftPieces.MineShaftPiece mineShaftPiece2 = WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + k, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                if (mineShaftPiece2 != null) {
                    StructureBoundingBox boundingBox2 = mineShaftPiece2.getBoundingBox();
                    this.childEntranceBoxes.add(new StructureBoundingBox(boundingBox2.minX(), boundingBox2.minY(), this.boundingBox.maxZ() - 1, boundingBox2.maxX(), boundingBox2.maxY(), this.boundingBox.maxZ()));
                }
            }

            for(k = 0; k < this.boundingBox.getZSpan(); k = k + 4) {
                k = k + random.nextInt(this.boundingBox.getZSpan());
                if (k + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                WorldGenMineshaftPieces.MineShaftPiece mineShaftPiece3 = WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.minZ() + k, EnumDirection.WEST, i);
                if (mineShaftPiece3 != null) {
                    StructureBoundingBox boundingBox3 = mineShaftPiece3.getBoundingBox();
                    this.childEntranceBoxes.add(new StructureBoundingBox(this.boundingBox.minX(), boundingBox3.minY(), boundingBox3.minZ(), this.boundingBox.minX() + 1, boundingBox3.maxY(), boundingBox3.maxZ()));
                }
            }

            for(k = 0; k < this.boundingBox.getZSpan(); k = k + 4) {
                k = k + random.nextInt(this.boundingBox.getZSpan());
                if (k + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                StructurePiece structurePiece = WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.minZ() + k, EnumDirection.EAST, i);
                if (structurePiece != null) {
                    StructureBoundingBox boundingBox4 = structurePiece.getBoundingBox();
                    this.childEntranceBoxes.add(new StructureBoundingBox(this.boundingBox.maxX() - 1, boundingBox4.minY(), boundingBox4.minZ(), this.boundingBox.maxX(), boundingBox4.maxY(), boundingBox4.maxZ()));
                }
            }

        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY() + 1, this.boundingBox.minZ(), this.boundingBox.maxX(), Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);

                for(StructureBoundingBox boundingBox : this.childEntranceBoxes) {
                    this.generateBox(world, chunkBox, boundingBox.minX(), boundingBox.maxY() - 2, boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                }

                this.generateUpperHalfSphere(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY() + 4, this.boundingBox.minZ(), this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, false);
            }
        }

        @Override
        public void move(int x, int y, int z) {
            super.move(x, y, z);

            for(StructureBoundingBox boundingBox : this.childEntranceBoxes) {
                boundingBox.move(x, y, z);
            }

        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            super.addAdditionalSaveData(context, nbt);
            StructureBoundingBox.CODEC.listOf().encodeStart(DynamicOpsNBT.INSTANCE, this.childEntranceBoxes).resultOrPartial(WorldGenMineshaftPieces.LOGGER::error).ifPresent((tag) -> {
                nbt.set("Entrances", tag);
            });
        }
    }

    public static class WorldGenMineshaftStairs extends WorldGenMineshaftPieces.MineShaftPiece {
        public WorldGenMineshaftStairs(int chainLength, StructureBoundingBox boundingBox, EnumDirection orientation, WorldGenMineshaft.Type type) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_STAIRS, chainLength, type, boundingBox);
            this.setOrientation(orientation);
        }

        public WorldGenMineshaftStairs(NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.MINE_SHAFT_STAIRS, nbt);
        }

        @Nullable
        public static StructureBoundingBox findStairs(StructurePieceAccessor holder, Random random, int x, int y, int z, EnumDirection orientation) {
            StructureBoundingBox boundingBox;
            switch(orientation) {
            case NORTH:
            default:
                boundingBox = new StructureBoundingBox(0, -5, -8, 2, 2, 0);
                break;
            case SOUTH:
                boundingBox = new StructureBoundingBox(0, -5, 0, 2, 2, 8);
                break;
            case WEST:
                boundingBox = new StructureBoundingBox(-8, -5, 0, 0, 2, 2);
                break;
            case EAST:
                boundingBox = new StructureBoundingBox(0, -5, 0, 8, 2, 2);
            }

            boundingBox.move(x, y, z);
            return holder.findCollisionPiece(boundingBox) != null ? null : boundingBox;
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            EnumDirection direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                default:
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, EnumDirection.NORTH, i);
                    break;
                case SOUTH:
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, EnumDirection.SOUTH, i);
                    break;
                case WEST:
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), EnumDirection.WEST, i);
                    break;
                case EAST:
                    WorldGenMineshaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), EnumDirection.EAST, i);
                }
            }

        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                this.generateBox(world, chunkBox, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
                this.generateBox(world, chunkBox, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

                for(int i = 0; i < 5; ++i) {
                    this.generateBox(world, chunkBox, 0, 5 - i - (i < 4 ? 1 : 0), 2 + i, 2, 7 - i, 2 + i, CAVE_AIR, CAVE_AIR, false);
                }

            }
        }
    }
}
