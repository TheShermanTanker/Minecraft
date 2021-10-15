package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import net.minecraft.world.level.World;

public class ItemEnchantedBook extends Item {
    public static final String TAG_STORED_ENCHANTMENTS = "StoredEnchantments";

    public ItemEnchantedBook(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    public static NBTTagList getEnchantments(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        return compoundTag != null ? compoundTag.getList("StoredEnchantments", 10) : new NBTTagList();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        ItemStack.appendEnchantmentNames(tooltip, getEnchantments(stack));
    }

    public static void addEnchantment(ItemStack stack, WeightedRandomEnchant entry) {
        NBTTagList listTag = getEnchantments(stack);
        boolean bl = true;
        MinecraftKey resourceLocation = EnchantmentManager.getEnchantmentId(entry.enchantment);

        for(int i = 0; i < listTag.size(); ++i) {
            NBTTagCompound compoundTag = listTag.getCompound(i);
            MinecraftKey resourceLocation2 = EnchantmentManager.getEnchantmentId(compoundTag);
            if (resourceLocation2 != null && resourceLocation2.equals(resourceLocation)) {
                if (EnchantmentManager.getEnchantmentLevel(compoundTag) < entry.level) {
                    EnchantmentManager.setEnchantmentLevel(compoundTag, entry.level);
                }

                bl = false;
                break;
            }
        }

        if (bl) {
            listTag.add(EnchantmentManager.storeEnchantment(resourceLocation, entry.level));
        }

        stack.getOrCreateTag().set("StoredEnchantments", listTag);
    }

    public static ItemStack createForEnchantment(WeightedRandomEnchant info) {
        ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
        addEnchantment(itemStack, info);
        return itemStack;
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (group == CreativeModeTab.TAB_SEARCH) {
            for(Enchantment enchantment : IRegistry.ENCHANTMENT) {
                if (enchantment.category != null) {
                    for(int i = enchantment.getStartLevel(); i <= enchantment.getMaxLevel(); ++i) {
                        stacks.add(createForEnchantment(new WeightedRandomEnchant(enchantment, i)));
                    }
                }
            }
        } else if (group.getEnchantmentCategories().length != 0) {
            for(Enchantment enchantment2 : IRegistry.ENCHANTMENT) {
                if (group.hasEnchantmentCategory(enchantment2.category)) {
                    stacks.add(createForEnchantment(new WeightedRandomEnchant(enchantment2, enchantment2.getMaxLevel())));
                }
            }
        }

    }
}
