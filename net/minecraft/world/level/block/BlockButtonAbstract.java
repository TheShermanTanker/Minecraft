package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
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

public abstract class BlockButtonAbstract extends BlockAttachable {
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final int PRESSED_DEPTH = 1;
    private static final int UNPRESSED_DEPTH = 2;
    protected static final int HALF_AABB_HEIGHT = 2;
    protected static final int HALF_AABB_WIDTH = 3;
    protected static final VoxelShape CEILING_AABB_X = Block.box(6.0D, 14.0D, 5.0D, 10.0D, 16.0D, 11.0D);
    protected static final VoxelShape CEILING_AABB_Z = Block.box(5.0D, 14.0D, 6.0D, 11.0D, 16.0D, 10.0D);
    protected static final VoxelShape FLOOR_AABB_X = Block.box(6.0D, 0.0D, 5.0D, 10.0D, 2.0D, 11.0D);
    protected static final VoxelShape FLOOR_AABB_Z = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 2.0D, 10.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(5.0D, 6.0D, 14.0D, 11.0D, 10.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 2.0D);
    protected static final VoxelShape WEST_AABB = Block.box(14.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 6.0D, 5.0D, 2.0D, 10.0D, 11.0D);
    protected static final VoxelShape PRESSED_CEILING_AABB_X = Block.box(6.0D, 15.0D, 5.0D, 10.0D, 16.0D, 11.0D);
    protected static final VoxelShape PRESSED_CEILING_AABB_Z = Block.box(5.0D, 15.0D, 6.0D, 11.0D, 16.0D, 10.0D);
    protected static final VoxelShape PRESSED_FLOOR_AABB_X = Block.box(6.0D, 0.0D, 5.0D, 10.0D, 1.0D, 11.0D);
    protected static final VoxelShape PRESSED_FLOOR_AABB_Z = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 1.0D, 10.0D);
    protected static final VoxelShape PRESSED_NORTH_AABB = Block.box(5.0D, 6.0D, 15.0D, 11.0D, 10.0D, 16.0D);
    protected static final VoxelShape PRESSED_SOUTH_AABB = Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 1.0D);
    protected static final VoxelShape PRESSED_WEST_AABB = Block.box(15.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);
    protected static final VoxelShape PRESSED_EAST_AABB = Block.box(0.0D, 6.0D, 5.0D, 1.0D, 10.0D, 11.0D);
    private final boolean sensitive;

    protected BlockButtonAbstract(boolean wooden, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(POWERED, Boolean.valueOf(false)).set(FACE, BlockPropertyAttachPosition.WALL));
        this.sensitive = wooden;
    }

    private int getPressDuration() {
        return this.sensitive ? 30 : 20;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        EnumDirection direction = state.get(FACING);
        boolean bl = state.get(POWERED);
        switch((BlockPropertyAttachPosition)state.get(FACE)) {
        case FLOOR:
            if (direction.getAxis() == EnumDirection.EnumAxis.X) {
                return bl ? PRESSED_FLOOR_AABB_X : FLOOR_AABB_X;
            }

            return bl ? PRESSED_FLOOR_AABB_Z : FLOOR_AABB_Z;
        case WALL:
            switch(direction) {
            case EAST:
                return bl ? PRESSED_EAST_AABB : EAST_AABB;
            case WEST:
                return bl ? PRESSED_WEST_AABB : WEST_AABB;
            case SOUTH:
                return bl ? PRESSED_SOUTH_AABB : SOUTH_AABB;
            case NORTH:
            default:
                return bl ? PRESSED_NORTH_AABB : NORTH_AABB;
            }
        case CEILING:
        default:
            if (direction.getAxis() == EnumDirection.EnumAxis.X) {
                return bl ? PRESSED_CEILING_AABB_X : CEILING_AABB_X;
            } else {
                return bl ? PRESSED_CEILING_AABB_Z : CEILING_AABB_Z;
            }
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (state.get(POWERED)) {
            return EnumInteractionResult.CONSUME;
        } else {
            this.press(state, world, pos);
            this.playSound(player, world, pos, true);
            world.gameEvent(player, GameEvent.BLOCK_PRESS, pos);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    public void press(IBlockData state, World world, BlockPosition pos) {
        world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(true)), 3);
        this.updateNeighbours(state, world, pos);
        world.scheduleTick(pos, this, this.getPressDuration());
    }

    protected void playSound(@Nullable EntityHuman player, GeneratorAccess world, BlockPosition pos, boolean powered) {
        world.playSound(powered ? player : null, pos, this.getSound(powered), EnumSoundCategory.BLOCKS, 0.3F, powered ? 0.6F : 0.5F);
    }

    protected abstract SoundEffect getSound(boolean powered);

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

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(POWERED)) {
            if (this.sensitive) {
                this.checkPressed(state, world, pos);
            } else {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)), 3);
                this.updateNeighbours(state, world, pos);
                this.playSound((EntityHuman)null, world, pos, false);
                world.gameEvent(GameEvent.BLOCK_UNPRESS, pos);
            }

        }
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide && this.sensitive && !state.get(POWERED)) {
            this.checkPressed(state, world, pos);
        }
    }

    private void checkPressed(IBlockData state, World world, BlockPosition pos) {
        List<? extends Entity> list = world.getEntitiesOfClass(EntityArrow.class, state.getShape(world, pos).getBoundingBox().move(pos));
        boolean bl = !list.isEmpty();
        boolean bl2 = state.get(POWERED);
        if (bl != bl2) {
            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl)), 3);
            this.updateNeighbours(state, world, pos);
            this.playSound((EntityHuman)null, world, pos, bl);
            world.gameEvent(list.stream().findFirst().orElse((Entity)null), bl ? GameEvent.BLOCK_PRESS : GameEvent.BLOCK_UNPRESS, pos);
        }

        if (bl) {
            world.scheduleTick(new BlockPosition(pos), this, this.getPressDuration());
        }

    }

    private void updateNeighbours(IBlockData state, World world, BlockPosition pos) {
        world.applyPhysics(pos, this);
        world.applyPhysics(pos.relative(getConnectedDirection(state).opposite()), this);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, POWERED, FACE);
    }
}
