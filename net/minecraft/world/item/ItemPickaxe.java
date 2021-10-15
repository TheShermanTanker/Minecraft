package net.minecraft.world.item;

import net.minecraft.tags.TagsBlock;

public class ItemPickaxe extends ItemTool {
    protected ItemPickaxe(ToolMaterial material, int attackDamage, float attackSpeed, Item.Info settings) {
        super((float)attackDamage, attackSpeed, material, TagsBlock.MINEABLE_WITH_PICKAXE, settings);
    }
}
