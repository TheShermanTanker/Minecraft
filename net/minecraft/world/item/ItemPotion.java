package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemPotion extends Item {
    private static final int DRINK_DURATION = 32;

    public ItemPotion(Item.Info settings) {
        super(settings);
    }

    @Override
    public ItemStack createItemStack() {
        return PotionUtil.setPotion(super.createItemStack(), Potions.WATER);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        EntityHuman player = user instanceof EntityHuman ? (EntityHuman)user : null;
        if (player instanceof EntityPlayer) {
            CriterionTriggers.CONSUME_ITEM.trigger((EntityPlayer)player, stack);
        }

        if (!world.isClientSide) {
            for(MobEffect mobEffectInstance : PotionUtil.getEffects(stack)) {
                if (mobEffectInstance.getMobEffect().isInstant()) {
                    mobEffectInstance.getMobEffect().applyInstantEffect(player, player, user, mobEffectInstance.getAmplifier(), 1.0D);
                } else {
                    user.addEffect(new MobEffect(mobEffectInstance));
                }
            }
        }

        if (player != null) {
            player.awardStat(StatisticList.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.subtract(1);
            }
        }

        if (player == null || !player.getAbilities().instabuild) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (player != null) {
                player.getInventory().pickup(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        world.gameEvent(user, GameEvent.DRINKING_FINISH, user.eyeBlockPosition());
        return stack;
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

    @Override
    public String getDescriptionId(ItemStack stack) {
        return PotionUtil.getPotion(stack).getName(this.getName() + ".effect.");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        PotionUtil.addPotionTooltip(stack, tooltip, 1.0F);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !PotionUtil.getEffects(stack).isEmpty();
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(group)) {
            for(PotionRegistry potion : IRegistry.POTION) {
                if (potion != Potions.EMPTY) {
                    stacks.add(PotionUtil.setPotion(new ItemStack(this), potion));
                }
            }
        }

    }
}
