package net.minecraft.world.item;

import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.level.World;

public class ItemPotionThrowable extends ItemPotion {
    public ItemPotionThrowable(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (!world.isClientSide) {
            EntityPotion thrownPotion = new EntityPotion(world, user);
            thrownPotion.setItem(itemStack);
            thrownPotion.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
            world.addEntity(thrownPotion);
        }

        user.awardStat(StatisticList.ITEM_USED.get(this));
        if (!user.getAbilities().instabuild) {
            itemStack.subtract(1);
        }

        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }
}
