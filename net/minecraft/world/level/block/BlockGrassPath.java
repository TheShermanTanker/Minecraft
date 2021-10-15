package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockGrassPath extends Block {
    protected static final VoxelShape SHAPE = BlockSoil.SHAPE;

    protected BlockGrassPath(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return !this.getBlockData().canPlace(ctx.getWorld(), ctx.getClickPosition()) ? Block.pushEntitiesUp(this.getBlockData(), Blocks.DIRT.getBlockData(), ctx.getWorld(), ctx.getClickPosition()) : super.getPlacedState(ctx);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.UP && !state.canPlace(world, pos)) {
            world.getBlockTickList().scheduleTick(pos, this, 1);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        BlockSoil.fade(state, world, pos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.above());
        return !blockState.getMaterial().isBuildable() || blockState.getBlock() instanceof BlockFenceGate;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
