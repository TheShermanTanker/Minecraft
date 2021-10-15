package net.minecraft.world.item;

public class ItemGoldenAppleEnchanted extends Item {
    public ItemGoldenAppleEnchanted(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
