package net.minecraft.world.item;

public class ItemNetherStar extends Item {
    public ItemNetherStar(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
