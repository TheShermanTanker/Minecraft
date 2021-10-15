package net.minecraft.world.item;

import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class SpyglassItem extends Item {
    public static final int USE_DURATION = 1200;
    public static final float ZOOM_FOV_MODIFIER = 0.1F;

    public SpyglassItem(Item.Info settings) {
        super(settings);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 1200;
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.SPYGLASS;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        user.playSound(SoundEffects.SPYGLASS_USE, 1.0F, 1.0F);
        user.awardStat(StatisticList.ITEM_USED.get(this));
        return ItemLiquidUtil.startUsingInstantly(world, user, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        this.stopUsing(user);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, EntityLiving user, int remainingUseTicks) {
        this.stopUsing(user);
    }

    private void stopUsing(EntityLiving user) {
        user.playSound(SoundEffects.SPYGLASS_STOP_USING, 1.0F, 1.0F);
    }
}
