package net.minecraft.world.level.levelgen;

import net.minecraft.world.level.block.state.IBlockData;

public class SingleBaseStoneSource implements BaseStoneSource {
    private final IBlockData state;

    public SingleBaseStoneSource(IBlockData state) {
        this.state = state;
    }

    @Override
    public IBlockData getBaseBlock(int x, int y, int z) {
        return this.state;
    }
}
