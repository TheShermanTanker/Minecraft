package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyAttachPosition;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockLever extends BlockAttachable {
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    protected static final int DEPTH = 6;
    protected static final int WIDTH = 6;
    protected static final int HEIGHT = 8;
    protected static final VoxelShape NORTH_AABB = Block.box(5.0D, 4.0D, 10.0D, 11.0D, 12.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0D, 4.0D, 0.0D, 11.0D, 12.0D, 6.0D);
    protected static final VoxelShape WEST_AABB = Block.box(10.0D, 4.0D, 5.0D, 16.0D, 12.0D, 11.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 4.0D, 5.0D, 6.0D, 12.0D, 11.0D);
    protected static final VoxelShape UP_AABB_Z = Block.box(5.0D, 0.0D, 4.0D, 11.0D, 6.0D, 12.0D);
    protected static final VoxelShape UP_AABB_X = Block.box(4.0D, 0.0D, 5.0D, 12.0D, 6.0D, 11.0D);
    protected static final VoxelShape DOWN_AABB_Z = Block.box(5.0D, 10.0D, 4.0D, 11.0D, 16.0D, 12.0D);
    protected static final VoxelShape DOWN_AABB_X = Block.box(4.0D, 10.0D, 5.0D, 12.0D, 16.0D, 11.0D);

    protected BlockLever(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(POWERED, Boolean.valueOf(false)).set(FACE, BlockPropertyAttachPosition.WALL));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((BlockPropertyAttachPosition)state.get(FACE)) {
        case FLOOR:
            switch(state.get(FACING).getAxis()) {
            case X:
                return UP_AABB_X;
            case Z:
            default:
                return UP_AABB_Z;
            }
        case WALL:
            switch((EnumDirection)state.get(FACING)) {
            case EAST:
                return EAST_AABB;
            case WEST:
                return WEST_AABB;
            case SOUTH:
                return SOUTH_AABB;
            case NORTH:
            default:
                return NORTH_AABB;
            }
        case CEILING:
        default:
            switch(state.get(FACING).getAxis()) {
            case X:
                return DOWN_AABB_X;
            case Z:
            default:
                return DOWN_AABB_Z;
            }
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            IBlockData blockState = state.cycle(POWERED);
            if (blockState.get(POWERED)) {
                makeParticle(blockState, world, pos, 1.0F);
            }

            return EnumInteractionResult.SUCCESS;
        } else {
            IBlockData blockState2 = this.pull(state, world, pos);
            float f = blockState2.get(POWERED) ? 0.6F : 0.5F;
            world.playSound((EntityHuman)null, pos, SoundEffects.LEVER_CLICK, EnumSoundCategory.BLOCKS, 0.3F, f);
            world.gameEvent(player, blockState2.get(POWERED) ? GameEvent.BLOCK_SWITCH : GameEvent.BLOCK_UNSWITCH, pos);
            return EnumInteractionResult.CONSUME;
        }
    }

    public IBlockData pull(IBlockData state, World world, BlockPosition pos) {
        state = state.cycle(POWERED);
        world.setTypeAndData(pos, state, 3);
        this.updateNeighbours(state, world, pos);
        return state;
    }

    private static void makeParticle(IBlockData state, GeneratorAccess world, BlockPosition pos, float alpha) {
        EnumDirection direction = state.get(FACING).opposite();
        EnumDirection direction2 = getConnectedDirection(state).opposite();
        double d = (double)pos.getX() + 0.5D + 0.1D * (double)direction.getAdjacentX() + 0.2D * (double)direction2.getAdjacentX();
        double e = (double)pos.getY() + 0.5D + 0.1D * (double)direction.getAdjacentY() + 0.2D * (double)direction2.getAdjacentY();
        double f = (double)pos.getZ() + 0.5D + 0.1D * (double)direction.getAdjacentZ() + 0.2D * (double)direction2.getAdjacentZ();
        world.addParticle(new ParticleParamRedstone(ParticleParamRedstone.REDSTONE_PARTICLE_COLOR, alpha), d, e, f, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(POWERED) && random.nextFloat() < 0.25F) {
            makeParticle(state, world, pos, 0.5F);
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            if (state.get(POWERED)) {
                this.updateNeighbours(state, world, pos);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    private void updateNeighbours(IBlockData state, World world, BlockPosition pos) {
        world.applyPhysics(pos, this);
        world.applyPhysics(pos.relative(getConnectedDirection(state).opposite()), this);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACE, FACING, POWERED);
    }
}
