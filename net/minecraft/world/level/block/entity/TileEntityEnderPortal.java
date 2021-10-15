package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityEnderPortal extends TileEntity {
    protected TileEntityEnderPortal(TileEntityTypes<?> type, BlockPosition pos, IBlockData state) {
        super(type, pos, state);
    }

    public TileEntityEnderPortal(BlockPosition pos, IBlockData state) {
        this(TileEntityTypes.END_PORTAL, pos, state);
    }

    public boolean shouldRenderFace(EnumDirection direction) {
        return direction.getAxis() == EnumDirection.EnumAxis.Y;
    }
}
