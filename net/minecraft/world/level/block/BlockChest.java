package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.InventoryLargeChest;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyChestType;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockChest extends BlockChestAbstract<TileEntityChest> implements IBlockWaterlogged {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateEnum<BlockPropertyChestType> TYPE = BlockProperties.CHEST_TYPE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final int EVENT_SET_OPEN_COUNT = 1;
    protected static final int AABB_OFFSET = 1;
    protected static final int AABB_HEIGHT = 14;
    protected static final VoxelShape NORTH_AABB = Block.box(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    protected static final VoxelShape EAST_AABB = Block.box(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D);
    protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    private static final DoubleBlockFinder.Combiner<TileEntityChest, Optional<IInventory>> CHEST_COMBINER = new DoubleBlockFinder.Combiner<TileEntityChest, Optional<IInventory>>() {
        @Override
        public Optional<IInventory> acceptDouble(TileEntityChest first, TileEntityChest second) {
            return Optional.of(new InventoryLargeChest(first, second));
        }

        @Override
        public Optional<IInventory> acceptSingle(TileEntityChest single) {
            return Optional.of(single);
        }

        @Override
        public Optional<IInventory> acceptNone() {
            return Optional.empty();
        }
    };
    private static final DoubleBlockFinder.Combiner<TileEntityChest, Optional<ITileInventory>> MENU_PROVIDER_COMBINER = new DoubleBlockFinder.Combiner<TileEntityChest, Optional<ITileInventory>>() {
        @Override
        public Optional<ITileInventory> acceptDouble(TileEntityChest first, TileEntityChest second) {
            final IInventory container = new InventoryLargeChest(first, second);
            return Optional.of(new ITileInventory() {
                @Nullable
                @Override
                public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
                    if (first.canOpen(player) && second.canOpen(player)) {
                        first.unpackLootTable(inv.player);
                        second.unpackLootTable(inv.player);
                        return ContainerChest.sixRows(syncId, inv, container);
                    } else {
                        return null;
                    }
                }

                @Override
                public IChatBaseComponent getScoreboardDisplayName() {
                    if (first.hasCustomName()) {
                        return first.getScoreboardDisplayName();
                    } else {
                        return (IChatBaseComponent)(second.hasCustomName() ? second.getScoreboardDisplayName() : new ChatMessage("container.chestDouble"));
                    }
                }
            });
        }

        @Override
        public Optional<ITileInventory> acceptSingle(TileEntityChest single) {
            return Optional.of(single);
        }

        @Override
        public Optional<ITileInventory> acceptNone() {
            return Optional.empty();
        }
    };

    protected BlockChest(BlockBase.Info settings, Supplier<TileEntityTypes<? extends TileEntityChest>> entityTypeSupplier) {
        super(settings, entityTypeSupplier);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(TYPE, BlockPropertyChestType.SINGLE).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    public static DoubleBlockFinder.BlockType getBlockType(IBlockData state) {
        BlockPropertyChestType chestType = state.get(TYPE);
        if (chestType == BlockPropertyChestType.SINGLE) {
            return DoubleBlockFinder.BlockType.SINGLE;
        } else {
            return chestType == BlockPropertyChestType.RIGHT ? DoubleBlockFinder.BlockType.FIRST : DoubleBlockFinder.BlockType.SECOND;
        }
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        if (neighborState.is(this) && direction.getAxis().isHorizontal()) {
            BlockPropertyChestType chestType = neighborState.get(TYPE);
            if (state.get(TYPE) == BlockPropertyChestType.SINGLE && chestType != BlockPropertyChestType.SINGLE && state.get(FACING) == neighborState.get(FACING) && getConnectedDirection(neighborState) == direction.opposite()) {
                return state.set(TYPE, chestType.getOpposite());
            }
        } else if (getConnectedDirection(state) == direction) {
            return state.set(TYPE, BlockPropertyChestType.SINGLE);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (state.get(TYPE) == BlockPropertyChestType.SINGLE) {
            return AABB;
        } else {
            switch(getConnectedDirection(state)) {
            case NORTH:
            default:
                return NORTH_AABB;
            case SOUTH:
                return SOUTH_AABB;
            case WEST:
                return WEST_AABB;
            case EAST:
                return EAST_AABB;
            }
        }
    }

    public static EnumDirection getConnectedDirection(IBlockData state) {
        EnumDirection direction = state.get(FACING);
        return state.get(TYPE) == BlockPropertyChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        BlockPropertyChestType chestType = BlockPropertyChestType.SINGLE;
        EnumDirection direction = ctx.getHorizontalDirection().opposite();
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        boolean bl = ctx.isSneaking();
        EnumDirection direction2 = ctx.getClickedFace();
        if (direction2.getAxis().isHorizontal() && bl) {
            EnumDirection direction3 = this.candidatePartnerFacing(ctx, direction2.opposite());
            if (direction3 != null && direction3.getAxis() != direction2.getAxis()) {
                direction = direction3;
                chestType = direction3.getCounterClockWise() == direction2.opposite() ? BlockPropertyChestType.RIGHT : BlockPropertyChestType.LEFT;
            }
        }

        if (chestType == BlockPropertyChestType.SINGLE && !bl) {
            if (direction == this.candidatePartnerFacing(ctx, direction.getClockWise())) {
                chestType = BlockPropertyChestType.LEFT;
            } else if (direction == this.candidatePartnerFacing(ctx, direction.getCounterClockWise())) {
                chestType = BlockPropertyChestType.RIGHT;
            }
        }

        return this.getBlockData().set(FACING, direction).set(TYPE, chestType).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    private EnumDirection candidatePartnerFacing(BlockActionContext ctx, EnumDirection dir) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().relative(dir));
        return blockState.is(this) && blockState.get(TYPE) == BlockPropertyChestType.SINGLE ? blockState.get(FACING) : null;
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityChest) {
                ((TileEntityChest)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof IInventory) {
                InventoryUtils.dropInventory(world, pos, (IInventory)blockEntity);
                world.updateAdjacentComparators(pos, this);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            ITileInventory menuProvider = this.getInventory(state, world, pos);
            if (menuProvider != null) {
                player.openContainer(menuProvider);
                player.awardStat(this.getOpenChestStat());
                PiglinAI.angerNearbyPiglins(player, true);
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    protected Statistic<MinecraftKey> getOpenChestStat() {
        return StatisticList.CUSTOM.get(StatisticList.OPEN_CHEST);
    }

    public TileEntityTypes<? extends TileEntityChest> blockEntityType() {
        return this.blockEntityType.get();
    }

    @Nullable
    public static IInventory getInventory(BlockChest block, IBlockData state, World world, BlockPosition pos, boolean ignoreBlocked) {
        return block.combine(state, world, pos, ignoreBlocked).<Optional<IInventory>>apply(CHEST_COMBINER).orElse((IInventory)null);
    }

    @Override
    public DoubleBlockFinder.Result<? extends TileEntityChest> combine(IBlockData state, World world, BlockPosition pos, boolean ignoreBlocked) {
        BiPredicate<GeneratorAccess, BlockPosition> biPredicate;
        if (ignoreBlocked) {
            biPredicate = (worldx, posx) -> {
                return false;
            };
        } else {
            biPredicate = BlockChest::isChestBlockedAt;
        }

        return DoubleBlockFinder.combineWithNeigbour(this.blockEntityType.get(), BlockChest::getBlockType, BlockChest::getConnectedDirection, FACING, state, world, pos, biPredicate);
    }

    @Nullable
    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return this.combine(state, world, pos, false).<Optional<ITileInventory>>apply(MENU_PROVIDER_COMBINER).orElse((ITileInventory)null);
    }

    public static DoubleBlockFinder.Combiner<TileEntityChest, Float2FloatFunction> opennessCombiner(LidBlockEntity lidBlockEntity) {
        return new DoubleBlockFinder.Combiner<TileEntityChest, Float2FloatFunction>() {
            @Override
            public Float2FloatFunction acceptDouble(TileEntityChest first, TileEntityChest second) {
                return (f) -> {
                    return Math.max(first.getOpenNess(f), second.getOpenNess(f));
                };
            }

            @Override
            public Float2FloatFunction acceptSingle(TileEntityChest single) {
                return single::getOpenNess;
            }

            @Override
            public Float2FloatFunction acceptNone() {
                return lidBlockEntity::getOpenNess;
            }
        };
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityChest(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return world.isClientSide ? createTickerHelper(type, this.blockEntityType(), TileEntityChest::lidAnimateTick) : null;
    }

    public static boolean isChestBlockedAt(GeneratorAccess world, BlockPosition pos) {
        return isBlockedChestByBlock(world, pos) || isCatSittingOnChest(world, pos);
    }

    private static boolean isBlockedChestByBlock(IBlockAccess world, BlockPosition pos) {
        BlockPosition blockPos = pos.above();
        return world.getType(blockPos).isOccluding(world, blockPos);
    }

    private static boolean isCatSittingOnChest(GeneratorAccess world, BlockPosition pos) {
        List<EntityCat> list = world.getEntitiesOfClass(EntityCat.class, new AxisAlignedBB((double)pos.getX(), (double)(pos.getY() + 1), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 2), (double)(pos.getZ() + 1)));
        if (!list.isEmpty()) {
            for(EntityCat cat : list) {
                if (cat.isSitting()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return Container.getRedstoneSignalFromContainer(getInventory(this, state, world, pos, false));
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, TYPE, WATERLOGGED);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityChest) {
            ((TileEntityChest)blockEntity).recheckOpen();
        }

    }
}
