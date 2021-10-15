package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.EntityHanging;
import net.minecraft.world.entity.player.EntityHuman;

public class ItemItemFrame extends ItemHanging {
    public ItemItemFrame(EntityTypes<? extends EntityHanging> type, Item.Info settings) {
        super(type, settings);
    }

    @Override
    protected boolean mayPlace(EntityHuman player, EnumDirection side, ItemStack stack, BlockPosition pos) {
        return !player.level.isOutsideWorld(pos) && player.mayUseItemAt(pos, side, stack);
    }
}
