package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidTypes;

public class BlockCoralFan extends BlockCoralFanAbstract {
    private final Block deadBlock;

    protected BlockCoralFan(Block deadCoralBlock, BlockBase.Info settings) {
        super(settings);
        this.deadBlock = deadCoralBlock;
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        this.tryScheduleDieTick(state, world, pos);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!scanForWater(state, world, pos)) {
            world.setTypeAndData(pos, this.deadBlock.getBlockData().set(WATERLOGGED, Boolean.valueOf(false)), 2);
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.DOWN && !state.canPlace(world, pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            this.tryScheduleDieTick(state, world, pos);
            if (state.get(WATERLOGGED)) {
                world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }
}
