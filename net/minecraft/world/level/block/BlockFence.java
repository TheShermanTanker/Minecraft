package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemLeash;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockFence extends BlockTall {
    private final VoxelShape[] occlusionByIndex;

    public BlockFence(BlockBase.Info settings) {
        super(2.0F, 2.0F, 16.0F, 16.0F, 24.0F, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)));
        this.occlusionByIndex = this.makeShapes(2.0F, 1.0F, 16.0F, 6.0F, 15.0F);
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return this.occlusionByIndex[this.getAABBIndex(state)];
    }

    @Override
    public VoxelShape getVisualShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getShape(state, world, pos, context);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    public boolean connectsTo(IBlockData state, boolean neighborIsFullSquare, EnumDirection dir) {
        Block block = state.getBlock();
        boolean bl = this.isSameFence(state);
        boolean bl2 = block instanceof BlockFenceGate && BlockFenceGate.connectsToDirection(state, dir);
        return !isExceptionForConnection(state) && neighborIsFullSquare || bl || bl2;
    }

    private boolean isSameFence(IBlockData state) {
        return state.is(TagsBlock.FENCES) && state.is(TagsBlock.WOODEN_FENCES) == this.getBlockData().is(TagsBlock.WOODEN_FENCES);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            ItemStack itemStack = player.getItemInHand(hand);
            return itemStack.is(Items.LEAD) ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
        } else {
            return ItemLeash.bindPlayerMobs(player, world, pos);
        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockAccess blockGetter = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        BlockPosition blockPos2 = blockPos.north();
        BlockPosition blockPos3 = blockPos.east();
        BlockPosition blockPos4 = blockPos.south();
        BlockPosition blockPos5 = blockPos.west();
        IBlockData blockState = blockGetter.getType(blockPos2);
        IBlockData blockState2 = blockGetter.getType(blockPos3);
        IBlockData blockState3 = blockGetter.getType(blockPos4);
        IBlockData blockState4 = blockGetter.getType(blockPos5);
        return super.getPlacedState(ctx).set(NORTH, Boolean.valueOf(this.connectsTo(blockState, blockState.isFaceSturdy(blockGetter, blockPos2, EnumDirection.SOUTH), EnumDirection.SOUTH))).set(EAST, Boolean.valueOf(this.connectsTo(blockState2, blockState2.isFaceSturdy(blockGetter, blockPos3, EnumDirection.WEST), EnumDirection.WEST))).set(SOUTH, Boolean.valueOf(this.connectsTo(blockState3, blockState3.isFaceSturdy(blockGetter, blockPos4, EnumDirection.NORTH), EnumDirection.NORTH))).set(WEST, Boolean.valueOf(this.connectsTo(blockState4, blockState4.isFaceSturdy(blockGetter, blockPos5, EnumDirection.EAST), EnumDirection.EAST))).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction.getAxis().getPlane() == EnumDirection.EnumDirectionLimit.HORIZONTAL ? state.set(PROPERTY_BY_DIRECTION.get(direction), Boolean.valueOf(this.connectsTo(neighborState, neighborState.isFaceSturdy(world, neighborPos, direction.opposite()), direction.opposite()))) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}
