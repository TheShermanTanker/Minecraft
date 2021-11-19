package net.minecraft.world.item;

import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityEnderPearl;
import net.minecraft.world.level.World;

public class ItemEnderPearl extends Item {
    public ItemEnderPearl(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.ENDER_PEARL_THROW, EnumSoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        user.getCooldownTracker().setCooldown(this, 20);
        if (!world.isClientSide) {
            EntityEnderPearl thrownEnderpearl = new EntityEnderPearl(world, user);
            thrownEnderpearl.setItem(itemStack);
            thrownEnderpearl.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.5F, 1.0F);
            world.addEntity(thrownEnderpearl);
        }

        user.awardStat(StatisticList.ITEM_USED.get(this));
        if (!user.getAbilities().instabuild) {
            itemStack.subtract(1);
        }

        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }
}
