package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public abstract class BlockGrowingAbstract extends Block {
    protected final EnumDirection growthDirection;
    protected final boolean scheduleFluidTicks;
    protected final VoxelShape shape;

    protected BlockGrowingAbstract(BlockBase.Info settings, EnumDirection growthDirection, VoxelShape outlineShape, boolean tickWater) {
        super(settings);
        this.growthDirection = growthDirection;
        this.shape = outlineShape;
        this.scheduleFluidTicks = tickWater;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().relative(this.growthDirection));
        return !blockState.is(this.getHeadBlock()) && !blockState.is(this.getBodyBlock()) ? this.getStateForPlacement(ctx.getWorld()) : this.getBodyBlock().getBlockData();
    }

    public IBlockData getStateForPlacement(GeneratorAccess world) {
        return this.getBlockData();
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.relative(this.growthDirection.opposite());
        IBlockData blockState = world.getType(blockPos);
        if (!this.canAttachTo(blockState)) {
            return false;
        } else {
            return blockState.is(this.getHeadBlock()) || blockState.is(this.getBodyBlock()) || blockState.isFaceSturdy(world, blockPos, this.growthDirection);
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    protected boolean canAttachTo(IBlockData state) {
        return true;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shape;
    }

    protected abstract BlockGrowingTop getHeadBlock();

    protected abstract Block getBodyBlock();
}
