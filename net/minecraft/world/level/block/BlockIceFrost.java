package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;

public class BlockIceFrost extends BlockIce {
    public static final int MAX_AGE = 3;
    public static final BlockStateInteger AGE = BlockProperties.AGE_3;
    private static final int NEIGHBORS_TO_AGE = 4;
    private static final int NEIGHBORS_TO_MELT = 2;

    public BlockIceFrost(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.tickAlways(state, world, pos, random);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if ((random.nextInt(3) == 0 || this.fewerNeigboursThan(world, pos, 4)) && world.getLightLevel(pos) > 11 - state.get(AGE) - state.getLightBlock(world, pos) && this.slightlyMelt(state, world, pos)) {
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(EnumDirection direction : EnumDirection.values()) {
                mutableBlockPos.setWithOffset(pos, direction);
                IBlockData blockState = world.getType(mutableBlockPos);
                if (blockState.is(this) && !this.slightlyMelt(blockState, world, mutableBlockPos)) {
                    world.scheduleTick(mutableBlockPos, this, MathHelper.nextInt(random, 20, 40));
                }
            }

        } else {
            world.scheduleTick(pos, this, MathHelper.nextInt(random, 20, 40));
        }
    }

    private boolean slightlyMelt(IBlockData state, World world, BlockPosition pos) {
        int i = state.get(AGE);
        if (i < 3) {
            world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(i + 1)), 2);
            return false;
        } else {
            this.melt(state, world, pos);
            return true;
        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (block.getBlockData().is(this) && this.fewerNeigboursThan(world, pos, 2)) {
            this.melt(state, world, pos);
        }

        super.doPhysics(state, world, pos, block, fromPos, notify);
    }

    private boolean fewerNeigboursThan(IBlockAccess world, BlockPosition pos, int maxNeighbors) {
        int i = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(EnumDirection direction : EnumDirection.values()) {
            mutableBlockPos.setWithOffset(pos, direction);
            if (world.getType(mutableBlockPos).is(this)) {
                ++i;
                if (i >= maxNeighbors) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return ItemStack.EMPTY;
    }
}
