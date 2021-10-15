package net.minecraft.world.item;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemMilkBucket extends Item {
    private static final int DRINK_DURATION = 32;

    public ItemMilkBucket(Item.Info settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        if (user instanceof EntityPlayer) {
            EntityPlayer serverPlayer = (EntityPlayer)user;
            CriterionTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(StatisticList.ITEM_USED.get(this));
        }

        if (user instanceof EntityHuman && !((EntityHuman)user).getAbilities().instabuild) {
            stack.subtract(1);
        }

        if (!world.isClientSide) {
            user.removeAllEffects();
        }

        return stack.isEmpty() ? new ItemStack(Items.BUCKET) : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.DRINK;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        return ItemLiquidUtil.startUsingInstantly(world, user, hand);
    }
}
