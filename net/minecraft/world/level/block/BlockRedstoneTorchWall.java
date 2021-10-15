package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockRedstoneTorchWall extends BlockRedstoneTorch {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean LIT = BlockRedstoneTorch.LIT;

    protected BlockRedstoneTorchWall(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(LIT, Boolean.valueOf(true)));
    }

    @Override
    public String getDescriptionId() {
        return this.getItem().getName();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return BlockTorchWall.getShape(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return Blocks.WALL_TORCH.canPlace(state, world, pos);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return Blocks.WALL_TORCH.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = Blocks.WALL_TORCH.getPlacedState(ctx);
        return blockState == null ? null : this.getBlockData().set(FACING, blockState.get(FACING));
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            EnumDirection direction = state.get(FACING).opposite();
            double d = 0.27D;
            double e = (double)pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D + 0.27D * (double)direction.getAdjacentX();
            double f = (double)pos.getY() + 0.7D + (random.nextDouble() - 0.5D) * 0.2D + 0.22D;
            double g = (double)pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D + 0.27D * (double)direction.getAdjacentZ();
            world.addParticle(this.flameParticle, e, f, g, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected boolean hasNeighborSignal(World world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING).opposite();
        return world.isBlockFacePowered(pos.relative(direction), direction);
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(LIT) && state.get(FACING) != direction ? 15 : 0;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return Blocks.WALL_TORCH.rotate(state, rotation);
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return Blocks.WALL_TORCH.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, LIT);
    }
}
