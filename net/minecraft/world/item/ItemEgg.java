package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityEgg;
import net.minecraft.world.level.World;

public class ItemEgg extends Item {
    public ItemEgg(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.EGG_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            EntityEgg thrownEgg = new EntityEgg(world, user);
            thrownEgg.setItem(itemStack);
            thrownEgg.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.5F, 1.0F);
            world.addEntity(thrownEgg);
        }

        user.awardStat(StatisticList.ITEM_USED.get(this));
        if (!user.getAbilities().instabuild) {
            itemStack.subtract(1);
        }

        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }
}
