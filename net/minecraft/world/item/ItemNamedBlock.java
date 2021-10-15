package net.minecraft.world.item;

import net.minecraft.world.level.block.Block;

public class ItemNamedBlock extends ItemBlock {
    public ItemNamedBlock(Block block, Item.Info settings) {
        super(block, settings);
    }

    @Override
    public String getName() {
        return this.getOrCreateDescriptionId();
    }
}
