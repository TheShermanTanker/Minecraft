package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerSmoker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntitySmoker extends TileEntityFurnace {
    public TileEntitySmoker(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.SMOKER, pos, state, Recipes.SMOKING);
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.smoker");
    }

    @Override
    protected int fuelTime(ItemStack fuel) {
        return super.fuelTime(fuel) / 2;
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerSmoker(syncId, playerInventory, this, this.dataAccess);
    }
}
