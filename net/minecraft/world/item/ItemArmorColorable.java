package net.minecraft.world.item;

import net.minecraft.world.entity.EnumItemSlot;

public class ItemArmorColorable extends ItemArmor implements IDyeable {
    public ItemArmorColorable(ArmorMaterial material, EnumItemSlot slot, Item.Info settings) {
        super(material, slot, settings);
    }
}
