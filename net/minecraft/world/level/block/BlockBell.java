package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBell;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyBellAttach;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockBell extends BlockTileEntity {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateEnum<BlockPropertyBellAttach> ATTACHMENT = BlockProperties.BELL_ATTACHMENT;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final VoxelShape NORTH_SOUTH_FLOOR_SHAPE = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);
    private static final VoxelShape EAST_WEST_FLOOR_SHAPE = Block.box(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    private static final VoxelShape BELL_TOP_SHAPE = Block.box(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D);
    private static final VoxelShape BELL_BOTTOM_SHAPE = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D);
    private static final VoxelShape BELL_SHAPE = VoxelShapes.or(BELL_BOTTOM_SHAPE, BELL_TOP_SHAPE);
    private static final VoxelShape NORTH_SOUTH_BETWEEN = VoxelShapes.or(BELL_SHAPE, Block.box(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 16.0D));
    private static final VoxelShape EAST_WEST_BETWEEN = VoxelShapes.or(BELL_SHAPE, Block.box(0.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
    private static final VoxelShape TO_WEST = VoxelShapes.or(BELL_SHAPE, Block.box(0.0D, 13.0D, 7.0D, 13.0D, 15.0D, 9.0D));
    private static final VoxelShape TO_EAST = VoxelShapes.or(BELL_SHAPE, Block.box(3.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
    private static final VoxelShape TO_NORTH = VoxelShapes.or(BELL_SHAPE, Block.box(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 13.0D));
    private static final VoxelShape TO_SOUTH = VoxelShapes.or(BELL_SHAPE, Block.box(7.0D, 13.0D, 3.0D, 9.0D, 15.0D, 16.0D));
    private static final VoxelShape CEILING_SHAPE = VoxelShapes.or(BELL_SHAPE, Block.box(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D));
    public static final int EVENT_BELL_RING = 1;

    public BlockBell(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(ATTACHMENT, BlockPropertyBellAttach.FLOOR).set(POWERED, Boolean.valueOf(false)));
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        boolean bl = world.isBlockIndirectlyPowered(pos);
        if (bl != state.get(POWERED)) {
            if (bl) {
                this.attemptToRing(world, pos, (EnumDirection)null);
            }

            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl)), 3);
        }

    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        Entity entity = projectile.getShooter();
        EntityHuman player = entity instanceof EntityHuman ? (EntityHuman)entity : null;
        this.onHit(world, state, hit, player, true);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        return this.onHit(world, state, hit, player, true) ? EnumInteractionResult.sidedSuccess(world.isClientSide) : EnumInteractionResult.PASS;
    }

    public boolean onHit(World world, IBlockData state, MovingObjectPositionBlock hitResult, @Nullable EntityHuman player, boolean bl) {
        EnumDirection direction = hitResult.getDirection();
        BlockPosition blockPos = hitResult.getBlockPosition();
        boolean bl2 = !bl || this.isProperHit(state, direction, hitResult.getPos().y - (double)blockPos.getY());
        if (bl2) {
            boolean bl3 = this.attemptToRing(player, world, blockPos, direction);
            if (bl3 && player != null) {
                player.awardStat(StatisticList.BELL_RING);
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isProperHit(IBlockData state, EnumDirection side, double y) {
        if (side.getAxis() != EnumDirection.EnumAxis.Y && !(y > (double)0.8124F)) {
            EnumDirection direction = state.get(FACING);
            BlockPropertyBellAttach bellAttachType = state.get(ATTACHMENT);
            switch(bellAttachType) {
            case FLOOR:
                return direction.getAxis() == side.getAxis();
            case SINGLE_WALL:
            case DOUBLE_WALL:
                return direction.getAxis() != side.getAxis();
            case CEILING:
                return true;
            default:
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean attemptToRing(World world, BlockPosition pos, @Nullable EnumDirection direction) {
        return this.attemptToRing((Entity)null, world, pos, direction);
    }

    public boolean attemptToRing(@Nullable Entity entity, World world, BlockPosition pos, @Nullable EnumDirection direction) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (!world.isClientSide && blockEntity instanceof TileEntityBell) {
            if (direction == null) {
                direction = world.getType(pos).get(FACING);
            }

            ((TileEntityBell)blockEntity).onHit(direction);
            world.playSound((EntityHuman)null, pos, SoundEffects.BELL_BLOCK, SoundCategory.BLOCKS, 2.0F, 1.0F);
            world.gameEvent(entity, GameEvent.RING_BELL, pos);
            return true;
        } else {
            return false;
        }
    }

    private VoxelShape getVoxelShape(IBlockData state) {
        EnumDirection direction = state.get(FACING);
        BlockPropertyBellAttach bellAttachType = state.get(ATTACHMENT);
        if (bellAttachType == BlockPropertyBellAttach.FLOOR) {
            return direction != EnumDirection.NORTH && direction != EnumDirection.SOUTH ? EAST_WEST_FLOOR_SHAPE : NORTH_SOUTH_FLOOR_SHAPE;
        } else if (bellAttachType == BlockPropertyBellAttach.CEILING) {
            return CEILING_SHAPE;
        } else if (bellAttachType == BlockPropertyBellAttach.DOUBLE_WALL) {
            return direction != EnumDirection.NORTH && direction != EnumDirection.SOUTH ? EAST_WEST_BETWEEN : NORTH_SOUTH_BETWEEN;
        } else if (direction == EnumDirection.NORTH) {
            return TO_NORTH;
        } else if (direction == EnumDirection.SOUTH) {
            return TO_SOUTH;
        } else {
            return direction == EnumDirection.EAST ? TO_EAST : TO_WEST;
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getVoxelShape(state);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getVoxelShape(state);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        EnumDirection direction = ctx.getClickedFace();
        BlockPosition blockPos = ctx.getClickPosition();
        World level = ctx.getWorld();
        EnumDirection.EnumAxis axis = direction.getAxis();
        if (axis == EnumDirection.EnumAxis.Y) {
            IBlockData blockState = this.getBlockData().set(ATTACHMENT, direction == EnumDirection.DOWN ? BlockPropertyBellAttach.CEILING : BlockPropertyBellAttach.FLOOR).set(FACING, ctx.getHorizontalDirection());
            if (blockState.canPlace(ctx.getWorld(), blockPos)) {
                return blockState;
            }
        } else {
            boolean bl = axis == EnumDirection.EnumAxis.X && level.getType(blockPos.west()).isFaceSturdy(level, blockPos.west(), EnumDirection.EAST) && level.getType(blockPos.east()).isFaceSturdy(level, blockPos.east(), EnumDirection.WEST) || axis == EnumDirection.EnumAxis.Z && level.getType(blockPos.north()).isFaceSturdy(level, blockPos.north(), EnumDirection.SOUTH) && level.getType(blockPos.south()).isFaceSturdy(level, blockPos.south(), EnumDirection.NORTH);
            IBlockData blockState2 = this.getBlockData().set(FACING, direction.opposite()).set(ATTACHMENT, bl ? BlockPropertyBellAttach.DOUBLE_WALL : BlockPropertyBellAttach.SINGLE_WALL);
            if (blockState2.canPlace(ctx.getWorld(), ctx.getClickPosition())) {
                return blockState2;
            }

            boolean bl2 = level.getType(blockPos.below()).isFaceSturdy(level, blockPos.below(), EnumDirection.UP);
            blockState2 = blockState2.set(ATTACHMENT, bl2 ? BlockPropertyBellAttach.FLOOR : BlockPropertyBellAttach.CEILING);
            if (blockState2.canPlace(ctx.getWorld(), ctx.getClickPosition())) {
                return blockState2;
            }
        }

        return null;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        BlockPropertyBellAttach bellAttachType = state.get(ATTACHMENT);
        EnumDirection direction2 = getConnectedDirection(state).opposite();
        if (direction2 == direction && !state.canPlace(world, pos) && bellAttachType != BlockPropertyBellAttach.DOUBLE_WALL) {
            return Blocks.AIR.getBlockData();
        } else {
            if (direction.getAxis() == state.get(FACING).getAxis()) {
                if (bellAttachType == BlockPropertyBellAttach.DOUBLE_WALL && !neighborState.isFaceSturdy(world, neighborPos, direction)) {
                    return state.set(ATTACHMENT, BlockPropertyBellAttach.SINGLE_WALL).set(FACING, direction.opposite());
                }

                if (bellAttachType == BlockPropertyBellAttach.SINGLE_WALL && direction2.opposite() == direction && neighborState.isFaceSturdy(world, neighborPos, state.get(FACING))) {
                    return state.set(ATTACHMENT, BlockPropertyBellAttach.DOUBLE_WALL);
                }
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        EnumDirection direction = getConnectedDirection(state).opposite();
        return direction == EnumDirection.UP ? Block.canSupportCenter(world, pos.above(), EnumDirection.DOWN) : BlockAttachable.canAttach(world, pos, direction);
    }

    private static EnumDirection getConnectedDirection(IBlockData state) {
        switch((BlockPropertyBellAttach)state.get(ATTACHMENT)) {
        case FLOOR:
            return EnumDirection.UP;
        case CEILING:
            return EnumDirection.DOWN;
        default:
            return state.get(FACING).opposite();
        }
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, ATTACHMENT, POWERED);
    }

    @Nullable
    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityBell(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createTickerHelper(type, TileEntityTypes.BELL, world.isClientSide ? TileEntityBell::clientTick : TileEntityBell::serverTick);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
