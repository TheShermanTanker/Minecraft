package net.minecraft.world.item;

public class ItemToolMaterial extends Item {
    private final ToolMaterial tier;

    public ItemToolMaterial(ToolMaterial material, Item.Info settings) {
        super(settings.defaultDurability(material.getUses()));
        this.tier = material;
    }

    public ToolMaterial getTier() {
        return this.tier;
    }

    @Override
    public int getEnchantmentValue() {
        return this.tier.getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return this.tier.getRepairIngredient().test(ingredient) || super.isValidRepairItem(stack, ingredient);
    }
}
