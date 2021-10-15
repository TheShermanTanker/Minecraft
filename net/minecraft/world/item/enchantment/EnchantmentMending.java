package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentMending extends Enchantment {
    public EnchantmentMending(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.BREAKABLE, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return level * 25;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 50;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
}
