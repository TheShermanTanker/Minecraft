package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentOxygen extends Enchantment {
    public EnchantmentOxygen(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.ARMOR_HEAD, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 10 * level;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 30;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
