package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentSoulSpeed extends Enchantment {
    public EnchantmentSoulSpeed(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.ARMOR_FEET, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return level * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 15;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
