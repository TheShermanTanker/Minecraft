package net.minecraft.world.item.trading;

import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.item.ItemStack;

public class MerchantRecipeList extends ArrayList<MerchantRecipe> {
    public MerchantRecipeList() {
    }

    public MerchantRecipeList(NBTTagCompound nbt) {
        NBTTagList listTag = nbt.getList("Recipes", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            this.add(new MerchantRecipe(listTag.getCompound(i)));
        }

    }

    @Nullable
    public MerchantRecipe getRecipeFor(ItemStack firstBuyItem, ItemStack secondBuyItem, int index) {
        if (index > 0 && index < this.size()) {
            MerchantRecipe merchantOffer = this.get(index);
            return merchantOffer.satisfiedBy(firstBuyItem, secondBuyItem) ? merchantOffer : null;
        } else {
            for(int i = 0; i < this.size(); ++i) {
                MerchantRecipe merchantOffer2 = this.get(i);
                if (merchantOffer2.satisfiedBy(firstBuyItem, secondBuyItem)) {
                    return merchantOffer2;
                }
            }

            return null;
        }
    }

    public void writeToStream(PacketDataSerializer buf) {
        buf.writeByte((byte)(this.size() & 255));

        for(int i = 0; i < this.size(); ++i) {
            MerchantRecipe merchantOffer = this.get(i);
            buf.writeItem(merchantOffer.getBaseCostA());
            buf.writeItem(merchantOffer.getSellingItem());
            ItemStack itemStack = merchantOffer.getBuyItem2();
            buf.writeBoolean(!itemStack.isEmpty());
            if (!itemStack.isEmpty()) {
                buf.writeItem(itemStack);
            }

            buf.writeBoolean(merchantOffer.isFullyUsed());
            buf.writeInt(merchantOffer.getUses());
            buf.writeInt(merchantOffer.getMaxUses());
            buf.writeInt(merchantOffer.getXp());
            buf.writeInt(merchantOffer.getSpecialPrice());
            buf.writeFloat(merchantOffer.getPriceMultiplier());
            buf.writeInt(merchantOffer.getDemand());
        }

    }

    public static MerchantRecipeList createFromStream(PacketDataSerializer buf) {
        MerchantRecipeList merchantOffers = new MerchantRecipeList();
        int i = buf.readByte() & 255;

        for(int j = 0; j < i; ++j) {
            ItemStack itemStack = buf.readItem();
            ItemStack itemStack2 = buf.readItem();
            ItemStack itemStack3 = ItemStack.EMPTY;
            if (buf.readBoolean()) {
                itemStack3 = buf.readItem();
            }

            boolean bl = buf.readBoolean();
            int k = buf.readInt();
            int l = buf.readInt();
            int m = buf.readInt();
            int n = buf.readInt();
            float f = buf.readFloat();
            int o = buf.readInt();
            MerchantRecipe merchantOffer = new MerchantRecipe(itemStack, itemStack3, itemStack2, k, l, m, f, o);
            if (bl) {
                merchantOffer.setToOutOfStock();
            }

            merchantOffer.setSpecialPrice(n);
            merchantOffers.add(merchantOffer);
        }

        return merchantOffers;
    }

    public NBTTagCompound createTag() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        NBTTagList listTag = new NBTTagList();

        for(int i = 0; i < this.size(); ++i) {
            MerchantRecipe merchantOffer = this.get(i);
            listTag.add(merchantOffer.createTag());
        }

        compoundTag.set("Recipes", listTag);
        return compoundTag;
    }
}
