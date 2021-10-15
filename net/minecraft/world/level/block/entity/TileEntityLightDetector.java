package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityLightDetector extends TileEntity {
    public TileEntityLightDetector(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.DAYLIGHT_DETECTOR, pos, state);
    }
}
