package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockSand extends BlockFalling {
    private final int dustColor;

    public BlockSand(int color, BlockBase.Info settings) {
        super(settings);
        this.dustColor = color;
    }

    @Override
    public int getDustColor(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return this.dustColor;
    }
}
