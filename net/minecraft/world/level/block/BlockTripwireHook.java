package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockTripwireHook extends Block {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean ATTACHED = BlockProperties.ATTACHED;
    protected static final int WIRE_DIST_MIN = 1;
    protected static final int WIRE_DIST_MAX = 42;
    private static final int RECHECK_PERIOD = 10;
    protected static final int AABB_OFFSET = 3;
    protected static final VoxelShape NORTH_AABB = Block.box(5.0D, 0.0D, 10.0D, 11.0D, 10.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0D, 0.0D, 0.0D, 11.0D, 10.0D, 6.0D);
    protected static final VoxelShape WEST_AABB = Block.box(10.0D, 0.0D, 5.0D, 16.0D, 10.0D, 11.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 5.0D, 6.0D, 10.0D, 11.0D);

    public BlockTripwireHook(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(POWERED, Boolean.valueOf(false)).set(ATTACHED, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((EnumDirection)state.get(FACING)) {
        case EAST:
        default:
            return EAST_AABB;
        case WEST:
            return WEST_AABB;
        case SOUTH:
            return SOUTH_AABB;
        case NORTH:
            return NORTH_AABB;
        }
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction.opposite());
        IBlockData blockState = world.getType(blockPos);
        return direction.getAxis().isHorizontal() && blockState.isFaceSturdy(world, blockPos, direction);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction.opposite() == state.get(FACING) && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = this.getBlockData().set(POWERED, Boolean.valueOf(false)).set(ATTACHED, Boolean.valueOf(false));
        IWorldReader levelReader = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        EnumDirection[] directions = ctx.getNearestLookingDirections();

        for(EnumDirection direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                EnumDirection direction2 = direction.opposite();
                blockState = blockState.set(FACING, direction2);
                if (blockState.canPlace(levelReader, blockPos)) {
                    return blockState;
                }
            }
        }

        return null;
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        this.calculateState(world, pos, state, false, false, -1, (IBlockData)null);
    }

    public void calculateState(World world, BlockPosition pos, IBlockData state, boolean beingRemoved, boolean bl, int i, @Nullable IBlockData blockState) {
        EnumDirection direction = state.get(FACING);
        boolean bl2 = state.get(ATTACHED);
        boolean bl3 = state.get(POWERED);
        boolean bl4 = !beingRemoved;
        boolean bl5 = false;
        int j = 0;
        IBlockData[] blockStates = new IBlockData[42];

        for(int k = 1; k < 42; ++k) {
            BlockPosition blockPos = pos.relative(direction, k);
            IBlockData blockState2 = world.getType(blockPos);
            if (blockState2.is(Blocks.TRIPWIRE_HOOK)) {
                if (blockState2.get(FACING) == direction.opposite()) {
                    j = k;
                }
                break;
            }

            if (!blockState2.is(Blocks.TRIPWIRE) && k != i) {
                blockStates[k] = null;
                bl4 = false;
            } else {
                if (k == i) {
                    blockState2 = MoreObjects.firstNonNull(blockState, blockState2);
                }

                boolean bl6 = !blockState2.get(BlockTripwire.DISARMED);
                boolean bl7 = blockState2.get(BlockTripwire.POWERED);
                bl5 |= bl6 && bl7;
                blockStates[k] = blockState2;
                if (k == i) {
                    world.getBlockTickList().scheduleTick(pos, this, 10);
                    bl4 &= bl6;
                }
            }
        }

        bl4 = bl4 & j > 1;
        bl5 = bl5 & bl4;
        IBlockData blockState3 = this.getBlockData().set(ATTACHED, Boolean.valueOf(bl4)).set(POWERED, Boolean.valueOf(bl5));
        if (j > 0) {
            BlockPosition blockPos2 = pos.relative(direction, j);
            EnumDirection direction2 = direction.opposite();
            world.setTypeAndData(blockPos2, blockState3.set(FACING, direction2), 3);
            this.notifyNeighbors(world, blockPos2, direction2);
            this.playSound(world, blockPos2, bl4, bl5, bl2, bl3);
        }

        this.playSound(world, pos, bl4, bl5, bl2, bl3);
        if (!beingRemoved) {
            world.setTypeAndData(pos, blockState3.set(FACING, direction), 3);
            if (bl) {
                this.notifyNeighbors(world, pos, direction);
            }
        }

        if (bl2 != bl4) {
            for(int l = 1; l < j; ++l) {
                BlockPosition blockPos3 = pos.relative(direction, l);
                IBlockData blockState4 = blockStates[l];
                if (blockState4 != null) {
                    world.setTypeAndData(blockPos3, blockState4.set(ATTACHED, Boolean.valueOf(bl4)), 3);
                    if (!world.getType(blockPos3).isAir()) {
                    }
                }
            }
        }

    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.calculateState(world, pos, state, false, true, -1, (IBlockData)null);
    }

    private void playSound(World world, BlockPosition pos, boolean attached, boolean on, boolean detached, boolean off) {
        if (on && !off) {
            world.playSound((EntityHuman)null, pos, SoundEffects.TRIPWIRE_CLICK_ON, SoundCategory.BLOCKS, 0.4F, 0.6F);
            world.gameEvent(GameEvent.BLOCK_PRESS, pos);
        } else if (!on && off) {
            world.playSound((EntityHuman)null, pos, SoundEffects.TRIPWIRE_CLICK_OFF, SoundCategory.BLOCKS, 0.4F, 0.5F);
            world.gameEvent(GameEvent.BLOCK_UNPRESS, pos);
        } else if (attached && !detached) {
            world.playSound((EntityHuman)null, pos, SoundEffects.TRIPWIRE_ATTACH, SoundCategory.BLOCKS, 0.4F, 0.7F);
            world.gameEvent(GameEvent.BLOCK_ATTACH, pos);
        } else if (!attached && detached) {
            world.playSound((EntityHuman)null, pos, SoundEffects.TRIPWIRE_DETACH, SoundCategory.BLOCKS, 0.4F, 1.2F / (world.random.nextFloat() * 0.2F + 0.9F));
            world.gameEvent(GameEvent.BLOCK_DETACH, pos);
        }

    }

    private void notifyNeighbors(World world, BlockPosition pos, EnumDirection direction) {
        world.applyPhysics(pos, this);
        world.applyPhysics(pos.relative(direction.opposite()), this);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            boolean bl = state.get(ATTACHED);
            boolean bl2 = state.get(POWERED);
            if (bl || bl2) {
                this.calculateState(world, pos, state, true, false, -1, (IBlockData)null);
            }

            if (bl2) {
                world.applyPhysics(pos, this);
                world.applyPhysics(pos.relative(state.get(FACING).opposite()), this);
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
        if (!state.get(POWERED)) {
            return 0;
        } else {
            return state.get(FACING) == direction ? 15 : 0;
        }
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, POWERED, ATTACHED);
    }
}
