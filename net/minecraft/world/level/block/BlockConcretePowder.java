package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockConcretePowder extends BlockFalling {
    private final IBlockData concrete;

    public BlockConcretePowder(Block hardened, BlockBase.Info settings) {
        super(settings);
        this.concrete = hardened.getBlockData();
    }

    @Override
    public void onLand(World world, BlockPosition pos, IBlockData fallingBlockState, IBlockData currentStateInPos, EntityFallingBlock fallingBlockEntity) {
        if (canHarden(world, pos, currentStateInPos)) {
            world.setTypeAndData(pos, this.concrete, 3);
        }

    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockAccess blockGetter = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        IBlockData blockState = blockGetter.getType(blockPos);
        return canHarden(blockGetter, blockPos, blockState) ? this.concrete : super.getPlacedState(ctx);
    }

    private static boolean canHarden(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return canSolidify(state) || touchesLiquid(world, pos);
    }

    private static boolean touchesLiquid(IBlockAccess world, BlockPosition pos) {
        boolean bl = false;
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(EnumDirection direction : EnumDirection.values()) {
            IBlockData blockState = world.getType(mutableBlockPos);
            if (direction != EnumDirection.DOWN || canSolidify(blockState)) {
                mutableBlockPos.setWithOffset(pos, direction);
                blockState = world.getType(mutableBlockPos);
                if (canSolidify(blockState) && !blockState.isFaceSturdy(world, pos, direction.opposite())) {
                    bl = true;
                    break;
                }
            }
        }

        return bl;
    }

    private static boolean canSolidify(IBlockData state) {
        return state.getFluid().is(TagsFluid.WATER);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return touchesLiquid(world, pos) ? this.concrete : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public int getDustColor(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return state.getMapColor(world, pos).col;
    }
}
