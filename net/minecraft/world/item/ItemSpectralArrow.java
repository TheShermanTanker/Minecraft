package net.minecraft.world.item;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntitySpectralArrow;
import net.minecraft.world.level.World;

public class ItemSpectralArrow extends ItemArrow {
    public ItemSpectralArrow(Item.Info settings) {
        super(settings);
    }

    @Override
    public EntityArrow createArrow(World world, ItemStack stack, EntityLiving shooter) {
        return new EntitySpectralArrow(world, shooter);
    }
}
