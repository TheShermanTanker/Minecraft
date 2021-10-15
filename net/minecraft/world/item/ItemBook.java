package net.minecraft.world.item;

public class ItemBook extends Item {
    public ItemBook(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
