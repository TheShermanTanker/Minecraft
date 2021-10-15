package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyAttachPosition;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;

public class BlockAttachable extends BlockFacingHorizontal {
    public static final BlockStateEnum<BlockPropertyAttachPosition> FACE = BlockProperties.ATTACH_FACE;

    protected BlockAttachable(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return canAttach(world, pos, getConnectedDirection(state).opposite());
    }

    public static boolean canAttach(IWorldReader world, BlockPosition pos, EnumDirection direction) {
        BlockPosition blockPos = pos.relative(direction);
        return world.getType(blockPos).isFaceSturdy(world, blockPos, direction.opposite());
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        for(EnumDirection direction : ctx.getNearestLookingDirections()) {
            IBlockData blockState;
            if (direction.getAxis() == EnumDirection.EnumAxis.Y) {
                blockState = this.getBlockData().set(FACE, direction == EnumDirection.UP ? BlockPropertyAttachPosition.CEILING : BlockPropertyAttachPosition.FLOOR).set(FACING, ctx.getHorizontalDirection());
            } else {
                blockState = this.getBlockData().set(FACE, BlockPropertyAttachPosition.WALL).set(FACING, direction.opposite());
            }

            if (blockState.canPlace(ctx.getWorld(), ctx.getClickPosition())) {
                return blockState;
            }
        }

        return null;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return getConnectedDirection(state).opposite() == direction && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    protected static EnumDirection getConnectedDirection(IBlockData state) {
        switch((BlockPropertyAttachPosition)state.get(FACE)) {
        case CEILING:
            return EnumDirection.DOWN;
        case FLOOR:
            return EnumDirection.UP;
        default:
            return state.get(FACING);
        }
    }
}
