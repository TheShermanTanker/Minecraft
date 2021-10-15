package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnchantmentDigging extends Enchantment {
    protected EnchantmentDigging(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.DIGGER, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 1 + 10 * (level - 1);
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.is(Items.SHEARS) ? true : super.canEnchant(stack);
    }
}
