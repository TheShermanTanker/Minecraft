package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemSplashPotion extends ItemPotionThrowable {
    public ItemSplashPotion(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.SPLASH_POTION_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        return super.use(world, user, hand);
    }
}
