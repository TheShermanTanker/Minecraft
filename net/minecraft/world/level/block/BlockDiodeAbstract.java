package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.ticks.TickPriority;

public abstract class BlockDiodeAbstract extends BlockFacingHorizontal {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;

    protected BlockDiodeAbstract(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return canSupportRigidBlock(world, pos.below());
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!this.isLocked(world, pos, state)) {
            boolean bl = state.get(POWERED);
            boolean bl2 = this.shouldTurnOn(world, pos, state);
            if (bl && !bl2) {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)), 2);
            } else if (!bl) {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(true)), 2);
                if (!bl2) {
                    world.scheduleTick(pos, this, this.getDelay(state), TickPriority.VERY_HIGH);
                }
            }

        }
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.getSignal(world, pos, direction);
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        if (!state.get(POWERED)) {
            return 0;
        } else {
            return state.get(FACING) == direction ? this.getOutputSignal(world, pos, state) : 0;
        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (state.canPlace(world, pos)) {
            this.checkTickOnNeighbor(world, pos, state);
        } else {
            TileEntity blockEntity = state.isTileEntity() ? world.getTileEntity(pos) : null;
            dropResources(state, world, pos, blockEntity);
            world.removeBlock(pos, false);

            for(EnumDirection direction : EnumDirection.values()) {
                world.applyPhysics(pos.relative(direction), this);
            }

        }
    }

    protected void checkTickOnNeighbor(World world, BlockPosition pos, IBlockData state) {
        if (!this.isLocked(world, pos, state)) {
            boolean bl = state.get(POWERED);
            boolean bl2 = this.shouldTurnOn(world, pos, state);
            if (bl != bl2 && !world.getBlockTicks().willTickThisTick(pos, this)) {
                TickPriority tickPriority = TickPriority.HIGH;
                if (this.shouldPrioritize(world, pos, state)) {
                    tickPriority = TickPriority.EXTREMELY_HIGH;
                } else if (bl) {
                    tickPriority = TickPriority.VERY_HIGH;
                }

                world.scheduleTick(pos, this, this.getDelay(state), tickPriority);
            }

        }
    }

    public boolean isLocked(IWorldReader world, BlockPosition pos, IBlockData state) {
        return false;
    }

    protected boolean shouldTurnOn(World world, BlockPosition pos, IBlockData state) {
        return this.getInputSignal(world, pos, state) > 0;
    }

    protected int getInputSignal(World world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction);
        int i = world.getBlockFacePower(blockPos, direction);
        if (i >= 15) {
            return i;
        } else {
            IBlockData blockState = world.getType(blockPos);
            return Math.max(i, blockState.is(Blocks.REDSTONE_WIRE) ? blockState.get(BlockRedstoneWire.POWER) : 0);
        }
    }

    protected int getAlternateSignal(IWorldReader world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING);
        EnumDirection direction2 = direction.getClockWise();
        EnumDirection direction3 = direction.getCounterClockWise();
        return Math.max(this.getAlternateSignalAt(world, pos.relative(direction2), direction2), this.getAlternateSignalAt(world, pos.relative(direction3), direction3));
    }

    protected int getAlternateSignalAt(IWorldReader world, BlockPosition pos, EnumDirection dir) {
        IBlockData blockState = world.getType(pos);
        if (this.isAlternateInput(blockState)) {
            if (blockState.is(Blocks.REDSTONE_BLOCK)) {
                return 15;
            } else {
                return blockState.is(Blocks.REDSTONE_WIRE) ? blockState.get(BlockRedstoneWire.POWER) : world.getDirectSignal(pos, dir);
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite());
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (this.shouldTurnOn(world, pos, state)) {
            world.scheduleTick(pos, this, 1);
        }

    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        this.updateNeighborsInFront(world, pos, state);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            super.remove(state, world, pos, newState, moved);
            this.updateNeighborsInFront(world, pos, state);
        }
    }

    protected void updateNeighborsInFront(World world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction.opposite());
        world.neighborChanged(blockPos, this, pos);
        world.updateNeighborsAtExceptFromFacing(blockPos, this, direction);
    }

    protected boolean isAlternateInput(IBlockData state) {
        return state.isPowerSource();
    }

    protected int getOutputSignal(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return 15;
    }

    public static boolean isDiode(IBlockData state) {
        return state.getBlock() instanceof BlockDiodeAbstract;
    }

    public boolean shouldPrioritize(IBlockAccess world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING).opposite();
        IBlockData blockState = world.getType(pos.relative(direction));
        return isDiode(blockState) && blockState.get(FACING) != direction;
    }

    protected abstract int getDelay(IBlockData state);
}
