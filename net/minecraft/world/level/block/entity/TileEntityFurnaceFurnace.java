package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerFurnaceFurnace;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityFurnaceFurnace extends TileEntityFurnace {
    public TileEntityFurnaceFurnace(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.FURNACE, pos, state, Recipes.SMELTING);
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.furnace");
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerFurnaceFurnace(syncId, playerInventory, this, this.dataAccess);
    }
}
