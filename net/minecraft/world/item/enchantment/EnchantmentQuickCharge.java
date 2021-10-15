package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentQuickCharge extends Enchantment {
    public EnchantmentQuickCharge(Enchantment.Rarity weight, EnumItemSlot... slot) {
        super(weight, EnchantmentSlotType.CROSSBOW, slot);
    }

    @Override
    public int getMinCost(int level) {
        return 12 + (level - 1) * 20;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
