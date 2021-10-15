package net.minecraft.world.level.block;

import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockHalfTransparent extends Block {
    protected BlockHalfTransparent(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean skipRendering(IBlockData state, IBlockData stateFrom, EnumDirection direction) {
        return stateFrom.is(this) ? true : super.skipRendering(state, stateFrom, direction);
    }
}
