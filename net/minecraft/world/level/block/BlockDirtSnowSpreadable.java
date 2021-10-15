package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.lighting.LightEngineLayer;

public abstract class BlockDirtSnowSpreadable extends BlockDirtSnow {
    protected BlockDirtSnowSpreadable(BlockBase.Info settings) {
        super(settings);
    }

    private static boolean canBeGrass(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.above();
        IBlockData blockState = world.getType(blockPos);
        if (blockState.is(Blocks.SNOW) && blockState.get(BlockSnow.LAYERS) == 1) {
            return true;
        } else if (blockState.getFluid().getAmount() == 8) {
            return false;
        } else {
            int i = LightEngineLayer.getLightBlockInto(world, state, pos, blockState, blockPos, EnumDirection.UP, blockState.getLightBlock(world, blockPos));
            return i < world.getMaxLightLevel();
        }
    }

    private static boolean canPropagate(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.above();
        return canBeGrass(state, world, pos) && !world.getFluid(blockPos).is(TagsFluid.WATER);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!canBeGrass(state, world, pos)) {
            world.setTypeUpdate(pos, Blocks.DIRT.getBlockData());
        } else {
            if (world.getLightLevel(pos.above()) >= 9) {
                IBlockData blockState = this.getBlockData();

                for(int i = 0; i < 4; ++i) {
                    BlockPosition blockPos = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                    if (world.getType(blockPos).is(Blocks.DIRT) && canPropagate(blockState, world, blockPos)) {
                        world.setTypeUpdate(blockPos, blockState.set(SNOWY, Boolean.valueOf(world.getType(blockPos.above()).is(Blocks.SNOW))));
                    }
                }
            }

        }
    }
}
