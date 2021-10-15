package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.grower.WorldGenTreeProvider;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSapling extends BlockPlant implements IBlockFragilePlantElement {
    public static final BlockStateInteger STAGE = BlockProperties.STAGE;
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);
    private final WorldGenTreeProvider treeGrower;

    protected BlockSapling(WorldGenTreeProvider generator, BlockBase.Info settings) {
        super(settings);
        this.treeGrower = generator;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(STAGE, Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getLightLevel(pos.above()) >= 9 && random.nextInt(7) == 0) {
            this.grow(world, pos, state, random);
        }

    }

    public void grow(WorldServer world, BlockPosition pos, IBlockData state, Random random) {
        if (state.get(STAGE) == 0) {
            world.setTypeAndData(pos, state.cycle(STAGE), 4);
        } else {
            this.treeGrower.growTree(world, world.getChunkSource().getChunkGenerator(), pos, state, random);
        }

    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return (double)world.random.nextFloat() < 0.45D;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        this.grow(world, pos, state, random);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(STAGE);
    }
}
