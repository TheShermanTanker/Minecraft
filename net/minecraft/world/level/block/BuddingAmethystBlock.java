package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.FluidTypes;

public class BuddingAmethystBlock extends BlockAmethyst {
    public static final int GROWTH_CHANCE = 5;
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();

    public BuddingAmethystBlock(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (random.nextInt(5) == 0) {
            EnumDirection direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            BlockPosition blockPos = pos.relative(direction);
            IBlockData blockState = world.getType(blockPos);
            Block block = null;
            if (canClusterGrowAtState(blockState)) {
                block = Blocks.SMALL_AMETHYST_BUD;
            } else if (blockState.is(Blocks.SMALL_AMETHYST_BUD) && blockState.get(BlockAmethystCluster.FACING) == direction) {
                block = Blocks.MEDIUM_AMETHYST_BUD;
            } else if (blockState.is(Blocks.MEDIUM_AMETHYST_BUD) && blockState.get(BlockAmethystCluster.FACING) == direction) {
                block = Blocks.LARGE_AMETHYST_BUD;
            } else if (blockState.is(Blocks.LARGE_AMETHYST_BUD) && blockState.get(BlockAmethystCluster.FACING) == direction) {
                block = Blocks.AMETHYST_CLUSTER;
            }

            if (block != null) {
                IBlockData blockState2 = block.getBlockData().set(BlockAmethystCluster.FACING, direction).set(BlockAmethystCluster.WATERLOGGED, Boolean.valueOf(blockState.getFluid().getType() == FluidTypes.WATER));
                world.setTypeUpdate(blockPos, blockState2);
            }

        }
    }

    public static boolean canClusterGrowAtState(IBlockData state) {
        return state.isAir() || state.is(Blocks.WATER) && state.getFluid().getAmount() == 8;
    }
}
