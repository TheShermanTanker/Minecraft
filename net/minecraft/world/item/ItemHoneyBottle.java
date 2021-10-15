package net.minecraft.world.item;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemHoneyBottle extends Item {
    private static final int DRINK_DURATION = 40;

    public ItemHoneyBottle(Item.Info settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        super.finishUsingItem(stack, world, user);
        if (user instanceof EntityPlayer) {
            EntityPlayer serverPlayer = (EntityPlayer)user;
            CriterionTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(StatisticList.ITEM_USED.get(this));
        }

        if (!world.isClientSide) {
            user.removeEffect(MobEffects.POISON);
        }

        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        } else {
            if (user instanceof EntityHuman && !((EntityHuman)user).getAbilities().instabuild) {
                ItemStack itemStack = new ItemStack(Items.GLASS_BOTTLE);
                EntityHuman player = (EntityHuman)user;
                if (!player.getInventory().pickup(itemStack)) {
                    player.drop(itemStack, false);
                }
            }

            return stack;
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.DRINK;
    }

    @Override
    public SoundEffect getDrinkingSound() {
        return SoundEffects.HONEY_DRINK;
    }

    @Override
    public SoundEffect getEatingSound() {
        return SoundEffects.HONEY_DRINK;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        return ItemLiquidUtil.startUsingInstantly(world, user, hand);
    }
}
