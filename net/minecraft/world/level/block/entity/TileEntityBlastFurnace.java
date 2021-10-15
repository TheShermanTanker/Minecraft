package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerBlastFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBlastFurnace extends TileEntityFurnace {
    public TileEntityBlastFurnace(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BLAST_FURNACE, pos, state, Recipes.BLASTING);
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.blast_furnace");
    }

    @Override
    protected int fuelTime(ItemStack fuel) {
        return super.fuelTime(fuel) / 2;
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerBlastFurnace(syncId, playerInventory, this, this.dataAccess);
    }
}
