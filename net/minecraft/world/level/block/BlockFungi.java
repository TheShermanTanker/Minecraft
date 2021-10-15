package net.minecraft.world.level.block;

import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureHugeFungiConfiguration;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockFungi extends BlockPlant implements IBlockFragilePlantElement {
    protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 9.0D, 12.0D);
    private static final double BONEMEAL_SUCCESS_PROBABILITY = 0.4D;
    private final Supplier<WorldGenFeatureConfigured<WorldGenFeatureHugeFungiConfiguration, ?>> feature;

    protected BlockFungi(BlockBase.Info settings, Supplier<WorldGenFeatureConfigured<WorldGenFeatureHugeFungiConfiguration, ?>> feature) {
        super(settings);
        this.feature = feature;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(TagsBlock.NYLIUM) || floor.is(Blocks.MYCELIUM) || floor.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(floor, world, pos);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        Block block = ((WorldGenFeatureHugeFungiConfiguration)(this.feature.get()).config).validBaseState.getBlock();
        IBlockData blockState = world.getType(pos.below());
        return blockState.is(block);
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return (double)random.nextFloat() < 0.4D;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        this.feature.get().place(world, world.getChunkSource().getChunkGenerator(), random, pos);
    }
}
