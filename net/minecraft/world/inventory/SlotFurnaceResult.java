package net.minecraft.world.inventory;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntityFurnace;

public class SlotFurnaceResult extends Slot {
    private final EntityHuman player;
    private int removeCount;

    public SlotFurnaceResult(EntityHuman player, IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.player = player;
    }

    @Override
    public boolean isAllowed(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }

        return super.remove(amount);
    }

    @Override
    public void onTake(EntityHuman player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        super.onTake(player, stack);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        stack.onCraftedBy(this.player.level, this.player, this.removeCount);
        if (this.player instanceof EntityPlayer && this.container instanceof TileEntityFurnace) {
            ((TileEntityFurnace)this.container).awardUsedRecipesAndPopExperience((EntityPlayer)this.player);
        }

        this.removeCount = 0;
    }
}
