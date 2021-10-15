package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.vehicle.EntityMinecartAbstract;
import net.minecraft.world.entity.vehicle.EntityMinecartCommandBlock;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.AxisAlignedBB;

public class BlockMinecartDetector extends BlockMinecartTrackAbstract {
    public static final BlockStateEnum<BlockPropertyTrackPosition> SHAPE = BlockProperties.RAIL_SHAPE_STRAIGHT;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final int PRESSED_CHECK_PERIOD = 20;

    public BlockMinecartDetector(BlockBase.Info settings) {
        super(true, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(POWERED, Boolean.valueOf(false)).set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide) {
            if (!state.get(POWERED)) {
                this.checkPressed(world, pos, state);
            }
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(POWERED)) {
            this.checkPressed(world, pos, state);
        }
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        if (!state.get(POWERED)) {
            return 0;
        } else {
            return direction == EnumDirection.UP ? 15 : 0;
        }
    }

    private void checkPressed(World world, BlockPosition pos, IBlockData state) {
        if (this.canPlace(state, world, pos)) {
            boolean bl = state.get(POWERED);
            boolean bl2 = false;
            List<EntityMinecartAbstract> list = this.getInteractingMinecartOfType(world, pos, EntityMinecartAbstract.class, (entity) -> {
                return true;
            });
            if (!list.isEmpty()) {
                bl2 = true;
            }

            if (bl2 && !bl) {
                IBlockData blockState = state.set(POWERED, Boolean.valueOf(true));
                world.setTypeAndData(pos, blockState, 3);
                this.updatePowerToConnected(world, pos, blockState, true);
                world.applyPhysics(pos, this);
                world.applyPhysics(pos.below(), this);
                world.setBlocksDirty(pos, state, blockState);
            }

            if (!bl2 && bl) {
                IBlockData blockState2 = state.set(POWERED, Boolean.valueOf(false));
                world.setTypeAndData(pos, blockState2, 3);
                this.updatePowerToConnected(world, pos, blockState2, false);
                world.applyPhysics(pos, this);
                world.applyPhysics(pos.below(), this);
                world.setBlocksDirty(pos, state, blockState2);
            }

            if (bl2) {
                world.getBlockTickList().scheduleTick(pos, this, 20);
            }

            world.updateAdjacentComparators(pos, this);
        }
    }

    protected void updatePowerToConnected(World world, BlockPosition pos, IBlockData state, boolean unpowering) {
        MinecartTrackLogic railState = new MinecartTrackLogic(world, pos, state);

        for(BlockPosition blockPos : railState.getConnections()) {
            IBlockData blockState = world.getType(blockPos);
            blockState.doPhysics(world, blockPos, blockState.getBlock(), pos, false);
        }

    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            IBlockData blockState = this.updateState(state, world, pos, notify);
            this.checkPressed(world, pos, blockState);
        }
    }

    @Override
    public IBlockState<BlockPropertyTrackPosition> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        if (state.get(POWERED)) {
            List<EntityMinecartCommandBlock> list = this.getInteractingMinecartOfType(world, pos, EntityMinecartCommandBlock.class, (entity) -> {
                return true;
            });
            if (!list.isEmpty()) {
                return list.get(0).getCommandBlock().getSuccessCount();
            }

            List<EntityMinecartAbstract> list2 = this.getInteractingMinecartOfType(world, pos, EntityMinecartAbstract.class, IEntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!list2.isEmpty()) {
                return Container.getRedstoneSignalFromContainer((IInventory)list2.get(0));
            }
        }

        return 0;
    }

    private <T extends EntityMinecartAbstract> List<T> getInteractingMinecartOfType(World world, BlockPosition pos, Class<T> entityClass, Predicate<Entity> entityPredicate) {
        return world.getEntitiesOfClass(entityClass, this.getSearchBB(pos), entityPredicate);
    }

    private AxisAlignedBB getSearchBB(BlockPosition pos) {
        double d = 0.2D;
        return new AxisAlignedBB((double)pos.getX() + 0.2D, (double)pos.getY(), (double)pos.getZ() + 0.2D, (double)(pos.getX() + 1) - 0.2D, (double)(pos.getY() + 1) - 0.2D, (double)(pos.getZ() + 1) - 0.2D);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            }
        case COUNTERCLOCKWISE_90:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case NORTH_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.EAST_WEST);
            case EAST_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);
            }
        case CLOCKWISE_90:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.EAST_WEST);
            case EAST_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);
            }
        default:
            return state;
        }
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        BlockPropertyTrackPosition railShape = state.get(SHAPE);
        switch(mirror) {
        case LEFT_RIGHT:
            switch(railShape) {
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            default:
                return super.mirror(state, mirror);
            }
        case FRONT_BACK:
            switch(railShape) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
            default:
                break;
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            }
        }

        return super.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }
}
