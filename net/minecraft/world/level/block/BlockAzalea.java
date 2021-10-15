package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.grower.WorldGenTreeProviderAzalea;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockAzalea extends BlockPlant implements IBlockFragilePlantElement {
    private static final WorldGenTreeProviderAzalea TREE_GROWER = new WorldGenTreeProviderAzalea();
    private static final VoxelShape SHAPE = VoxelShapes.or(Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D), Block.box(6.0D, 0.0D, 6.0D, 10.0D, 8.0D, 10.0D));

    protected BlockAzalea(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(Blocks.CLAY) || super.mayPlaceOn(floor, world, pos);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return world.getFluid(pos.above()).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return (double)world.random.nextFloat() < 0.45D;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        TREE_GROWER.growTree(world, world.getChunkSource().getChunkGenerator(), pos, state, random);
    }
}
