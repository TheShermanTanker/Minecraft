package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentWaterWorker extends Enchantment {
    public EnchantmentWaterWorker(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.ARMOR_HEAD, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 1;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 40;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
}
