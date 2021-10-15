package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockLeaves extends Block {
    public static final int DECAY_DISTANCE = 7;
    public static final BlockStateInteger DISTANCE = BlockProperties.DISTANCE;
    public static final BlockStateBoolean PERSISTENT = BlockProperties.PERSISTENT;
    private static final int TICK_DELAY = 1;

    public BlockLeaves(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(DISTANCE, Integer.valueOf(7)).set(PERSISTENT, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getBlockSupportShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.empty();
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.get(DISTANCE) == 7 && !state.get(PERSISTENT);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.get(PERSISTENT) && state.get(DISTANCE) == 7) {
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }

    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        world.setTypeAndData(pos, updateDistance(state, world, pos), 3);
    }

    @Override
    public int getLightBlock(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return 1;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        int i = getDistanceAt(neighborState) + 1;
        if (i != 1 || state.get(DISTANCE) != i) {
            world.getBlockTickList().scheduleTick(pos, this, 1);
        }

        return state;
    }

    private static IBlockData updateDistance(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        int i = 7;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(EnumDirection direction : EnumDirection.values()) {
            mutableBlockPos.setWithOffset(pos, direction);
            i = Math.min(i, getDistanceAt(world.getType(mutableBlockPos)) + 1);
            if (i == 1) {
                break;
            }
        }

        return state.set(DISTANCE, Integer.valueOf(i));
    }

    private static int getDistanceAt(IBlockData state) {
        if (state.is(TagsBlock.LOGS)) {
            return 0;
        } else {
            return state.getBlock() instanceof BlockLeaves ? state.get(DISTANCE) : 7;
        }
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (world.isRainingAt(pos.above())) {
            if (random.nextInt(15) == 1) {
                BlockPosition blockPos = pos.below();
                IBlockData blockState = world.getType(blockPos);
                if (!blockState.canOcclude() || !blockState.isFaceSturdy(world, blockPos, EnumDirection.UP)) {
                    double d = (double)pos.getX() + random.nextDouble();
                    double e = (double)pos.getY() - 0.05D;
                    double f = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(Particles.DRIPPING_WATER, d, e, f, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(DISTANCE, PERSISTENT);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return updateDistance(this.getBlockData().set(PERSISTENT, Boolean.valueOf(true)), ctx.getWorld(), ctx.getClickPosition());
    }
}
