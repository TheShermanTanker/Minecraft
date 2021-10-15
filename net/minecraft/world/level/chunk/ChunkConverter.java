package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.EnumDirection8;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.BlockAccessAir;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.BlockFacingHorizontal;
import net.minecraft.world.level.block.BlockStem;
import net.minecraft.world.level.block.BlockStemmed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyChestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkConverter {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ChunkConverter EMPTY = new ChunkConverter(BlockAccessAir.INSTANCE);
    private static final String TAG_INDICES = "Indices";
    private static final EnumDirection8[] DIRECTIONS = EnumDirection8.values();
    private final EnumSet<EnumDirection8> sides = EnumSet.noneOf(EnumDirection8.class);
    private final int[][] index;
    static final Map<Block, ChunkConverter.BlockFixer> MAP = new IdentityHashMap<>();
    static final Set<ChunkConverter.BlockFixer> CHUNKY_FIXERS = Sets.newHashSet();

    private ChunkConverter(IWorldHeightAccess world) {
        this.index = new int[world.getSectionsCount()][];
    }

    public ChunkConverter(NBTTagCompound nbt, IWorldHeightAccess world) {
        this(world);
        if (nbt.hasKeyOfType("Indices", 10)) {
            NBTTagCompound compoundTag = nbt.getCompound("Indices");

            for(int i = 0; i < this.index.length; ++i) {
                String string = String.valueOf(i);
                if (compoundTag.hasKeyOfType(string, 11)) {
                    this.index[i] = compoundTag.getIntArray(string);
                }
            }
        }

        int j = nbt.getInt("Sides");

        for(EnumDirection8 direction8 : EnumDirection8.values()) {
            if ((j & 1 << direction8.ordinal()) != 0) {
                this.sides.add(direction8);
            }
        }

    }

    public void upgrade(Chunk chunk) {
        this.upgradeInside(chunk);

        for(EnumDirection8 direction8 : DIRECTIONS) {
            upgradeSides(chunk, direction8);
        }

        World level = chunk.getWorld();
        CHUNKY_FIXERS.forEach((blockFixer) -> {
            blockFixer.processChunk(level);
        });
    }

    private static void upgradeSides(Chunk chunk, EnumDirection8 side) {
        World level = chunk.getWorld();
        if (chunk.getUpgradeData().sides.remove(side)) {
            Set<EnumDirection> set = side.getDirections();
            int i = 0;
            int j = 15;
            boolean bl = set.contains(EnumDirection.EAST);
            boolean bl2 = set.contains(EnumDirection.WEST);
            boolean bl3 = set.contains(EnumDirection.SOUTH);
            boolean bl4 = set.contains(EnumDirection.NORTH);
            boolean bl5 = set.size() == 1;
            ChunkCoordIntPair chunkPos = chunk.getPos();
            int k = chunkPos.getMinBlockX() + (!bl5 || !bl4 && !bl3 ? (bl2 ? 0 : 15) : 1);
            int l = chunkPos.getMinBlockX() + (!bl5 || !bl4 && !bl3 ? (bl2 ? 0 : 15) : 14);
            int m = chunkPos.getMinBlockZ() + (!bl5 || !bl && !bl2 ? (bl4 ? 0 : 15) : 1);
            int n = chunkPos.getMinBlockZ() + (!bl5 || !bl && !bl2 ? (bl4 ? 0 : 15) : 14);
            EnumDirection[] directions = EnumDirection.values();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(BlockPosition blockPos : BlockPosition.betweenClosed(k, level.getMinBuildHeight(), m, l, level.getMaxBuildHeight() - 1, n)) {
                IBlockData blockState = level.getType(blockPos);
                IBlockData blockState2 = blockState;

                for(EnumDirection direction : directions) {
                    mutableBlockPos.setWithOffset(blockPos, direction);
                    blockState2 = updateState(blockState2, direction, level, blockPos, mutableBlockPos);
                }

                Block.updateOrDestroy(blockState, blockState2, level, blockPos, 18);
            }

        }
    }

    private static IBlockData updateState(IBlockData oldState, EnumDirection dir, GeneratorAccess world, BlockPosition currentPos, BlockPosition otherPos) {
        return MAP.getOrDefault(oldState.getBlock(), ChunkConverter.Type.DEFAULT).updateShape(oldState, dir, world.getType(otherPos), world, currentPos, otherPos);
    }

    private void upgradeInside(Chunk chunk) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();
        ChunkCoordIntPair chunkPos = chunk.getPos();
        GeneratorAccess levelAccessor = chunk.getWorld();

        for(int i = 0; i < this.index.length; ++i) {
            ChunkSection levelChunkSection = chunk.getSections()[i];
            int[] is = this.index[i];
            this.index[i] = null;
            if (levelChunkSection != null && is != null && is.length > 0) {
                EnumDirection[] directions = EnumDirection.values();
                DataPaletteBlock<IBlockData> palettedContainer = levelChunkSection.getBlocks();

                for(int j : is) {
                    int k = j & 15;
                    int l = j >> 8 & 15;
                    int m = j >> 4 & 15;
                    mutableBlockPos.set(chunkPos.getMinBlockX() + k, levelChunkSection.getYPosition() + l, chunkPos.getMinBlockZ() + m);
                    IBlockData blockState = palettedContainer.get(j);
                    IBlockData blockState2 = blockState;

                    for(EnumDirection direction : directions) {
                        mutableBlockPos2.setWithOffset(mutableBlockPos, direction);
                        if (SectionPosition.blockToSectionCoord(mutableBlockPos.getX()) == chunkPos.x && SectionPosition.blockToSectionCoord(mutableBlockPos.getZ()) == chunkPos.z) {
                            blockState2 = updateState(blockState2, direction, levelAccessor, mutableBlockPos, mutableBlockPos2);
                        }
                    }

                    Block.updateOrDestroy(blockState, blockState2, levelAccessor, mutableBlockPos, 18);
                }
            }
        }

        for(int n = 0; n < this.index.length; ++n) {
            if (this.index[n] != null) {
                LOGGER.warn("Discarding update data for section {} for chunk ({} {})", levelAccessor.getSectionYFromSectionIndex(n), chunkPos.x, chunkPos.z);
            }

            this.index[n] = null;
        }

    }

    public boolean isEmpty() {
        for(int[] is : this.index) {
            if (is != null) {
                return false;
            }
        }

        return this.sides.isEmpty();
    }

    public NBTTagCompound write() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        NBTTagCompound compoundTag2 = new NBTTagCompound();

        for(int i = 0; i < this.index.length; ++i) {
            String string = String.valueOf(i);
            if (this.index[i] != null && this.index[i].length != 0) {
                compoundTag2.setIntArray(string, this.index[i]);
            }
        }

        if (!compoundTag2.isEmpty()) {
            compoundTag.set("Indices", compoundTag2);
        }

        int j = 0;

        for(EnumDirection8 direction8 : this.sides) {
            j |= 1 << direction8.ordinal();
        }

        compoundTag.setByte("Sides", (byte)j);
        return compoundTag;
    }

    public interface BlockFixer {
        IBlockData updateShape(IBlockData blockState, EnumDirection direction, IBlockData blockState2, GeneratorAccess world, BlockPosition blockPos, BlockPosition blockPos2);

        default void processChunk(GeneratorAccess world) {
        }
    }

    static enum Type implements ChunkConverter.BlockFixer {
        BLACKLIST(Blocks.OBSERVER, Blocks.NETHER_PORTAL, Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER, Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.BLACK_CONCRETE_POWDER, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.DRAGON_EGG, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN, Blocks.ACACIA_SIGN, Blocks.JUNGLE_SIGN, Blocks.DARK_OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.DARK_OAK_WALL_SIGN) {
            @Override
            public IBlockData updateShape(IBlockData blockState, EnumDirection direction, IBlockData blockState2, GeneratorAccess world, BlockPosition blockPos, BlockPosition blockPos2) {
                return blockState;
            }
        },
        DEFAULT {
            @Override
            public IBlockData updateShape(IBlockData blockState, EnumDirection direction, IBlockData blockState2, GeneratorAccess world, BlockPosition blockPos, BlockPosition blockPos2) {
                return blockState.updateState(direction, world.getType(blockPos2), world, blockPos, blockPos2);
            }
        },
        CHEST(Blocks.CHEST, Blocks.TRAPPED_CHEST) {
            @Override
            public IBlockData updateShape(IBlockData blockState, EnumDirection direction, IBlockData blockState2, GeneratorAccess world, BlockPosition blockPos, BlockPosition blockPos2) {
                if (blockState2.is(blockState.getBlock()) && direction.getAxis().isHorizontal() && blockState.get(BlockChest.TYPE) == BlockPropertyChestType.SINGLE && blockState2.get(BlockChest.TYPE) == BlockPropertyChestType.SINGLE) {
                    EnumDirection direction2 = blockState.get(BlockChest.FACING);
                    if (direction.getAxis() != direction2.getAxis() && direction2 == blockState2.get(BlockChest.FACING)) {
                        BlockPropertyChestType chestType = direction == direction2.getClockWise() ? BlockPropertyChestType.LEFT : BlockPropertyChestType.RIGHT;
                        world.setTypeAndData(blockPos2, blockState2.set(BlockChest.TYPE, chestType.getOpposite()), 18);
                        if (direction2 == EnumDirection.NORTH || direction2 == EnumDirection.EAST) {
                            TileEntity blockEntity = world.getTileEntity(blockPos);
                            TileEntity blockEntity2 = world.getTileEntity(blockPos2);
                            if (blockEntity instanceof TileEntityChest && blockEntity2 instanceof TileEntityChest) {
                                TileEntityChest.swapContents((TileEntityChest)blockEntity, (TileEntityChest)blockEntity2);
                            }
                        }

                        return blockState.set(BlockChest.TYPE, chestType);
                    }
                }

                return blockState;
            }
        },
        LEAVES(true, Blocks.ACACIA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES) {
            private final ThreadLocal<List<ObjectSet<BlockPosition>>> queue = ThreadLocal.withInitial(() -> {
                return Lists.newArrayListWithCapacity(7);
            });

            @Override
            public IBlockData updateShape(IBlockData blockState, EnumDirection direction, IBlockData blockState2, GeneratorAccess world, BlockPosition blockPos, BlockPosition blockPos2) {
                IBlockData blockState3 = blockState.updateState(direction, world.getType(blockPos2), world, blockPos, blockPos2);
                if (blockState != blockState3) {
                    int i = blockState3.get(BlockProperties.DISTANCE);
                    List<ObjectSet<BlockPosition>> list = this.queue.get();
                    if (list.isEmpty()) {
                        for(int j = 0; j < 7; ++j) {
                            list.add(new ObjectOpenHashSet<>());
                        }
                    }

                    list.get(i).add(blockPos.immutableCopy());
                }

                return blockState;
            }

            @Override
            public void processChunk(GeneratorAccess world) {
                BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
                List<ObjectSet<BlockPosition>> list = this.queue.get();

                for(int i = 2; i < list.size(); ++i) {
                    int j = i - 1;
                    ObjectSet<BlockPosition> objectSet = list.get(j);
                    ObjectSet<BlockPosition> objectSet2 = list.get(i);

                    for(BlockPosition blockPos : objectSet) {
                        IBlockData blockState = world.getType(blockPos);
                        if (blockState.get(BlockProperties.DISTANCE) >= j) {
                            world.setTypeAndData(blockPos, blockState.set(BlockProperties.DISTANCE, Integer.valueOf(j)), 18);
                            if (i != 7) {
                                for(EnumDirection direction : DIRECTIONS) {
                                    mutableBlockPos.setWithOffset(blockPos, direction);
                                    IBlockData blockState2 = world.getType(mutableBlockPos);
                                    if (blockState2.hasProperty(BlockProperties.DISTANCE) && blockState.get(BlockProperties.DISTANCE) > i) {
                                        objectSet2.add(mutableBlockPos.immutableCopy());
                                    }
                                }
                            }
                        }
                    }
                }

                list.clear();
            }
        },
        STEM_BLOCK(Blocks.MELON_STEM, Blocks.PUMPKIN_STEM) {
            @Override
            public IBlockData updateShape(IBlockData blockState, EnumDirection direction, IBlockData blockState2, GeneratorAccess world, BlockPosition blockPos, BlockPosition blockPos2) {
                if (blockState.get(BlockStem.AGE) == 7) {
                    BlockStemmed stemGrownBlock = ((BlockStem)blockState.getBlock()).getFruit();
                    if (blockState2.is(stemGrownBlock)) {
                        return stemGrownBlock.getAttachedStem().getBlockData().set(BlockFacingHorizontal.FACING, direction);
                    }
                }

                return blockState;
            }
        };

        public static final EnumDirection[] DIRECTIONS = EnumDirection.values();

        Type(Block... blocks) {
            this(false, blocks);
        }

        Type(boolean bl, Block... blocks) {
            for(Block block : blocks) {
                ChunkConverter.MAP.put(block, this);
            }

            if (bl) {
                ChunkConverter.CHUNKY_FIXERS.add(this);
            }

        }
    }
}
