package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityThrownExpBottle;
import net.minecraft.world.level.World;

public class ItemExpBottle extends Item {
    public ItemExpBottle(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.EXPERIENCE_BOTTLE_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            EntityThrownExpBottle thrownExperienceBottle = new EntityThrownExpBottle(world, user);
            thrownExperienceBottle.setItem(itemStack);
            thrownExperienceBottle.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.7F, 1.0F);
            world.addEntity(thrownExperienceBottle);
        }

        user.awardStat(StatisticList.ITEM_USED.get(this));
        if (!user.getAbilities().instabuild) {
            itemStack.subtract(1);
        }

        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }
}
