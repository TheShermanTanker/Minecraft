package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockSoulFire extends BlockFireAbstract {
    public BlockSoulFire(BlockBase.Info settings) {
        super(settings, 2.0F);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return this.canPlace(state, world, pos) ? this.getBlockData() : Blocks.AIR.getBlockData();
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return canSurviveOnBlock(world.getType(pos.below()));
    }

    public static boolean canSurviveOnBlock(IBlockData state) {
        return state.is(TagsBlock.SOUL_FIRE_BASE_BLOCKS);
    }

    @Override
    protected boolean canBurn(IBlockData state) {
        return true;
    }
}
