package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityDropper extends TileEntityDispenser {
    public TileEntityDropper(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.DROPPER, pos, state);
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.dropper");
    }
}
