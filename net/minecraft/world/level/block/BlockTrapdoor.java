package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
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
import net.minecraft.world.level.block.state.properties.BlockPropertyHalf;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockTrapdoor extends BlockFacingHorizontal implements IBlockWaterlogged {
    public static final BlockStateBoolean OPEN = BlockProperties.OPEN;
    public static final BlockStateEnum<BlockPropertyHalf> HALF = BlockProperties.HALF;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final int AABB_THICKNESS = 3;
    protected static final VoxelShape EAST_OPEN_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_OPEN_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_OPEN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_OPEN_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape BOTTOM_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
    protected static final VoxelShape TOP_AABB = Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    protected BlockTrapdoor(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(OPEN, Boolean.valueOf(false)).set(HALF, BlockPropertyHalf.BOTTOM).set(POWERED, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (!state.get(OPEN)) {
            return state.get(HALF) == BlockPropertyHalf.TOP ? TOP_AABB : BOTTOM_AABB;
        } else {
            switch((EnumDirection)state.get(FACING)) {
            case NORTH:
            default:
                return NORTH_OPEN_AABB;
            case SOUTH:
                return SOUTH_OPEN_AABB;
            case WEST:
                return WEST_OPEN_AABB;
            case EAST:
                return EAST_OPEN_AABB;
            }
        }
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        switch(type) {
        case LAND:
            return state.get(OPEN);
        case WATER:
            return state.get(WATERLOGGED);
        case AIR:
            return state.get(OPEN);
        default:
            return false;
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (this.material == Material.METAL) {
            return EnumInteractionResult.PASS;
        } else {
            state = state.cycle(OPEN);
            world.setTypeAndData(pos, state, 2);
            if (state.get(WATERLOGGED)) {
                world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            this.playSound(player, world, pos, state.get(OPEN));
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    protected void playSound(@Nullable EntityHuman player, World world, BlockPosition pos, boolean open) {
        if (open) {
            int i = this.material == Material.METAL ? 1037 : 1007;
            world.levelEvent(player, i, pos, 0);
        } else {
            int j = this.material == Material.METAL ? 1036 : 1013;
            world.levelEvent(player, j, pos, 0);
        }

        world.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide) {
            boolean bl = world.isBlockIndirectlyPowered(pos);
            if (bl != state.get(POWERED)) {
                if (state.get(OPEN) != bl) {
                    state = state.set(OPEN, Boolean.valueOf(bl));
                    this.playSound((EntityHuman)null, world, pos, bl);
                }

                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl)), 2);
                if (state.get(WATERLOGGED)) {
                    world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
                }
            }

        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = this.getBlockData();
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        EnumDirection direction = ctx.getClickedFace();
        if (!ctx.replacingClickedOnBlock() && direction.getAxis().isHorizontal()) {
            blockState = blockState.set(FACING, direction).set(HALF, ctx.getPos().y - (double)ctx.getClickPosition().getY() > 0.5D ? BlockPropertyHalf.TOP : BlockPropertyHalf.BOTTOM);
        } else {
            blockState = blockState.set(FACING, ctx.getHorizontalDirection().opposite()).set(HALF, direction == EnumDirection.UP ? BlockPropertyHalf.BOTTOM : BlockPropertyHalf.TOP);
        }

        if (ctx.getWorld().isBlockIndirectlyPowered(ctx.getClickPosition())) {
            blockState = blockState.set(OPEN, Boolean.valueOf(true)).set(POWERED, Boolean.valueOf(true));
        }

        return blockState.set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, OPEN, HALF, POWERED, WATERLOGGED);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }
}
