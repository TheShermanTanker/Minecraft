package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
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
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockFenceGate extends BlockFacingHorizontal {
    public static final BlockStateBoolean OPEN = BlockProperties.OPEN;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean IN_WALL = BlockProperties.IN_WALL;
    protected static final VoxelShape Z_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape X_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    protected static final VoxelShape Z_SHAPE_LOW = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
    protected static final VoxelShape X_SHAPE_LOW = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D);
    protected static final VoxelShape Z_COLLISION_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 24.0D, 10.0D);
    protected static final VoxelShape X_COLLISION_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 24.0D, 16.0D);
    protected static final VoxelShape Z_OCCLUSION_SHAPE = VoxelShapes.or(Block.box(0.0D, 5.0D, 7.0D, 2.0D, 16.0D, 9.0D), Block.box(14.0D, 5.0D, 7.0D, 16.0D, 16.0D, 9.0D));
    protected static final VoxelShape X_OCCLUSION_SHAPE = VoxelShapes.or(Block.box(7.0D, 5.0D, 0.0D, 9.0D, 16.0D, 2.0D), Block.box(7.0D, 5.0D, 14.0D, 9.0D, 16.0D, 16.0D));
    protected static final VoxelShape Z_OCCLUSION_SHAPE_LOW = VoxelShapes.or(Block.box(0.0D, 2.0D, 7.0D, 2.0D, 13.0D, 9.0D), Block.box(14.0D, 2.0D, 7.0D, 16.0D, 13.0D, 9.0D));
    protected static final VoxelShape X_OCCLUSION_SHAPE_LOW = VoxelShapes.or(Block.box(7.0D, 2.0D, 0.0D, 9.0D, 13.0D, 2.0D), Block.box(7.0D, 2.0D, 14.0D, 9.0D, 13.0D, 16.0D));

    public BlockFenceGate(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(OPEN, Boolean.valueOf(false)).set(POWERED, Boolean.valueOf(false)).set(IN_WALL, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (state.get(IN_WALL)) {
            return state.get(FACING).getAxis() == EnumDirection.EnumAxis.X ? X_SHAPE_LOW : Z_SHAPE_LOW;
        } else {
            return state.get(FACING).getAxis() == EnumDirection.EnumAxis.X ? X_SHAPE : Z_SHAPE;
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        EnumDirection.EnumAxis axis = direction.getAxis();
        if (state.get(FACING).getClockWise().getAxis() != axis) {
            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        } else {
            boolean bl = this.isWall(neighborState) || this.isWall(world.getType(pos.relative(direction.opposite())));
            return state.set(IN_WALL, Boolean.valueOf(bl));
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (state.get(OPEN)) {
            return VoxelShapes.empty();
        } else {
            return state.get(FACING).getAxis() == EnumDirection.EnumAxis.Z ? Z_COLLISION_SHAPE : X_COLLISION_SHAPE;
        }
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        if (state.get(IN_WALL)) {
            return state.get(FACING).getAxis() == EnumDirection.EnumAxis.X ? X_OCCLUSION_SHAPE_LOW : Z_OCCLUSION_SHAPE_LOW;
        } else {
            return state.get(FACING).getAxis() == EnumDirection.EnumAxis.X ? X_OCCLUSION_SHAPE : Z_OCCLUSION_SHAPE;
        }
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        switch(type) {
        case LAND:
            return state.get(OPEN);
        case WATER:
            return false;
        case AIR:
            return state.get(OPEN);
        default:
            return false;
        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        World level = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        boolean bl = level.isBlockIndirectlyPowered(blockPos);
        EnumDirection direction = ctx.getHorizontalDirection();
        EnumDirection.EnumAxis axis = direction.getAxis();
        boolean bl2 = axis == EnumDirection.EnumAxis.Z && (this.isWall(level.getType(blockPos.west())) || this.isWall(level.getType(blockPos.east()))) || axis == EnumDirection.EnumAxis.X && (this.isWall(level.getType(blockPos.north())) || this.isWall(level.getType(blockPos.south())));
        return this.getBlockData().set(FACING, direction).set(OPEN, Boolean.valueOf(bl)).set(POWERED, Boolean.valueOf(bl)).set(IN_WALL, Boolean.valueOf(bl2));
    }

    private boolean isWall(IBlockData state) {
        return state.is(TagsBlock.WALLS);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (state.get(OPEN)) {
            state = state.set(OPEN, Boolean.valueOf(false));
            world.setTypeAndData(pos, state, 10);
        } else {
            EnumDirection direction = player.getDirection();
            if (state.get(FACING) == direction.opposite()) {
                state = state.set(FACING, direction);
            }

            state = state.set(OPEN, Boolean.valueOf(true));
            world.setTypeAndData(pos, state, 10);
        }

        boolean bl = state.get(OPEN);
        world.triggerEffect(player, bl ? 1008 : 1014, pos, 0);
        world.gameEvent(player, bl ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide) {
            boolean bl = world.isBlockIndirectlyPowered(pos);
            if (state.get(POWERED) != bl) {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl)).set(OPEN, Boolean.valueOf(bl)), 2);
                if (state.get(OPEN) != bl) {
                    world.triggerEffect((EntityHuman)null, bl ? 1008 : 1014, pos, 0);
                    world.gameEvent(bl ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
                }
            }

        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, OPEN, POWERED, IN_WALL);
    }

    public static boolean connectsToDirection(IBlockData state, EnumDirection side) {
        return state.get(FACING).getAxis() == side.getClockWise().getAxis();
    }
}
