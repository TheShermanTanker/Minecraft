package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBed extends TileEntity {
    public EnumColor color;

    public TileEntityBed(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BED, pos, state);
        this.color = ((BlockBed)state.getBlock()).getColor();
    }

    public TileEntityBed(BlockPosition pos, IBlockData state, EnumColor color) {
        super(TileEntityTypes.BED, pos, state);
        this.color = color;
    }

    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 11, this.getUpdateTag());
    }

    public EnumColor getColor() {
        return this.color;
    }

    public void setColor(EnumColor color) {
        this.color = color;
    }
}
