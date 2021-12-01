package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public abstract class BlockPressurePlateAbstract extends Block {
    protected static final VoxelShape PRESSED_AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 0.5D, 15.0D);
    protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    protected static final AxisAlignedBB TOUCH_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.25D, 0.875D);

    protected BlockPressurePlateAbstract(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getPower(state) > 0 ? PRESSED_AABB : AABB;
    }

    protected int getPressedTime() {
        return 20;
    }

    @Override
    public boolean isPossibleToRespawnInThis() {
        return true;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.DOWN && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        return canSupportRigidBlock(world, blockPos) || canSupportCenter(world, blockPos, EnumDirection.UP);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        int i = this.getPower(state);
        if (i > 0) {
            this.checkPressed((Entity)null, world, pos, state, i);
        }

    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide) {
            int i = this.getPower(state);
            if (i == 0) {
                this.checkPressed(entity, world, pos, state, i);
            }

        }
    }

    protected void checkPressed(@Nullable Entity entity, World world, BlockPosition pos, IBlockData state, int output) {
        int i = this.getSignalStrength(world, pos);
        boolean bl = output > 0;
        boolean bl2 = i > 0;
        if (output != i) {
            IBlockData blockState = this.setSignalForState(state, i);
            world.setTypeAndData(pos, blockState, 2);
            this.updateNeighbours(world, pos);
            world.setBlocksDirty(pos, state, blockState);
        }

        if (!bl2 && bl) {
            this.playOffSound(world, pos);
            world.gameEvent(entity, GameEvent.BLOCK_UNPRESS, pos);
        } else if (bl2 && !bl) {
            this.playOnSound(world, pos);
            world.gameEvent(entity, GameEvent.BLOCK_PRESS, pos);
        }

        if (bl2) {
            world.scheduleTick(new BlockPosition(pos), this, this.getPressedTime());
        }

    }

    protected abstract void playOnSound(GeneratorAccess world, BlockPosition pos);

    protected abstract void playOffSound(GeneratorAccess world, BlockPosition pos);

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            if (this.getPower(state) > 0) {
                this.updateNeighbours(world, pos);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    protected void updateNeighbours(World world, BlockPosition pos) {
        world.applyPhysics(pos, this);
        world.applyPhysics(pos.below(), this);
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return this.getPower(state);
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return direction == EnumDirection.UP ? this.getPower(state) : 0;
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    protected abstract int getSignalStrength(World world, BlockPosition pos);

    protected abstract int getPower(IBlockData state);

    protected abstract IBlockData setSignalForState(IBlockData state, int rsOut);
}
