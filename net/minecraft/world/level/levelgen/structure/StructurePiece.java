package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.BlockFacingHorizontal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.entity.TileEntityDispenser;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StructurePiece {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final IBlockData CAVE_AIR = Blocks.CAVE_AIR.getBlockData();
    protected StructureBoundingBox boundingBox;
    @Nullable
    private EnumDirection orientation;
    private EnumBlockMirror mirror;
    private EnumBlockRotation rotation;
    protected int genDepth;
    private final WorldGenFeatureStructurePieceType type;
    private static final Set<Block> SHAPE_CHECK_BLOCKS = ImmutableSet.<Block>builder().add(Blocks.NETHER_BRICK_FENCE).add(Blocks.TORCH).add(Blocks.WALL_TORCH).add(Blocks.OAK_FENCE).add(Blocks.SPRUCE_FENCE).add(Blocks.DARK_OAK_FENCE).add(Blocks.ACACIA_FENCE).add(Blocks.BIRCH_FENCE).add(Blocks.JUNGLE_FENCE).add(Blocks.LADDER).add(Blocks.IRON_BARS).build();

    protected StructurePiece(WorldGenFeatureStructurePieceType type, int length, StructureBoundingBox boundingBox) {
        this.type = type;
        this.genDepth = length;
        this.boundingBox = boundingBox;
    }

    public StructurePiece(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
        this(type, nbt.getInt("GD"), StructureBoundingBox.CODEC.parse(DynamicOpsNBT.INSTANCE, nbt.get("BB")).resultOrPartial(LOGGER::error).orElseThrow(() -> {
            return new IllegalArgumentException("Invalid boundingbox");
        }));
        int i = nbt.getInt("O");
        this.setOrientation(i == -1 ? null : EnumDirection.fromType2(i));
    }

    protected static StructureBoundingBox makeBoundingBox(int x, int y, int z, EnumDirection orientation, int width, int height, int depth) {
        return orientation.getAxis() == EnumDirection.EnumAxis.Z ? new StructureBoundingBox(x, y, z, x + width - 1, y + height - 1, z + depth - 1) : new StructureBoundingBox(x, y, z, x + depth - 1, y + height - 1, z + width - 1);
    }

    protected static EnumDirection getRandomHorizontalDirection(Random random) {
        return EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
    }

    public final NBTTagCompound createTag(StructurePieceSerializationContext context) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("id", IRegistry.STRUCTURE_PIECE.getKey(this.getType()).toString());
        StructureBoundingBox.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.boundingBox).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            compoundTag.set("BB", tag);
        });
        EnumDirection direction = this.getOrientation();
        compoundTag.setInt("O", direction == null ? -1 : direction.get2DRotationValue());
        compoundTag.setInt("GD", this.genDepth);
        this.addAdditionalSaveData(context, compoundTag);
        return compoundTag;
    }

    protected abstract void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt);

    public NoiseEffect getNoiseEffect() {
        return NoiseEffect.BEARD;
    }

    public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
    }

    public abstract void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos);

    public StructureBoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public int getGenDepth() {
        return this.genDepth;
    }

    public boolean isCloseToChunk(ChunkCoordIntPair pos, int offset) {
        int i = pos.getMinBlockX();
        int j = pos.getMinBlockZ();
        return this.boundingBox.intersects(i - offset, j - offset, i + 15 + offset, j + 15 + offset);
    }

    public BlockPosition getLocatorPosition() {
        return new BlockPosition(this.boundingBox.getCenter());
    }

    protected BlockPosition.MutableBlockPosition getWorldPos(int x, int y, int z) {
        return new BlockPosition.MutableBlockPosition(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
    }

    protected int getWorldX(int x, int z) {
        EnumDirection direction = this.getOrientation();
        if (direction == null) {
            return x;
        } else {
            switch(direction) {
            case NORTH:
            case SOUTH:
                return this.boundingBox.minX() + x;
            case WEST:
                return this.boundingBox.maxX() - z;
            case EAST:
                return this.boundingBox.minX() + z;
            default:
                return x;
            }
        }
    }

    protected int getWorldY(int y) {
        return this.getOrientation() == null ? y : y + this.boundingBox.minY();
    }

    protected int getWorldZ(int x, int z) {
        EnumDirection direction = this.getOrientation();
        if (direction == null) {
            return z;
        } else {
            switch(direction) {
            case NORTH:
                return this.boundingBox.maxZ() - z;
            case SOUTH:
                return this.boundingBox.minZ() + z;
            case WEST:
            case EAST:
                return this.boundingBox.minZ() + x;
            default:
                return z;
            }
        }
    }

    protected void placeBlock(GeneratorAccessSeed world, IBlockData block, int x, int y, int z, StructureBoundingBox box) {
        BlockPosition blockPos = this.getWorldPos(x, y, z);
        if (box.isInside(blockPos)) {
            if (this.canBeReplaced(world, x, y, z, box)) {
                if (this.mirror != EnumBlockMirror.NONE) {
                    block = block.mirror(this.mirror);
                }

                if (this.rotation != EnumBlockRotation.NONE) {
                    block = block.rotate(this.rotation);
                }

                world.setTypeAndData(blockPos, block, 2);
                Fluid fluidState = world.getFluid(blockPos);
                if (!fluidState.isEmpty()) {
                    world.scheduleTick(blockPos, fluidState.getType(), 0);
                }

                if (SHAPE_CHECK_BLOCKS.contains(block.getBlock())) {
                    world.getChunk(blockPos).markPosForPostprocessing(blockPos);
                }

            }
        }
    }

    protected boolean canBeReplaced(IWorldReader world, int x, int y, int z, StructureBoundingBox box) {
        return true;
    }

    protected IBlockData getBlock(IBlockAccess world, int x, int y, int z, StructureBoundingBox box) {
        BlockPosition blockPos = this.getWorldPos(x, y, z);
        return !box.isInside(blockPos) ? Blocks.AIR.getBlockData() : world.getType(blockPos);
    }

    protected boolean isInterior(IWorldReader world, int x, int z, int y, StructureBoundingBox box) {
        BlockPosition blockPos = this.getWorldPos(x, z + 1, y);
        if (!box.isInside(blockPos)) {
            return false;
        } else {
            return blockPos.getY() < world.getHeight(HeightMap.Type.OCEAN_FLOOR_WG, blockPos.getX(), blockPos.getZ());
        }
    }

    protected void generateAirBox(GeneratorAccessSeed world, StructureBoundingBox bounds, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    this.placeBlock(world, Blocks.AIR.getBlockData(), j, i, k, bounds);
                }
            }
        }

    }

    protected void generateBox(GeneratorAccessSeed world, StructureBoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IBlockData outline, IBlockData inside, boolean cantReplaceAir) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    if (!cantReplaceAir || !this.getBlock(world, j, i, k, box).isAir()) {
                        if (i != minY && i != maxY && j != minX && j != maxX && k != minZ && k != maxZ) {
                            this.placeBlock(world, inside, j, i, k, box);
                        } else {
                            this.placeBlock(world, outline, j, i, k, box);
                        }
                    }
                }
            }
        }

    }

    protected void generateBox(GeneratorAccessSeed world, StructureBoundingBox box, StructureBoundingBox fillBox, IBlockData outline, IBlockData inside, boolean cantReplaceAir) {
        this.generateBox(world, box, fillBox.minX(), fillBox.minY(), fillBox.minZ(), fillBox.maxX(), fillBox.maxY(), fillBox.maxZ(), outline, inside, cantReplaceAir);
    }

    protected void generateBox(GeneratorAccessSeed world, StructureBoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean cantReplaceAir, Random random, StructurePiece.StructurePieceBlockSelector randomizer) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    if (!cantReplaceAir || !this.getBlock(world, j, i, k, box).isAir()) {
                        randomizer.next(random, j, i, k, i == minY || i == maxY || j == minX || j == maxX || k == minZ || k == maxZ);
                        this.placeBlock(world, randomizer.getNext(), j, i, k, box);
                    }
                }
            }
        }

    }

    protected void generateBox(GeneratorAccessSeed world, StructureBoundingBox box, StructureBoundingBox fillBox, boolean cantReplaceAir, Random random, StructurePiece.StructurePieceBlockSelector randomizer) {
        this.generateBox(world, box, fillBox.minX(), fillBox.minY(), fillBox.minZ(), fillBox.maxX(), fillBox.maxY(), fillBox.maxZ(), cantReplaceAir, random, randomizer);
    }

    protected void generateMaybeBox(GeneratorAccessSeed world, StructureBoundingBox box, Random random, float blockChance, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IBlockData outline, IBlockData inside, boolean cantReplaceAir, boolean stayBelowSeaLevel) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    if (!(random.nextFloat() > blockChance) && (!cantReplaceAir || !this.getBlock(world, j, i, k, box).isAir()) && (!stayBelowSeaLevel || this.isInterior(world, j, i, k, box))) {
                        if (i != minY && i != maxY && j != minX && j != maxX && k != minZ && k != maxZ) {
                            this.placeBlock(world, inside, j, i, k, box);
                        } else {
                            this.placeBlock(world, outline, j, i, k, box);
                        }
                    }
                }
            }
        }

    }

    protected void maybeGenerateBlock(GeneratorAccessSeed world, StructureBoundingBox bounds, Random random, float threshold, int x, int y, int z, IBlockData state) {
        if (random.nextFloat() < threshold) {
            this.placeBlock(world, state, x, y, z, bounds);
        }

    }

    protected void generateUpperHalfSphere(GeneratorAccessSeed world, StructureBoundingBox bounds, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IBlockData block, boolean cantReplaceAir) {
        float f = (float)(maxX - minX + 1);
        float g = (float)(maxY - minY + 1);
        float h = (float)(maxZ - minZ + 1);
        float i = (float)minX + f / 2.0F;
        float j = (float)minZ + h / 2.0F;

        for(int k = minY; k <= maxY; ++k) {
            float l = (float)(k - minY) / g;

            for(int m = minX; m <= maxX; ++m) {
                float n = ((float)m - i) / (f * 0.5F);

                for(int o = minZ; o <= maxZ; ++o) {
                    float p = ((float)o - j) / (h * 0.5F);
                    if (!cantReplaceAir || !this.getBlock(world, m, k, o, bounds).isAir()) {
                        float q = n * n + l * l + p * p;
                        if (q <= 1.05F) {
                            this.placeBlock(world, block, m, k, o, bounds);
                        }
                    }
                }
            }
        }

    }

    protected void fillColumnDown(GeneratorAccessSeed world, IBlockData state, int x, int y, int z, StructureBoundingBox box) {
        BlockPosition.MutableBlockPosition mutableBlockPos = this.getWorldPos(x, y, z);
        if (box.isInside(mutableBlockPos)) {
            while(this.isReplaceableByStructures(world.getType(mutableBlockPos)) && mutableBlockPos.getY() > world.getMinBuildHeight() + 1) {
                world.setTypeAndData(mutableBlockPos, state, 2);
                mutableBlockPos.move(EnumDirection.DOWN);
            }

        }
    }

    protected boolean isReplaceableByStructures(IBlockData state) {
        return state.isAir() || state.getMaterial().isLiquid() || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
    }

    protected boolean createChest(GeneratorAccessSeed world, StructureBoundingBox boundingBox, Random random, int x, int y, int z, MinecraftKey lootTableId) {
        return this.createChest(world, boundingBox, random, this.getWorldPos(x, y, z), lootTableId, (IBlockData)null);
    }

    public static IBlockData reorient(IBlockAccess world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = null;

        for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction2);
            IBlockData blockState = world.getType(blockPos);
            if (blockState.is(Blocks.CHEST)) {
                return state;
            }

            if (blockState.isSolidRender(world, blockPos)) {
                if (direction != null) {
                    direction = null;
                    break;
                }

                direction = direction2;
            }
        }

        if (direction != null) {
            return state.set(BlockFacingHorizontal.FACING, direction.opposite());
        } else {
            EnumDirection direction3 = state.get(BlockFacingHorizontal.FACING);
            BlockPosition blockPos2 = pos.relative(direction3);
            if (world.getType(blockPos2).isSolidRender(world, blockPos2)) {
                direction3 = direction3.opposite();
                blockPos2 = pos.relative(direction3);
            }

            if (world.getType(blockPos2).isSolidRender(world, blockPos2)) {
                direction3 = direction3.getClockWise();
                blockPos2 = pos.relative(direction3);
            }

            if (world.getType(blockPos2).isSolidRender(world, blockPos2)) {
                direction3 = direction3.opposite();
                pos.relative(direction3);
            }

            return state.set(BlockFacingHorizontal.FACING, direction3);
        }
    }

    protected boolean createChest(WorldAccess world, StructureBoundingBox boundingBox, Random random, BlockPosition pos, MinecraftKey lootTableId, @Nullable IBlockData block) {
        if (boundingBox.isInside(pos) && !world.getType(pos).is(Blocks.CHEST)) {
            if (block == null) {
                block = reorient(world, pos, Blocks.CHEST.getBlockData());
            }

            world.setTypeAndData(pos, block, 2);
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityChest) {
                ((TileEntityChest)blockEntity).setLootTable(lootTableId, random.nextLong());
            }

            return true;
        } else {
            return false;
        }
    }

    protected boolean createDispenser(GeneratorAccessSeed world, StructureBoundingBox boundingBox, Random random, int x, int y, int z, EnumDirection facing, MinecraftKey lootTableId) {
        BlockPosition blockPos = this.getWorldPos(x, y, z);
        if (boundingBox.isInside(blockPos) && !world.getType(blockPos).is(Blocks.DISPENSER)) {
            this.placeBlock(world, Blocks.DISPENSER.getBlockData().set(BlockDispenser.FACING, facing), x, y, z, boundingBox);
            TileEntity blockEntity = world.getTileEntity(blockPos);
            if (blockEntity instanceof TileEntityDispenser) {
                ((TileEntityDispenser)blockEntity).setLootTable(lootTableId, random.nextLong());
            }

            return true;
        } else {
            return false;
        }
    }

    public void move(int x, int y, int z) {
        this.boundingBox.move(x, y, z);
    }

    public static StructureBoundingBox createBoundingBox(Stream<StructurePiece> pieces) {
        return StructureBoundingBox.encapsulatingBoxes(pieces.map(StructurePiece::getBoundingBox)::iterator).orElseThrow(() -> {
            return new IllegalStateException("Unable to calculate boundingbox without pieces");
        });
    }

    @Nullable
    public static StructurePiece findCollisionPiece(List<StructurePiece> pieces, StructureBoundingBox box) {
        for(StructurePiece structurePiece : pieces) {
            if (structurePiece.getBoundingBox().intersects(box)) {
                return structurePiece;
            }
        }

        return null;
    }

    @Nullable
    public EnumDirection getOrientation() {
        return this.orientation;
    }

    public void setOrientation(@Nullable EnumDirection orientation) {
        this.orientation = orientation;
        if (orientation == null) {
            this.rotation = EnumBlockRotation.NONE;
            this.mirror = EnumBlockMirror.NONE;
        } else {
            switch(orientation) {
            case SOUTH:
                this.mirror = EnumBlockMirror.LEFT_RIGHT;
                this.rotation = EnumBlockRotation.NONE;
                break;
            case WEST:
                this.mirror = EnumBlockMirror.LEFT_RIGHT;
                this.rotation = EnumBlockRotation.CLOCKWISE_90;
                break;
            case EAST:
                this.mirror = EnumBlockMirror.NONE;
                this.rotation = EnumBlockRotation.CLOCKWISE_90;
                break;
            default:
                this.mirror = EnumBlockMirror.NONE;
                this.rotation = EnumBlockRotation.NONE;
            }
        }

    }

    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    public EnumBlockMirror getMirror() {
        return this.mirror;
    }

    public WorldGenFeatureStructurePieceType getType() {
        return this.type;
    }

    protected abstract static class StructurePieceBlockSelector {
        protected IBlockData next = Blocks.AIR.getBlockData();

        public abstract void next(Random random, int x, int y, int z, boolean placeBlock);

        public IBlockData getNext() {
            return this.next;
        }
    }
}
