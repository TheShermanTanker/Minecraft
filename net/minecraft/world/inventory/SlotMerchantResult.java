package net.minecraft.world.inventory;

import net.minecraft.stats.StatisticList;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.IMerchant;
import net.minecraft.world.item.trading.MerchantRecipe;

public class SlotMerchantResult extends Slot {
    private final InventoryMerchant slots;
    private final EntityHuman player;
    private int removeCount;
    private final IMerchant merchant;

    public SlotMerchantResult(EntityHuman player, IMerchant merchant, InventoryMerchant merchantInventory, int index, int x, int y) {
        super(merchantInventory, index, x, y);
        this.player = player;
        this.merchant = merchant;
        this.slots = merchantInventory;
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
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        stack.onCraftedBy(this.player.level, this.player, this.removeCount);
        this.removeCount = 0;
    }

    @Override
    public void onTake(EntityHuman player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        MerchantRecipe merchantOffer = this.slots.getRecipe();
        if (merchantOffer != null) {
            ItemStack itemStack = this.slots.getItem(0);
            ItemStack itemStack2 = this.slots.getItem(1);
            if (merchantOffer.take(itemStack, itemStack2) || merchantOffer.take(itemStack2, itemStack)) {
                this.merchant.notifyTrade(merchantOffer);
                player.awardStat(StatisticList.TRADED_WITH_VILLAGER);
                this.slots.setItem(0, itemStack);
                this.slots.setItem(1, itemStack2);
            }

            this.merchant.setForcedExperience(this.merchant.getExperience() + merchantOffer.getXp());
        }

    }
}
