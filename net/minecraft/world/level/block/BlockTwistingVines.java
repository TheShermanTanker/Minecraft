package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockTwistingVines extends BlockGrowingTop {
    public static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 15.0D, 12.0D);

    public BlockTwistingVines(BlockBase.Info settings) {
        super(settings, EnumDirection.UP, SHAPE, false, 0.1D);
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(Random random) {
        return BlockNetherVinesUtil.getBlocksToGrowWhenBonemealed(random);
    }

    @Override
    protected Block getBodyBlock() {
        return Blocks.TWISTING_VINES_PLANT;
    }

    @Override
    protected boolean canGrowInto(IBlockData state) {
        return BlockNetherVinesUtil.isValidGrowthState(state);
    }
}
