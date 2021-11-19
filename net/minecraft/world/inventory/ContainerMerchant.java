package net.minecraft.world.inventory;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.MerchantWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.IMerchant;
import net.minecraft.world.item.trading.MerchantRecipeList;

public class ContainerMerchant extends Container {
    protected static final int PAYMENT1_SLOT = 0;
    protected static final int PAYMENT2_SLOT = 1;
    protected static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private static final int SELLSLOT1_X = 136;
    private static final int SELLSLOT2_X = 162;
    private static final int BUYSLOT_X = 220;
    private static final int ROW_Y = 37;
    private final IMerchant trader;
    private final InventoryMerchant tradeContainer;
    private int merchantLevel;
    private boolean showProgressBar;
    private boolean canRestock;

    public ContainerMerchant(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new MerchantWrapper(playerInventory.player));
    }

    public ContainerMerchant(int syncId, PlayerInventory playerInventory, IMerchant merchant) {
        super(Containers.MERCHANT, syncId);
        this.trader = merchant;
        this.tradeContainer = new InventoryMerchant(merchant);
        this.addSlot(new Slot(this.tradeContainer, 0, 136, 37));
        this.addSlot(new Slot(this.tradeContainer, 1, 162, 37));
        this.addSlot(new SlotMerchantResult(playerInventory.player, merchant, this.tradeContainer, 2, 220, 37));

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 108 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 108 + k * 18, 142));
        }

    }

    public void setShowProgressBar(boolean canLevel) {
        this.showProgressBar = canLevel;
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        this.tradeContainer.updateSellItem();
        super.slotsChanged(inventory);
    }

    public void setSelectionHint(int index) {
        this.tradeContainer.setSelectionHint(index);
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return this.trader.getTrader() == player;
    }

    public int getTraderXp() {
        return this.trader.getExperience();
    }

    public int getFutureTraderXp() {
        return this.tradeContainer.getFutureXp();
    }

    public void setXp(int experience) {
        this.trader.setForcedExperience(experience);
    }

    public int getTraderLevel() {
        return this.merchantLevel;
    }

    public void setMerchantLevel(int progress) {
        this.merchantLevel = progress;
    }

    public void setCanRestock(boolean refreshable) {
        this.canRestock = refreshable;
    }

    public boolean canRestock() {
        return this.canRestock;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
                this.playTradeSound();
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 30) {
                    if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    private void playTradeSound() {
        if (!this.trader.getWorld().isClientSide) {
            Entity entity = (Entity)this.trader;
            this.trader.getWorld().playLocalSound(entity.locX(), entity.locY(), entity.locZ(), this.trader.getTradeSound(), EnumSoundCategory.NEUTRAL, 1.0F, 1.0F, false);
        }

    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.trader.setTradingPlayer((EntityHuman)null);
        if (!this.trader.getWorld().isClientSide) {
            if (!player.isAlive() || player instanceof EntityPlayer && ((EntityPlayer)player).hasDisconnected()) {
                ItemStack itemStack = this.tradeContainer.splitWithoutUpdate(0);
                if (!itemStack.isEmpty()) {
                    player.drop(itemStack, false);
                }

                itemStack = this.tradeContainer.splitWithoutUpdate(1);
                if (!itemStack.isEmpty()) {
                    player.drop(itemStack, false);
                }
            } else if (player instanceof EntityPlayer) {
                player.getInventory().placeItemBackInInventory(this.tradeContainer.splitWithoutUpdate(0));
                player.getInventory().placeItemBackInInventory(this.tradeContainer.splitWithoutUpdate(1));
            }

        }
    }

    public void tryMoveItems(int recipeIndex) {
        if (this.getOffers().size() > recipeIndex) {
            ItemStack itemStack = this.tradeContainer.getItem(0);
            if (!itemStack.isEmpty()) {
                if (!this.moveItemStackTo(itemStack, 3, 39, true)) {
                    return;
                }

                this.tradeContainer.setItem(0, itemStack);
            }

            ItemStack itemStack2 = this.tradeContainer.getItem(1);
            if (!itemStack2.isEmpty()) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return;
                }

                this.tradeContainer.setItem(1, itemStack2);
            }

            if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
                ItemStack itemStack3 = this.getOffers().get(recipeIndex).getBuyItem1();
                this.moveFromInventoryToPaymentSlot(0, itemStack3);
                ItemStack itemStack4 = this.getOffers().get(recipeIndex).getBuyItem2();
                this.moveFromInventoryToPaymentSlot(1, itemStack4);
            }

        }
    }

    private void moveFromInventoryToPaymentSlot(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            for(int i = 3; i < 39; ++i) {
                ItemStack itemStack = this.slots.get(i).getItem();
                if (!itemStack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemStack)) {
                    ItemStack itemStack2 = this.tradeContainer.getItem(slot);
                    int j = itemStack2.isEmpty() ? 0 : itemStack2.getCount();
                    int k = Math.min(stack.getMaxStackSize() - j, itemStack.getCount());
                    ItemStack itemStack3 = itemStack.cloneItemStack();
                    int l = j + k;
                    itemStack.subtract(k);
                    itemStack3.setCount(l);
                    this.tradeContainer.setItem(slot, itemStack3);
                    if (l >= stack.getMaxStackSize()) {
                        break;
                    }
                }
            }
        }

    }

    public void setOffers(MerchantRecipeList offers) {
        this.trader.overrideOffers(offers);
    }

    public MerchantRecipeList getOffers() {
        return this.trader.getOffers();
    }

    public boolean showProgressBar() {
        return this.showProgressBar;
    }
}
