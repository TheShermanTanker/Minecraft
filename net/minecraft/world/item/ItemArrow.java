package net.minecraft.world.item;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityTippedArrow;
import net.minecraft.world.level.World;

public class ItemArrow extends Item {
    public ItemArrow(Item.Info settings) {
        super(settings);
    }

    public EntityArrow createArrow(World world, ItemStack stack, EntityLiving shooter) {
        EntityTippedArrow arrow = new EntityTippedArrow(world, shooter);
        arrow.setEffectsFromItem(stack);
        return arrow;
    }
}
