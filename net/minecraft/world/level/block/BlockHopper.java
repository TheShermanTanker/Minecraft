package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.IHopper;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityHopper;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockHopper extends BlockTileEntity {
    public static final BlockStateDirection FACING = BlockProperties.FACING_HOPPER;
    public static final BlockStateBoolean ENABLED = BlockProperties.ENABLED;
    private static final VoxelShape TOP = Block.box(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape FUNNEL = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 10.0D, 12.0D);
    private static final VoxelShape CONVEX_BASE = VoxelShapes.or(FUNNEL, TOP);
    private static final VoxelShape BASE = VoxelShapes.join(CONVEX_BASE, IHopper.INSIDE, OperatorBoolean.ONLY_FIRST);
    private static final VoxelShape DOWN_SHAPE = VoxelShapes.or(BASE, Block.box(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D));
    private static final VoxelShape EAST_SHAPE = VoxelShapes.or(BASE, Block.box(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D));
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.or(BASE, Block.box(6.0D, 4.0D, 0.0D, 10.0D, 8.0D, 4.0D));
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.or(BASE, Block.box(6.0D, 4.0D, 12.0D, 10.0D, 8.0D, 16.0D));
    private static final VoxelShape WEST_SHAPE = VoxelShapes.or(BASE, Block.box(0.0D, 4.0D, 6.0D, 4.0D, 8.0D, 10.0D));
    private static final VoxelShape DOWN_INTERACTION_SHAPE = IHopper.INSIDE;
    private static final VoxelShape EAST_INTERACTION_SHAPE = VoxelShapes.or(IHopper.INSIDE, Block.box(12.0D, 8.0D, 6.0D, 16.0D, 10.0D, 10.0D));
    private static final VoxelShape NORTH_INTERACTION_SHAPE = VoxelShapes.or(IHopper.INSIDE, Block.box(6.0D, 8.0D, 0.0D, 10.0D, 10.0D, 4.0D));
    private static final VoxelShape SOUTH_INTERACTION_SHAPE = VoxelShapes.or(IHopper.INSIDE, Block.box(6.0D, 8.0D, 12.0D, 10.0D, 10.0D, 16.0D));
    private static final VoxelShape WEST_INTERACTION_SHAPE = VoxelShapes.or(IHopper.INSIDE, Block.box(0.0D, 8.0D, 6.0D, 4.0D, 10.0D, 10.0D));

    public BlockHopper(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.DOWN).set(ENABLED, Boolean.valueOf(true)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((EnumDirection)state.get(FACING)) {
        case DOWN:
            return DOWN_SHAPE;
        case NORTH:
            return NORTH_SHAPE;
        case SOUTH:
            return SOUTH_SHAPE;
        case WEST:
            return WEST_SHAPE;
        case EAST:
            return EAST_SHAPE;
        default:
            return BASE;
        }
    }

    @Override
    public VoxelShape getInteractionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        switch((EnumDirection)state.get(FACING)) {
        case DOWN:
            return DOWN_INTERACTION_SHAPE;
        case NORTH:
            return NORTH_INTERACTION_SHAPE;
        case SOUTH:
            return SOUTH_INTERACTION_SHAPE;
        case WEST:
            return WEST_INTERACTION_SHAPE;
        case EAST:
            return EAST_INTERACTION_SHAPE;
        default:
            return IHopper.INSIDE;
        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        EnumDirection direction = ctx.getClickedFace().opposite();
        return this.getBlockData().set(FACING, direction.getAxis() == EnumDirection.EnumAxis.Y ? EnumDirection.DOWN : direction).set(ENABLED, Boolean.valueOf(true));
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityHopper(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return world.isClientSide ? null : createTickerHelper(type, TileEntityTypes.HOPPER, TileEntityHopper::pushItemsTick);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityHopper) {
                ((TileEntityHopper)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.checkPoweredState(world, pos, state);
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityHopper) {
                player.openContainer((TileEntityHopper)blockEntity);
                player.awardStat(StatisticList.INSPECT_HOPPER);
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        this.checkPoweredState(world, pos, state);
    }

    private void checkPoweredState(World world, BlockPosition pos, IBlockData state) {
        boolean bl = !world.isBlockIndirectlyPowered(pos);
        if (bl != state.get(ENABLED)) {
            world.setTypeAndData(pos, state.set(ENABLED, Boolean.valueOf(bl)), 4);
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityHopper) {
                InventoryUtils.dropInventory(world, pos, (TileEntityHopper)blockEntity);
                world.updateAdjacentComparators(pos, this);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return Container.getRedstoneSignalFromBlockEntity(world.getTileEntity(pos));
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
        builder.add(FACING, ENABLED);
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityHopper) {
            TileEntityHopper.entityInside(world, pos, state, entity, (TileEntityHopper)blockEntity);
        }

    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
