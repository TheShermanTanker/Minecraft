package net.minecraft.world.item.enchantment;

import java.util.Random;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemStack;

public class EnchantmentDurability extends Enchantment {
    protected EnchantmentDurability(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.BREAKABLE, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.isDamageableItem() ? true : super.canEnchant(stack);
    }

    public static boolean shouldIgnoreDurabilityDrop(ItemStack item, int level, Random random) {
        if (item.getItem() instanceof ItemArmor && random.nextFloat() < 0.6F) {
            return false;
        } else {
            return random.nextInt(level + 1) > 0;
        }
    }
}
