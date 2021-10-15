package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockFire extends BlockFireAbstract {
    public static final int MAX_AGE = 15;
    public static final BlockStateInteger AGE = BlockProperties.AGE_15;
    public static final BlockStateBoolean NORTH = BlockSprawling.NORTH;
    public static final BlockStateBoolean EAST = BlockSprawling.EAST;
    public static final BlockStateBoolean SOUTH = BlockSprawling.SOUTH;
    public static final BlockStateBoolean WEST = BlockSprawling.WEST;
    public static final BlockStateBoolean UP = BlockSprawling.UP;
    private static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = BlockSprawling.PROPERTY_BY_DIRECTION.entrySet().stream().filter((entry) -> {
        return entry.getKey() != EnumDirection.DOWN;
    }).collect(SystemUtils.toMap());
    private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private final Map<IBlockData, VoxelShape> shapesCache;
    private static final int FLAME_INSTANT = 60;
    private static final int FLAME_EASY = 30;
    private static final int FLAME_MEDIUM = 15;
    private static final int FLAME_HARD = 5;
    private static final int BURN_INSTANT = 100;
    private static final int BURN_EASY = 60;
    private static final int BURN_MEDIUM = 20;
    private static final int BURN_HARD = 5;
    public final Object2IntMap<Block> flameOdds = new Object2IntOpenHashMap<>();
    private final Object2IntMap<Block> burnOdds = new Object2IntOpenHashMap<>();

    public BlockFire(BlockBase.Info settings) {
        super(settings, 1.0F);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)).set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)).set(UP, Boolean.valueOf(false)));
        this.shapesCache = ImmutableMap.copyOf(this.stateDefinition.getPossibleStates().stream().filter((state) -> {
            return state.get(AGE) == 0;
        }).collect(Collectors.toMap(Function.identity(), BlockFire::calculateShape)));
    }

    private static VoxelShape calculateShape(IBlockData state) {
        VoxelShape voxelShape = VoxelShapes.empty();
        if (state.get(UP)) {
            voxelShape = UP_AABB;
        }

        if (state.get(NORTH)) {
            voxelShape = VoxelShapes.or(voxelShape, NORTH_AABB);
        }

        if (state.get(SOUTH)) {
            voxelShape = VoxelShapes.or(voxelShape, SOUTH_AABB);
        }

        if (state.get(EAST)) {
            voxelShape = VoxelShapes.or(voxelShape, EAST_AABB);
        }

        if (state.get(WEST)) {
            voxelShape = VoxelShapes.or(voxelShape, WEST_AABB);
        }

        return voxelShape.isEmpty() ? DOWN_AABB : voxelShape;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return this.canPlace(state, world, pos) ? this.getStateWithAge(world, pos, state.get(AGE)) : Blocks.AIR.getBlockData();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapesCache.get(state.set(AGE, Integer.valueOf(0)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getPlacedState(ctx.getWorld(), ctx.getClickPosition());
    }

    protected IBlockData getPlacedState(IBlockAccess world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        if (!this.canBurn(blockState) && !blockState.isFaceSturdy(world, blockPos, EnumDirection.UP)) {
            IBlockData blockState2 = this.getBlockData();

            for(EnumDirection direction : EnumDirection.values()) {
                BlockStateBoolean booleanProperty = PROPERTY_BY_DIRECTION.get(direction);
                if (booleanProperty != null) {
                    blockState2 = blockState2.set(booleanProperty, Boolean.valueOf(this.canBurn(world.getType(pos.relative(direction)))));
                }
            }

            return blockState2;
        } else {
            return this.getBlockData();
        }
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        return world.getType(blockPos).isFaceSturdy(world, blockPos, EnumDirection.UP) || this.canBurn(world, pos);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        world.getBlockTicks().scheduleTick(pos, this, getFireTickDelay(world.random));
        if (world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            if (!state.canPlace(world, pos)) {
                world.removeBlock(pos, false);
            }

            IBlockData blockState = world.getType(pos.below());
            boolean bl = blockState.is(world.getDimensionManager().infiniburn());
            int i = state.get(AGE);
            if (!bl && world.isRaining() && this.isNearRain(world, pos) && random.nextFloat() < 0.2F + (float)i * 0.03F) {
                world.removeBlock(pos, false);
            } else {
                int j = Math.min(15, i + random.nextInt(3) / 2);
                if (i != j) {
                    state = state.set(AGE, Integer.valueOf(j));
                    world.setTypeAndData(pos, state, 4);
                }

                if (!bl) {
                    if (!this.canBurn(world, pos)) {
                        BlockPosition blockPos = pos.below();
                        if (!world.getType(blockPos).isFaceSturdy(world, blockPos, EnumDirection.UP) || i > 3) {
                            world.removeBlock(pos, false);
                        }

                        return;
                    }

                    if (i == 15 && random.nextInt(4) == 0 && !this.canBurn(world.getType(pos.below()))) {
                        world.removeBlock(pos, false);
                        return;
                    }
                }

                boolean bl2 = world.isHumidAt(pos);
                int k = bl2 ? -50 : 0;
                this.trySpread(world, pos.east(), 300 + k, random, i);
                this.trySpread(world, pos.west(), 300 + k, random, i);
                this.trySpread(world, pos.below(), 250 + k, random, i);
                this.trySpread(world, pos.above(), 250 + k, random, i);
                this.trySpread(world, pos.north(), 300 + k, random, i);
                this.trySpread(world, pos.south(), 300 + k, random, i);
                BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

                for(int l = -1; l <= 1; ++l) {
                    for(int m = -1; m <= 1; ++m) {
                        for(int n = -1; n <= 4; ++n) {
                            if (l != 0 || n != 0 || m != 0) {
                                int o = 100;
                                if (n > 1) {
                                    o += (n - 1) * 100;
                                }

                                mutableBlockPos.setWithOffset(pos, l, n, m);
                                int p = this.getFireOdds(world, mutableBlockPos);
                                if (p > 0) {
                                    int q = (p + 40 + world.getDifficulty().getId() * 7) / (i + 30);
                                    if (bl2) {
                                        q /= 2;
                                    }

                                    if (q > 0 && random.nextInt(o) <= q && (!world.isRaining() || !this.isNearRain(world, mutableBlockPos))) {
                                        int r = Math.min(15, i + random.nextInt(5) / 4);
                                        world.setTypeAndData(mutableBlockPos, this.getStateWithAge(world, mutableBlockPos, r), 3);
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    protected boolean isNearRain(World world, BlockPosition pos) {
        return world.isRainingAt(pos) || world.isRainingAt(pos.west()) || world.isRainingAt(pos.east()) || world.isRainingAt(pos.north()) || world.isRainingAt(pos.south());
    }

    private int getBurnChance(IBlockData state) {
        return state.hasProperty(BlockProperties.WATERLOGGED) && state.get(BlockProperties.WATERLOGGED) ? 0 : this.burnOdds.getInt(state.getBlock());
    }

    private int getFlameChance(IBlockData state) {
        return state.hasProperty(BlockProperties.WATERLOGGED) && state.get(BlockProperties.WATERLOGGED) ? 0 : this.flameOdds.getInt(state.getBlock());
    }

    private void trySpread(World world, BlockPosition pos, int spreadFactor, Random rand, int currentAge) {
        int i = this.getBurnChance(world.getType(pos));
        if (rand.nextInt(spreadFactor) < i) {
            IBlockData blockState = world.getType(pos);
            if (rand.nextInt(currentAge + 10) < 5 && !world.isRainingAt(pos)) {
                int j = Math.min(currentAge + rand.nextInt(5) / 4, 15);
                world.setTypeAndData(pos, this.getStateWithAge(world, pos, j), 3);
            } else {
                world.removeBlock(pos, false);
            }

            Block block = blockState.getBlock();
            if (block instanceof BlockTNT) {
                BlockTNT var10000 = (BlockTNT)block;
                BlockTNT.explode(world, pos);
            }
        }

    }

    private IBlockData getStateWithAge(GeneratorAccess world, BlockPosition pos, int age) {
        IBlockData blockState = getState(world, pos);
        return blockState.is(Blocks.FIRE) ? blockState.set(AGE, Integer.valueOf(age)) : blockState;
    }

    private boolean canBurn(IBlockAccess world, BlockPosition pos) {
        for(EnumDirection direction : EnumDirection.values()) {
            if (this.canBurn(world.getType(pos.relative(direction)))) {
                return true;
            }
        }

        return false;
    }

    private int getFireOdds(IWorldReader world, BlockPosition pos) {
        if (!world.isEmpty(pos)) {
            return 0;
        } else {
            int i = 0;

            for(EnumDirection direction : EnumDirection.values()) {
                IBlockData blockState = world.getType(pos.relative(direction));
                i = Math.max(this.getFlameChance(blockState), i);
            }

            return i;
        }
    }

    @Override
    protected boolean canBurn(IBlockData state) {
        return this.getFlameChance(state) > 0;
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        world.getBlockTickList().scheduleTick(pos, this, getFireTickDelay(world.random));
    }

    private static int getFireTickDelay(Random random) {
        return 30 + random.nextInt(10);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE, NORTH, EAST, SOUTH, WEST, UP);
    }

    private void setFlammable(Block block, int burnChance, int spreadChance) {
        this.flameOdds.put(block, burnChance);
        this.burnOdds.put(block, spreadChance);
    }

    public static void bootStrap() {
        BlockFire fireBlock = (BlockFire)Blocks.FIRE;
        fireBlock.setFlammable(Blocks.OAK_PLANKS, 5, 20);
        fireBlock.setFlammable(Blocks.SPRUCE_PLANKS, 5, 20);
        fireBlock.setFlammable(Blocks.BIRCH_PLANKS, 5, 20);
        fireBlock.setFlammable(Blocks.JUNGLE_PLANKS, 5, 20);
        fireBlock.setFlammable(Blocks.ACACIA_PLANKS, 5, 20);
        fireBlock.setFlammable(Blocks.DARK_OAK_PLANKS, 5, 20);
        fireBlock.setFlammable(Blocks.OAK_SLAB, 5, 20);
        fireBlock.setFlammable(Blocks.SPRUCE_SLAB, 5, 20);
        fireBlock.setFlammable(Blocks.BIRCH_SLAB, 5, 20);
        fireBlock.setFlammable(Blocks.JUNGLE_SLAB, 5, 20);
        fireBlock.setFlammable(Blocks.ACACIA_SLAB, 5, 20);
        fireBlock.setFlammable(Blocks.DARK_OAK_SLAB, 5, 20);
        fireBlock.setFlammable(Blocks.OAK_FENCE_GATE, 5, 20);
        fireBlock.setFlammable(Blocks.SPRUCE_FENCE_GATE, 5, 20);
        fireBlock.setFlammable(Blocks.BIRCH_FENCE_GATE, 5, 20);
        fireBlock.setFlammable(Blocks.JUNGLE_FENCE_GATE, 5, 20);
        fireBlock.setFlammable(Blocks.DARK_OAK_FENCE_GATE, 5, 20);
        fireBlock.setFlammable(Blocks.ACACIA_FENCE_GATE, 5, 20);
        fireBlock.setFlammable(Blocks.OAK_FENCE, 5, 20);
        fireBlock.setFlammable(Blocks.SPRUCE_FENCE, 5, 20);
        fireBlock.setFlammable(Blocks.BIRCH_FENCE, 5, 20);
        fireBlock.setFlammable(Blocks.JUNGLE_FENCE, 5, 20);
        fireBlock.setFlammable(Blocks.DARK_OAK_FENCE, 5, 20);
        fireBlock.setFlammable(Blocks.ACACIA_FENCE, 5, 20);
        fireBlock.setFlammable(Blocks.OAK_STAIRS, 5, 20);
        fireBlock.setFlammable(Blocks.BIRCH_STAIRS, 5, 20);
        fireBlock.setFlammable(Blocks.SPRUCE_STAIRS, 5, 20);
        fireBlock.setFlammable(Blocks.JUNGLE_STAIRS, 5, 20);
        fireBlock.setFlammable(Blocks.ACACIA_STAIRS, 5, 20);
        fireBlock.setFlammable(Blocks.DARK_OAK_STAIRS, 5, 20);
        fireBlock.setFlammable(Blocks.OAK_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.SPRUCE_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.BIRCH_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.JUNGLE_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.ACACIA_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.DARK_OAK_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_OAK_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_SPRUCE_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_BIRCH_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_JUNGLE_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_ACACIA_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_DARK_OAK_LOG, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_OAK_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_SPRUCE_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_BIRCH_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_JUNGLE_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_ACACIA_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.STRIPPED_DARK_OAK_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.OAK_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.SPRUCE_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.BIRCH_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.JUNGLE_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.ACACIA_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.DARK_OAK_WOOD, 5, 5);
        fireBlock.setFlammable(Blocks.OAK_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.SPRUCE_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.BIRCH_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.JUNGLE_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.ACACIA_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.DARK_OAK_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.BOOKSHELF, 30, 20);
        fireBlock.setFlammable(Blocks.TNT, 15, 100);
        fireBlock.setFlammable(Blocks.GRASS, 60, 100);
        fireBlock.setFlammable(Blocks.FERN, 60, 100);
        fireBlock.setFlammable(Blocks.DEAD_BUSH, 60, 100);
        fireBlock.setFlammable(Blocks.SUNFLOWER, 60, 100);
        fireBlock.setFlammable(Blocks.LILAC, 60, 100);
        fireBlock.setFlammable(Blocks.ROSE_BUSH, 60, 100);
        fireBlock.setFlammable(Blocks.PEONY, 60, 100);
        fireBlock.setFlammable(Blocks.TALL_GRASS, 60, 100);
        fireBlock.setFlammable(Blocks.LARGE_FERN, 60, 100);
        fireBlock.setFlammable(Blocks.DANDELION, 60, 100);
        fireBlock.setFlammable(Blocks.POPPY, 60, 100);
        fireBlock.setFlammable(Blocks.BLUE_ORCHID, 60, 100);
        fireBlock.setFlammable(Blocks.ALLIUM, 60, 100);
        fireBlock.setFlammable(Blocks.AZURE_BLUET, 60, 100);
        fireBlock.setFlammable(Blocks.RED_TULIP, 60, 100);
        fireBlock.setFlammable(Blocks.ORANGE_TULIP, 60, 100);
        fireBlock.setFlammable(Blocks.WHITE_TULIP, 60, 100);
        fireBlock.setFlammable(Blocks.PINK_TULIP, 60, 100);
        fireBlock.setFlammable(Blocks.OXEYE_DAISY, 60, 100);
        fireBlock.setFlammable(Blocks.CORNFLOWER, 60, 100);
        fireBlock.setFlammable(Blocks.LILY_OF_THE_VALLEY, 60, 100);
        fireBlock.setFlammable(Blocks.WITHER_ROSE, 60, 100);
        fireBlock.setFlammable(Blocks.WHITE_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.ORANGE_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.MAGENTA_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.LIGHT_BLUE_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.YELLOW_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.LIME_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.PINK_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.GRAY_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.LIGHT_GRAY_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.CYAN_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.PURPLE_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.BLUE_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.BROWN_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.GREEN_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.RED_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.BLACK_WOOL, 30, 60);
        fireBlock.setFlammable(Blocks.VINE, 15, 100);
        fireBlock.setFlammable(Blocks.COAL_BLOCK, 5, 5);
        fireBlock.setFlammable(Blocks.HAY_BLOCK, 60, 20);
        fireBlock.setFlammable(Blocks.TARGET, 15, 20);
        fireBlock.setFlammable(Blocks.WHITE_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.ORANGE_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.MAGENTA_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.LIGHT_BLUE_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.YELLOW_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.LIME_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.PINK_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.GRAY_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.LIGHT_GRAY_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.CYAN_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.PURPLE_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.BLUE_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.BROWN_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.GREEN_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.RED_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.BLACK_CARPET, 60, 20);
        fireBlock.setFlammable(Blocks.DRIED_KELP_BLOCK, 30, 60);
        fireBlock.setFlammable(Blocks.BAMBOO, 60, 60);
        fireBlock.setFlammable(Blocks.SCAFFOLDING, 60, 60);
        fireBlock.setFlammable(Blocks.LECTERN, 30, 20);
        fireBlock.setFlammable(Blocks.COMPOSTER, 5, 20);
        fireBlock.setFlammable(Blocks.SWEET_BERRY_BUSH, 60, 100);
        fireBlock.setFlammable(Blocks.BEEHIVE, 5, 20);
        fireBlock.setFlammable(Blocks.BEE_NEST, 30, 20);
        fireBlock.setFlammable(Blocks.AZALEA_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.FLOWERING_AZALEA_LEAVES, 30, 60);
        fireBlock.setFlammable(Blocks.CAVE_VINES, 15, 60);
        fireBlock.setFlammable(Blocks.CAVE_VINES_PLANT, 15, 60);
        fireBlock.setFlammable(Blocks.SPORE_BLOSSOM, 60, 100);
        fireBlock.setFlammable(Blocks.AZALEA, 30, 60);
        fireBlock.setFlammable(Blocks.FLOWERING_AZALEA, 30, 60);
        fireBlock.setFlammable(Blocks.BIG_DRIPLEAF, 60, 100);
        fireBlock.setFlammable(Blocks.BIG_DRIPLEAF_STEM, 60, 100);
        fireBlock.setFlammable(Blocks.SMALL_DRIPLEAF, 60, 100);
        fireBlock.setFlammable(Blocks.HANGING_ROOTS, 30, 60);
        fireBlock.setFlammable(Blocks.GLOW_LICHEN, 15, 100);
    }
}
