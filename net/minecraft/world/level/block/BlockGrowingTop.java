package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockGrowingTop extends BlockGrowingAbstract implements IBlockFragilePlantElement {
    public static final BlockStateInteger AGE = BlockProperties.AGE_25;
    public static final int MAX_AGE = 25;
    private final double growPerTickProbability;

    protected BlockGrowingTop(BlockBase.Info settings, EnumDirection growthDirection, VoxelShape outlineShape, boolean tickWater, double growthChance) {
        super(settings, growthDirection, outlineShape, tickWater);
        this.growPerTickProbability = growthChance;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public IBlockData getStateForPlacement(GeneratorAccess world) {
        return this.getBlockData().set(AGE, Integer.valueOf(world.getRandom().nextInt(25)));
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.get(AGE) < 25;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(AGE) < 25 && random.nextDouble() < this.growPerTickProbability) {
            BlockPosition blockPos = pos.relative(this.growthDirection);
            if (this.canGrowInto(world.getType(blockPos))) {
                world.setTypeUpdate(blockPos, this.getGrowIntoState(state, world.random));
            }
        }

    }

    protected IBlockData getGrowIntoState(IBlockData state, Random random) {
        return state.cycle(AGE);
    }

    protected IBlockData updateBodyAfterConvertedFromHead(IBlockData from, IBlockData to) {
        return to;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == this.growthDirection.opposite() && !state.canPlace(world, pos)) {
            world.getBlockTickList().scheduleTick(pos, this, 1);
        }

        if (direction != this.growthDirection || !neighborState.is(this) && !neighborState.is(this.getBodyBlock())) {
            if (this.scheduleFluidTicks) {
                world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return this.updateBodyAfterConvertedFromHead(state, this.getBodyBlock().getBlockData());
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return this.canGrowInto(world.getType(pos.relative(this.growthDirection)));
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        BlockPosition blockPos = pos.relative(this.growthDirection);
        int i = Math.min(state.get(AGE) + 1, 25);
        int j = this.getBlocksToGrowWhenBonemealed(random);

        for(int k = 0; k < j && this.canGrowInto(world.getType(blockPos)); ++k) {
            world.setTypeUpdate(blockPos, state.set(AGE, Integer.valueOf(i)));
            blockPos = blockPos.relative(this.growthDirection);
            i = Math.min(i + 1, 25);
        }

    }

    protected abstract int getBlocksToGrowWhenBonemealed(Random random);

    protected abstract boolean canGrowInto(IBlockData state);

    @Override
    protected BlockGrowingTop getHeadBlock() {
        return this;
    }
}
