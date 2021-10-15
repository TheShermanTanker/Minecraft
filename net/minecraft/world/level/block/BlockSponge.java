package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.Queue;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Material;

public class BlockSponge extends Block {
    public static final int MAX_DEPTH = 6;
    public static final int MAX_COUNT = 64;

    protected BlockSponge(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.tryAbsorbWater(world, pos);
        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        this.tryAbsorbWater(world, pos);
        super.doPhysics(state, world, pos, block, fromPos, notify);
    }

    protected void tryAbsorbWater(World world, BlockPosition pos) {
        if (this.removeWaterBreadthFirstSearch(world, pos)) {
            world.setTypeAndData(pos, Blocks.WET_SPONGE.getBlockData(), 2);
            world.triggerEffect(2001, pos, Block.getCombinedId(Blocks.WATER.getBlockData()));
        }

    }

    private boolean removeWaterBreadthFirstSearch(World world, BlockPosition pos) {
        Queue<Tuple<BlockPosition, Integer>> queue = Lists.newLinkedList();
        queue.add(new Tuple<>(pos, 0));
        int i = 0;

        while(!queue.isEmpty()) {
            Tuple<BlockPosition, Integer> tuple = queue.poll();
            BlockPosition blockPos = tuple.getA();
            int j = tuple.getB();

            for(EnumDirection direction : EnumDirection.values()) {
                BlockPosition blockPos2 = blockPos.relative(direction);
                IBlockData blockState = world.getType(blockPos2);
                Fluid fluidState = world.getFluid(blockPos2);
                Material material = blockState.getMaterial();
                if (fluidState.is(TagsFluid.WATER)) {
                    if (blockState.getBlock() instanceof IFluidSource && !((IFluidSource)blockState.getBlock()).removeFluid(world, blockPos2, blockState).isEmpty()) {
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockPos2, j + 1));
                        }
                    } else if (blockState.getBlock() instanceof BlockFluids) {
                        world.setTypeAndData(blockPos2, Blocks.AIR.getBlockData(), 3);
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockPos2, j + 1));
                        }
                    } else if (material == Material.WATER_PLANT || material == Material.REPLACEABLE_WATER_PLANT) {
                        TileEntity blockEntity = blockState.isTileEntity() ? world.getTileEntity(blockPos2) : null;
                        dropResources(blockState, world, blockPos2, blockEntity);
                        world.setTypeAndData(blockPos2, Blocks.AIR.getBlockData(), 3);
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockPos2, j + 1));
                        }
                    }
                }
            }

            if (i > 64) {
                break;
            }
        }

        return i > 0;
    }
}
