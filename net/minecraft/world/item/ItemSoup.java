package net.minecraft.world.item;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemSoup extends Item {
    public ItemSoup(Item.Info settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        ItemStack itemStack = super.finishUsingItem(stack, world, user);
        return user instanceof EntityHuman && ((EntityHuman)user).getAbilities().instabuild ? itemStack : new ItemStack(Items.BOWL);
    }
}
