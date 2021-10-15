package net.minecraft.world.level.block;

import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockTripwire extends Block {
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean ATTACHED = BlockProperties.ATTACHED;
    public static final BlockStateBoolean DISARMED = BlockProperties.DISARMED;
    public static final BlockStateBoolean NORTH = BlockSprawling.NORTH;
    public static final BlockStateBoolean EAST = BlockSprawling.EAST;
    public static final BlockStateBoolean SOUTH = BlockSprawling.SOUTH;
    public static final BlockStateBoolean WEST = BlockSprawling.WEST;
    private static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = BlockTall.PROPERTY_BY_DIRECTION;
    protected static final VoxelShape AABB = Block.box(0.0D, 1.0D, 0.0D, 16.0D, 2.5D, 16.0D);
    protected static final VoxelShape NOT_ATTACHED_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final int RECHECK_PERIOD = 10;
    private final BlockTripwireHook hook;

    public BlockTripwire(BlockTripwireHook hookBlock, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(POWERED, Boolean.valueOf(false)).set(ATTACHED, Boolean.valueOf(false)).set(DISARMED, Boolean.valueOf(false)).set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)));
        this.hook = hookBlock;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return state.get(ATTACHED) ? AABB : NOT_ATTACHED_AABB;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockAccess blockGetter = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        return this.getBlockData().set(NORTH, Boolean.valueOf(this.shouldConnectTo(blockGetter.getType(blockPos.north()), EnumDirection.NORTH))).set(EAST, Boolean.valueOf(this.shouldConnectTo(blockGetter.getType(blockPos.east()), EnumDirection.EAST))).set(SOUTH, Boolean.valueOf(this.shouldConnectTo(blockGetter.getType(blockPos.south()), EnumDirection.SOUTH))).set(WEST, Boolean.valueOf(this.shouldConnectTo(blockGetter.getType(blockPos.west()), EnumDirection.WEST)));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction.getAxis().isHorizontal() ? state.set(PROPERTY_BY_DIRECTION.get(direction), Boolean.valueOf(this.shouldConnectTo(neighborState, direction))) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateSource(world, pos, state);
        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            this.updateSource(world, pos, state.set(POWERED, Boolean.valueOf(true)));
        }
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide && !player.getItemInMainHand().isEmpty() && player.getItemInMainHand().is(Items.SHEARS)) {
            world.setTypeAndData(pos, state.set(DISARMED, Boolean.valueOf(true)), 4);
            world.gameEvent(player, GameEvent.SHEAR, pos);
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    private void updateSource(World world, BlockPosition pos, IBlockData state) {
        for(EnumDirection direction : new EnumDirection[]{EnumDirection.SOUTH, EnumDirection.WEST}) {
            for(int i = 1; i < 42; ++i) {
                BlockPosition blockPos = pos.relative(direction, i);
                IBlockData blockState = world.getType(blockPos);
                if (blockState.is(this.hook)) {
                    if (blockState.get(BlockTripwireHook.FACING) == direction.opposite()) {
                        this.hook.calculateState(world, blockPos, blockState, false, true, i, state);
                    }
                    break;
                }

                if (!blockState.is(this)) {
                    break;
                }
            }
        }

    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide) {
            if (!state.get(POWERED)) {
                this.checkPressed(world, pos);
            }
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getType(pos).get(POWERED)) {
            this.checkPressed(world, pos);
        }
    }

    private void checkPressed(World world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        boolean bl = blockState.get(POWERED);
        boolean bl2 = false;
        List<? extends Entity> list = world.getEntities((Entity)null, blockState.getShape(world, pos).getBoundingBox().move(pos));
        if (!list.isEmpty()) {
            for(Entity entity : list) {
                if (!entity.isIgnoreBlockTrigger()) {
                    bl2 = true;
                    break;
                }
            }
        }

        if (bl2 != bl) {
            blockState = blockState.set(POWERED, Boolean.valueOf(bl2));
            world.setTypeAndData(pos, blockState, 3);
            this.updateSource(world, pos, blockState);
        }

        if (bl2) {
            world.getBlockTickList().scheduleTick(new BlockPosition(pos), this, 10);
        }

    }

    public boolean shouldConnectTo(IBlockData state, EnumDirection facing) {
        if (state.is(this.hook)) {
            return state.get(BlockTripwireHook.FACING) == facing.opposite();
        } else {
            return state.is(this);
        }
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.set(NORTH, state.get(SOUTH)).set(EAST, state.get(WEST)).set(SOUTH, state.get(NORTH)).set(WEST, state.get(EAST));
        case COUNTERCLOCKWISE_90:
            return state.set(NORTH, state.get(EAST)).set(EAST, state.get(SOUTH)).set(SOUTH, state.get(WEST)).set(WEST, state.get(NORTH));
        case CLOCKWISE_90:
            return state.set(NORTH, state.get(WEST)).set(EAST, state.get(NORTH)).set(SOUTH, state.get(EAST)).set(WEST, state.get(SOUTH));
        default:
            return state;
        }
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.set(NORTH, state.get(SOUTH)).set(SOUTH, state.get(NORTH));
        case FRONT_BACK:
            return state.set(EAST, state.get(WEST)).set(WEST, state.get(EAST));
        default:
            return super.mirror(state, mirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(POWERED, ATTACHED, DISARMED, NORTH, EAST, WEST, SOUTH);
    }
}
