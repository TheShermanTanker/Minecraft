package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;

public class BlockDirtSnow extends Block {
    public static final BlockStateBoolean SNOWY = BlockProperties.SNOWY;

    protected BlockDirtSnow(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(SNOWY, Boolean.valueOf(false)));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.UP ? state.set(SNOWY, Boolean.valueOf(isSnowySetting(neighborState))) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().above());
        return this.getBlockData().set(SNOWY, Boolean.valueOf(isSnowySetting(blockState)));
    }

    private static boolean isSnowySetting(IBlockData state) {
        return state.is(TagsBlock.SNOW);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(SNOWY);
    }
}
