package net.minecraft.world.item.trading;

import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.ItemStack;

public class MerchantRecipe {
    public ItemStack baseCostA;
    public ItemStack costB;
    public final ItemStack result;
    public int uses;
    public int maxUses;
    public boolean rewardExp = true;
    private int specialPriceDiff;
    private int demand;
    public float priceMultiplier;
    public int xp = 1;

    public MerchantRecipe(NBTTagCompound nbt) {
        this.baseCostA = ItemStack.of(nbt.getCompound("buy"));
        this.costB = ItemStack.of(nbt.getCompound("buyB"));
        this.result = ItemStack.of(nbt.getCompound("sell"));
        this.uses = nbt.getInt("uses");
        if (nbt.hasKeyOfType("maxUses", 99)) {
            this.maxUses = nbt.getInt("maxUses");
        } else {
            this.maxUses = 4;
        }

        if (nbt.hasKeyOfType("rewardExp", 1)) {
            this.rewardExp = nbt.getBoolean("rewardExp");
        }

        if (nbt.hasKeyOfType("xp", 3)) {
            this.xp = nbt.getInt("xp");
        }

        if (nbt.hasKeyOfType("priceMultiplier", 5)) {
            this.priceMultiplier = nbt.getFloat("priceMultiplier");
        }

        this.specialPriceDiff = nbt.getInt("specialPrice");
        this.demand = nbt.getInt("demand");
    }

    public MerchantRecipe(ItemStack buyItem, ItemStack sellItem, int maxUses, int merchantExperience, float priceMultiplier) {
        this(buyItem, ItemStack.EMPTY, sellItem, maxUses, merchantExperience, priceMultiplier);
    }

    public MerchantRecipe(ItemStack firstBuyItem, ItemStack secondBuyItem, ItemStack sellItem, int maxUses, int merchantExperience, float priceMultiplier) {
        this(firstBuyItem, secondBuyItem, sellItem, 0, maxUses, merchantExperience, priceMultiplier);
    }

    public MerchantRecipe(ItemStack firstBuyItem, ItemStack secondBuyItem, ItemStack sellItem, int uses, int maxUses, int merchantExperience, float priceMultiplier) {
        this(firstBuyItem, secondBuyItem, sellItem, uses, maxUses, merchantExperience, priceMultiplier, 0);
    }

    public MerchantRecipe(ItemStack firstBuyItem, ItemStack secondBuyItem, ItemStack sellItem, int uses, int maxUses, int merchantExperience, float priceMultiplier, int demandBonus) {
        this.baseCostA = firstBuyItem;
        this.costB = secondBuyItem;
        this.result = sellItem;
        this.uses = uses;
        this.maxUses = maxUses;
        this.xp = merchantExperience;
        this.priceMultiplier = priceMultiplier;
        this.demand = demandBonus;
    }

    public ItemStack getBaseCostA() {
        return this.baseCostA;
    }

    public ItemStack getBuyItem1() {
        int i = this.baseCostA.getCount();
        ItemStack itemStack = this.baseCostA.cloneItemStack();
        int j = Math.max(0, MathHelper.floor((float)(i * this.demand) * this.priceMultiplier));
        itemStack.setCount(MathHelper.clamp(i + j + this.specialPriceDiff, 1, this.baseCostA.getItem().getMaxStackSize()));
        return itemStack;
    }

    public ItemStack getBuyItem2() {
        return this.costB;
    }

    public ItemStack getSellingItem() {
        return this.result;
    }

    public void updateDemand() {
        this.demand = this.demand + this.uses - (this.maxUses - this.uses);
    }

    public ItemStack assemble() {
        return this.result.cloneItemStack();
    }

    public int getUses() {
        return this.uses;
    }

    public void resetUses() {
        this.uses = 0;
    }

    public int getMaxUses() {
        return this.maxUses;
    }

    public void increaseUses() {
        ++this.uses;
    }

    public int getDemand() {
        return this.demand;
    }

    public void increaseSpecialPrice(int increment) {
        this.specialPriceDiff += increment;
    }

    public void setSpecialPrice() {
        this.specialPriceDiff = 0;
    }

    public int getSpecialPrice() {
        return this.specialPriceDiff;
    }

    public void setSpecialPrice(int specialPrice) {
        this.specialPriceDiff = specialPrice;
    }

    public float getPriceMultiplier() {
        return this.priceMultiplier;
    }

    public int getXp() {
        return this.xp;
    }

    public boolean isFullyUsed() {
        return this.uses >= this.maxUses;
    }

    public void setToOutOfStock() {
        this.uses = this.maxUses;
    }

    public boolean needsRestock() {
        return this.uses > 0;
    }

    public boolean isRewardExp() {
        return this.rewardExp;
    }

    public NBTTagCompound createTag() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.set("buy", this.baseCostA.save(new NBTTagCompound()));
        compoundTag.set("sell", this.result.save(new NBTTagCompound()));
        compoundTag.set("buyB", this.costB.save(new NBTTagCompound()));
        compoundTag.setInt("uses", this.uses);
        compoundTag.setInt("maxUses", this.maxUses);
        compoundTag.setBoolean("rewardExp", this.rewardExp);
        compoundTag.setInt("xp", this.xp);
        compoundTag.setFloat("priceMultiplier", this.priceMultiplier);
        compoundTag.setInt("specialPrice", this.specialPriceDiff);
        compoundTag.setInt("demand", this.demand);
        return compoundTag;
    }

    public boolean satisfiedBy(ItemStack first, ItemStack second) {
        return this.isRequiredItem(first, this.getBuyItem1()) && first.getCount() >= this.getBuyItem1().getCount() && this.isRequiredItem(second, this.costB) && second.getCount() >= this.costB.getCount();
    }

    private boolean isRequiredItem(ItemStack given, ItemStack sample) {
        if (sample.isEmpty() && given.isEmpty()) {
            return true;
        } else {
            ItemStack itemStack = given.cloneItemStack();
            if (itemStack.getItem().usesDurability()) {
                itemStack.setDamage(itemStack.getDamage());
            }

            return ItemStack.isSame(itemStack, sample) && (!sample.hasTag() || itemStack.hasTag() && GameProfileSerializer.compareNbt(sample.getTag(), itemStack.getTag(), false));
        }
    }

    public boolean take(ItemStack firstBuyStack, ItemStack secondBuyStack) {
        if (!this.satisfiedBy(firstBuyStack, secondBuyStack)) {
            return false;
        } else {
            firstBuyStack.subtract(this.getBuyItem1().getCount());
            if (!this.getBuyItem2().isEmpty()) {
                secondBuyStack.subtract(this.getBuyItem2().getCount());
            }

            return true;
        }
    }
}
